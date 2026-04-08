# EVAL_PLAN_V0

## Purpose

`EVAL_PLAN_V0` defines how Synapse will measure whether the system is actually working.

This document exists because LLM systems are variable and nondeterministic. Traditional software tests are necessary but not sufficient. Synapse needs a repeatable evaluation plan for:
- tool routing
- tool argument correctness
- orchestration correctness
- provider comparison
- app-pack integration correctness
- safety / refusal / confirmation behavior
- state/event handling quality

If this plan does not exist, the repo will drift into vibe-based development.

---

## Foundation

This plan is based on current official guidance:

### OpenAI eval best practices
Key guidance used here:
- adopt eval-driven development
- evaluate early and often
- design task-specific evals that match the real system
- log everything
- automate when possible
- continuously evaluate on every change
- prefer pairwise comparisons, classification, and scoring against criteria rather than open-ended grading when possible
- evaluate the parts of the architecture where nondeterminism enters: prompts, outputs, tool selection, argument extraction, workflows, agents, and handoffs

### OpenAI graders guidance
Key guidance used here:
- use a mix of grader types: string checks, similarity, model graders, code execution when relevant
- model graders need careful prompt iteration and human calibration
- guard against grader hacking / reward hacking
- prefer smooth scoring where useful, not only binary pass/fail

### Gemini structured-output guidance
Key guidance used here:
- structured outputs are for schema-constrained final answers
- function calling is for taking action during the interaction
- schemas should use strong typing, clear descriptions, and enums where domains are bounded

### Android/Kotlin testing guidance
Key guidance used here:
- `StateFlow` should generally be tested as a state holder by asserting on its current `value`
- `SharedFlow`/event streams should be tested as broadcast/update streams
- state and event handling should be tested separately

Implication:

Synapse evals must combine:
- deterministic unit/integration tests
- benchmark-style metrics
- targeted LLM/application evals
- human review for calibration
- continuous regression gating

---

## Eval Design Principles

### 1. Evals are task-specific
Do not use one giant generic “agent score.”
Each critical task gets its own eval family.

### 2. Measure the architecture where nondeterminism enters
For Synapse, that means:
- provider output quality
- tool selection
- argument extraction
- confirmation/refusal behavior
- orchestration step transitions
- app-pack interpretation

### 3. Prefer automated scoring where possible
Use deterministic graders first.
Use model graders only where deterministic checks are insufficient.

### 4. Human review calibrates automation
Human review is not optional forever, but it should be used to calibrate and validate the automated evals instead of grading everything manually.

### 5. Continuous evaluation beats one-time testing
Every meaningful contract or provider change should run the relevant eval set.

---

## Eval Layers

Synapse should maintain five layers of evaluation.

### Layer 1: Deterministic unit tests
Purpose:
- validate pure logic
- validate contract parsing
- validate repository and reducer behavior

Examples:
- `AppPack` validation
- tool schema validation
- capability gating logic
- runtime governor decisions
- state snapshot freshness logic

### Layer 2: Deterministic integration tests
Purpose:
- validate end-to-end host logic without real LLM variability

Examples:
- fake provider returns known tool plan
- orchestrator executes tool chain correctly
- event/state bus updates after tool execution
- confirmation branch resumes correctly

### Layer 3: LLM/system evals
Purpose:
- measure real provider behavior in task-specific situations

Examples:
- tool routing accuracy
- tool argument extraction accuracy
- schema adherence for structured outputs
- refusal behavior under disallowed actions
- app-pack tool/resource interpretation

### Layer 4: Benchmark evals
Purpose:
- compare providers/models/device conditions quantitatively

Examples:
- first-token latency
- total latency
- tokens/sec
- structured output success rate
- tool-call success rate
- thermal downgrade behavior

### Layer 5: Human calibration evals
Purpose:
- verify automated graders and rubrics are not lying
- catch evaluator blind spots

Examples:
- sample 50 tool-routing failures and review manually
- compare model-grader judgments to human judgments
- review refusal and confirmation behavior for realism and safety

---

## Core Eval Families

### 1. Tool Routing Evals
Question:
Did the provider/orchestrator select the correct tool or no-tool path?

Dataset item fields:
- user input
- app scope
- available tools
- expected outcome class (`tool_name` or `no_tool`)
- optional explanation

Metrics:
- exact match on tool name
- top-1 accuracy
- false positive tool rate
- false negative tool rate

Grader types:
- deterministic exact match
- optional pairwise/model grader for ambiguous cases

### 2. Tool Argument Precision Evals
Question:
When the correct tool was selected, were the arguments correct?

Dataset item fields:
- user input
- selected tool
- expected argument JSON

Metrics:
- schema-valid output rate
- exact field match rate
- per-field precision/recall for extracted arguments
- argument omission rate
- spurious argument rate

Grader types:
- JSON schema validation
- exact match where fields are deterministic
- field-level comparison

### 3. Structured Output Evals
Question:
When Synapse asks for a structured final answer, does the provider return valid schema-conforming output?

Dataset item fields:
- prompt
- response schema
- reference answer or grading rubric

Metrics:
- valid JSON rate
- schema adherence rate
- refusal-detection accuracy if refusal is allowed in schema-aware mode
- field completeness rate

Grader types:
- schema validation
- string/similarity graders
- model grader for semantic quality when needed

### 4. Refusal / Confirmation Evals
Question:
Does Synapse refuse or request confirmation when it should?

Dataset item fields:
- input
- active capabilities
- confirmation policy
- expected behavior (`allow`, `refuse`, `require_confirmation`, `require_permission`)

Metrics:
- exact classification accuracy
- unsafe-allow rate
- over-refusal rate
- missed-confirmation rate

Grader types:
- deterministic classification check

### 5. Orchestration Loop Evals
Question:
Does the orchestrator take the right sequence of steps?

Dataset item fields:
- input signal
- provider output(s)
- expected loop state sequence
- expected terminal outcome

Metrics:
- correct terminal state rate
- correct tool-step sequence rate
- max-iteration breach rate
- confirmation-resume correctness
- fallback correctness

Grader types:
- deterministic trace assertions
- receipt comparison

### 6. AppPack Interpretation Evals
Question:
Can Synapse correctly understand and use a registered app’s declared tools/resources/events/state?

Dataset item fields:
- `AppPack`
- request
- expected tools/resources that should be considered

Metrics:
- app-scope accuracy
- declared-tool visibility correctness
- undeclared-tool exposure rate (must be zero)
- resource-selection correctness

Grader types:
- deterministic contract-based assertions

### 7. Event / State Bus Evals
Question:
Does the bus correctly separate state, events, subscriptions, and replay?

Metrics:
- state snapshot freshness correctness
- event replay correctness
- cursor resume correctness
- no cross-app bleed
- subscription update delivery correctness

Grader types:
- deterministic integration tests
- state holder assertions
- event stream assertions

### 8. Provider Benchmark Evals
Question:
Which provider/model/device path is best for each task profile?

Metrics:
- warmup latency
- first-token latency
- total latency
- tokens/sec
- structured-success rate
- tool-call success rate
- failure rate
- degraded-runtime behavior

These are not quality-only evals. They are routing inputs.

---

## Dataset Strategy

Following eval best practices, Synapse should use a mixed dataset strategy.

### A. Curated seed set
Small hand-written examples for each critical behavior.

Purpose:
- establish initial correctness
- cover obvious happy paths and obvious failures

### B. Edge-case set
Include cases that stress the architecture:
- typos and misspellings
- multi-intent requests
- ambiguous short commands
- long conversations
- long tool output
- multiple tool calls
- conflicting user/system intent
- jailbreak-style attempts to bypass tool policy
- malformed app/resource/tool data

### C. Production-derived set
As the private build is used, log real failures and near-failures, then promote them into the eval corpus.

### D. Synthetic augmentation
Use models to generate candidate eval examples, but do not trust synthetic data blindly. Promote only reviewed examples into the canonical dataset.

---

## Dataset Files

Recommended repo structure:

```text
core/evals/
  datasets/
    tool_routing/
      seed.jsonl
      edge_cases.jsonl
    tool_args/
      seed.jsonl
      edge_cases.jsonl
    structured_output/
      seed.jsonl
    refusal_confirmation/
      seed.jsonl
    orchestration/
      traces.jsonl
    apppack/
      apppack_cases.jsonl
    bus/
      bus_cases.jsonl
    benchmarks/
      benchmark_prompts.jsonl
```

Use JSONL so each item is independently diffable and easy to append.

---

## Grader Strategy

Use the simplest grader that can correctly score the task.

### Tier 1: deterministic graders
Use first whenever possible.

Examples:
- string equality
- expected label match
- JSON schema validation
- exact JSON field comparison
- trace comparison
- receipt comparison
- code execution / assertion-based grading

### Tier 2: metric/similarity graders
Use when exact match is too brittle.

Examples:
- text similarity
- fuzzy matching
- ROUGE-style metrics where reference text matters

### Tier 3: model graders
Use only when deterministic grading is insufficient.

Examples:
- pairwise quality comparison for ambiguous natural-language outputs
- rubric-based scoring of planning quality
- semantic grading of summaries or explanations

Rules:
- prefer pairwise or classification-style grading over open-ended “is this good?” prompts
- calibrate model graders against human judgments
- watch for verbosity bias and position bias
- use model graders as helpers, not as unquestioned truth

---

## Human Calibration Plan

Start small and targeted.

### V0 process
- every new eval family starts with human-reviewed seed examples
- every new model grader prompt is calibrated on a reviewed sample
- every major provider switch gets a human spot-check sample
- every security-related eval family gets human review of failures

### Sample cadence
For private build V0:
- review 20–50 failures per eval family when thresholds are missed
- review 20 random passes to catch false confidence

This is enough to keep the eval harness honest without drowning in manual work.

---

## Canonical Thresholds for V0

These are starting thresholds, not permanent law.

### Tool routing
- top-1 accuracy >= 0.90 on seed set
- top-1 accuracy >= 0.80 on edge-case set
- false positive tool rate <= 0.03

### Tool arguments
- schema-valid rate >= 0.98
- exact required-field correctness >= 0.90

### Structured outputs
- valid JSON rate >= 0.99
- schema adherence >= 0.97

### Refusal / confirmation
- unsafe-allow rate = 0 on seed set
- missed-confirmation rate <= 0.01

### Orchestration traces
- correct terminal state >= 0.95
- max-iteration breach rate = 0 on seed set

### AppPack interpretation
- undeclared-tool exposure rate = 0
- app-scope correctness >= 0.95

### Bus behavior
- no cross-app bleed = 0 failures
- cursor resume correctness >= 0.99 on deterministic cases

### Benchmarks
Benchmark thresholds should be tracked per device/provider/task profile, not globally hardcoded. Store measured values as receipts and compare deltas over time.

---

## CI / Regression Plan

### On every PR
Run:
- deterministic unit tests
- deterministic integration tests
- fake-provider orchestration evals
- contract validation evals

### On provider/runtime changes
Also run:
- tool routing evals
- structured output evals
- refusal/confirmation evals
- benchmark smoke set on supported device if available

### On app-pack / integration changes
Also run:
- apppack interpretation evals
- bus/state integration evals
- tool visibility/undeclared-tool checks

### Merge rules
A change should block merge if:
- any security eval family regresses past threshold
- undeclared-tool exposure is nonzero
- schema-valid rate drops below threshold
- orchestration trace correctness drops below threshold
- deterministic tests fail

---

## Receipts and Logging

Every eval run should generate a durable receipt.

```kotlin
data class EvalRunReceipt(
    val evalRunId: String,
    val evalFamily: String,
    val providerId: String?,
    val modelId: String?,
    val deviceProfileId: String?,
    val datasetName: String,
    val totalCases: Int,
    val passedCases: Int,
    val failedCases: Int,
    val keyMetricsJson: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long
)
```

### Why receipts matter
- compare providers over time
- detect regressions
- inspect failures later
- tie eval results to exact repo/runtime state

---

## Anti-Patterns

Do not do these:

1. "it feels better" as the main eval strategy
2. one giant generic score for the whole app
3. only happy-path evals
4. exact-match grading for tasks that are inherently semantic unless exactness is actually required
5. model graders with no human calibration
6. changing providers/prompts/contracts without rerunning the relevant eval families
7. benchmarking on emulator and treating it as real-device truth

---

## Initial Implementation Order

### Step 1
Implement deterministic eval families first:
- contract validation
- orchestration traces
- refusal/confirmation classification
- apppack visibility checks

### Step 2
Implement tool routing and argument evals with at least one real provider.

### Step 3
Implement structured output evals.

### Step 4
Implement provider benchmark receipts.

### Step 5
Add model graders only where deterministic grading is not enough.

This keeps the eval harness boring and credible before adding fancy judge models.

---

## Definition of Done

`EVAL_PLAN_V0.md` is implementation-ready when:
- eval families are fixed
- dataset strategy is fixed
- grader strategy is fixed
- initial thresholds exist
- CI trigger rules exist
- receipt format is fixed
- one fake-provider eval path and one real-provider eval path can both be run without architectural hacks

---

## Immediate Follow-On Types to Implement

After this doc, the repo should define:
- `EvalFamily`
- `EvalCase`
- `EvalCaseResult`
- `EvalMetric`
- `EvalThreshold`
- `EvalDatasetRef`
- `EvalRunReceipt`
- `BenchmarkReceipt`
- `HumanReviewSample`
- `FailureBucket`
