package com.synapse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapse.feature.orchestrator.OrchestrationEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ChatScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    var text by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Event List (The "Thought Trace")
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(viewModel.events) { event ->
                EventItem(event)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Area
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Synapse...") },
                enabled = !viewModel.isProcessing
            )
            Button(
                onClick = {
                    viewModel.sendRequest(text)
                    text = ""
                },
                modifier = Modifier.padding(start = 8.dp),
                enabled = !viewModel.isProcessing
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun EventItem(event: OrchestrationEvent) {
    val backgroundColor = when (event) {
        is OrchestrationEvent.Thought -> Color.LightGray.copy(alpha = 0.2f)
        is OrchestrationEvent.ToolCallStarted -> Color.Yellow.copy(alpha = 0.1f)
        is OrchestrationEvent.ResponseChunk -> Color.Blue.copy(alpha = 0.1f)
        is OrchestrationEvent.Error -> Color.Red.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = event.javaClass.simpleName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            val content = when (event) {
                is OrchestrationEvent.Thought -> event.message
                is OrchestrationEvent.ToolCallStarted -> "Calling: ${event.toolName}(${event.arguments})"
                is OrchestrationEvent.ToolCallFinished -> "Result: ${event.result}"
                is OrchestrationEvent.ResponseChunk -> event.text
                is OrchestrationEvent.Error -> "ERROR: ${event.message}"
                else -> ""
            }
            if (content.isNotEmpty()) {
                Text(text = content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
