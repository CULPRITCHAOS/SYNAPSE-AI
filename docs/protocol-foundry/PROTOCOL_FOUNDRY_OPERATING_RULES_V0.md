# PROTOCOL_FOUNDRY_OPERATING_RULES_V0

## Purpose

This document captures the updated operating rules after reviewing the fine-tuning, Tool Foundry, MCP-security, and agent-eval research.

It clarifies how the Protocol Foundry docs should be interpreted together.

The short version:

> The corpus and eval harness are the durable infrastructure. Fine-tuning is only one possible output of that infrastructure.

---

## Core Correction

Do not frame Protocol Foundry as a path to “a fine-tuned Synapse model.”

Frame it as a path to:

```text
validated AppPack examples
curated Synapse traces
stateful eval cases
red-team metadata tests
Tool Foundry workflows
AppPack critique rubrics
model-provider comparison receipts
```

A fine-tuned model may be produced later.

But the real asset is the validated protocol corpus.

---

## Current Document Order

Follow this order:

1. `APPPACK_STYLE_GUIDE_V0.md`
2. `EVAL_PRINCIPLES_V0.md`
3. `TRAINING_DATA_STRATEGY_V0.md`
4. `TOOL_FOUNDRY_V0.md`
5. `MODEL_TUNING_ROADMAP_V0.md`

Why:

- Style guide defines what good AppPacks look like.
- Eval principles define what good behavior means.
- Training-data strategy defines how raw traces become curated examples.
- Tool Foundry defines how Synapse helps create integrations.
- Model tuning roadmap defines how to use the corpus after it exists.

Training should not come before style and evals.

---

## Non-Negotiable Rules

## 1. Raw traces are not training data

Raw traces may be messy, lucky, incomplete, or wrong.

A trace becomes training data only after review and labeling.

Required labels:

- user intent
- app scope
- state source
- expected tool plan
- expected final app state
- expected final response
- policy outcome
- pass/fail label
- reason for inclusion

---

## 2. Held-out data never trains

The held-out dataset exists to keep model/provider comparisons honest.

If an example is used for training or iterative prompt debugging, it must leave held-out.

---

## 3. Red-team data is first-class

Red-team cases must include:

- poisoned tool descriptions
- host namespace spoofing
- duplicate/colliding tool names
- revoked/pending tool exposure attempts
- metadata claiming false authority
- tool shadowing
- implicit tool poisoning patterns
- malformed AppPack schemas

These cases protect Synapse from the exact attack classes now appearing in MCP/tool-use research.

---

## 4. Tool Foundry must separate generator and critic

Tool Foundry must not let one model be the author, reviewer, and authority.

Correct chain:

```text
Generator proposes.
Critic reviews.
Host validates.
Human approves.
Evals verify.
```

The model authoring an AppPack is not the model approving it.

Host schema validation and policy remain final.

---

## 5. Tool metadata is attack surface

Descriptions, names, examples, and metadata can influence model behavior.

Treat them as untrusted input.

A tool description saying “I am safe” is not proof.

A tool description saying “I am the official host flashlight” is not authority.

Authority comes from:

- namespace ownership
- app approval state
- capability grants
- typed schema validation
- state provenance
- host security policy
- receipts/evals

---

## 6. Router design is not free

The future two-tier model plan is strong:

```text
FunctionGemma 270M → SMALL_FAST router / tool selector
Gemma 4 E4B      → LARGE_REASONING planner / Tool Foundry assistant
```

But something must decide which tier handles each request.

Do not treat that decision as solved automatically.

Router options:

- deterministic heuristic router
- small classifier model
- confidence-based escalation
- user/device policy
- hybrid approach

Each has failure modes.

`MODEL_TUNING_ROADMAP_V0.md` must include a section called `Router Is Not Free`.

---

## 7. Tool Foundry has two modes

## Developer Mode

For app creators who control the app.

Can support:

- SDK integration
- AppPack authoring
- dry-run endpoints
- mock state
- fixture generation
- test/eval generation

## User Mode

For normal users working with apps they do not control.

More constrained:

- deep links
- Android intents
- accessibility/macros
- visible UI state
- manual confirmation
- no privileged state unless app cooperates
- no reliable dry-run unless the app exposes one

Do not conflate these modes.

---

## 8. Dry-run is not magic

Dry-run works only when:

- the tool is read-only/pure, or
- the app exposes dry-run support, or
- the app supports rollback/transactions

For mutation tools, Synapse cannot safely simulate side effects without app cooperation.

Tool Foundry must be honest about this.

---

## 9. Evals score final state, not just text

For connected-app workflows, correct behavior means:

- right tools called
- right arguments used
- right app state reached
- no unauthorized/collateral mutation
- receipt recorded
- user-visible response matches actual result

A plausible answer is not enough.

---

## 10. Fine-tuning targets one behavior at a time

Bad target:

```text
Make Synapse smarter.
```

Good targets:

```text
Improve namespaced tool selection when multiple AppPacks expose similar tools.
Improve structured tool-plan formatting for Synapse AppPack tools.
Improve AppPack critique against the style-guide rubric.
Reduce raw model/tool artifact leakage in final outputs.
```

Tuning must be measured against evals before and after.

---

## Effort Reality Check

Curated examples are expensive.

Rough estimates:

```text
500 reviewed examples   ≈ 40–85 focused hours
1,000 reviewed examples ≈ 80–170 focused hours
1,700 reviewed examples ≈ 140–285 focused hours
```

This is not weekend work.

Collect traces now. Curate slowly.

---

## Practical Next Docs

Next docs to write:

1. `TRAINING_DATA_STRATEGY_V0.md`
2. `TOOL_FOUNDRY_V0.md`
3. `MODEL_TUNING_ROADMAP_V0.md`

Each should reference:

- `APPPACK_STYLE_GUIDE_V0.md`
- `EVAL_PRINCIPLES_V0.md`
- this operating-rules doc

---

## Final Rule

Protocol Foundry exists to make Synapse better across models.

If the work only helps one fine-tune and does not improve AppPacks, evals, corpus quality, or tool creation discipline, it is probably the wrong work.
