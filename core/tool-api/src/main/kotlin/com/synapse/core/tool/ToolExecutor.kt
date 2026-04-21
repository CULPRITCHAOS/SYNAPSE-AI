package com.synapse.core.tool

import com.synapse.core.apppack.ToolDefinition
import com.synapse.core.common.SynapseResult

/**
 * Interface for executing a tool call.
 * This is the bridge between the Orchestrator's plan and the actual system/app action.
 */
public interface ToolExecutor {
    /**
     * Executes a tool with the given arguments.
     * @param tool The definition of the tool being called.
     * @param argumentsJson The JSON string containing the arguments for the tool.
     * @return A [SynapseResult] containing the tool's output as a JSON string or an error.
     */
    public suspend fun execute(
        tool: ToolDefinition,
        argumentsJson: String
    ): SynapseResult<String>
}

/**
 * Registry to find the appropriate [ToolExecutor] for a given [ToolDefinition].
 */
public interface ToolRegistry {
    public fun getExecutorFor(tool: ToolDefinition): ToolExecutor?
}
