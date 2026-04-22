package com.synapse.core.model.registry

import com.synapse.core.model.ModelProvider
import com.synapse.core.model.ModelTier

/**
 * Interface for the Model Registry.
 * Inventory of every [ModelProvider] available to Synapse.
 */
public interface ModelRegistry {
    /**
     * Registers a new [ModelProvider].
     */
    public fun registerProvider(provider: ModelProvider)

    /**
     * Returns all registered [ModelProvider]s for a given [ModelTier].
     */
    public fun getProvidersFor(tier: ModelTier): List<ModelProvider>

    /**
     * Returns the "best" [ModelProvider] for a given [ModelTier].
     * (Simplified for now: returns the first available).
     */
    public fun getBestProviderFor(tier: ModelTier): ModelProvider?
}

/**
 * In-memory implementation of the [ModelRegistry].
 */
public class InMemoryModelRegistry : ModelRegistry {
    private val providers = mutableListOf<ModelProvider>()

    override fun registerProvider(provider: ModelProvider) {
        providers.add(provider)
    }

    override fun getProvidersFor(tier: ModelTier): List<ModelProvider> {
        return providers.filter { it.tier == tier }
    }

    override fun getBestProviderFor(tier: ModelTier): ModelProvider? {
        return providers.firstOrNull { it.tier == tier }
    }
}
