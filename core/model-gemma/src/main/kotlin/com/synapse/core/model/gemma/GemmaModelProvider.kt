package com.synapse.core.model.gemma

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.synapse.core.common.SynapseError
import com.synapse.core.common.SynapseResult
import com.synapse.core.model.GenerateRequest
import com.synapse.core.model.GenerateResult
import com.synapse.core.model.ModelProvider
import com.synapse.core.model.ProviderCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import com.synapse.core.common.DispatcherProvider

/**
 * Implementation of [ModelProvider] using Google's MediaPipe LLM Inference (Gemma).
 * Targeted for high-performance execution on the S26 Ultra.
 */
public class GemmaModelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) : ModelProvider {

    override val providerId: String = "google-mediapipe"
    override val modelId: String = "gemma-2b-it" // Initial target
    
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        onDevice = true,
        streaming = true,
        toolCalling = false, // V0: Gemma 2B via MediaPipe requires prompt-based tool calling
        structuredOutput = false
    )

    private var llmInference: LlmInference? = null

    /**
     * Initializes the model. 
     * Note: In a real app, this should be managed by a lifecycle-aware repository.
     */
    private fun initializeInference(modelPath: String) {
        if (llmInference == null) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setTemperature(0.7f)
                .setMaxTokens(1024)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
        }
    }

    override suspend fun generate(request: GenerateRequest): SynapseResult<GenerateResult> = 
        withContext(dispatcherProvider.io) {
            try {
                val inference = llmInference ?: return@withContext SynapseResult.failure(
                    SynapseError.ModelError("Gemma model not initialized. Path missing.", "MODEL_NOT_READY")
                )

                // V0: Simple prompt concatenation for MediaPipe
                // MediaPipe currently doesn't have a native "Chat" API like OpenAI, 
                // so we format the history into a single string.
                val prompt = formatPrompt(request)
                
                val response = inference.generateResponse(prompt)
                
                // V0: Basic text output. 
                // Future: Add regex-based tool extraction for Gemma.
                SynapseResult.success(GenerateResult.Success(response))
            } catch (e: Exception) {
                SynapseResult.failure(SynapseError.ModelError(
                    message = e.message ?: "Gemma inference failed",
                    code = "GEMMA_ERROR",
                    throwable = e
                ))
            }
        }

    private fun formatPrompt(request: GenerateRequest): String {
        val sb = StringBuilder()
        request.messages.forEach { msg ->
            val roleName = when(msg.role.name) {
                "SYSTEM" -> "system"
                "USER" -> "user"
                "ASSISTANT" -> "model"
                else -> "user"
            }
            sb.append("<start_of_turn>$roleName\n${msg.content}<end_of_turn>\n")
        }
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }
}
