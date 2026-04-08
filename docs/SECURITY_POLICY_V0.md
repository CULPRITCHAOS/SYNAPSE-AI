# SECURITY_POLICY_V0

## Purpose

This is the **minimum private-build security policy** for Synapse.

It is intentionally narrower than a public-release security program.

The goal is not enterprise hardening. The goal is to prevent the most realistic failure modes for a private Android host app that can:
- expose or consume Android IPC surfaces
- let models request tool calls
- execute device/app actions
- store sensitive app state
- export or back up data

If this minimum policy is followed, Synapse avoids the dumbest and most likely local attack and data-leak paths while staying lean enough for early development.

---

## Scope

This policy applies to:
- the Synapse host app
- connected app integrations
- tool execution
- exported Android components
- runtime permission checks
- local data export / backup behavior
- basic audit receipts

This policy does **not** try to solve:
- public marketplace hardening
- enterprise identity / RBAC
- multi-user cloud tenancy
- advanced third-party trust chains
- formal incident response playbooks

Those are deferred until the app is mature enough to justify them.

---

## Private-Build Security Objectives

For V0, security only needs to guarantee these things:

1. internal components are not accidentally exposed
2. sensitive exported surfaces are protected
3. undeclared or malformed tool calls cannot run
4. risky actions require explicit user confirmation
5. missing Android runtime permissions fail closed
6. local backup/export flows do not leak app data to readable locations
7. all important allow/deny decisions are reconstructable later

That is the bar.

---

## What Stays Mandatory Even for a Private App

### 1. Explicit exported-component policy
Every activity, service, receiver, and provider relevant to Synapse integration must set `android:exported` explicitly.

Rules:
- default internal-only components to `false`
- only export components that are intentionally part of the Synapse integration surface
- if a component is exported and does sensitive work, protect it with permissions and/or code-level checks
- prefer explicit intents for service binding

Why this stays mandatory:
Accidentally exported components are one of the clearest local attack/data-leak paths on Android.

---

### 2. Registration is not trust
A connected app being registered in Synapse means only that it is known to the host.

It does **not** mean:
- trusted
- auto-approved
- allowed to execute all declared tools
- allowed to access all resources/state

Rules:
- connected apps must still declare tools/resources/events/state through `AppPack`
- the host must still validate declarations
- the host must still decide what gets activated

---

### 3. Tool execution is host-controlled
The model may request a tool call.
Only Synapse may authorize and execute it.

Every tool call must pass:
1. tool existence check
2. argument schema validation
3. capability grant check
4. runtime permission check if needed
5. confirmation check if needed
6. rate-limit / loop-guard check

Rules:
- undeclared tools are denied
- malformed arguments are denied
- hallucinated tool names are denied
- tool descriptions are helpful but not trusted as policy truth
- strict schemas are preferred by default

---

### 4. Confirmation for high-impact actions
A private app can still do damage to the device or user state if it acts too freely.

Require explicit confirmation for at least:
- device-state mutation
- screen automation / screen control
- external-effect actions (messages, purchases, IoT scene changes, etc.)
- sensitive data export
- mutation-capable user-defined tools/macros

The point is simple: AI can propose. The user still approves risky actions.

---

### 5. Android runtime permission checks
Synapse capabilities are not the same thing as Android runtime permissions.

Rules:
- if a dangerous Android permission is required, check it at runtime before execution
- if the permission is missing, fail closed or route into an explicit permission request flow
- request permissions in context, tied to the feature that needs them

Do not assume a permission was already granted just because a capability exists in Synapse.

---

### 6. Backup / export leak prevention
This is mandatory even for a private build.

Rules:
- do not create “backup” or “export” files in directories readable by other apps unless the user explicitly asked for it and understands the exposure
- prefer the standard Android backup path where possible
- treat custom export flows as sensitive operations
- exported logs, receipts, memories, or model traces must not silently land in world-readable storage

This is one of the easiest ways a private app leaks its own sensitive data.

---

### 7. Basic audit receipts
A private build still needs enough logging to reconstruct what happened when something goes wrong.

Log at least:
- tool execution requests
- tool execution denials
- confirmation prompts and outcomes
- app registration attempts
- capability grants / revocations
- permission-related denials
- runtime governor blocks for risky or runaway paths

Use a request ID / correlation ID so a single chain can be followed.

---

## Minimal Trust Model

You do not need a giant trust system yet.

Use this instead:

```kotlin
enum class TrustLevel {
    HOST,
    USER_APPROVED_APP,
    USER_DEFINED,
    REMOTE_PROVIDER
}
```

### Meaning
- `HOST`: Synapse built-ins
- `USER_APPROVED_APP`: connected app the user approved
- `USER_DEFINED`: macro/tool created locally by the user
- `REMOTE_PROVIDER`: model provider that can generate but never execute

This is enough for V0.

---

## Minimal Confirmation Model

```kotlin
enum class ConfirmationLevel {
    NONE,
    FIRST_USE,
    EVERY_TIME,
    HIGH_IMPACT_ONLY
}
```

Recommended defaults:
- read-only tool → `NONE`
- app mutation → `FIRST_USE` or `HIGH_IMPACT_ONLY`
- device mutation → `HIGH_IMPACT_ONLY`
- external effect → `EVERY_TIME`
- user-defined mutation tool → `EVERY_TIME`

---

## Minimal Default-Deny Rules

Deny by default if any of these are true:
- unknown app identity
- undeclared tool/resource/event/state
- invalid or missing capability grant
- missing Android runtime permission for a sensitive action
- malformed tool arguments
- missing required user confirmation
- exported component mismatch
- unsafe backup/export target

This gives you a clean fail-closed posture without overengineering.

---

## Minimum IPC Rules

For the private build:

1. keep non-integration components unexported
2. keep the local inference runtime private to the host app
3. do not allow third-party apps to bind directly to the inference service
4. if an exported service or receiver performs sensitive work, protect it with permissions and/or explicit code-level checks
5. use explicit intents for service interactions

This is the part that matters now. The rest can wait.

---

## Minimum User-Defined Tool Rules

User-defined tools/macros are allowed, but keep them fenced in.

Rules:
- no arbitrary code execution in V0
- require host validation before activation
- require stricter confirmation defaults for mutation-capable tools
- enforce the same schema/binding checks as host and app-defined tools

User-created does not mean auto-trusted.

---

## Deferred Until Public / Broader Release

Move these out of the private-build critical path:
- advanced trust tiers
- same-signer cross-app trust optimization unless actually needed
- remote-provider segmentation by data sensitivity class
- public marketplace app-pack review/signature workflows
- stronger third-party incident response / abuse workflows
- enterprise scope/RBAC systems

These are not zero value. They are just not the current bottleneck.

---

## Security Receipts

Keep one simple receipt type for now.

```kotlin
data class SecurityDecisionReceipt(
    val receiptId: String,
    val requestId: String?,
    val action: String,
    val subjectId: String,
    val decision: SecurityDecision,
    val reasonCode: String,
    val timestampEpochMs: Long
)

enum class SecurityDecision {
    ALLOW,
    DENY,
    REQUIRE_CONFIRMATION,
    REQUIRE_PERMISSION
}
```

This is enough for debugging and trust without building a whole governance bureaucracy.

---

## Definition of Done

`SECURITY_POLICY_V0.md` is good enough for the private build when:

- exported-component rules are fixed
- runtime permission rules are fixed
- tool validation gates are fixed
- confirmation rules are fixed
- backup/export leak rules are fixed
- default-deny conditions are fixed
- receipt model is fixed

If those seven things are true, the private build has the minimum viable hardening it actually needs.

---

## Immediate Follow-On Types to Implement

After this doc, the repo should define:

- `TrustLevel`
- `ConfirmationLevel`
- `SecurityDecision`
- `SecurityDecisionReceipt`
- `PermissionRequirement`
- `ExecutionGateResult`
- `PolicyReasonCode`
