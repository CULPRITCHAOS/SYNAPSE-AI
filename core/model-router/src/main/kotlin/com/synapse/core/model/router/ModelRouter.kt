package com.synapse.core.model.router

import com.synapse.core.model.ModelTier
import com.synapse.core.model.TaskProfile

/**
 * Interface for the Model Router.
 * Centralizes the policy for choosing which model tier handles a request.
 */
public interface ModelRouter {
    /**
     * Determines the appropriate [ModelTier] for a given [TaskProfile].
     */
    public fun route(taskProfile: TaskProfile): ModelTier
}

/**
 * Default implementation of the routing policy.
 */
public class DefaultModelRouter : ModelRouter {
    override fun route(taskProfile: TaskProfile): ModelTier {
        return when (taskProfile) {
            TaskProfile.COMMAND_ROUTING,
            TaskProfile.EXTRACTION,
            TaskProfile.SAFETY_CHECK -> ModelTier.SMALL_FAST

            TaskProfile.PLANNING,
            TaskProfile.CHAT -> ModelTier.LARGE_REASONING
        }
    }
}
