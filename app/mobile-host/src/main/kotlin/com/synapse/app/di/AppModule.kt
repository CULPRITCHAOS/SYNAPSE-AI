package com.synapse.app.di

import com.synapse.core.common.DefaultDispatcherProvider
import com.synapse.core.common.DispatcherProvider
import com.synapse.core.model.ModelProvider
import com.synapse.core.model.gemma.GemmaModelProvider
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
    abstract fun bindModelProvider(
        impl: GemmaModelProvider
    ): ModelProvider

    @Binds
    @Singleton
    abstract fun bindOrchestrator(
        impl: DefaultOrchestrator
    ): Orchestrator

    companion object {
        @Provides
        @Singleton
        fun provideDispatcherProvider(): DefaultDispatcherProvider = DefaultDispatcherProvider()

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
