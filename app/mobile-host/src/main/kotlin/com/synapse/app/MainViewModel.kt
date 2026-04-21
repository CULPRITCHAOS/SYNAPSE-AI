package com.synapse.app

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.feature.orchestrator.OrchestrationEvent
import com.synapse.feature.orchestrator.Orchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val orchestrator: Orchestrator
) : ViewModel() {

    val events = mutableStateListOf<OrchestrationEvent>()
    var isProcessing = false

    fun sendRequest(request: String) {
        if (request.isBlank()) return
        
        viewModelScope.launch {
            isProcessing = true
            events.clear()
            
            orchestrator.process(request).collect { event ->
                events.add(event)
                if (event is OrchestrationEvent.Finished || event is OrchestrationEvent.Error) {
                    isProcessing = false
                }
            }
        }
    }
}
