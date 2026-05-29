# Protocol Foundry Docs

## Purpose

This folder contains **future-facing protocol, corpus, Tool Foundry, and model-tuning guidance** for Synapse.

These docs do **not** replace the core implementation guides in `docs/`.

The core guides remain the current implementation spine:

- `APPPACK_V0.md`
- `TOOL_CONTRACT_V0.md`
- `EVENT_STATE_BUS_V0.md`
- `ORCHESTRATION_LOOP.md`
- `SECURITY_POLICY_V0.md`
- `EVAL_PLAN_V0.md`
- `MODEL_PROVIDER_V0.md`
- `REPO_MODULE_SCAFFOLDING_V0.md`

This folder is for the next layer:

- AppPack style rules
- protocol-specific eval principles
- training data quality rules
- Tool Foundry design
- model tuning roadmap
- Synapse protocol fluency
- FunctionGemma / Gemma model role separation

---

## Updated Framing

Protocol Foundry is **not** the fine-tuning folder.

Protocol Foundry is the model-independent infrastructure layer for Synapse's future intelligence work.

The durable asset is:

```text
validated AppPack examples
curated Synapse traces
stateful eval cases
red-team tool metadata tests
AppPack style rubrics
Tool Foundry workflows
model-provider comparison receipts
```

A fine-tuned model is one possible artifact produced from that corpus.

The corpus and evals are the moat. The fine-tune is only an expression of the moat.

---

## Why This Folder Exists

The project now has a working platform prototype path. That means the next risk is not only building features, but building **consistent examples and trustworthy evals**.

Fine-tuning and Tool Foundry will only work if Synapse has:

- a coherent protocol style
- an eval rubric that defines what good means
- curated examples instead of raw logs
- held-out data that never enters training
- red-team data that specifically probes AppPack/tool attacks
- clear separation between model generation, model critique, host validation, and human approval

So this folder exists to answer:

- What makes a good AppPack?
- What makes a bad AppPack?
- What does a good AppPack/tool eval measure?
- What examples are good enough for future training data?
- What rules should a Tool Foundry assistant enforce?
- How do we prevent model tuning from learning inconsistent or unsafe patterns?

---

## Correct Order

Recommended order for this folder:

1. `APPPACK_STYLE_GUIDE_V0.md`
2. `EVAL_PRINCIPLES_V0.md`
3. `TRAINING_DATA_STRATEGY_V0.md`
4. `TOOL_FOUNDRY_V0.md`
5. `MODEL_TUNING_ROADMAP_V0.md`

The ordering matters.

Do not create training data until eval principles define what good means.

Do not build Tool Foundry until the AppPack style guide and eval principles exist.

Do not start fine-tuning before the style guide, eval principles, and training-data strategy are strong.

---

## Required Dataset Folders Later

When training data work begins, use this structure:

```text
training-data/
  raw-traces/
  reviewed/
  curated/
  rejected/
  held-out/
  red-team/
  apppack-examples/
  tool-contract-examples/
  eval-cases/
  labeling-guides/
```

Rules:

- raw traces are not training data
- held-out examples never enter training
- red-team examples probe security and metadata poisoning
- generated evals require human review before becoming canonical

---

## Tool Foundry Authority Rule

Tool Foundry must separate these roles:

```text
Generator proposes.
Critic reviews.
Host validates.
Human approves.
Evals verify.
```

The model that authors an AppPack must not be the authority that approves it.

Descriptions and metadata are hints, not authority.

Host schema validation, capability grants, state provenance, receipts, and evals remain the enforcement layer.

---

## Router Is Not Free

The future two-tier model idea is promising:

```text
FunctionGemma 270M → SMALL_FAST routing/tool-selection candidate
Gemma 4 E4B      → LARGE_REASONING planning/Tool Foundry candidate
```

But routing itself is a system design problem.

A small model may not know when it should escalate. A heuristic router can be brittle. A large model for everything wastes latency, battery, and thermal budget.

`MODEL_TUNING_ROADMAP_V0.md` must treat routing as a first-class problem, not a free side effect.

---

## Core Rule

The core system remains:

```text
Model proposes.
Host validates.
Host executes.
Host renders.
Host logs.
```

The Protocol Foundry layer teaches models and humans how to speak Synapse's protocol better.

It does not weaken host-side enforcement.
