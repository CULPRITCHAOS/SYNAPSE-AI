# ORCHESTRATION_LOOP

## Purpose

`OrchestrationLoop` is the canonical control loop that turns Synapse from a pile of contracts into a working agent host.

It is responsible for taking an input signal—user message, app event, scheduled routine, perception update, or system change—and turning that into one of four outcomes:

1. a final user-visible response
2. one or more validated tool executions
3. a refusal / clarification / confirmation request
4. a state/event update with no direct user response

If this loop is weak, every other repo contract becomes dead paperwork.

---

## Foundation

This design is grounded in current official docs:

1. MCP lifecycle requires initialization and capability negotiation first, and both sides should only use capabilities that were actually negotiated. That means the orchestration loop must not assume tools/resources/subscriptions exist until the connected app and host have agreed on them.

2. MCP tools are model-controlled actions, while resources are application-driven context. That is the key split the loop must respect: the model may request tool calls, but resources/state are assembled and supplied by the host.

3. OpenAI’s function-calling docs define a multi-step loop: send tools to the model, receive tool call(s), execute application-side code, send tool outputs back, then get a final response or more tool calls. OpenAI also explicitly recommends assuming there may be zero, one, or multiple tool calls in a single turn.

4. Gemini’s docs distinguish function calling from structured outputs: function calling is for taking action during the interaction, while structured outputs are for constraining the final answer. That means the orchestration loop needs separate branches for "call a tool" versus "return a schema-shaped final answer."

5. Android’s architecture guidance recommends unidirectional data flow, screen-level state holders, repositories as data-layer entry points, and a single source of truth for each data type. That means the orchestration loop should consume normalized state/resources/events from repositories and publish state updates back through repositories/bus abstractions, not mutate UI or raw data sources directly.

Implication:

- Synapse needs a host-controlled loop that sits between model providers, tool execution, event/state ingestion, policy, and UI.
- The loop must be iterative, not one-shot.
- The loop must support multiple tool calls, replayable state/event context, and a final synthesis pass.

---

## Relationship to Other Repo Contracts

### `APPPACK_V0`
Defines what an app can expose: tools, resources, events, state snapshots, capabilities.

### `MODEL_PROVIDER_V0`
Defines how Synapse talks to a model runtime and receives final text, structured outputs, or tool plans.

### `TOOL_CONTRACT_V0`
Defines what a tool is, how it is validated, and how it is executed.

### `EVENT_STATE_BUS_V0`
Defines how current state and transient events are ingested, stored, replayed, and subscribed to.

### `SECURITY_POLICY_V0`
Defines the final authority on whether a tool/resource/action is permitted.

The orchestration loop ties all of these together.

---

## Module Placement

```text
core/orchestrator/
```

Recommended dependencies:
- `core/common`
- `core/model-api`
- `core/tool-api`
- `core/apppack-api`
- `core/event-api`
- `core/security`
- `core/storage`
- `core/model-registry`
- `core/device-profile`
- `core/runtime-governor`

The orchestrator should not depend on feature `impl` modules.

---

## Core Design Principles

### 1. Host-controlled loop
The model never owns execution. It can request actions. The host validates, decides, executes, logs, and may feed results back.

### 2. Iterative, not one-shot
The loop must support multiple sequential tool calls until it reaches a terminal state. OpenAI’s official tool-calling flow is explicitly multi-step and may include multiple tool calls.

### 3. Context assembly is explicit
Resources/state/events do not magically appear in the prompt. The host assembles them according to task profile, app scope, freshness, permissions, and token budget.

### 4. Policy before execution
No tool runs until the request has passed capability, confirmation, rate-limit, and security checks.

### 5. State flows down, events flow up
UI and connected apps emit events upward into the loop; the loop updates repositories/bus; state holders then expose updated state downward to UI. This is aligned with Android UDF guidance.

### 6. Terminal outputs are explicit
Every turn must end in a clearly classified terminal result:
- final answer
- structured result
- refusal
- confirmation request
- background update only

---

## Loop Inputs

The loop must accept these input classes:

```kotlin
enum class OrchestrationInputType {
    USER_MESSAGE,
    APP_EVENT,
    SCHEDULED_TASK,
    PERCEPTION_UPDATE,
    SYSTEM_EVENT,
    TOOL_RESULT,
    MODEL_CONTINUATION
}
```

### Meaning

- `USER_MESSAGE`: standard conversational/control request
- `APP_EVENT`: connected app emitted an event
- `SCHEDULED_TASK`: routine or scheduled automation fired
- `PERCEPTION_UPDATE`: OCR/screen/accessibility update
- `SYSTEM_EVENT`: battery/thermal/network/session state change
- `TOOL_RESULT`: result from a prior tool execution step
- `MODEL_CONTINUATION`: continuation of an already-running orchestration chain

---

## Loop Context

Before asking a model anything, the orchestrator assembles a `LoopContext`.

```kotlin
data class LoopContext(
    val requestId: String,
    val sessionId: String?,
    val appScope: AppScope?,
    val taskProfile: TaskProfile,
    val deviceProfile: DeviceProfile,
    val runtimeCondition: RuntimeCondition,
    val activeCapabilities: Set<String>,
    val availableTools: List<ToolContract>,
    val resourceBundle: List<ResourceDescriptor>,
    val latestStateSnapshots: List<StateSnapshotRecord>,
    val recentEvents: List<EventEnvelope>,
    val conversationWindow: List<MessageTurn>,
    val userIntentSummary: String? = null,
    val tokenBudget: TokenBudget
)
```

### Why this is explicit

MCP splits tools and resources, and Android recommends clear state/data boundaries via repositories and state holders. The orchestrator should therefore assemble context from known sources, not let providers reach directly into app state or raw stores.

---

## App Scope

```kotlin
data class AppScope(
    val activeAppId: String?,
    val eligibleAppIds: Set<String>,
    val source: AppScopeSource
)

enum class AppScopeSource {
    USER_SELECTED,
    FOREGROUND_APP,
    ROUTINE_TARGET,
    EVENT_ORIGIN,
    HOST_GLOBAL
}
```

This matters because Synapse should not blindly expose every app’s tools/resources on every request.

---

## Token Budget

```kotlin
data class TokenBudget(
    val maxInputTokens: Int,
    val maxOutputTokens: Int,
    val reservedForToolRoundtrip: Int,
    val reservedForFinalSynthesis: Int
)
```

### Rules

- Reserve room for at least one tool roundtrip if tool use is plausible.
- Prioritize current state/resources over stale event history when constrained.
- Prefer compact summaries over dumping full event logs into the model.

---

## Task Profile Selection

Task profile selection should happen before model selection.

```kotlin
enum class TaskProfile {
    CHAT,
    COMMAND_ROUTING,
    PLANNING,
    SCREEN_UNDERSTANDING,
    EXTRACTION,
    TOOL_INDUCTION,
    SAFETY_CHECK
}
```

### Suggested routing heuristic

- `USER_MESSAGE` asking for action → `COMMAND_ROUTING`
- multi-step request / ambiguous strategy request → `PLANNING`
- perception-heavy request → `SCREEN_UNDERSTANDING`
- schema extraction request → `EXTRACTION`
- user teaching a new flow → `TOOL_INDUCTION`
- suspicious / high-risk step → `SAFETY_CHECK`
- otherwise → `CHAT`

Task profiles exist because model/provider quality is workload-dependent.

---

## High-Level Loop

This is the canonical V0 orchestration flow.

```text
1. Receive input
2. Normalize input into request envelope
3. Determine app scope
4. Select task profile
5. Load device/runtime condition
6. Ask runtime governor for constraints
7. Assemble loop context
8. Select provider/model from registry
9. Build model request
10. Invoke provider
11. Classify provider output
12. If tool call(s): validate + execute + feed results back into loop
13. If final answer/structured result/refusal: persist and publish terminal output
14. Update state/event bus and audit receipts
15. End turn or continue if more tool calls are needed
```

This directly follows the multi-step tool-calling pattern documented by OpenAI and the host-controlled tool/resource split documented by MCP.

---

## Canonical State Machine

```kotlin
enum class LoopState {
    RECEIVED,
    NORMALIZED,
    CONTEXT_ASSEMBLED,
    PROVIDER_SELECTED,
    MODEL_INVOKED,
    TOOLS_REQUESTED,
    TOOLS_VALIDATED,
    TOOLS_EXECUTED,
    SYNTHESIS_REQUESTED,
    COMPLETED,
    REFUSED,
    FAILED,
    WAITING_FOR_CONFIRMATION
}
```

### Why a state machine

This prevents the loop from becoming an invisible mess of nested callbacks and retries.

---

## Provider Invocation Rules

### Build request
The orchestrator should send the provider:
- system instructions
- conversation window
- normalized tools
- selected resources/state summaries
- optional response schema
- task metadata

### Important split
- If the goal is a schema-constrained final answer, set `responseSchema`.
- If the goal is tool use, include tools.
- A request can include both, but the loop must still treat tool calls and final structured answers as separate result classes because that is how both OpenAI and Gemini document them.

---

## Provider Output Classification

Following `MODEL_PROVIDER_V0`, provider outputs must be normalized into one of:

```kotlin
sealed interface OrchestrationModelResult {
    data class FinalText(val text: String) : OrchestrationModelResult
    data class StructuredFinal(val json: String) : OrchestrationModelResult
    data class ToolPlan(val calls: List<RequestedToolCall>) : OrchestrationModelResult
    data class Refusal(val reason: String) : OrchestrationModelResult
    data class Failure(val code: String, val retryable: Boolean) : OrchestrationModelResult
}
```

The loop should not care which vendor produced it.

---

## Multi-Tool Step Handling

OpenAI explicitly recommends assuming there may be zero, one, or multiple tool calls.

So Synapse should handle a `ToolPlan` like this:

1. validate all requested tools exist
2. validate arguments against schemas
3. run security/policy checks per tool
4. choose execution strategy:
   - sequential by default
   - parallel only when tools are read-only or otherwise declared safe for parallelism
5. collect results
6. append tool results to the loop context
7. re-enter provider invocation for synthesis or additional tool planning

### Parallelism rule

V0 default:
- **sequential** unless every tool in the batch is `READ_ONLY` and explicitly marked safe for parallel execution

That keeps the initial system boring and debuggable.

---

## Confirmation Branch

If a requested tool requires confirmation:

1. the loop transitions to `WAITING_FOR_CONFIRMATION`
2. it persists a resumable pending action record
3. UI receives a confirmation request
4. user response resumes the same requestId/sessionId chain

This keeps confirmation as part of the orchestration loop rather than a UI hack.

---

## Tool Execution Step

Each tool execution step should create a `ToolExecutionStep` record.

```kotlin
data class ToolExecutionStep(
    val requestId: String,
    val stepId: String,
    val toolName: String,
    val argumentsJson: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val status: ToolStepStatus,
    val resultJson: String? = null,
    val errorCode: String? = null
)

enum class ToolStepStatus {
    REQUESTED,
    VALIDATED,
    DENIED,
    EXECUTING,
    SUCCEEDED,
    FAILED,
    TIMED_OUT
}
```

### Why persist step records

- auditability
- replay/debugging
- evals
- UI traceability

---

## Synthesis Step

After tool execution, the loop may call the provider again for synthesis.

### Use synthesis when
- a user-facing explanation is needed
- multiple tool outputs need to be summarized
- the final answer must be schema-shaped
- the model needs updated context to decide whether another tool is required

### Skip synthesis when
- the tool result itself is already the terminal payload
- the user does not need narrative output
- policy says no additional model pass is needed

This matters for cost, latency, and local thermal load.

---

## Event and State Integration

The loop must both **consume** and **produce** bus updates.

### Consume from bus
- recent event window
- current resource bundle
- freshest state snapshots

### Produce to bus
- emitted tool-result events
- state snapshot updates
- orchestration receipts
- resource list changed notifications where applicable

The bus remains the single source of truth for durable event/state data; the orchestrator only reads projections and writes mutations through the data layer/repositories, per Android architecture guidance.

---

## Repositories and Source of Truth

Android’s data-layer guidance says repositories expose data, centralize changes, resolve conflicts, and define a single source of truth. Other layers should not access raw data sources directly.

So the orchestration loop should interact with:
- `ConversationRepository`
- `ToolRegistryRepository`
- `AppRegistryRepository`
- `EventRepository`
- `StateSnapshotRepository`
- `AuditRepository`
- `ModelRegistryRepository`

### Hard rule

The orchestrator must not talk directly to DAOs, raw databases, or SDK transport clients.

---

## Failure Handling

Every provider/tool step should fail into one of these buckets:

### Provider failure
- load failed
- generation failed
- malformed tool call payload
- provider timeout

### Policy failure
- capability missing
- confirmation denied
- background execution forbidden
- rate limit exceeded

### Tool failure
- tool unknown
- input invalid
- execution binding unavailable
- app bridge failed
- deep link/intent failed

### State/data failure
- required resource missing
- stale snapshot too old
- bus replay unavailable

Failures should create receipts and either:
- retry with another provider/model
- ask user for clarification/confirmation
- degrade gracefully
- hard fail with explanation

---

## Runtime Governor Integration

Before each provider invocation, the loop must consult the runtime governor.

The governor may instruct the loop to:
- downgrade the local model
- switch providers
- reduce output budget
- disable background inference
- block the request entirely

That prevents the orchestration loop from blindly pushing through heavy local inference when runtime conditions are bad.

---

## Session and Resumption

Inspired by MCP transport resumability, Synapse should treat long-running orchestration chains as resumable sessions with stable request/session IDs and cursorable event history.

### V0 rules

- every orchestration run gets a `requestId`
- user-visible conversation/session context may also carry a `sessionId`
- tool-confirmation resumes must reuse the same `requestId`
- event replay should use cursors, not “best effort” timestamp guessing

---

## Pseudocode

```kotlin
suspend fun runLoop(input: OrchestrationInput): OrchestrationTerminalResult {
    val request = normalize(input)
    val appScope = determineAppScope(request)
    val taskProfile = selectTaskProfile(request, appScope)
    val runtimeCondition = runtimeGovernor.currentCondition()
    val governorDecision = runtimeGovernor.preflight(taskProfile, runtimeCondition)
    if (governorDecision.blocks) return blocked(governorDecision)

    val context = assembleContext(
        request = request,
        appScope = appScope,
        taskProfile = taskProfile,
        runtimeCondition = runtimeCondition,
        governorDecision = governorDecision
    )

    val candidate = modelRegistry.selectCandidate(context)
    val provider = providerManager.get(candidate)

    var loopContext = context
    var iteration = 0

    while (iteration < MAX_ITERATIONS) {
        val providerRequest = buildProviderRequest(loopContext, candidate, governorDecision)
        when (val result = provider.generate(providerRequest)) {
            is GenerateResult.FinalText -> return completeText(result, loopContext)
            is GenerateResult.Refusal -> return completeRefusal(result, loopContext)
            is GenerateResult.ToolPlan -> {
                val validated = validateToolPlan(result.calls, loopContext)
                if (validated.requiresConfirmation) return awaitConfirmation(validated, loopContext)
                val toolResults = executeToolPlan(validated, loopContext)
                loopContext = loopContext.withToolResults(toolResults)
            }
            is GenerateResult.Failure -> {
                val fallback = tryFallback(result, loopContext)
                if (!fallback.canContinue) return completeFailure(result, loopContext)
                loopContext = fallback.updatedContext
            }
        }
        iteration++
    }

    return completeFailure(maxIterationFailure(), loopContext)
}
```

---

## Loop Guardrails

1. cap max iterations per request
2. cap max total tool calls per request
3. cap max provider round-trips per request
4. cap context growth per iteration
5. require explicit confirmation branch for sensitive tools
6. persist receipts for every nontrivial state transition

These are basic anti-runaway protections.

---

## Receipts

Every orchestration run should emit an `OrchestrationReceipt`.

```kotlin
data class OrchestrationReceipt(
    val requestId: String,
    val sessionId: String?,
    val taskProfile: String,
    val selectedProviderId: String,
    val selectedModelId: String,
    val totalIterations: Int,
    val totalToolCalls: Int,
    val terminalState: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val notes: String? = null
)
```

### Why receipts matter

- debugging
- evals
- trust
- regression analysis
- provider comparison

---

## Hard Rules

1. The model never executes tools directly.
2. The orchestrator never bypasses security policy.
3. The orchestrator never mutates raw data sources directly; it goes through repositories.
4. The orchestrator never assumes a connected app’s capabilities until they are negotiated and active.
5. The orchestrator treats resources/state and tools as separate surfaces.
6. The orchestrator must assume there can be multiple tool calls in one turn.
7. The orchestrator must support both structured final answers and tool-calling flows as distinct branches.

---

## Definition of Done

`ORCHESTRATION_LOOP.md` is implementation-ready when:

- loop inputs are fixed
- context assembly shape is fixed
- state machine is fixed
- provider invocation branch is fixed
- tool execution branch is fixed
- confirmation branch is fixed
- failure/fallback rules are fixed
- receipt model is fixed
- one demo app request can run end-to-end through the loop without architecture hacks

---

## Immediate Follow-On Types to Implement

After this doc, the repo should define:

- `OrchestrationInput`
- `OrchestrationInputType`
- `LoopContext`
- `AppScope`
- `AppScopeSource`
- `TokenBudget`
- `LoopState`
- `OrchestrationModelResult`
- `ToolExecutionStep`
- `ToolStepStatus`
- `OrchestrationReceipt`
- `PendingConfirmationRecord`
- `ProviderFallbackDecision`
