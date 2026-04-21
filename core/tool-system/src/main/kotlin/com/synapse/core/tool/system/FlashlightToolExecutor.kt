package com.synapse.core.tool.system

import android.content.Context
import android.hardware.camera2.CameraManager
import com.synapse.core.apppack.ToolDefinition
import com.synapse.core.common.SynapseError
import com.synapse.core.common.SynapseResult
import com.synapse.core.tool.ToolExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * A concrete implementation of [ToolExecutor] that controls the device flashlight.
 * This proves the agent's ability to interact with physical hardware.
 */
public class FlashlightToolExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) : ToolExecutor {

    override suspend fun execute(
        tool: ToolDefinition,
        argumentsJson: String
    ): SynapseResult<String> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() 
                ?: return SynapseResult.failure(SynapseError.ToolError("No camera found", "flashlight"))

            // Parse arguments (Expected: {"enabled": true/false})
            val json = Json.parseToJsonElement(argumentsJson).jsonObject
            val enabled = json["enabled"]?.jsonPrimitive?.boolean ?: false

            cameraManager.setTorchMode(cameraId, enabled)

            SynapseResult.success("Flashlight is now ${if (enabled) "ON" else "OFF"}")
        } catch (e: Exception) {
            SynapseResult.failure(SynapseError.ToolError(
                message = e.message ?: "Failed to toggle flashlight",
                toolName = "flashlight",
                throwable = e
            ))
        }
    }
}
