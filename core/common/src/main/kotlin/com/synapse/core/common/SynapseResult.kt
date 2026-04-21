package com.synapse.core.common

import kotlinx.serialization.Serializable

/**
 * Canonical Result wrapper for all Synapse operations.
 * Forces explicit handling of success and failure states.
 */
public sealed interface SynapseResult<out T> {
    public data class Success<out T>(val data: T) : SynapseResult<T>
    public data class Failure(val error: SynapseError) : SynapseResult<Nothing>

    public companion object {
        public fun <T> success(data: T): SynapseResult<T> = Success(data)
        public fun failure(error: SynapseError): SynapseResult<Nothing> = Failure(error)
    }
}

/**
 * Hierarchical error type for Synapse.
 * Categorizes errors to drive orchestration decisions (e.g., retries vs. downgrades).
 */
@Serializable
public sealed class SynapseError {
    public abstract val message: String
    public abstract val throwable: Throwable?

    @Serializable
    public data class ModelError(
        override val message: String,
        val code: String,
        override val throwable: Throwable? = null
    ) : SynapseError()

    @Serializable
    public data class ToolError(
        override val message: String,
        val toolName: String,
        override val throwable: Throwable? = null
    ) : SynapseError()

    @Serializable
    public data class SecurityError(
        override val message: String,
        override val throwable: Throwable? = null
    ) : SynapseError()

    @Serializable
    public data class InfrastructureError(
        override val message: String,
        override val throwable: Throwable? = null
    ) : SynapseError()

    @Serializable
    public data class UnknownError(
        override val message: String,
        override val throwable: Throwable? = null
    ) : SynapseError()
}

/**
 * Functional mapping for success state.
 */
public inline fun <T, R> SynapseResult<T>.map(transform: (T) -> R): SynapseResult<R> {
    return when (this) {
        is SynapseResult.Success -> SynapseResult.Success(transform(data))
        is SynapseResult.Failure -> SynapseResult.Failure(error)
    }
}
