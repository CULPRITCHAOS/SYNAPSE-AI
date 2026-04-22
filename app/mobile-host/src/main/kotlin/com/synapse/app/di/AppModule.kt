package com.synapse.app.di

import com.synapse.core.model.registry.InMemoryModelRegistry
import com.synapse.core.model.registry.ModelRegistry
import com.synapse.core.model.router.DefaultModelRouter
import com.synapse.core.model.router.ModelRouter
import com.synapse.core.tool.ToolExecutor
import com.synapse.core.tool.ToolRegistry
import com.synapse.core.tool.system.FlashlightToolExecutor
import com.synapse.feature.orchestrator.DefaultOrchestrator
import com.synapse.feature.orchestrator.Orchestrator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.synapse.core.apppack.ToolDefinition

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        impl: DefaultDispatcherProvider
    ): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindOrchestrator(
        impl: DefaultOrchestrator
    ): Orchestrator

    @Binds
    @Singleton
    abstract fun bindModelRouter(
        impl: DefaultModelRouter
    ): ModelRouter

    companion object {
        @Provides
        @Singleton
        fun provideDispatcherProvider(): DefaultDispatcherProvider = DefaultDispatcherProvider()

        @Provides
        @Singleton
        fun provideModelRegistry(
            gemmaProvider: GemmaModelProvider
        ): ModelRegistry {
            val registry = InMemoryModelRegistry()
            registry.registerProvider(gemmaProvider)
            // Future: register a SMALL_FAST provider here
            return registry
        }

        @Provides
        @Singleton
        fun provideToolRegistry(
            flashlightExecutor: FlashlightToolExecutor
        ): ToolRegistry = object : ToolRegistry {
            override fun getExecutorFor(tool: ToolDefinition): ToolExecutor? {
                return if (tool.name == "flashlight") flashlightExecutor else null
            }
        }
    }
}
