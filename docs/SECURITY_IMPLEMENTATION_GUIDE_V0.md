# SECURITY_IMPLEMENTATION_GUIDE_V0

## Purpose

This document hardens `SECURITY_POLICY_V0.md` into an implementation-focused guide for Android Studio development.

`SECURITY_POLICY_V0.md` defines the minimum private-build security posture.
This guide translates that posture into:

- explicit trust boundaries
- execution-gate behavior
- Android component rules
- storage and export rules
- event/state/resource security rules
- anti-loop / anti-runaway rules
- review checklist items for implementation sprints

This document is meant to be used while coding.
It is not a public security whitepaper.

---

## Relationship to Existing Docs

### `SECURITY_POLICY_V0.md`
Defines the minimum mandatory private-build security rules.

### `APPPACK_V0.md`
Defines app registration contracts and declared integration surfaces.

### `TOOL_CONTRACT_V0.md`
Defines tool declarations, normalized contracts, bindings, and execution metadata.

### `EVENT_STATE_BUS_V0.md`
Defines events, state snapshots, resources, subscriptions, replay, and session behavior.

### `MODEL_PROVIDER_V0.md`
Defines model provider boundaries and explicitly prevents providers from executing tools directly.

### `RUNTIME_GOVERNOR_V0.md`
Defines runtime downgrade and intervention logic under thermal/battery/provider stress.

This guide does not replace those docs. It hardens how they are implemented together.

---

## Core Security Thesis

> **Synapse is a host-controlled Android agent runtime. Connected apps, model providers, and user-defined tools may request behavior, but they do not get automatic execution authority.**

That rule must stay true in code.

If the implementation lets declarations, model output, or app metadata bypass the host execution gate, the architecture is broken.

---

## Explicit Trust Boundaries

The first hardening step is to name the actors clearly.

### 1. Synapse Host App
This is the trusted authority for:
- activation of app packs
- capability grants
- tool validation
- execution gating
- runtime permission checks
- confirmation prompts
- audit/security receipts
- provider routing

The host is authoritative for policy and execution decisions.

### 2. Connected App
A connected app is **known** after registration, but not fully trusted.
It may:
- declare tools
- declare resources
- emit events
- expose state snapshots
- request capabilities

It may **not**:
- self-grant capabilities
- self-authorize execution
- bypass host confirmation rules
- bind directly to the private model runtime

### 3. User-Defined Tool / Macro
A user-created tool is locally authored but not automatically trusted.
It may:
- define safe macros or bounded actions
- participate in the same execution gate as host/app tools

It may **not**:
- execute arbitrary code in V0
- bypass confirmation defaults for mutation-capable actions
- bypass host validation

### 4. Local Model Provider
A local model provider is private to the host app runtime.
It may:
- load models
- generate text
- request tool calls
- emit structured responses

It may **not**:
- execute tools directly
- enforce security policy
- claim final truth about identity, capabilities, permissions, or success state

### 5. Remote Model Provider
A remote model provider is less trusted than a local provider because it introduces network and data-boundary concerns.
It may:
- generate text
- request tool calls
- emit structured responses

It may **not**:
- directly execute anything
- weaken local policy
- receive data not explicitly allowed by host policy

### 6. Android OS / System Surfaces
Android permissions, exported components, intents, services, accessibility surfaces, and storage APIs remain external enforcement boundaries.

Synapse capabilities are not a substitute for Android security controls.

---

## Non-Negotiable Security Rules

### Rule 1 — Registration is not trust
A registered app pack is merely known to the host.
Nothing is executable until validation, activation, and capability approval are complete.

### Rule 2 — Declaration is not authority
An `AppPack` or `ToolDeclaration` may describe behavior.
It does not grant execution rights.

### Rule 3 — Model output is advisory
Model output may suggest:
- a tool to call
- arguments to use
- a next step
- a structured response

Model output is **never** authoritative for:
- app identity
- granted capabilities
- Android permission state
- execution success
- policy outcomes

### Rule 4 — Host decides execution
All execution-capable actions must pass through the host execution gate.

### Rule 5 — Fail closed on uncertainty
If identity, capability, permission, schema validity, confirmation, or policy state is unclear, the request is denied or paused for explicit user action.

---

## Confused Deputy Protection

This is one of the main real risks in the Synapse design.

### Threat
A connected app may try to cause Synapse to use broader host authority than the app itself should have.

Example:
- app declares a benign-looking tool
- tool path triggers host builtin behavior or device-level mutation
- Synapse becomes an escalation path for the app

### Hard rule

> **A connected app must never cause Synapse to exercise broader authority than the user granted for that app + action class, unless the user explicitly approves a host-level action at execution time.**

### Implementation consequences
- every app-declared tool must map to declared capabilities
- every capability must be approved independently of registration
- host-builtin tools and app-declared tools must remain distinguishable
- dangerous host-level tools should require host-level confirmation even if the requesting app is user-approved
- app identity must be carried into receipts and execution context

### Review question
Before merging any integration path, ask:

> Can this app cause Synapse to do something the app itself was never granted or the user never explicitly approved?

If yes, stop and redesign.

---

## Execution Gate Sequence

All tool execution must pass this exact gate sequence.

### Step 1 — Resolve subject identity
Determine:
- host builtin vs app-declared vs user-defined
- appId if app-originated
- requestId / correlationId
- initiating user/session context if available

### Step 2 — Check declaration existence
Deny if:
- tool is unknown
- tool is disabled
- source app is not registered/active
- tool binding cannot be resolved

### Step 3 — Validate input schema
Deny if:
- required fields missing
- types invalid
- enum values invalid
- additional properties violate strict schema

### Step 4 — Validate capability grants
Deny if:
- required capability was not approved
- tool references undeclared capability
- capability scope does not cover the request path

### Step 5 — Validate Android permission state
Deny or route to permission request if:
- required dangerous runtime permission is not granted
- Android permission state disagrees with assumed Synapse capability state

### Step 6 — Check confirmation policy
Pause for confirmation if:
- side effect class is high impact
- confirmation policy requires first use / every time / host policy decision
- request is background and user presence is required

### Step 7 — Check rate limits / step limits / loop guards
Deny or cool down if:
- repeated tool retries are detected
- max per-request step count is exceeded
- per-tool or per-app rate limit is exceeded
- repeated denial/failure loop is detected

### Step 8 — Execute binding
Only now may the binding execute.

### Step 9 — Validate result
Deny or mark execution failed if:
- result does not match output schema where required
- returned data claims success without host-verifiable success state
- result is malformed or unexpectedly broad

### Step 10 — Emit receipts
Always emit a receipt for:
- deny
- require confirmation
- require permission
- allow of any nontrivial action

---

## Android Component Rules

### Exported components
Every exported component must be intentional.

Rules:
- internal-only components → `android:exported="false"`
- integration entrypoints only → `android:exported="true"`
- sensitive exported surfaces must have both manifest-level and code-level protection where appropriate

### Private model runtime
The local model runtime must remain private to the host app.

Rules:
- do not expose direct third-party binding to the inference runtime
- keep same-app bound service private where possible
- connected apps interact through App Pack / tool/resource/event/state surfaces, not provider internals

### Explicit intents preferred
For service and component interactions:
- prefer explicit intents
- avoid broad implicit exposure for sensitive operations

### Accessibility and screen control
Treat accessibility-driven control as a high-risk path.

Rules:
- accessibility macro paths are not the default preferred integration surface
- require explicit confirmation or stricter policy for mutation-capable accessibility actions
- log all accessibility-driven execution paths with clear source identity

---

## Capability vs Permission vs Confirmation

These are separate layers and must remain separate in code.

| Layer | Example | Granted by |
|---|---|---|
| Synapse capability | `inventory_control`, `screen_control` | Host/user approval |
| Android permission | microphone, camera, notifications, exact alarms, etc. | Android OS/user |
| Confirmation at execution time | flashlight toggle, external-effect action, export action | Host/user |

### Hard rule
A granted Synapse capability does not imply Android permission.
An Android permission does not imply user approval for a dangerous action.

All three layers may be required.

---

## Event / State / Resource Security Rules

The event/state bus is an attack and confusion surface if implemented lazily.

### Rule 1 — App-originated updates are claims, not truth
An app event or state snapshot should be treated as an asserted update from a known source, not as unquestionable truth.

### Rule 2 — Identity must travel with updates
Every inbound app-originated event/resource/state update must remain associated with:
- appId
- source class
- timestamp
- schema version where relevant

### Rule 3 — Reads/subscriptions require capability and policy checks
Do not allow undeclared or unauthorized reads, subscriptions, or replays.

### Rule 4 — High-impact actions should not hinge on one weak signal
A single event should not directly trigger a dangerous action without confirmation/policy checks.

### Rule 5 — Replay must be bounded
Replay should respect:
- retention class
- session rules
- authorization
- logical stream partition

### Rule 6 — Event spam is a security and reliability issue
Noisy apps can poison context and planning.

Mitigations:
- per-app rate limits
- priority filtering
- deduplication
- retention-based pruning
- explicit audit of repeated abusive emission patterns

---

## Anti-Loop and Anti-Runaway Rules

This is both a stability and security concern.

### Required guards
- max orchestration steps per request
- max repeated tool retries per request
- max repeated confirmation prompts per request
- cooldown for failing provider/model pairs
- cooldown for repeatedly denied tool requests
- block or degrade expensive background inference paths

### Recommended defaults for V0
- max tool/planning steps per request: fixed small cap
- repeated denial threshold: small cap before temporary cooldown
- repeated timeout threshold: small cap before provider downgrade

### Hard rule
The model must not be allowed to recursively keep itself alive through tool call loops without host limits.

---

## Storage and Export Rules

### Default storage posture
Store private operational data in app-private storage by default.

Examples:
- app registry metadata
- capability grants
- routines/tools
- audit/security receipts
- event/state persistence
- local model metadata

### Export posture
Treat export as sensitive.

Rules:
- do not silently export logs, receipts, traces, memories, or contracts
- do not write sensitive artifacts to world-readable locations by default
- require explicit user action for exports
- make destination and exposure visible to the user

### Secrets and tokens
Even in V0, treat secrets differently from normal state.

Minimum rule:
- secrets/tokens must not be logged in plain text
- secrets/tokens must not be included in receipts unless explicitly redacted or hashed
- secrets/tokens must not be exported automatically

---

## Result Validation Rules

A tool execution result should not be trusted just because a binding returned something.

### Required checks
- validate output schema when defined
- ensure success/failure is represented explicitly
- avoid blindly feeding malformed output back into the model
- distinguish execution failure from protocol/contract failure

### Hard rule
A tool/provider/app result must not be treated as security truth without host validation.

---

## Background and Autonomy Rules

### Default V0 posture
- background local inference should be conservative
- dangerous actions require user presence or explicit policy support
- nonessential high-cost planning should be foreground-only

### Hard rule
A powerful device does not justify silent background autonomy for dangerous operations.

---

## Security Receipts

Keep receipts simple, but make them useful.

### Minimum fields
- receiptId
- requestId / correlationId if available
- subjectId (tool/app/provider)
- source type
- action
- decision
- reasonCode
- timestamp

### Receipt-worthy events
Emit a receipt for at least:
- allow of a nontrivial action
- denial
- confirmation request
- permission request/denial
- app registration attempt
- capability grant / revocation
- runtime governor intervention affecting execution

### Logging rule
Receipts must not contain raw secrets or silent world-readable paths.

---

## PR / Sprint Security Review Checklist

Use this during implementation sprints.

### New Android surface
- Did this add a new exported component?
- If yes, is `android:exported` explicit?
- If yes, is the surface intentionally exposed and code-gated?

### New tool path
- Did this add a new tool or binding?
- Does it pass the execution gate sequence?
- Does it distinguish declaration from authority?

### New app integration
- Did this add a new App Pack field or integration path?
- Can this app escalate host authority or bypass confirmation?

### New provider path
- Did this let a provider directly execute behavior?
- Did provider-specific assumptions leak into core contracts?

### New data path
- Did this add new event/state/resource data?
- Is source identity preserved?
- Is replay/subscription access gated?

### New storage/export path
- Did this add a new export or backup path?
- Could sensitive logs/traces land in world-readable storage?

### New autonomy/background path
- Did this make dangerous actions more autonomous?
- Is user presence/confirmation still enforced where required?

If any answer is unclear, stop and review before merging.

---

## Definition of Done

This guide is good enough for implementation use when:
- trust boundaries are fixed
- confused deputy protection is explicit
- execution gate sequence is fixed
- capability vs permission vs confirmation distinction is fixed
- event/state/resource security rules are fixed
- anti-loop rules are fixed
- storage/export rules are fixed
- sprint review checklist is fixed

When those are true, Android Studio implementation can use this as an actual reference instead of vague security vibes.
