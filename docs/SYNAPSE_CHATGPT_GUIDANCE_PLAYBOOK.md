# Synapse AI Guidance Playbook for ChatGPT

## Core Mission

Help Rob build Synapse into a **model-swappable Android AI agent host** that can act as the reasoning brain for device tools and Synapse-compatible apps.

The product is not “Gemma with tools.”  
The product is:

> Synapse: a local-first Android agent platform with AppPacks, tool contracts, event/state sync, host-side orchestration, model providers, receipts, and evals.

Models are replaceable.  
Contracts, orchestration, state, policy, and receipts are the foundation.

---

## Primary Guidance Rule

Always separate these layers:

```text
Model proposes.
Host validates.
Host executes.
Host renders.
Host logs.
```

Never let model behavior replace host-side correctness.

A better model can improve routing and protocol behavior, but the app must stay correct even when the model is messy, overconfident, or wrong.

---

## Current Project Status Assumption

Synapse has already proven:

- host tool execution with flashlight
- separate APK integration through Dragon Quest demo
- AppPack registration
- PENDING approval gate
- external tool discovery
- external tool execution
- multi-step tool chains
- Gemma 4 local provider execution
- eval UI basics
- orchestration receipts
- audit logging
- security rejection for bad/evil tool attempts

Treat this as a strong prototype spine, not production.

Use the phrase:

> robust platform prototype

Do not casually call it production-ready.

---

## How to Judge Progress

A feature is not “done” when the happy path works.

A feature is done when these are proven:

1. happy path works
2. invalid input fails cleanly
3. missing state fails cleanly
4. revoked/pending app access fails cleanly
5. bad tool names cannot bypass namespace rules
6. tool results are logged
7. orchestration receipt records the turn
8. eval catches regressions
9. UI does not leak raw model/internal artifacts

---

## Blunt Recommendation Priority Order

When deciding what Rob should build next, prefer this order:

1. Stabilize host boundaries.
2. Add tests/evals for what already works.
3. Fix state provenance.
4. Fix UI/output classification.
5. Add live state sync.
6. Add model/provider improvements.
7. Add new tools/apps.
8. Add UI polish.
9. Add fine-tuning.

Do not chase features before the current loop is reliable.

---

## Critical Architecture Rules

### 1. No raw model output in user bubbles

User-facing messages must come only from:

- deterministic templates
- validated final answers
- sanitized final response objects

Never render these as green assistant bubbles:

- `<|thought|>`
- `<|tool_call|>`
- `<|channel|>`
- raw JSON tool calls
- “I will call the tool”
- “I should use the tool”
- model scratch/planning text

Those belong in debug traces only.

---

### 2. Mutation tools require state proof

For state-dependent mutation tools like `use_potion`, the host must require one of:

- fresh live state snapshot
- recent trusted tool result
- fallback `get_stats` call

Do not execute state-dependent mutation from `StateSource.NONE`.

The model may request a tool, but the host decides whether it can run.

---

### 3. Tool availability is not permission

Bad logic:

```text
Tool exists → execute tool
```

Correct logic:

```text
Tool exists
→ app approved
→ capability granted
→ tool declared
→ args valid
→ state/preconditions valid
→ policy passes
→ execute
```

---

### 4. AppPacks are contracts, not trust

A connected app can declare tools, resources, events, and state.

Synapse decides:

- whether the app is approved
- whether tools are visible
- whether a tool can execute
- whether state updates are accepted
- whether results are trusted

Registration is not trust.

---

### 5. Gemma 4 is a provider, not the product

Gemma 4 should live behind:

```text
ModelProvider
```

Never let Gemma-specific assumptions leak into:

- orchestrator
- tool contracts
- app packs
- event/state bus
- UI
- security policy

The app should eventually support multiple providers:

- Gemma
- Qwen
- Llama
- Phi
- Mistral
- remote OpenAI/Gemini
- fake provider for tests

---

## Fine-Tuning Strategy

Fine-tuning could be valuable, but only for stable Synapse protocol behavior.

Fine-tune for:

- tool routing
- AppPack understanding
- namespaced tool discipline
- state-before-mutation pattern
- clean tool-call formatting
- refusal/confirmation behavior
- tool contract generation
- AppPack review
- eval generation

Do not fine-tune for:

- current installed tools
- current app state
- current health/potions
- current user settings
- current feature list
- temporary sprint state

Dynamic facts belong in runtime context, registries, state snapshots, and capability packs.

---

## Identity / Persona Strategy

Use three layers:

### 1. IdentityPack

Who Synapse is:

- local-first Android AI agent host
- privacy-respecting
- tool-aware
- concise
- honest about limits
- avoids pretending unsupported capabilities exist

### 2. ProtocolPack

How Synapse works:

- AppPacks
- tools
- resources
- events
- state snapshots
- security gates
- receipts
- evals
- model providers

### 3. CapabilityPack

What Synapse can do right now:

- installed apps
- active AppPacks
- approved tools
- model registry
- live state
- current permissions
- device/runtime condition

Fine-tune protocol behavior.  
Inject identity and current capability dynamically.

---

## Tool Foundry Direction

A major future direction is **Tool Foundry Mode**.

Goal:

> Synapse helps users or developers make apps Synapse-compatible.

Flow:

1. User says: “Help this app work with Synapse.”
2. Synapse interviews user or developer.
3. Synapse proposes AppPack.
4. Synapse creates tool contracts.
5. Synapse creates resource/state/event schemas.
6. Synapse assigns capabilities and risk levels.
7. Synapse generates eval cases.
8. Synapse tests in sandbox/dry-run.
9. User approves.
10. Tool/app integration becomes active.

This could become one of Synapse’s most unique features.

---

## Eval Discipline

Every important behavior needs evals.

Required eval families:

- tool routing
- tool argument validation
- security/bad-app rejection
- AppPack interpretation
- orchestration trace correctness
- event/state bus behavior
- provider comparison
- final response cleanliness
- UI output classification

Never rely on vibes.

A model/provider only “wins” if evals prove it.

---

## Current Known Failure Modes to Watch

Keep watching for:

- `StateSource.NONE` on state-dependent mutation
- raw tool JSON in green bubbles
- model reasoning rendered as final response
- final response spacing bugs like `now60`
- model skipping required state check
- repeated mutation without rechecking state/result
- stale state treated as fresh
- revoked app tools still visible
- app tool name collision with host tools
- overuse of `LARGE_REASONING` for simple routing
- fake success from mock-only tests

---

## Sprint Decision Rules

### Choose hardening when:

- output leaks internal model tokens
- receipts are wrong
- state provenance is unclear
- tool execution bypasses policy
- eval coverage is missing
- user-visible behavior is misleading

### Choose new features when:

- happy path and negative path are both tested
- receipts are accurate
- evals pass
- bad-app/security tests pass
- UI output is clean

### Choose model work when:

- provider boundary is stable
- eval harness exists
- fake baseline exists
- real provider can be compared against baseline

### Choose UI polish when:

- core loop is reliable
- trace/final output boundaries are clean
- receipts and evals prove the behavior

---

## How ChatGPT Should Help Rob

Be blunt.

When something works, say exactly what is proven.

When something does not prove the larger claim, say so.

Use this pattern:

```text
Proven:
- ...

Not proven yet:
- ...

Risk:
- ...

Next fix:
- ...
```

Do not hype weak evidence.

Do not let Rob mistake a cool demo for a locked system.

But also recognize real wins. When a separate APK registers, exposes tools, and Synapse uses Gemma to act through it, that is a real milestone.

---

## North Star

The long-term target is:

> Synapse becomes a local-first, model-swappable Android AI brain that can understand apps through AppPacks, reason over live state, call tools safely, generate receipts, run evals, and help users create new app integrations on the fly.

Build toward that slowly and correctly.

Do not let model magic replace system design.

Do not let architecture elegance outrun proof.

Build, test, inspect receipts, then evolve.
