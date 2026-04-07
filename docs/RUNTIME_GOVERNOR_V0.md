# RUNTIME_GOVERNOR_V0

## Purpose

`RuntimeGovernor` is the control layer that keeps Synapse stable during local AI execution.

Its job is to prevent the app from acting stupid under:
- thermal stress
- battery stress
- memory pressure
- unhealthy providers
- runaway tool/model loops

This module is required because the S26 Ultra is strong enough to make sustained local AI realistic, which means runtime control now matters.

---

## Foundation Notes

Current official guidance relevant to this document:

- Samsung markets S26 Ultra with stronger AI hardware and improved cooling.
- Google’s on-device Android LLM guidance still assumes high-end devices and warns against trusting emulator behavior.

Implication:

- Local-first is viable on the S26 Ultra.
- Sustained local-first needs thermal and battery governance.

---

## Module Placement

```text
core/runtime-governor/
```

Recommended dependencies:
- `core/common`
- `core/device-profile`
- `core/model-registry`
- `core/security`
- `core/storage`

---

## Responsibilities

`RuntimeGovernor` is responsible for:

1. Observing runtime signals.
2. Deciding whether the current provider/model/task is still safe.
3. Downgrading or rerouting when conditions degrade.
4. Emitting audit and runtime receipts for these decisions.
5. Preventing repeated failure loops.

It is not responsible for:
- writing prompts
- choosing tools directly
- UI rendering
- defining provider implementations

---

## Signals

The governor should observe at least:

- current thermal state
- battery level
- charging state
- provider health
- model load state
- benchmark receipts
- recent timeout/error frequency
- current task profile
- foreground/background state
- current app/tool sensitivity level

---

## Main Types

### RuntimeCondition

```kotlin
data class RuntimeCondition(
    val thermalState: ThermalState,
    val batteryPct: Int,
    val isCharging: Boolean,
    val providerHealthy: Boolean,
    val recentFailureRate: Double,
    val inForeground: Boolean,
    val taskProfile: String,
    val timestampEpochMs: Long
)
```

### ThermalState

```kotlin
enum class ThermalState {
    COOL,
    NORMAL,
    WARM,
    HOT,
    CRITICAL
}
```

### GovernorDecision

```kotlin
data class GovernorDecision(
    val action: GovernorAction,
    val reason: String,
    val fromProviderId: String?,
    val fromModelId: String?,
    val toProviderId: String?,
    val toModelId: String?,
    val timestampEpochMs: Long
)
```

### GovernorAction

```kotlin
enum class GovernorAction {
    ALLOW,
    DOWNGRADE_MODEL,
    SWITCH_PROVIDER,
    REDUCE_OUTPUT_BUDGET,
    DISABLE_BACKGROUND_INFERENCE,
    REQUIRE_USER_CONFIRMATION,
    BLOCK_REQUEST
}
```

---

## Default Policy

### COOL / NORMAL
- allow local heavy workloads if benchmark receipts are healthy
- allow normal routing

### WARM
- keep local inference
- prefer smaller local model for long-form planning
- reduce output budget where appropriate
- disable noncritical background inference

### HOT
- stop heavy planning on large local models
- switch to smaller local model or remote fallback if policy allows
- require user confirmation for nonessential expensive tasks

### CRITICAL
- block nonessential local inference
- keep only tiny routing paths alive if needed
- otherwise fail closed and surface a clear explanation

---

## Failure Loop Control

The governor should track:

- repeated provider load failures
- repeated generation timeouts
- repeated malformed structured outputs
- repeated tool-plan retries

If a threshold is crossed, governor should:
1. demote the provider/model pair
2. mark it temporarily degraded
3. switch to the next ranked candidate
4. emit an audit/runtime receipt

---

## Background Policy

Default:
- background local inference should be disabled unless explicitly allowed by policy
- expensive planning should be foreground-only
- dangerous tool flows should require user presence

This is stricter and safer for v0.

---

## Receipts

Every nontrivial governor intervention should create a receipt.

### RuntimeGovernorReceipt

```kotlin
data class RuntimeGovernorReceipt(
    val receiptId: String,
    val decision: GovernorAction,
    val reason: String,
    val providerId: String?,
    val modelId: String?,
    val thermalState: String,
    val batteryPct: Int,
    val timestampEpochMs: Long
)
```

These receipts should be queryable in logs and visible in developer mode.

---

## Downgrade Chain

Recommended order:

1. reduce output budget
2. switch to smaller local model
3. switch to lighter local provider
4. disable background inference
5. remote fallback if policy allows
6. hard block with clear explanation

Do not jump straight to remote unless local options are genuinely no longer viable.

---

## Hard Rules

1. Never let the model decide governor policy.
2. Never hide governor interventions from logs.
3. Never keep retrying a broken provider/model forever.
4. Never allow background dangerous actions just because the device is powerful.
5. Never trust emulator thermal behavior for tuning.

---

## Definition of Done

`RuntimeGovernor` is complete enough for early sprints when:

- it can read runtime signals
- it can produce a `GovernorDecision`
- orchestrator respects that decision
- downgrade chain works for at least one local provider
- receipts are persisted and inspectable

---

## References

- Samsung Galaxy S26 Ultra official product page
- Google AI Edge: LLM Inference guide for Android
