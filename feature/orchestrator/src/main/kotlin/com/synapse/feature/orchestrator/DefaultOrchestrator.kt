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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Production implementation of the Orchestrator.
 * Implements the "Reasoning Loop" (ReAct pattern).
 */
public class DefaultOrchestrator @Inject constructor(
    private val modelProvider: ModelProvider,
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
            
            val generateRequest = GenerateRequest(
                taskProfile = TaskProfile.COMMAND_ROUTING,
                messages = history
            )

            emit(OrchestrationEvent.Thought(sessionId, "Consulting model (${modelProvider.modelId})..."))

            when (val result = modelProvider.generate(generateRequest)) {
                is SynapseResult.Success -> {
                    when (val data = result.data) {
                        is GenerateResult.Success -> {
                            emit(OrchestrationEvent.ResponseChunk(sessionId, data.text))
                            history.add(ChatMessage(MessageRole.ASSISTANT, data.text))
                            isFinished = true
                        }
                        is GenerateResult.ToolCalls -> {
                            history.add(ChatMessage(MessageRole.ASSISTANT, "Calling tools..."))
                            
                            for (call in data.calls) {
                                emit(OrchestrationEvent.ToolCallStarted(sessionId, call.toolName, call.argumentsJson))
                                
                                // 1. Find the tool definition (mocked for now, will come from AppPackRegistry)
                                // 2. Find executor
                                // 3. Execute
                                // For now, we emit a thought about the limitation
                                emit(OrchestrationEvent.Thought(sessionId, "Tool execution logic is being initialized..."))
                                
                                // Placeholder for tool result
                                val toolResult = "Tool result placeholder for ${call.toolName}"
                                emit(OrchestrationEvent.ToolCallFinished(sessionId, call.toolName, toolResult))
                                
                                history.add(ChatMessage(
                                    role = MessageRole.TOOL,
                                    content = toolResult,
                                    toolCallId = call.toolName // Using name as ID for V0
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
            You have access to device tools and connected app packs.
            Always be concise and helpful.
            If you need to use a tool, call it explicitly.
        """.trimIndent()
    }
}
