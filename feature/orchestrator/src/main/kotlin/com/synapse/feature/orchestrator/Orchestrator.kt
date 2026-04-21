package com.synapse.feature.orchestrator

import com.synapse.core.common.SessionId
import com.synapse.core.common.SynapseResult
import kotlinx.coroutines.flow.Flow

/**
 * The brain of Synapse.
 * Coordinates between models, tools, and apps to fulfill user requests.
 */
public interface Orchestrator {
    /**
     * Processes a user request and streams the orchestrated response.
     * This may involve multiple rounds of model reasoning and tool execution.
     */
    public fun process(
        request: String,
        sessionId: SessionId = SessionId.generate()
    ): Flow<OrchestrationEvent>
}

/**
 * Events emitted by the Orchestrator during a processing session.
 * Used to provide the "Thought Trace" to the UI.
 */
public sealed interface OrchestrationEvent {
    public val sessionId: SessionId

    public data class Thought(
        override val sessionId: SessionId,
        val message: String
    ) : OrchestrationEvent

    public data class ToolCallStarted(
        override val sessionId: SessionId,
        val toolName: String,
        val arguments: String
    ) : OrchestrationEvent

    public data class ToolCallFinished(
        override val sessionId: SessionId,
        val toolName: String,
        val result: String
    ) : OrchestrationEvent

    public data class ResponseChunk(
        override val sessionId: SessionId,
        val text: String
    ) : OrchestrationEvent

    public data class Error(
        override val sessionId: SessionId,
        val message: String,
        val throwable: Throwable? = null
    ) : OrchestrationEvent

    public data class Finished(
        override val sessionId: SessionId
    ) : OrchestrationEvent
}
