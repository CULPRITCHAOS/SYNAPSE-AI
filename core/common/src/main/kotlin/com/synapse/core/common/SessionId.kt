package com.synapse.core.common

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Type-safe wrapper for a Session ID.
 * Ensures we don't accidentally mix up strings for different IDs.
 * Used for the "Thought Trace" observability.
 */
@JvmInline
@Serializable
public value class SessionId(public val value: String) {
    public companion object {
        public fun generate(): SessionId = SessionId(UUID.randomUUID().toString())
    }
}
