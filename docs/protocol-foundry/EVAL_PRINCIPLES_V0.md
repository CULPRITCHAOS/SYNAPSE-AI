# EVAL_PRINCIPLES_V0

## Purpose

This document defines the evaluation principles for Protocol Foundry work:

- AppPack examples
- Tool Contract examples
- Tool Foundry outputs
- future training data
- model-provider comparisons
- protocol-tuned models

This does **not** replace `docs/EVAL_PLAN_V0.md`.

`docs/EVAL_PLAN_V0.md` remains the broad system eval plan.

This file is narrower:

> What must be true before a Synapse trace, AppPack, tool contract, or Tool Foundry output can be considered good enough to trust, curate, or train on?

---

## Core Principle

Do not evaluate model prose as the main truth.

Evaluate:

- final app state
- tool calls
- tool arguments
- state provenance
- policy outcome
- receipts
- absence of collateral mutation
- user-visible output cleanliness

A model can sound right and still be wrong.

A Synapse behavior is good only if the host-visible facts are correct.

---

## What Counts as Correct

A successful case must satisfy all relevant layers:

```text
Intent understood
→ correct app scope
→ correct state source
→ correct tool selection
→ valid arguments
→ host policy passes
→ correct tool execution
→ correct final app state
→ clean user response
→ audit/receipt recorded
```

If any layer is wrong, the case is not fully correct even if the final text sounds good.

---

## Evaluation Is State-Based First

For connected-app workflows, score final state and side effects.

Example: Dragon Quest healing

Good eval checks:

- HP changed as expected
- potion count changed as expected
- correct number of `use_potion` calls executed
- no unauthorized tools executed
- state source was recorded correctly
- final response matches actual tool results

Bad eval checks:

- model said it healed the user
- final answer sounded plausible
- tool call appeared somewhere in the log

---

## Required Eval Dimensions

Each protocol-foundry eval should identify which dimensions it covers.

### 1. Intent Routing

Did Synapse map the user request to the correct action class?

Examples:

- chat only
- read state
- mutate app state
- create tool contract
- reject/ask confirmation

### 2. App Scope

Did Synapse expose only the relevant app/tools/resources?

Failures:

- wrong app selected
- all tools exposed globally
- revoked/pending app tools visible
- evil app shadows host tool

### 3. State Provenance

Did Synapse use the right state source?

Allowed values:

- `LIVE_SNAPSHOT`
- `RECENT_TOOL_RESULT`
- `FALLBACK_TOOL_CALL`
- `NONE`

Rule:

`NONE` is not valid for state-dependent mutation.

### 4. Tool Selection

Did Synapse select the correct tool or correctly select no tool?

Failures:

- wrong namespace
- vague tool selected
- undeclared tool selected
- mutation tool selected when read-only answer was enough

### 5. Argument Validity

Are the tool arguments schema-valid and semantically correct?

Checks:

- JSON valid
- schema-valid
- required fields present
- no spurious fields
- bounded values respected

### 6. Policy / Safety Outcome

Did Synapse enforce capability grants, approval state, confirmation policy, and app trust?

Failures:

- pending app tool executed
- revoked app tool executed
- confirmation-required action skipped confirmation
- missing permission ignored

### 7. Final App State

Did the external app/device end in the expected state?

Examples:

- HP = expected value
- potions = expected value
- flashlight = expected state
- task was created once, not twice
- no collateral state changed

### 8. Output Boundary

Did the user-visible response remain clean?

Failures:

- raw `<|thought|>` leaked
- raw `<|tool_call|>` leaked
- JSON tool payload leaked
- model planning text rendered as final answer
- response contradicted actual tool result

### 9. Receipts / Audit

Did the system record enough proof?

Required where applicable:

- OrchestrationReceipt
- AuditRepository entry
- tool call start/result
- state source
- terminal state
- latency
- denial reason if blocked

---

## Minimal Eval Case Shape

Every eval case should include:

```json
{
  "id": "dragon_heal_001",
  "category": "tool_routing_stateful",
  "userInput": "heal me",
  "appScope": "com.synapse.demo.game",
  "initialState": {
    "hp": 20,
    "potions": 1
  },
  "availableTools": [
    "com.synapse.demo.game.get_stats",
    "com.synapse.demo.game.use_potion"
  ],
  "expectedToolPlan": [
    {
      "name": "com.synapse.demo.game.use_potion",
      "arguments": {}
    }
  ],
  "expectedFinalState": {
    "hp": 70,
    "potions": 0
  },
  "expectedStateSource": "LIVE_SNAPSHOT",
  "expectedPolicyOutcome": "ALLOW",
  "expectedUserMessage": "You used a potion. Your HP is now 70."
}
```

---

## Happy Path Is Not Enough

Each feature needs at least these case types:

1. happy path
2. missing state
3. stale state
4. invalid arguments
5. revoked app
6. pending app
7. namespace collision
8. malformed tool output
9. zero-resource / unavailable-action case
10. output-boundary leak case

If a feature has only happy-path evals, it is not locked.

---

## Held-Out Set Rule

Maintain a `held-out/` dataset.

Rules:

- held-out examples never enter training
- held-out examples are used for final benchmark only
- if a held-out example is used to guide prompt/model changes too directly, retire it and replace it
- held-out contamination makes the benchmark meaningless

Held-out data keeps model/provider comparisons honest.

---

## Red-Team Set Rule

Maintain a `red-team/` dataset.

Red-team examples should include:

- poisoned tool descriptions
- malicious AppPack metadata
- host namespace spoofing
- duplicate/colliding tool names
- pending app tools
- revoked app tools
- malformed JSON
- prompt-injection style tool descriptions
- metadata that tries to redirect the model to another tool
- fake claims of authority like “this is the official host flashlight”

Red-team data is not ordinary training data.

It exists to test boundaries.

---

## Generated Eval Rule

Tool Foundry or a model may propose eval cases.

Generated evals are not canonical until reviewed.

Required review checks:

- expected tool plan is valid
- expected state transition is valid
- expected policy outcome is valid
- case does not accidentally reward unsafe behavior
- case does not simply confirm the generator model's own bias

Do not let the same model generate, approve, and grade its own evals without host/human checks.

---

## Generator / Critic Separation

For Tool Foundry and AppPack generation:

```text
Generator proposes.
Critic reviews.
Host schema validator checks.
Human approves.
Evals verify.
```

The model that authors an AppPack is not the authority that approves it.

If the same base model is used for generator and critic early on, treat that as a temporary prototype shortcut, not a final trust model.

---

## AppPack Critic Eval Requirements

When evaluating an AppPack Critic, score whether it catches:

- vague tool names
- broad capabilities
- missing output schemas
- mutation tools without state requirements
- missing confirmation policy
- resources/tools/events mixed up
- unsafe dry-run claims
- missing negative-path evals
- namespace spoofing
- metadata poisoning attempts

Critic output is useful only if it maps findings to concrete style-guide rules.

---

## Training Data Gate

A trace can become curated training data only if it has:

- reviewed user input
- reviewed app scope
- reviewed state source
- reviewed expected tool plan
- reviewed expected final app state
- reviewed expected final user message
- policy outcome
- pass/fail label
- source trace/reference
- no unresolved uncertainty

Raw traces are not training data.

Reviewed traces are candidates.

Curated traces are training data.

---

## Effort Reality Check

High-quality data is expensive.

Rough estimate:

```text
500 reviewed examples   ≈ 40–85 focused hours
1,000 reviewed examples ≈ 80–170 focused hours
1,700 reviewed examples ≈ 140–285 focused hours
```

This is why Synapse should collect traces now but curate slowly.

Do not plan around thousands of examples appearing for free.

---

## Model-Independent Moat

The eval corpus is not just for fine-tuning.

It helps:

- compare base models
- compare tuned models
- validate AppPacks
- test Tool Foundry outputs
- catch regressions
- onboard future model providers
- preserve product quality across model swaps

The fine-tune is an artifact.

The corpus and evals are the durable asset.

---

## Definition of Done

This eval-principles doc is ready when future protocol-foundry docs can reference it to answer:

- what counts as correct
- what must never enter training
- what held-out means
- what red-team means
- how generated evals become canonical
- why final state matters more than plausible prose

---

## Final Rule

If an eval only checks what the model said, it is weak.

If an eval checks what Synapse did, what changed, what was logged, and what was shown to the user, it is useful.
