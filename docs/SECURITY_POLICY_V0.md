# SECURITY_POLICY_V0

## Purpose

`SecurityPolicy` is the canonical host-enforcement policy for Synapse.

It defines what the host is allowed to expose, what connected apps are allowed to request, what models are allowed to trigger, what requires user confirmation, and what must be denied or degraded.

This document exists because the Synapse architecture is inherently high-risk:
- local AI can request actions
- tools can mutate device/app state
- connected apps can expose custom tool surfaces
- Android IPC surfaces can be accidentally over-exposed
- models can hallucinate, overreach, or chain unsafe actions

If the repo gets this wrong, Synapse becomes a privilege proxy and attack surface instead of a trustworthy host.

---

## Foundation

This policy is grounded in current official docs:

1. MCP security guidance says there should be a **human in the loop**, tool inputs should be validated, outputs should be sanitized and validated before being returned to the model, invocations should be rate-limited, and tool usage should be logged. MCP also warns that tool descriptions and annotations should be treated as **untrusted unless from a trusted server**. citeturn832580search2turn255378view0

2. MCP security guidance also recommends a **progressive, least-privilege scope model**, avoiding wildcard or omnibus scopes and logging elevation events with correlation IDs. citeturn832580search2

3. Android security guidance says exported components should explicitly set `android:exported`, and accidentally exposed components can lead to data leaks, code execution in the vulnerable app’s context, or denial of service. citeturn886582search0

4. Android security guidance says exported components performing sensitive work should be protected by permissions, ideally manifest-declared and/or code-level checks; it also recommends **signature** permissions when sharing between apps you control and explicit separation of concerns for endpoints. citeturn886582search1turn832580search5

5. Android’s security checklist says IPC should use Android mechanisms like `Intent`, `Binder`, `Messenger`, or `BroadcastReceiver`, and if IPC is not intended for other apps, `android:exported` should be `false`. If IPC is exposed, apply permissions and prefer explicit intents for services. It also warns that binding to a service with an implicit intent is a security hazard. citeturn886582search4turn886582search3

6. Android package visibility guidance says apps targeting Android 11+ only see a filtered set of packages by default and must declare specific visibility needs with `<queries>`. That supports Synapse’s explicit registration model rather than broad device scanning. citeturn886582search7

7. Android permission guidance says dangerous permissions require runtime approval on Android 6.0+ and should only be requested in context. citeturn832580search0turn832580search6

8. Android custom-permission guidance says custom permissions are risky when misused and that **signature** protection levels are preferred wherever possible for app-to-app sharing you control; it also warns about pitfalls in custom permission definitions. citeturn832580search1turn832580search3turn832580search4

9. OpenAI’s function-calling docs say function calling connects models to external tools and systems, and `strict: true` in function definitions ensures arguments match the supplied JSON Schema. That is a strong reason to keep tool schemas strict by default and to separate tool execution from the model. citeturn886582search2

Implication:

- Synapse security must be **host-enforced, least-privilege, explicit, and auditable**.
- Models are not trusted to enforce policy.
- Connected apps are not trusted by default.
- Exported Android surfaces are opt-in, explicit, and permission-protected.
- Dangerous capabilities and dangerous tool calls require additional friction.

---

## Security Objectives

V0 security policy optimizes for these goals:

1. prevent unauthorized tool execution
2. prevent unauthorized app/resource access
3. prevent the model from escalating privileges through tool misuse
4. prevent over-exposed Android IPC/components
5. preserve user agency for high-impact actions
6. preserve auditability and replayability
7. support least-privilege connected-app integration

---

## Non-Goals for V0

This policy does **not** attempt to fully solve:
- cloud account federation
- zero-trust enterprise deployment
- remote multi-user RBAC
- full OAuth scope brokerage for arbitrary third-party APIs

Those may come later. V0 is a single-user, device-first host security model.

---

## Policy Layers

Synapse security should be enforced in layers, not by one giant gate.

### Layer 1: Registration trust
Can this app/tool/provider even register?

### Layer 2: Capability grant
What may the app expose or request?

### Layer 3: Runtime permission state
Does the device/app currently have the required Android permission?

### Layer 4: Tool execution policy
Is this specific tool call allowed right now?

### Layer 5: Result validation / egress policy
Is the tool result safe to surface back to the model or UI?

### Layer 6: Audit / replay / incident visibility
Can we reconstruct what happened?

---

## Security Domains

```kotlin
enum class SecurityDomain {
    HOST,
    CONNECTED_APP,
    MODEL_PROVIDER,
    TOOL_EXECUTION,
    EVENT_STATE_BUS,
    ANDROID_IPC,
    USER_DEFINED_AUTOMATION
}
```

These domains exist so policies can be reasoned about separately instead of flattening everything into a generic “secure/not secure” boolean.

---

## Trust Levels

```kotlin
enum class TrustLevel {
    TRUSTED_HOST,
    TRUSTED_SAME_SIGNER_APP,
    USER_APPROVED_APP,
    UNTRUSTED_APP,
    USER_DEFINED,
    REMOTE_PROVIDER
}
```

### Interpretation

- `TRUSTED_HOST`: Synapse built-ins owned by the host app
- `TRUSTED_SAME_SIGNER_APP`: app signed with same certificate or otherwise strongly bound
- `USER_APPROVED_APP`: user explicitly approved but not same-signer trusted
- `UNTRUSTED_APP`: registered or seen, but not approved / low confidence
- `USER_DEFINED`: tool/macro created by the user in Tool Studio
- `REMOTE_PROVIDER`: external network model provider, trusted for generation only, not execution

### Rule

Higher trust may reduce friction, but **never bypasses policy entirely**.

---

## Capability Model

Security should use a least-privilege capability model.

### Capability classes

```kotlin
enum class CapabilityClass {
    READ_ONLY,
    APP_MUTATION,
    DEVICE_MUTATION,
    EXTERNAL_EFFECT,
    SENSITIVE_DATA_ACCESS
}
```

### Examples

- `screen_read` → `SENSITIVE_DATA_ACCESS`
- `inventory_control` → `APP_MUTATION`
- `scene_activation` → `EXTERNAL_EFFECT`
- `screen_control` → `DEVICE_MUTATION`
- `sensor_reading` → `READ_ONLY`

### Policy rule

Following MCP’s least-privilege guidance, capabilities should start minimal and elevate only when needed. Avoid omnibus “full access” style grants. citeturn832580search2

---

## Capability Grant States

```kotlin
enum class CapabilityGrantState {
    NOT_REQUESTED,
    REQUESTED,
    APPROVED,
    DENIED,
    REVOKED,
    SUSPENDED
}
```

### Rules

- apps can request capabilities
- host/user grants them
- tools requiring ungranted capabilities are not executable
- revocation takes effect immediately
- suspended apps can be blocked without deleting registry state

---

## Android Permission Rules

Android permissions are separate from Synapse capabilities.

### Rule 1
A Synapse capability does **not** imply the app currently has the corresponding Android runtime permission.

### Rule 2
Dangerous Android permissions must be checked at runtime before execution if the platform requires them. Android explicitly requires dangerous permissions to be requested at runtime on Android 6.0+ devices. citeturn832580search0turn832580search6

### Rule 3
If the underlying Android permission is missing, tool execution must fail closed or request permission through an explicit user flow.

---

## Connected App Registration Policy

### Registration requirements
A connected app must provide:
- valid `AppPack`
- stable package identity
- compatible Synapse version declaration
- declared capabilities
- declared tools/resources/events/state

### Registration is not trust
Registration only means “known to host,” not “trusted.”

### Explicit registration over scanning
Android package visibility is filtered by default on Android 11+, which supports Synapse’s explicit registration model. Do not build trust logic around blind package scanning. citeturn886582search7

---

## Exported Component Policy

### Rule 1: Explicit export state
Every component relevant to Synapse integration must explicitly declare `android:exported`. Android’s security guidance explicitly recommends this to avoid accidental exposure. citeturn886582search0

### Rule 2: Default internal surfaces to `false`
If a component is not intentionally part of the Synapse integration surface, it must not be exported. Android’s security guidance recommends not exporting components unless necessary. citeturn886582search1turn886582search4

### Rule 3: Protect exported sensitive surfaces
If a component is exported and performs sensitive work, require a matching permission or enforce permission checks in code. Android’s security guidance explicitly recommends both manifest and code-level checks. citeturn886582search1turn886582search4

### Rule 4: Prefer explicit intents for service binding
Binding with implicit intents is a security hazard; Android explicitly warns against it. citeturn886582search3

### Rule 5: Prefer single-purpose endpoints
Android recommends separation of concerns / single-task endpoints for exported surfaces. Synapse-connected apps should expose narrow, specific endpoints rather than giant all-powerful ones. citeturn886582search1

---

## Custom Permission Policy

### Same-signer integrations
When two apps are controlled by the same developer/signing key, prefer **signature-level** permissions or explicit same-signer verification rather than weak custom permissions. Android security guidance recommends signature permissions for this use case. citeturn832580search1turn832580search5

### Avoid weak protection levels
Do not use `normal` or `dangerous` custom permissions to protect high-value IPC surfaces unless there is a compelling reason and explicit user-facing rationale. Android’s custom-permission guidance says these levels provide weak protection for inter-app security. citeturn832580search1

### Define permissions carefully
Custom permission definitions must be explicit and correctly namespaced; misspelled/missing custom permissions can be exploited. citeturn832580search1turn832580search4

---

## Tool Execution Policy

### Core rule
The model may request a tool call. Only Synapse may authorize and execute it.

### Validation gates
Every tool call must pass:
1. tool existence check
2. argument schema validation
3. capability grant check
4. Android permission state check if relevant
5. rate-limit check
6. trust-level check
7. confirmation policy check
8. runtime-governor check

### Strict schema by default
OpenAI documents `strict: true` for function calling, which ensures arguments match the provided schema. Synapse should therefore prefer strict tool schemas by default. citeturn886582search2

### Model descriptions are not security truth
Per MCP guidance, annotations and descriptions are not authoritative security policy. Policy decisions must use host metadata and enforcement rules. citeturn832580search2turn255378view0

---

## Confirmation Policy

### Confirmation levels

```kotlin
enum class ConfirmationLevel {
    NONE,
    FIRST_USE,
    EVERY_TIME,
    HIGH_IMPACT_ONLY,
    HOST_POLICY_DECIDES
}
```

### Require confirmation for
- device-state mutation
- screen control / screen automation
- external-effect actions (messaging, purchases, IoT scene changes, etc.)
- sensitive data exfiltration
- user-defined tools with mutation side effects
- actions triggered from low-trust app surfaces

### Human in the loop
MCP explicitly recommends a human in the loop and says users should understand what each tool does before authorizing it. citeturn832580search2turn255378view0

---

## Input and Output Validation Policy

### Input validation
MCP says tool inputs should be validated. Synapse must reject malformed arguments before execution. citeturn832580search2turn255378view0

### Output validation
MCP says clients should validate tool results before passing them back to the model. Synapse should validate tool outputs against declared output schemas where available and sanitize text/binary outputs before model re-ingestion. citeturn832580search2turn255378view0

### Resource URI validation
MCP recommends validating resource URIs and applying access control to sensitive resources. Synapse must treat resource identifiers as untrusted input. citeturn963417view4

---

## Rate Limits and Abuse Controls

### Why
MCP security guidance explicitly recommends rate limits and logging. citeturn832580search2turn255378view0

### Minimum limits for V0
- per-app tool call rate limit
- per-tool rate limit
- per-session confirmation flood limit
- per-provider retry limit
- repeated-failure backoff

### Abuse cases to defend
- event spam from a connected app
- repeated confirmation prompts to pressure the user
- rapid repeated tool calls against device controls
- repeated provider retries after deterministic schema failure

---

## Runtime Governor Security Role

The runtime governor is not just about thermals—it is also a security boundary.

It may:
- disable background inference for risky workflows
- downgrade providers after repeated malformed outputs
- block actions under degraded runtime conditions
- halt runaway loops

This prevents unstable runtime state from becoming a security bypass.

---

## Event / State Bus Security Rules

### Rule 1
Only declared resources/events/state snapshots may be published by a connected app.

### Rule 2
Subscriptions and replay require capability checks and should use negotiated capabilities only, in line with MCP lifecycle guidance. citeturn576899search1

### Rule 3
Sensitive resources require access control and URI validation. MCP explicitly recommends both. citeturn963417view4

### Rule 4
Sessions/cursors must not allow cross-app data bleed.

---

## Model Provider Security Rules

### Remote providers
Treat remote providers as untrusted execution surfaces:
- they can generate text/tool requests
- they cannot execute host actions directly
- they should not receive more context than required for the task
- sensitive local-only workflows may be restricted from remote fallback

### Local providers
Local does not mean automatically safe:
- malformed tool requests are still invalid
- hallucinated tool names are still denied
- context minimization still matters

### Tool calling vs structured outputs
Keep these separate. OpenAI and Gemini both document them separately, and mixing them into one generic “JSON capability” makes policy weaker and harder to reason about. citeturn886582search2turn693795search3turn113681search1

---

## Logging and Audit Policy

MCP recommends logging tool usage and elevation events. citeturn832580search2

Synapse should log at least:
- app registration attempts
- capability requests and grants
- tool execution requests
- denied tool executions and reason codes
- confirmation requests and user decisions
- resource subscription/replay attempts
- provider selection and fallback
- runtime governor interventions

### Correlation
Use correlation IDs / request IDs consistently so a full chain can be reconstructed.

---

## Default Deny Posture

V0 should default to deny in these cases:
- undeclared tool/resource/event/state
- unknown app identity
- invalid or missing capability grant
- missing runtime permission for dangerous operation
- unvalidated arguments
- security policy mismatch
- exported component mismatch
- cross-app surface with weak protection and no user approval
- missing confirmation for high-impact action

---

## User-Defined Automation Policy

User-defined tools/macros are allowed, but start from a lower trust baseline.

### Rules
- user-defined tools may be created declaratively
- arbitrary code execution is not allowed in V0
- mutation-capable user-defined tools require stricter confirmation defaults
- user-defined tools must still pass schema validation and binding checks

---

## Receipts

Every security-relevant decision should produce a receipt.

```kotlin
data class SecurityDecisionReceipt(
    val receiptId: String,
    val requestId: String?,
    val domain: String,
    val subjectId: String,
    val action: String,
    val decision: SecurityDecision,
    val reasonCode: String,
    val timestampEpochMs: Long
)

enum class SecurityDecision {
    ALLOW,
    DENY,
    REQUIRE_CONFIRMATION,
    REQUIRE_PERMISSION,
    REQUIRE_REAUTH,
    SUSPEND
}
```

These receipts are necessary for debugging, evals, and trust.

---

## Definitions of Done

`SECURITY_POLICY_V0.md` is implementation-ready when:

- trust levels are fixed
- capability model is fixed
- confirmation levels are fixed
- exported/IPC rules are fixed
- runtime permission rules are fixed
- tool execution validation gates are fixed
- default-deny conditions are fixed
- receipt model is fixed

---

## Immediate Follow-On Types to Implement

After this doc, the repo should define:

- `SecurityDomain`
- `TrustLevel`
- `CapabilityClass`
- `CapabilityGrantState`
- `ConfirmationLevel`
- `SecurityDecision`
- `SecurityDecisionReceipt`
- `PermissionRequirement`
- `ExecutionGateResult`
- `RateLimitPolicy`
- `PolicyReasonCode`
