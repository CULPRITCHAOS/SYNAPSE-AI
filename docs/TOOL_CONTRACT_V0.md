# TOOL_CONTRACT_V0

## Purpose

`ToolContract` is the canonical executable action contract inside Synapse.

It exists to solve a hard problem:

- different app domains need different actions
- models need a normalized tool surface
- apps need a safe way to declare custom actions
- the host must control validation, policy, execution, and logging

If this contract is sloppy, Synapse becomes unsafe and unmaintainable.

---

## Foundation

This design is grounded in current official docs:

1. MCP defines tools as **model-controlled** functions that servers expose, with a unique `name`, a human-readable `description`, and an `inputSchema`. It also defines `tools/list`, `tools/call`, `listChanged`, and tool results that can contain text, images, audio, and embedded resources. It explicitly says there should always be a **human in the loop** for safety-sensitive tool use and that tool annotations/descriptions should be treated as **untrusted** unless they come from a trusted server. citeturn739057search0turn255378view0

2. MCP’s security guidance says servers must validate tool inputs, implement access controls, rate limit invocations, and sanitize outputs; clients should confirm sensitive operations, validate tool results before passing them to the LLM, implement timeouts, and log tool usage. citeturn739057search0turn255378view0

3. OpenAI’s function-calling docs say a function/tool definition contains `name`, `description`, `parameters` as a JSON Schema object, and optional `strict`; OpenAI recommends enabling strict mode so function calls adhere reliably to schema. OpenAI’s structured-output docs also say structured outputs via JSON schema are for shaping the model’s final response, while function calling is for bridging the model to your application’s tools/functions/data. citeturn151992search4turn151992search1turn780632view4

4. Gemini’s function-calling docs recommend clear parameter descriptions, examples, strong typing, and `enum` when values come from a fixed set because that improves accuracy. Gemini’s structured-output docs also separate structured outputs from function calling the same way: structured outputs format the final answer; function calling takes action during the conversation. citeturn780632view0turn780632view1turn780632view2

5. MCP lifecycle and capability negotiation also matter here: capabilities like `tools.listChanged` are negotiated at initialization, and both sides should only use negotiated capabilities. That gives Synapse a clean model for dynamic tool-list updates from connected apps. citeturn255378view1turn780632view7

Implication:

- Synapse should model tools as **declared contracts**, not ad hoc code blobs.
- Tool creation by apps is allowed, but only through **validated declarations** that the host can accept, reject, activate, update, and revoke.
- The execution boundary stays host-controlled.

---

## Relationship to Other Repo Contracts

### `APPPACK_V0`
`AppPack` declares the tools an app wants to expose.

### `TOOL_CONTRACT_V0`
This document defines what a valid tool is and how Synapse should normalize, validate, execute, and observe it.

### `MODEL_PROVIDER_V0`
Providers consume normalized tool descriptors when generating tool calls.

### `SECURITY_POLICY_V0`
Security policy decides whether a validated tool call is allowed to execute.

### `EVENT_STATE_BUS_V0`
Tool execution can emit events and update state snapshots.

---

## Core Design Principle

A tool contract is **not** executable authority.

A tool contract is:
- a declaration of an action surface
- a typed schema for inputs and outputs
- metadata for policy and routing
- a binding target for host-controlled execution

The host still owns:
- capability grants
- confirmation flows
- argument validation
- timeout handling
- rate limiting
- audit logging
- result validation

---

## Module Placement

```text
core/tool-api/
```

Host-side execution adapters can live under:

```text
core/actuation-api/
feature/tool-studio/
runtime/local-service/
sdk/synapse-client-sdk/
```

---

## Top-Level Model

For V0, split tooling into three layers:

1. `ToolDeclaration`
   - what the app or host declares
2. `ToolContract`
   - host-normalized validated tool shape
3. `ToolBinding`
   - how the host actually executes it

This prevents apps from mixing public schema with private execution details.

---

## Layer 1: ToolDeclaration

This is what an app or host feature declares.

```kotlin
data class ToolDeclaration(
    val name: String,
    val description: String,
    val inputSchema: JsonSchema,
    val outputSchema: JsonSchema? = null,
    val requiredCapabilities: List<String>,
    val tags: List<String> = emptyList(),
    val annotations: ToolAnnotations = ToolAnnotations(),
    val executionSpec: ExecutionSpec,
    val source: ToolSource
)
```

### `ToolSource`

```kotlin
enum class ToolSource {
    HOST_BUILTIN,
    APPPACK_DECLARED,
    USER_DEFINED
}
```

### Why this matters

- `HOST_BUILTIN`: Synapse-native tools like device controls
- `APPPACK_DECLARED`: tools contributed by a Synapse-compatible app
- `USER_DEFINED`: future Tool Studio / macro-created tools

This allows multiple tool-authoring paths without losing normalization.

---

## Layer 2: ToolContract

This is the host-normalized validated shape exposed to the model and orchestrator.

```kotlin
data class ToolContract(
    val toolId: String,
    val name: String,
    val description: String,
    val inputSchema: JsonSchema,
    val outputSchema: JsonSchema?,
    val strictInput: Boolean,
    val requiredCapabilities: List<String>,
    val sideEffectClass: SideEffectClass,
    val confirmationPolicy: ConfirmationPolicy,
    val timeoutMs: Long,
    val rateLimit: ToolRateLimit?,
    val resultMode: ResultMode,
    val tags: List<String>,
    val source: ToolSource,
    val trustLevel: ToolTrustLevel,
    val enabled: Boolean
)
```

### `SideEffectClass`

```kotlin
enum class SideEffectClass {
    READ_ONLY,
    USER_VISIBLE_MUTATION,
    APP_STATE_MUTATION,
    DEVICE_STATE_MUTATION,
    EXTERNAL_EFFECT
}
```

### `ConfirmationPolicy`

```kotlin
enum class ConfirmationPolicy {
    NEVER,
    FIRST_USE,
    ALWAYS,
    HOST_POLICY_DECIDES
}
```

### `ResultMode`

```kotlin
enum class ResultMode {
    TEXT_ONLY,
    STRUCTURED_JSON,
    MIXED_CONTENT,
    RESOURCE_EMBED
}
```

### `ToolTrustLevel`

```kotlin
enum class ToolTrustLevel {
    TRUSTED_HOST,
    TRUSTED_APP,
    UNTRUSTED_APP,
    USER_DEFINED
}
```

### Key rule

`ToolContract` is the only shape the model and orchestrator should see.

Apps do **not** get to inject raw execution logic directly into the orchestrator.

---

## Layer 3: ToolBinding

This is how the host executes the tool.

```kotlin
sealed interface ToolBinding {
    data class AppBridge(
        val appId: String,
        val endpoint: String
    ) : ToolBinding

    data class DeepLink(
        val uriTemplate: String
    ) : ToolBinding

    data class AndroidIntent(
        val action: String,
        val packageName: String?,
        val extrasMapping: Map<String, String>
    ) : ToolBinding

    data class BoundService(
        val packageName: String,
        val serviceClassName: String
    ) : ToolBinding

    data class ContentProvider(
        val authority: String,
        val operation: String
    ) : ToolBinding

    data class HostBuiltin(
        val operationId: String
    ) : ToolBinding

    data class AccessibilityMacro(
        val macroId: String
    ) : ToolBinding
}
```

### Why bindings are separate

The model needs a stable tool schema.
The host needs transport/execution details.
Those should not be the same object.

---

## Can an App Create Tool Contracts?

### Yes — but only declaratively and under host control.

A Synapse-compatible app can create tool declarations in two ways:

#### 1. Static declaration in `AppPack`
The app ships tools in its registered `AppPack`.

#### 2. Dynamic tool updates
The app may request addition/removal/update of tool declarations at runtime **only if**:
- the pack declares support for dynamic tools
- Synapse has negotiated/allowed that capability
- the updated tools pass validation
- the user/host policy allows activation

This mirrors the usefulness of MCP’s `tools.listChanged` pattern without giving apps arbitrary execution freedom. MCP explicitly defines a `listChanged` capability and `notifications/tools/list_changed` for dynamic tool surfaces. citeturn739057search0turn780632view7

### No — an app cannot self-grant execution rights.

Apps can declare tools.
Only Synapse can:
- validate
- activate
- expose to models
- bind execution
- enforce policy
- revoke

That is the non-negotiable host boundary.

---

## Tool Declaration Rules

### Required fields
Every declared tool must provide:
- `name`
- `description`
- `inputSchema`
- `requiredCapabilities`
- `executionSpec`

### Strong typing
Use JSON Schema objects for arguments. JSON Schema 2020-12 is the current spec base and is appropriate for validation/documentation. citeturn122788search1turn122788search2

### Descriptions matter
Descriptions should explain purpose and argument format clearly. Both OpenAI and Gemini emphasize that descriptions improve tool-call quality. citeturn151992search4turn780632view1

### Enums beat vague text
If a value is from a bounded set, use `enum` rather than burying allowed values in prose. Gemini explicitly recommends this because it improves accuracy. citeturn780632view0

### Strict input by default
Synapse should normalize tools to strict argument validation when possible. OpenAI explicitly recommends enabling `strict` for reliable schema adherence in function calls. citeturn151992search1turn151992search7

### Bad examples
- `run`
- `do_action`
- `perform`

### Good examples
- `equip_item`
- `create_task`
- `activate_scene`
- `set_flashlight`

---

## ExecutionSpec

`ExecutionSpec` is declaration-time binding metadata that later compiles into a `ToolBinding`.

```kotlin
data class ExecutionSpec(
    val mode: ExecutionMode,
    val target: String,
    val extras: Map<String, String> = emptyMap()
)
```

### `ExecutionMode`

```kotlin
enum class ExecutionMode {
    APP_BRIDGE,
    DEEP_LINK,
    INTENT,
    BOUND_SERVICE,
    CONTENT_PROVIDER,
    HOST_BUILTIN,
    ACCESSIBILITY_MACRO
}
```

### Notes

- `APP_BRIDGE`: preferred for Synapse-native app integrations
- `DEEP_LINK` / `INTENT`: useful but should be treated as coarser execution paths
- `BOUND_SERVICE`: for explicit app/service integrations where the Android surface is intentional
- `CONTENT_PROVIDER`: for structured read/write style interactions where appropriate
- `ACCESSIBILITY_MACRO`: last-resort or assistive automation path, not the preferred primary integration surface

---

## Input Schema Rules

All tool inputs should follow these defaults unless there is a strong reason not to:

```json
{
  "type": "object",
  "properties": {},
  "required": [],
  "additionalProperties": false
}
```

### Rules

1. top-level type should be `object`
2. every property should have a type
3. every property should have a description
4. use `enum` when values are bounded
5. use `required` explicitly
6. default `additionalProperties` to `false`

OpenAI’s structured outputs also only guarantee schema adherence when structured outputs are used; JSON mode alone does not guarantee matching a target schema. That is another reason to keep host-side validation strict. citeturn780632view4turn780632view3

---

## Output Schema Rules

`outputSchema` is optional but strongly recommended.

Why:
- it lets the host validate tool results before passing them to the model
- it improves auditability
- it reduces garbage round-tripping into the LLM

MCP explicitly recommends validating tool results before passing them to the LLM. citeturn739057search0turn255378view0

---

## Tool Result Model

Borrow MCP’s flexibility here, but normalize it.

```kotlin
data class ToolExecutionResult(
    val content: List<ToolResultContent>,
    val isError: Boolean,
    val structuredJson: String? = null,
    val emittedResourceUris: List<String> = emptyList(),
    val executionTimeMs: Long? = null
)
```

### `ToolResultContent`

```kotlin
sealed interface ToolResultContent {
    data class Text(val text: String) : ToolResultContent
    data class Image(val base64: String, val mimeType: String) : ToolResultContent
    data class Audio(val base64: String, val mimeType: String) : ToolResultContent
    data class ResourceRef(val uri: String, val mimeType: String?) : ToolResultContent
}
```

MCP tool results already support text, image, audio, and embedded resources, so this is aligned with a real protocol shape instead of made-up fluff. citeturn739057search0

---

## Errors

Keep two error classes separate, following MCP’s pattern:

### 1. Contract / protocol errors
Examples:
- unknown tool
- malformed arguments
- missing required capability
- unsupported execution mode

### 2. Execution errors
Examples:
- target app unavailable
- API failure
- invalid business data
- timeout
- denied by policy

MCP explicitly separates protocol errors from tool execution errors. citeturn739057search0

---

## Validation Pipeline

Every tool declaration should pass through this pipeline:

1. schema validation
2. uniqueness checks
3. capability-reference validation
4. execution-mode validation
5. binding compilation
6. policy classification
7. trust classification
8. activation decision

Only after that does it become an active `ToolContract`.

---

## Dynamic Tool Updates

Dynamic tools are allowed in V0, but tightly constrained.

### Allowed update actions
- `add`
- `update`
- `remove`
- `disable`
- `enable`

### Required host checks
- app identity still valid
- pack/version compatibility still valid
- updated declaration passes validation
- existing grants still compatible
- user approval if required

### Recommended event

```kotlin
data class ToolListChangedEvent(
    val appId: String,
    val reason: String,
    val changedAtEpochMs: Long
)
```

This mirrors the spirit of MCP’s `notifications/tools/list_changed` without requiring full MCP transport semantics inside the app host. citeturn739057search0turn780632view7

---

## Security Rules

### Rule 1: Descriptions are helpful, not authoritative
MCP explicitly says tool descriptions/annotations should be treated as untrusted unless from a trusted server. Synapse must not treat them as security truth. citeturn780632view5turn780632view6

### Rule 2: Validate inputs before execution
Required by MCP’s security guidance. citeturn739057search0

### Rule 3: Validate outputs before returning them to the model
Also recommended by MCP’s security guidance. citeturn739057search0

### Rule 4: Human in the loop for sensitive operations
MCP says there should always be a human in the loop for tool safety and users should understand what each tool does before authorizing it. citeturn739057search0turn255378view0

### Rule 5: Timeouts and rate limits are part of the contract
MCP recommends timeouts, rate limiting, and audit logs. citeturn739057search0turn255378view1

### Rule 6: Android execution surfaces must be intentionally exposed
If a tool ultimately binds to an Android component, the host must treat exported surfaces carefully. Android’s guidance is to always explicitly set `android:exported`. citeturn780632view8

---

## Tool Studio / User-Defined Tools

User-defined tools are allowed, but V0 should treat them as a special source class:
- `USER_DEFINED`
- lower default trust
- host validation required
- confirmation policies likely stricter
- direct arbitrary code execution disallowed

That means users can compose:
- macros
- deep links
- bounded intents
- safe host operations

But not arbitrary “run this code” tools.

---

## Definition of Done

`TOOL_CONTRACT_V0.md` is implementation-ready when:

- `ToolDeclaration`, `ToolContract`, and `ToolBinding` are fixed
- source classes are fixed
- execution modes are fixed
- input/output/result models are fixed
- validation pipeline is fixed
- dynamic tool update policy is fixed
- one host-builtin tool, one app-declared tool, and one user-defined macro tool can all fit without schema hacks

---

## Immediate Follow-On Types to Implement

After this doc, the repo should define:

- `ToolDeclaration`
- `ToolContract`
- `ToolBinding`
- `ToolSource`
- `ExecutionSpec`
- `ExecutionMode`
- `SideEffectClass`
- `ConfirmationPolicy`
- `ResultMode`
- `ToolTrustLevel`
- `ToolRateLimit`
- `ToolExecutionRequest`
- `ToolExecutionResult`
- `ToolResultContent`
- `ToolValidationResult`
- `ToolListChangedEvent`
