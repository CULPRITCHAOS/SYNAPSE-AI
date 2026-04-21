package com.synapse.core.model

import com.synapse.core.common.SynapseResult
import kotlinx.serialization.Serializable

/**
 * Canonical runtime contract for any AI model backend in Synapse.
 */
public interface ModelProvider {
    public val providerId: String
    public val modelId: String
    public val capabilities: ProviderCapabilities

    /**
     * Prepares the model for inference.
     * @param config A map of implementation-specific configuration (e.g., "model_path").
     */
    public suspend fun load(config: Map<String, String>): SynapseResult<Unit>

    /**
     * Releases model resources.
     */
    public suspend fun unload(): SynapseResult<Unit>

    public suspend fun generate(request: GenerateRequest): SynapseResult<GenerateResult>
}

@Serializable
public data class ProviderCapabilities(
    val onDevice: Boolean,
    val streaming: Boolean,
    val toolCalling: Boolean,
    val structuredOutput: Boolean,
    val maxContextTokens: Int? = null
)

@Serializable
public enum class TaskProfile {
    CHAT,
    COMMAND_ROUTING,
    PLANNING,
    EXTRACTION,
    SAFETY_CHECK
}

@Serializable
public data class GenerateRequest(
    val taskProfile: TaskProfile,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024
)

@Serializable
public data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val toolCallId: String? = null
)

@Serializable
public enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

@Serializable
public sealed interface GenerateResult {
    @Serializable
    public data class Success(val text: String) : GenerateResult
    
    @Serializable
    public data class ToolCalls(val calls: List<RequestedToolCall>) : GenerateResult
}

@Serializable
public data class RequestedToolCall(
    val toolName: String,
    val argumentsJson: String
)
