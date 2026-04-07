# MODEL_PROVIDER_V0

## Purpose

`ModelProvider` is the canonical runtime contract between Synapse and any AI model backend.

It exists to solve four problems:

1. Synapse must not be hardwired to one model vendor or one runtime.
2. On-device Android runtimes and remote APIs expose different capabilities.
3. Tool calling and structured outputs are related but not identical features.
4. The rest of the app needs one stable interface for loading models, generating outputs, streaming, and requesting tool calls.

If this contract is weak, the whole repo turns into provider-specific hacks.

---

## Foundation

This provider spec is grounded in current official docs:

1. Google’s MediaPipe LLM Inference for Android is optimized for **high-end Android devices** such as **Pixel 8 and Samsung S23 or later**, and does **not reliably support emulators**. That makes it a real local-AI path for Synapse, but a real-device path, not an emulator-first one. citeturn693795search4

2. LiteRT’s current guidance says the **`CompiledModel` API** is the recommended modern runtime for high-performance on-device inference, with new performance and accelerator features prioritized there over the older `Interpreter` API. citeturn625820search0turn625820search3turn625820search8

3. Android’s bound-service guidance says that if the service is private to your own app and runs in the same process as the client, extending **`Binder`** is the **preferred technique**. citeturn113681search0

4. OpenAI’s docs distinguish **function calling** from **structured outputs**: function calling is for connecting the model to tools/functions/data in your system, while structured outputs via JSON schema are for making the model’s final response conform to a schema. OpenAI also says structured outputs ensure schema adherence and recommends using structured outputs over JSON mode when possible. citeturn693795search2turn693795search3turn693795search6

5. Google’s Gemini docs make the same distinction: structured outputs are for formatting the final response, while function calling is for taking action during the conversation. Gemini also recommends clear descriptions, strong typing, and enums where domains are bounded. citeturn113681search1turn113681search4

Implication:

- Synapse should expose **tool calling** and **structured outputs** as **separate provider capabilities**.
- Synapse should treat local Android runtimes as first-class providers.
- Synapse should keep the actual inference runtime behind a private host boundary.

---

## Design Principles

### 1. Provider-neutral core
The orchestrator, features, and app integrations must depend on the `ModelProvider` contract, not on vendor SDKs directly.

### 2. Capability-driven routing
The orchestrator should choose providers by capability, health, and benchmark receipts—not by brand loyalty.

### 3. Tool calling is not the same as structured output
Some providers are good at tool calling, some are good at final structured responses, some are good at both. The contract must represent that explicitly. citeturn693795search3turn113681search1

### 4. Local-first where viable
On-device providers should be first-class citizens in the registry and routing logic, especially on high-capability devices. MediaPipe and LiteRT are the initial local targets. citeturn693795search4turn625820search0

### 5. Host-controlled execution
Providers may **request** tool calls. Providers must never directly execute tools or mutate device/app state.

### 6. Bounded runtime surface
The provider runtime should sit behind a private service boundary within Synapse. Android’s guidance makes a same-app same-process `Binder` service the preferred starting point. citeturn113681search0

---

## Module Placement

```text
core/model-api/
runtime/local-service/
runtime/provider-mediapipe/
runtime/provider-litert/
runtime/provider-openai/
runtime/provider-gemini/
runtime/provider-fake/
```

The shared contract lives in:

```text
core/model-api/
```

All provider implementations must depend on this module.

---

## Provider Classes

```kotlin
enum class ProviderClass {
    ON_DEVICE_NATIVE,
    LOCAL_SERVICE,
    REMOTE
}
```

### `ON_DEVICE_NATIVE`
Examples:
- MediaPipe LLM Inference
- LiteRT `CompiledModel`

### `LOCAL_SERVICE`
Examples:
- private in-app service
- sidecar local process on the same device

### `REMOTE`
Examples:
- OpenAI API
- Gemini API

These classes matter because startup cost, reliability, privacy, latency, and thermal behavior differ substantially across them. citeturn693795search4turn625820search0turn113681search0

---

## Provider Capabilities

`ModelProvider` must expose declared capabilities.

```kotlin
data class ProviderCapabilities(
    val onDevice: Boolean,
    val streaming: Boolean,
    val toolCalling: Boolean,
    val structuredOutput: Boolean,
    val multimodalTextVision: Boolean,
    val supportsJsonSchemaResponse: Boolean,
    val supportsStrictToolSchemas: Boolean,
    val maxContextTokens: Int?,
    val supportsTokenCounting: Boolean,
    val supportsEmbeddings: Boolean = false
)
```

### Why these matter

- `toolCalling`: provider can request application-side tool execution. OpenAI and Gemini both document function calling/tool calling for this role. citeturn693795search2turn113681search4
- `structuredOutput`: provider can produce schema-constrained final responses. OpenAI and Gemini both document structured outputs separately from function calling. citeturn693795search3turn113681search1
- `streaming`: important for responsive UI and incremental structured parsing; OpenAI documents streaming for structured outputs and function-call arguments. citeturn693795search6
- `onDevice`: distinguishes private local runtimes from network-dependent ones.
- `supportsJsonSchemaResponse`: some providers can natively target JSON-schema-constrained outputs; some cannot or only do so partially.
- `supportsStrictToolSchemas`: needed because tool-call fidelity varies by provider.

---

## Top-Level Contract

```kotlin
interface ModelProvider {
    val providerId: String
    val providerClass: ProviderClass
    val modelId: String
    val displayName: String
    val capabilities: ProviderCapabilities

    suspend fun load(spec: ModelLoadSpec): LoadResult
    suspend fun unload(): UnloadResult
    suspend fun health(): ProviderHealth

    suspend fun generate(request: GenerateRequest): GenerateResult

    suspend fun stream(
        request: GenerateRequest,
        onEvent: suspend (StreamEvent) -> Unit
    ): GenerateResult

    suspend fun countTokens(request: TokenCountRequest): TokenCountResult
    suspend fun benchmark(request: BenchmarkRequest): BenchmarkResult
}
```

### Design notes

- `load` / `unload`: needed because local providers have explicit lifecycle and memory pressure.
- `health`: needed because providers fail in different ways and the orchestrator must not guess.
- `generate`: single non-streaming call.
- `stream`: token/event streaming path.
- `countTokens`: optional capability, but worth standardizing because providers expose token behavior differently.
- `benchmark`: required for device-aware routing and provider comparison.

---

## Lifecycle Types

```kotlin
data class ModelLoadSpec(
    val localModelPath: String? = null,
    val remoteConfig: RemoteConfig? = null,
    val maxOutputTokensDefault: Int = 1024,
    val temperatureDefault: Double = 0.2,
    val topPDefault: Double = 0.9,
    val preferredAccelerator: AcceleratorHint? = null
)

enum class AcceleratorHint {
    AUTO,
    CPU,
    GPU,
    NPU
}
```

`preferredAccelerator` exists because LiteRT and other on-device runtimes explicitly care about accelerator choice and prioritization. LiteRT’s `CompiledModel` path is built around hardware acceleration and a modern runtime interface for that purpose. citeturn625820search0turn625820search1turn625820search3

```kotlin
data class LoadResult(
    val success: Boolean,
    val warmupMs: Long?,
    val errorCode: String? = null,
    val message: String? = null
)

data class UnloadResult(
    val success: Boolean,
    val message: String? = null
)

data class ProviderHealth(
    val healthy: Boolean,
    val loaded: Boolean,
    val message: String? = null,
    val lastCheckedEpochMs: Long
)
```

---

## Request Contract

```kotlin
data class GenerateRequest(
    val taskProfile: TaskProfile,
    val systemPrompt: String,
    val conversation: List<MessageTurn>,
    val tools: List<ToolDescriptor> = emptyList(),
    val resources: List<ResourceDescriptor> = emptyList(),
    val responseSchema: JsonSchema? = null,
    val maxOutputTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val metadata: Map<String, String> = emptyMap()
)
```

### `tools`
Application-side callable tools exposed to the provider.

### `resources`
Context surfaced to the provider. This is aligned with MCP’s distinction between tools and resources, though Synapse uses its own host contracts above the provider layer. citeturn107516view0turn242236view0

### `responseSchema`
Used when Synapse wants a final structured answer, not a tool call. This follows the function-calling vs structured-output split documented by both OpenAI and Gemini. citeturn693795search3turn113681search1

---

## Task Profiles

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

### Why task profiles exist

Providers are not interchangeable across all workloads. Some are better at:
- fast routing
- strict schema extraction
- large-context planning
- multimodal interpretation
- low-latency local control

The orchestrator should route by task profile rather than sending every request to one giant default model.

---

## Tool Descriptors

`ToolDescriptor` should be provider-facing and normalized.

```kotlin
data class ToolDescriptor(
    val name: String,
    val description: String,
    val inputSchema: JsonSchema,
    val strict: Boolean = true
)
```

### Design rules

- tools must be normalized before they reach the provider
- provider must never see undeclared tools
- `strict=true` should be the default where the provider supports it

This reflects current tool-calling guidance from OpenAI and Gemini, both of which rely on structured function definitions with schema-like arguments. citeturn693795search2turn693795search5turn113681search4

---

## Stream Contract

Streaming should be event-based rather than raw-token-only.

```kotlin
sealed interface StreamEvent {
    data class OutputTextDelta(val text: String) : StreamEvent
    data class StructuredFieldDelta(val fieldPath: String, val valueFragment: String) : StreamEvent
    data class ToolCallDelta(val toolName: String, val argumentsFragment: String) : StreamEvent
    data class RefusalDelta(val text: String) : StreamEvent
    data class Error(val code: String, val message: String) : StreamEvent
    data object Done : StreamEvent
}
```

Why:
- OpenAI explicitly documents streaming for structured outputs and streamed function-call arguments. citeturn693795search6
- Different providers emit different event shapes, so Synapse should normalize them before UI/orchestrator consumption.

---

## Result Contract

```kotlin
sealed interface GenerateResult {
    data class FinalText(
        val text: String,
        val structuredJson: String? = null,
        val usage: UsageStats,
        val latencyMs: Long
    ) : GenerateResult

    data class ToolPlan(
        val calls: List<RequestedToolCall>,
        val usage: UsageStats,
        val latencyMs: Long
    ) : GenerateResult

    data class Refusal(
        val reason: String,
        val usage: UsageStats,
        val latencyMs: Long
    ) : GenerateResult

    data class Failure(
        val errorCode: String,
        val message: String,
        val retryable: Boolean
    ) : GenerateResult
}
```

### Key rule

Providers may only ever return:
- final text
- structured final response
- requested tool calls
- refusal
- failure

They must not execute actions themselves.

---

## Usage / Metrics Contract

```kotlin
data class UsageStats(
    val inputTokens: Int?,
    val outputTokens: Int?,
    val totalTokens: Int?,
    val toolCallCount: Int,
    val firstTokenMs: Long? = null
)
```

The exact token accounting will vary by provider. That is fine. The point is to normalize what can be measured.

---

## Benchmark Contract

```kotlin
data class BenchmarkRequest(
    val taskProfile: TaskProfile,
    val prompt: String,
    val responseSchema: JsonSchema? = null,
    val tools: List<ToolDescriptor> = emptyList()
)

data class BenchmarkResult(
    val providerId: String,
    val modelId: String,
    val success: Boolean,
    val warmupMs: Long?,
    val totalLatencyMs: Long,
    val firstTokenMs: Long?,
    val tokensPerSecond: Double?,
    val structuredSuccess: Boolean?,
    val toolCallAccuracyNotes: String? = null,
    val errorCode: String? = null
)
```

Benchmarking is required because model/provider quality is empirical. Synapse should not assume one provider is better across all tasks.

---

## Local Runtime Boundary

### Recommended V0 approach

Use a **private bound service** inside the Synapse host app.

Reason:
- Android says extending `Binder` is the preferred technique when the service is private to your own app and in the same process. citeturn113681search0

So the first concrete local runtime structure should be:

```text
runtime/local-service/
```

Responsibilities:
- provider lifecycle management
- load/unload
- generation and streaming dispatch
- health checks
- cancellation
- benchmark execution

### Hard rule

Third-party apps must **not** bind directly to the inference runtime.

They talk to Synapse through app contracts and approved tool/resource/event/state surfaces. The provider runtime remains private to the host.

---

## Initial Provider Set

### `provider-fake`
Purpose:
- deterministic tests
- CI
- UI development without real inference

### `provider-mediapipe`
Purpose:
- primary Android on-device LLM path

Grounding:
- MediaPipe LLM Inference is the first real Android local path Google documents for high-end devices. citeturn693795search4

### `provider-litert`
Purpose:
- accelerator-first on-device runtime path
- future-facing local runtime experiments

Grounding:
- LiteRT `CompiledModel` is the recommended modern runtime choice for top on-device performance. citeturn625820search0turn625820search3

### `provider-openai`
Purpose:
- remote structured-output and tool-calling benchmark/fallback provider

Grounding:
- OpenAI explicitly supports function calling and structured outputs, and clearly distinguishes their use cases. citeturn693795search2turn693795search3

### `provider-gemini`
Purpose:
- remote structured-output and function-calling benchmark/fallback provider

Grounding:
- Gemini explicitly documents function calling and structured outputs as separate features with different purposes. citeturn113681search1turn113681search4

---

## Hard Rules

1. Features and UI must never call vendor SDKs directly.
2. Providers must never directly execute tools.
3. Providers must never be trusted to enforce security policy.
4. Tool calling and structured output must remain separate capability flags.
5. Local providers must be benchmarked on real devices, not emulators. citeturn693795search4
6. The private local runtime boundary must stay host-controlled. citeturn113681search0
7. Provider-specific quirks must be normalized before leaving the provider module.

---

## Definition of Done

`MODEL_PROVIDER_V0.md` is implementation-ready when:

- the top-level interface is fixed
- provider classes are fixed
- capability fields are fixed
- request/result/stream contracts are fixed
- benchmark contract is fixed
- at least one local provider and one remote provider can implement it without type hacks

---

## Immediate Follow-On Types to Implement

After this doc, the repo should define:

- `ProviderClass`
- `ProviderCapabilities`
- `ModelLoadSpec`
- `LoadResult`
- `UnloadResult`
- `ProviderHealth`
- `GenerateRequest`
- `TaskProfile`
- `ToolDescriptor`
- `StreamEvent`
- `GenerateResult`
- `UsageStats`
- `RequestedToolCall`
- `BenchmarkRequest`
- `BenchmarkResult`
- `TokenCountRequest`
- `TokenCountResult`
