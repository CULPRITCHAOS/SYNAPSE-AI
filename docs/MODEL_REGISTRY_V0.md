# MODEL_REGISTRY_V0

## Purpose

`ModelRegistry` is the canonical inventory of every provider/model combination Synapse can use.

It exists so model selection is a **data problem**, not a hardcoded switch statement.

The registry must support:
- local on-device models
- local service providers
- remote fallback providers
- per-device benchmark receipts
- install-state and health-state tracking

---

## Foundation Notes

Current official guidance relevant to this document:

- MediaPipe LLM Inference on Android is a supported on-device path for high-end Android devices.
- LiteRT presents the `CompiledModel` API as the recommended modern runtime for high-performance on-device AI.

Implication:

- Synapse should treat providers like MediaPipe and LiteRT as swappable runtime backends behind one stable interface.
- Provider/model routing should be driven by capabilities, compatibility, and benchmark receipts.

---

## Module Placement

```text
core/model-registry/
```

Recommended dependencies:
- `core/common`
- `core/model-api`
- `core/device-profile`
- `core/storage`

---

## Responsibilities

`ModelRegistry` is responsible for:

1. Tracking what providers exist.
2. Tracking what models are installed and usable.
3. Tracking compatibility between models and the current device.
4. Tracking provider/model health.
5. Exposing candidate providers/models for each task profile.
6. Recording benchmark receipts and routing notes.

`ModelRegistry` is not responsible for:
- prompt construction
- policy enforcement
- UI rendering
- app capability grants

---

## Main Types

### ProviderRecord

```kotlin
data class ProviderRecord(
    val providerId: String,
    val displayName: String,
    val providerClass: ProviderClass,
    val capabilities: ProviderCapabilities,
    val isEnabled: Boolean,
    val isHealthy: Boolean,
    val notes: String? = null
)
```

### ProviderClass

```kotlin
enum class ProviderClass {
    ON_DEVICE_NATIVE,
    LOCAL_SERVICE,
    REMOTE
}
```

### ModelRecord

```kotlin
data class ModelRecord(
    val recordId: String,
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val installState: InstallState,
    val localPath: String? = null,
    val remoteName: String? = null,
    val diskFootprintMb: Int?,
    val estimatedRamMb: Int?,
    val quantization: String? = null,
    val allowedTaskProfiles: List<String>,
    val lastHealthCheckEpochMs: Long?,
    val lastBenchmarkReceiptId: String?,
    val notes: String? = null
)
```

### InstallState

```kotlin
enum class InstallState {
    NOT_INSTALLED,
    DOWNLOADING,
    INSTALLED,
    BROKEN,
    DISABLED
}
```

### RoutingCandidate

```kotlin
data class RoutingCandidate(
    val providerId: String,
    val modelId: String,
    val taskProfile: String,
    val fitnessScore: Double,
    val reason: String
)
```

---

## Registry Rules

1. Every model must belong to exactly one provider.
2. Every model must declare allowed task profiles.
3. Every local model must declare disk footprint and estimated memory pressure where known.
4. Every candidate provider/model pair must be filterable by current `DeviceProfile` and current runtime state.
5. Every benchmark receipt must be attached to a provider/model pair.
6. Broken providers/models must be visible to the orchestrator and UI.

---

## Selection Inputs

The registry should consider the following inputs when producing candidates:

- `DeviceProfile.preferredInferenceTier`
- current battery / charging state
- current thermal state
- task profile
- provider capabilities
- model install state
- provider health state
- benchmark receipts
- policy constraints

---

## First-Pass Candidate Ranking

Suggested scoring inputs:

- provider supports task profile
- provider supports tool calling if needed
- provider supports structured output if needed
- provider supports multimodal if needed
- local provider preferred when device/runtime conditions allow
- lower latency benchmark score wins among equivalent candidates
- recent successful benchmark beats stale benchmark
- healthier provider beats less healthy provider

---

## Example Routing Policy

### Command routing
Prefer:
1. small local routing model
2. general local model
3. remote fallback

### Structured extraction
Prefer:
1. local model with reliable JSON / schema support
2. remote model with strong structured-output reliability

### Planning
Prefer:
1. local heavy model on `LOCAL_HEAVY` devices
2. remote fallback when local latency/thermal conditions degrade

### Screen understanding
Prefer:
1. multimodal local provider when supported
2. hybrid path using perception modules + text model
3. remote fallback only when policy allows

---

## Minimum Initial Provider Set

For the repo foundation, start with:

1. `provider-fake`
   - deterministic CI/test provider
2. `provider-mediapipe`
   - primary on-device Android path
3. `provider-litert`
   - experimental/high-performance on-device path
4. `provider-openai` or `provider-gemini`
   - optional remote comparison/fallback

This is enough to prove model-agnostic architecture without provider sprawl.

---

## Hard Rules

1. Do not let features call providers directly; go through orchestrator + registry.
2. Do not hardcode one provider as the permanent default.
3. Do not let routing depend only on marketing model names.
4. Do not select a provider/model that cannot satisfy the required capabilities.
5. Do not hide install/health failures.

---

## Definition of Done

`ModelRegistry` is complete enough for early sprints when:

- providers can be registered and listed
- models can be registered and listed
- install state is persisted
- health state is persisted
- benchmark receipts are attached
- orchestrator can request ranked candidates for a task profile

---

## References

- Google AI Edge: LLM Inference guide for Android
- Google AI Edge: LiteRT overview
