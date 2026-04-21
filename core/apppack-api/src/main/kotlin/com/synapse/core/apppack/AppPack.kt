package com.synapse.core.apppack

import kotlinx.serialization.Serializable

/**
 * The V0 AppPack schema.
 * Defines the identity and interface of a Synapse-compatible app.
 */
@Serializable
public data class AppPack(
    val id: String,
    val version: String,
    val name: String,
    val description: String,
    val tools: List<ToolDefinition>,
    val events: List<EventDefinition> = emptyList(),
    val permissions: List<String> = emptyList()
)

/**
 * Definition of a capability (Tool) that the app provides to the Orchestrator.
 */
@Serializable
public data class ToolDefinition(
    val name: String,
    val description: String,
    val parametersJsonSchema: String, // JSON Schema for the tool's arguments
    val category: ToolCategory = ToolCategory.UTILITY
)

@Serializable
public enum class ToolCategory {
    UTILITY,
    ACTION,
    QUERY,
    COMMUNICATION,
    SYSTEM
}

/**
 * Definition of an event that this app might broadcast.
 */
@Serializable
public data class EventDefinition(
    val type: String,
    val description: String,
    val payloadJsonSchema: String
)
