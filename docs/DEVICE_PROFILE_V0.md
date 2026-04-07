# DEVICE_PROFILE_V0

## Purpose

`DeviceProfile` is the canonical runtime description of the current Android device as it relates to local AI execution.

Synapse must route by **measured device capability**, not by hardcoded phone-name assumptions.

The Galaxy S26 Ultra is the current primary development device and should be treated as a **high-capability reference target**, but the architecture must remain generic enough to profile and support other devices.

---

## Foundation Notes

Current official guidance relevant to this document:

- Google AI Edge says MediaPipe LLM Inference for Android is optimized for **high-end Android devices**, citing **Pixel 8 and Samsung S23 or later**, and warns that device emulators are not reliable for this work.
- LiteRT positions the newer **CompiledModel** API as the recommended runtime for state-of-the-art on-device performance and hardware acceleration.
- Samsung markets Galaxy S26 Ultra as a stronger AI device than prior Ultra models, with **Snapdragon 8 Elite Gen 5 for Galaxy**, a **39% faster NPU** than S25 Ultra, improved cooling, and storage tiers up to **1 TB / 16 GB RAM**.

Implication:

- The S26 Ultra should be expected to support more aggressive local-AI workloads than older baseline devices.
- Synapse should still verify this through local benchmarks and runtime telemetry.

---

## Responsibilities

`DeviceProfile` is responsible for:

1. Detecting device/runtime capability relevant to local AI.
2. Producing a normalized view for provider selection.
3. Recording benchmark receipts used by routing and governor logic.
4. Classifying the device into a conservative inference tier.
5. Updating routing decisions when conditions change (thermal, battery, storage, etc.).

`DeviceProfile` is **not** responsible for:

- selecting prompts
- deciding tool permissions
- directly executing inference
- owning model installation logic

---

## Module Placement

```text
core/device-profile/
```

This module should depend only on:
- `core/common`
- `core/model-api`
- `core/storage`

It should not depend on UI features.

---

## Main Types

### DeviceProfile

```kotlin
data class DeviceProfile(
    val deviceId: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val ramClass: RamClass,
    val storageHeadroomGb: Int,
    val supportsOnDeviceAI: Boolean,
    val supportsHighEndOnDeviceAI: Boolean,
    val preferredInferenceTier: DeviceInferenceTier,
    val thermalCapabilities: ThermalCapabilities,
    val benchmarkReceiptIds: List<String>,
    val updatedAtEpochMs: Long
)
```

### DeviceInferenceTier

```kotlin
enum class DeviceInferenceTier {
    LOCAL_HEAVY,
    LOCAL_STANDARD,
    LOCAL_LIGHT,
    REMOTE_FALLBACK
}
```

### RamClass

```kotlin
enum class RamClass {
    LOW,
    MID,
    HIGH,
    ULTRA
}
```

### ThermalCapabilities

```kotlin
data class ThermalCapabilities(
    val hasThermalSignals: Boolean,
    val supportsSustainedHeavyInference: Boolean,
    val notes: String? = null
)
```

### DeviceBenchmarkReceipt

```kotlin
data class DeviceBenchmarkReceipt(
    val receiptId: String,
    val providerId: String,
    val modelId: String,
    val taskProfile: String,
    val promptTokens: Int,
    val outputTokens: Int,
    val warmupMs: Long,
    val firstTokenMs: Long?,
    val totalLatencyMs: Long,
    val tokensPerSecond: Double?,
    val peakMemoryMb: Int?,
    val thermalDelta: String?,
    val batteryDeltaPct: Double?,
    val timestampEpochMs: Long
)
```

---

## Profile Collection Phases

### Phase 1: Static collection
Collected at first launch and on app update:

- manufacturer
- model
- Android version
- RAM class
- storage headroom
- ABI / chipset metadata where available

### Phase 2: Runtime signals
Collected during normal operation:

- battery level
- charging state
- thermal signals
- foreground/background status
- current provider/model in use

### Phase 3: Benchmark receipts
Collected when:

- a provider is installed
- a model is installed or updated
- the user runs a benchmark
- Synapse needs to re-evaluate routing after runtime changes

---

## Initial Routing Policy

### LOCAL_HEAVY
Use when:
- benchmarks show strong throughput/latency
- storage headroom is healthy
- thermal behavior is stable
- current battery/charging state is acceptable

Allowed task profiles:
- command routing
- structured extraction
- planning
- conversation
- some multimodal/screen tasks where supported

### LOCAL_STANDARD
Use when:
- device is capable but not ideal for sustained heavy work
- current runtime conditions are moderate

Allowed task profiles:
- command routing
- conversation
- short structured outputs
- light planning

### LOCAL_LIGHT
Use when:
- local inference is possible but constrained
- thermals, battery, or model footprint make heavier workloads risky

Allowed task profiles:
- short routing
- tiny extraction tasks
- fallback chat with smaller models

### REMOTE_FALLBACK
Use when:
- no supported local provider is available
- current local model is unavailable/unhealthy
- policy requires a remote provider
- local runtime is blocked by device conditions

---

## Hard Rules

1. Do not classify by marketing name alone.
2. Do not assume emulator results reflect real-device behavior.
3. Do not promote a device into `LOCAL_HEAVY` without benchmark receipts.
4. Do not downgrade only by RAM class; use runtime conditions too.
5. Do not allow benchmark data to silently expire—stale receipts should be revalidated.

---

## S26 Ultra Development Assumption

For current development, Synapse should assume:

- S26 Ultra is the primary reference device.
- Local-first should be the default strategy on that device.
- Benchmark receipts are still the source of truth for model/provider routing.

This lets the repo exploit current hardware without poisoning the architecture with one-device assumptions.

---

## Definition of Done

`DeviceProfile` is complete enough for Sprint 0/1 when:

- Synapse can create and persist a normalized `DeviceProfile`.
- Synapse can persist benchmark receipts.
- Synapse can classify the device into an inference tier.
- Orchestrator/provider selection can read that tier.
- Runtime governor can react to profile changes.

---

## References

- Samsung Galaxy S26 Ultra official product page
- Google AI Edge: LLM Inference guide for Android
- Google AI Edge: LiteRT overview
