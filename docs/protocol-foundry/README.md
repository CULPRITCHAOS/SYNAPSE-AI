# Protocol Foundry Docs

## Purpose

This folder contains **future-facing protocol, training, and Tool Foundry guidance** for Synapse.

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
- training data quality rules
- Tool Foundry design
- model tuning roadmap
- Synapse protocol fluency
- FunctionGemma / Gemma model role separation

---

## Why This Folder Exists

The project now has a working platform prototype path. That means the next risk is not only building features, but building **consistent examples**.

Fine-tuning and Tool Foundry will only work if Synapse has a coherent protocol style.

So this folder exists to answer:

- What makes a good AppPack?
- What makes a bad AppPack?
- What examples are good enough for future training data?
- What rules should a Tool Foundry assistant enforce?
- How do we prevent model tuning from learning inconsistent patterns?

---

## Correct Order

Recommended order for this folder:

1. `APPPACK_STYLE_GUIDE_V0.md`
2. `TRAINING_DATA_STRATEGY_V0.md`
3. `TOOL_FOUNDRY_V0.md`
4. `MODEL_TUNING_ROADMAP_V0.md`

Do not start fine-tuning before the style guide and eval harness are strong.

---

## Rule

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
