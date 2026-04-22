package com.synapse.feature.orchestrator

import com.synapse.core.apppack.AppPack
import com.synapse.core.common.DispatcherProvider
import com.synapse.core.common.SessionId
import com.synapse.core.common.SynapseResult
import com.synapse.core.model.ChatMessage
import com.synapse.core.model.GenerateRequest
import com.synapse.core.model.GenerateResult
import com.synapse.core.model.MessageRole
import com.synapse.core.model.ModelProvider
import com.synapse.core.model.TaskProfile
import com.synapse.core.tool.ToolRegistry
import com.synapse.core.model.RequestedToolCall
import com.synapse.core.model.registry.ModelRegistry
import com.synapse.core.model.router.ModelRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import com.synapse.core.apppack.ToolDefinition
import com.synapse.core.model.SynapseResult
import com.synapse.core.model.GenerateResult
import kotlinx.serialization.json.Json

/**
 * Production implementation of the Orchestrator.
 * Implements the "Reasoning Loop" (ReAct pattern).
 */
public class DefaultOrchestrator @Inject constructor(
    private val modelRouter: ModelRouter,
    private val modelRegistry: ModelRegistry,
    private val toolRegistry: ToolRegistry,
    private val dispatcherProvider: DispatcherProvider
) : Orchestrator {

    override fun process(
        request: String,
        sessionId: SessionId
    ): Flow<OrchestrationEvent> = flow {
        emit(OrchestrationEvent.Thought(sessionId, "Starting orchestration for request: $request"))

        val history = mutableListOf(
            ChatMessage(MessageRole.SYSTEM, createSystemPrompt()),
            ChatMessage(MessageRole.USER, request)
        )

        var isFinished = false
        var loopCount = 0
        val maxLoops = 5 // Safety gate

        while (!isFinished && loopCount < maxLoops) {
            loopCount++

            // 1. Determine which tier we need for this step
            val currentTask = if (loopCount == 1) TaskProfile.COMMAND_ROUTING else TaskProfile.PLANNING
            val tier = modelRouter.route(currentTask)
            
            // 2. Get the best model for that tier
            val modelProvider = modelRegistry.getBestProviderFor(tier)
                ?: throw IllegalStateException("No model provider found for tier $tier")

            val generateRequest = GenerateRequest(
                taskProfile = currentTask,
                messages = history
            )

            emit(OrchestrationEvent.Thought(sessionId, "Consulting ${tier.name} model (${modelProvider.modelId})..."))

            when (val result = modelProvider.generate(generateRequest)) {
                is SynapseResult.Success -> {
                    when (val data = result.data) {
                        is GenerateResult.Success -> {
                            val text = data.text
                            // Logic: If the text contains tool call syntax, parse it.
                            // We use a simple ReAct style: THOUGHT: ... CALL: {json}
                            if (text.contains("CALL:")) {
                                try {
                                    val callJson = text.substringAfter("CALL:").trim()
                                    val call = Json.decodeFromString<RequestedToolCall>(callJson)
                                    
                                    emit(OrchestrationEvent.ToolCallStarted(sessionId, call.toolName, call.argumentsJson))
                                    
                                    val toolDefinition = ToolDefinition(call.toolName, "", emptyList())
                                    val executor = toolRegistry.getExecutorFor(toolDefinition)
                                    
                                    val toolResult = if (executor != null) {
                                        executor.execute(call.argumentsJson).toString()
                                    } else {
                                        "Error: Tool ${call.toolName} not found"
                                    }

                                    emit(OrchestrationEvent.ToolCallFinished(sessionId, call.toolName, toolResult))
                                    
                                    history.add(ChatMessage(MessageRole.ASSISTANT, text))
                                    history.add(ChatMessage(MessageRole.TOOL, toolResult, call.toolName))
                                } catch (e: Exception) {
                                    emit(OrchestrationEvent.Error(sessionId, "Failed to parse tool call: ${e.message}"))
                                    isFinished = true
                                }
                            } else {
                                emit(OrchestrationEvent.ResponseChunk(sessionId, text))
                                history.add(ChatMessage(MessageRole.ASSISTANT, text))
                                isFinished = true
                            }
                        }
                        is GenerateResult.ToolCalls -> {
                            // Native tool calling (if supported by provider)
                            for (call in data.calls) {
                                emit(OrchestrationEvent.ToolCallStarted(sessionId, call.toolName, call.argumentsJson))
                                
                                val toolDefinition = ToolDefinition(call.toolName, "", emptyList())
                                val executor = toolRegistry.getExecutorFor(toolDefinition)
                                
                                val toolResult = executor?.execute(call.argumentsJson)?.toString() ?: "Error: Tool not found"
                                
                                emit(OrchestrationEvent.ToolCallFinished(sessionId, call.toolName, toolResult))
                                
                                history.add(ChatMessage(
                                    role = MessageRole.TOOL,
                                    content = toolResult,
                                    toolCallId = call.toolName
                                ))
                            }
                        }
                    }
                }
                is SynapseResult.Failure -> {
                    emit(OrchestrationEvent.Error(sessionId, result.error.message, result.error.throwable))
                    isFinished = true
                }
            }
        }

        if (loopCount >= maxLoops) {
            emit(OrchestrationEvent.Error(sessionId, "Max reasoning loops reached. Aborting for safety."))
        }

        emit(OrchestrationEvent.Finished(sessionId))
    }.flowOn(dispatcherProvider.io)

    private fun createSystemPrompt(): String {
        return """
            You are Synapse, a local-first Android AI agent.
            You have access to device tools. 
            
            To use a tool, respond exactly with:
            THOUGHT: <your reasoning>
            CALL: {"toolName": "flashlight", "argumentsJson": "{\"action\":\"on\"}"}
            
            Current tools:
            - flashlight: Arguments: {"action": "on" | "off"}
            
            If you have the answer, respond normally.
        """.trimIndent()
    }
}
