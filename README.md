# SYNAPSE-AI

## Synapse Overview

**Synapse is a local-first Android AI agent host and app-brain platform.**

It is not supposed to be just another chatbot app.  
It is supposed to be the **reasoning, tool-calling, perception, and control layer** for:
- the device itself
- Synapse-native tools and routines
- third-party apps built to integrate with Synapse
- future local and remote model providers

Synapse should let a phone act like an **agent runtime** that can understand requests, inspect state, select tools, and act safely.

---

## Core Product Intent

Synapse exists to turn Android into a **local AI execution platform**.

The product intent is:

1. **Run locally by default**
   - on-device inference where possible
   - local memory and state by default
   - minimal dependence on cloud services

2. **Do real work**
   - not just answer questions
   - control device features
   - operate tools
   - reason over current state
   - automate sequences
   - integrate with other apps

3. **Act as a reusable brain**
   - Synapse should be able to power multiple Synapse-compatible apps
   - apps should register contracts instead of requiring custom per-app hacks
   - app integrations should be portable across model providers

4. **Stay model-agnostic**
   - Gemma is one possible provider
   - Synapse must not be locked to one model family
   - future models should be testable without rebuilding the product architecture

5. **Respect trust boundaries**
   - permissions
   - capability gates
   - audit logs
   - policy enforcement
   - developer/app identity
   - explicit user approval for sensitive actions

---

## What Synapse Is

Synapse combines:

- conversational interface
- tool-calling agent
- device control surface
- perception and actuation layers
- automation engine
- app-integration platform
- model-provider abstraction
- security and audit layer

A clean one-line description:

> **Synapse is a local-first Android agent host that acts as the AI brain for device tools and Synapse-compatible apps through registered contracts, swappable model backends, and safe execution controls.**

Short version:

> **Synapse turns a phone into a local AI agent that can understand, observe, and act.**

---

## What Synapse Is Not

Synapse is **not** supposed to be:

- a thin wrapper around a single model vendor
- a cloud-first chatbot pretending to be an agent
- a regex command parser sold as full AI
- a random tool launcher with no trust model
- a generic MCP clone that ignores Android-native constraints
- a platform with per-app hardcoded behavior and no schema discipline

---

## Product Principles

### 1. Local-first
Core operation should favor local execution, local storage, and local inference.

### 2. Model-agnostic
The runtime must support multiple model providers without changing the product identity.

### 3. Tool-first
The value comes from what Synapse can **do**, not only what it can say.

### 4. Context-aware
Synapse should reason over:
- user request
- current device state
- current app state
- available tools
- recent history
- policy constraints
- permissions
- event stream
- perception data when available

### 5. Safe-by-default
Sensitive actions should be capability-gated, inspectable, and auditable.

### 6. Extensible
Developers and users should be able to add:
- tools
- app packs
- routines
- examples
- model providers
- perception modules

---

## Primary User Experience

A user should be able to say things like:

- “Turn on the flashlight.”
- “Open Spotify.”
- “Set brightness to 40%.”
- “Run my Focus Mode routine.”
- “Learn how to do this task.”
- “Look at this screen and tell me what I can do.”
- “Use the healing item in this game.”
- “Track the current quest.”
- “Use the productivity app’s task tools.”
- “Switch to a different local model for planning.”

Ideal flow:

1. User expresses intent naturally
2. Synapse assembles relevant context
3. Synapse chooses a model/provider
4. Synapse selects or proposes the right tool
5. Synapse checks capability/policy requirements
6. Synapse executes
7. Synapse explains what happened
8. Synapse stores reusable state when appropriate

---

## Main Feature Areas

### Conversational Interface
- natural language input
- voice input
- voice output
- conversation history
- visible tool call traces
- suggested actions
- mode-aware behavior (chat vs control)

### Device Control
- flashlight
- brightness
- battery status
- network status
- haptics
- settings navigation
- app launching
- volume and connectivity controls where native APIs allow

### App Launching and Deep Links
- open installed apps by name
- resolve aliases
- use deep links
- route users into specific app flows

### Routines / Automation
- preset routines
- custom routines
- chained tools
- progress tracking
- execution history
- local persistence
- future scheduling support

### Tool Studio
- create custom tools without coding
- macro tools
- deep-link tools
- intent tools
- parameterized tools
- exportable schemas

### AI Learning / Teach-by-Demonstration
- user teaches tasks by example
- Synapse observes and proposes action sequences
- user confirms or edits
- saved output becomes reusable tools or routines

### Training Playground / Context Injection
- define tools with examples
- retrieve relevant examples at runtime
- inject examples into model context
- lightweight specialization without full fine-tuning

### Perception Layer
- accessibility-tree understanding
- OCR
- screen semantics
- object/UI detection
- fused state representation
- actionable element extraction

### Actuation Layer
- taps
- swipes
- gesture generation
- humanization options
- action replay/history
- native and accessibility execution paths

### Connected Apps / Platform Mode
- app registration
- capability grants
- tool contracts
- event ingestion
- state sync
- trust scoring
- audit trails
- app-specific reasoning

### Security / Trust
- permission awareness
- capability enforcement
- caller verification
- app identity
- audit logging
- key/token rotation where needed
- confirmation flows for risky actions

### Onboarding / Academy
- guided onboarding
- tutorials
- workflow examples
- progress tracking
- feature discovery

---

## Synapse as a Brain for Other Apps

This is a core product direction, not an optional side feature.

Synapse should function as a **brain/runtime** for apps that are built to work with it.

That means:

- the external app exposes structured tools
- the external app emits structured events
- the external app may expose structured state
- Synapse reasons over that interface
- Synapse chooses actions using the current model provider
- Synapse acts through validated tool contracts, not random app-specific hacks

Examples:

### Game Brain
A game can expose:
- inventory tools
- quest tools
- combat actions
- event updates
- player state

Synapse can then:
- suggest tactics
- use registered actions
- monitor health/resources
- act as an in-game assistant or semi-automated companion

### Productivity Brain
A productivity app can expose:
- tasks
- projects
- notes
- reminders
- workflow actions

Synapse can then:
- summarize state
- create/update tasks
- extract action items
- run multi-step workflows

### Smart Device / Utility Brain
A control app can expose:
- device state
- scene activation
- sensor readings
- automation tools

Synapse can then:
- operate smart devices
- react to events
- coordinate routines across surfaces

---

## App Packs

To support real integrations, Synapse should use **App Packs**.

An **App Pack** is a versioned machine-readable contract that tells Synapse how to interact with an app.

This is stronger than “some JSON tools.”

An App Pack should define:
- app identity
- package name
- display name
- developer identity
- version
- category
- capability requests
- tool schemas
- event schemas
- optional state schema
- examples/hints
- policy metadata
- compatibility metadata
- signature/trust metadata

Example shape:

```json
{
  "appId": "com.example.game",
  "displayName": "Example Game",
  "version": "1.2.0",
  "developer": "Example Studio",
  "category": "game",
  "capabilities": ["inventory_control", "quest_tracking"],
  "tools": [],
  "events": [],
  "state": {},
  "examples": [],
  "policies": {},
  "compatibility": {},
  "signature": {}
}
```

---

## Why App Packs Matter

Without App Packs:
- every new app becomes a custom integration mess
- model prompts get brittle
- tool discovery becomes inconsistent
- trust boundaries get sloppy
- versioning breaks quietly

With App Packs:
- integrations become portable
- tools become typed and inspectable
- events and state become predictable
- model providers stay replaceable
- app behavior becomes auditable
- Synapse becomes a platform instead of a pile of hacks

---

## Model Runtime Direction

### Non-Negotiable Rule
Synapse must support **multiple model backends**.

Do not lock the product to only Gemma.

### Required Runtime Abstraction
Synapse should define a **Model Provider interface** that abstracts:

- model identity
- load/unload
- prompt submission
- streaming output
- structured output / tool-call output
- context window limits
- capability flags
- runtime metrics
- cancellation
- error handling

Conceptual shape:

```ts
type ModelProvider = {
  id: string;
  displayName: string;
  capabilities: {
    toolCalling: boolean;
    multimodal: boolean;
    structuredOutput: boolean;
    streaming: boolean;
    onDevice: boolean;
  };
  load(config: ModelConfig): Promise<void>;
  unload(): Promise<void>;
  generate(request: GenerationRequest): Promise<GenerationResult>;
  stream(request: GenerationRequest, onToken: (token: string) => void): Promise<GenerationResult>;
};
```

### Provider Classes
Synapse should support:

#### A. On-device native providers
- Gemma-family local runtimes
- future Android-native runtimes
- other local instruction / tool-calling models

#### B. Local service providers
- local companion service
- device-side runtime process
- LAN-hosted test runtime

#### C. Remote fallback providers
Optional, not primary.
Useful for:
- comparison
- unsupported devices
- heavier planning tasks
- eval and benchmarking

### Capability-Based Selection
Model choice should be driven by:
- tool-calling quality
- latency budget
- RAM/storage limits
- multimodal support
- JSON reliability
- token throughput
- power usage
- offline availability
- policy behavior

Synapse should route by **capability and task**, not by brand loyalty.

---

## Suggested Task Profiles for Model Routing

Different tasks may use different providers.

Suggested task profiles:
- command routing
- general conversation
- planning
- screen understanding
- structured extraction
- learning / tool induction
- safety check / policy pass
- fallback provider path

---

## Architecture Layers

### 1. UI Layer
- chat
- routines
- tools
- perception monitor
- settings
- academy
- connected apps manager

### 2. Agent Orchestrator
- request intake
- context assembly
- model selection
- tool planning
- policy checks
- result synthesis
- memory/state update hooks

### 3. Tool Layer
- device tools
- app tools
- macro tools
- external integration tools
- normalized execution contract

### 4. App Registry
- registered apps
- capability grants
- trust state
- install status
- version compatibility
- developer metadata

### 5. App Pack Registry
- schema validation
- version tracking
- signature/trust metadata
- compatibility checks

### 6. Event/State Bus
- app-to-Synapse events
- state updates
- retention policy
- rate limits
- provenance

### 7. Perception Layer
- OCR
- accessibility
- object/UI detection
- fused scene state
- actionability extraction

### 8. Actuation Layer
- deep links
- intents
- accessibility actions
- taps/swipes
- app-defined tool calls

### 9. Model Runtime Layer
- provider interface
- model lifecycle
- model registry
- streaming
- structured output adapters
- benchmarking hooks

### 10. Security Layer
- permission checks
- capability enforcement
- policy engine
- audit logs
- key/token management
- trust scoring

### 11. Storage Layer
- local settings
- routines
- tools
- examples
- audit logs
- app metadata
- model metadata
- benchmark receipts
- optional encrypted memory/state

---

## Android / Mobile-Native Constraints

Synapse should be treated as **MCP-inspired but mobile-native**.

Why generic MCP alone is not enough:
- Android has permission and lifecycle constraints
- local execution must respect battery and memory limits
- app identity and install state matter
- event/state streaming matters
- background control is risky
- trust boundaries need to be explicit
- tool calling must align with mobile execution realities

So Synapse should support:
- MCP-style tool schemas
- plus app registration
- plus capabilities
- plus events
- plus state sync
- plus policies
- plus signatures/trust metadata
- plus Android-specific execution rules

---

## Critical Footguns

### 1. Per-App Hardcoding
If every app needs custom logic in Synapse core, the platform will collapse into maintenance hell.

### 2. Vague Tools
Generic tools like `do_action` are garbage.  
Tools need specific names, typed parameters, and clear semantics.

### 3. Hallucinated Actions
The model must not invent tools, state fields, or app behaviors.  
Everything callable should come from validated contracts.

### 4. Event Spam
Noisy event streams will destroy context quality.  
You need rate limits, priority, deduplication, and retention rules.

### 5. Malicious App Packs
Unsigned or over-permissioned app contracts can become a privilege escalation surface.  
Default to least privilege, visible trust state, revocation, and auditability.

### 6. Game Abuse / Cheating Surface
If games expose control hooks, some uses will drift toward automation or cheating.  
You need policy separation between:
- assistive companion mode
- user-triggered automation
- autonomous control

Do not blur those lines.

### 7. Expo-Only Thinking
A serious local model runtime and deeper Android control path will require native boundaries.  
Do not pretend Expo alone solves the final architecture.

---

## Definition of Done

Synapse should only be considered “real” when:

1. It can run at least one local model on-device.
2. It can switch between at least two model backends without major architecture changes.
3. The model can call validated tools through a stable interface.
4. The user can see what tools were called and what happened.
5. A third-party app can register with Synapse through a clean contract.
6. Synapse can ingest app tools, events, and state.
7. Sensitive actions are policy-gated and auditable.
8. User-taught tools and routines persist and remain reusable.
9. The system still degrades gracefully when advanced perception or model features are unavailable.

---

## Immediate Build Direction

The next serious steps should be:

1. Define the **Model Provider interface**
2. Define the **App Pack schema**
3. Define the **Tool Execution contract**
4. Define the **Event/State bus**
5. Define the **policy / capability model**
6. Build one real vertical slice:
   - one game demo app
   - one productivity or utility demo app
   - two model providers
   - one approval flow
   - one audit trail
   - one state/event loop

That is the fastest way to prove whether Synapse is a real platform or just a cool concept.

---

## Current Project Status (Scaffolded)

The project has been initialized with a robust, contract-first multi-module architecture.

### 🏗️ Architecture Stack
- **Build Logic**: Centralized convention plugins (`synapse.kotlin.library`, `synapse.android.library`, `synapse.android.compose`, `synapse.android.hilt`).
- **Core APIs**: Model-agnostic contracts (`core:model-api`), App Capability contracts (`core:apppack-api`), and Execution contracts (`core:tool-api`).
- **Reasoning**: `feature:orchestrator` implements a ReAct reasoning loop with real-time "Thought Trace" event streaming.
- **Local Intelligence**: `core:model-gemma` utilizes MediaPipe LLM Inference for on-device execution.
- **Agency**: `core:tool-system` provides physical device control (initial tool: Flashlight).

### 🚀 Getting Started (S26 Ultra / S24 Ultra Target)

1. **Sync Project**: Open in Android Studio and sync Gradle.
2. **Download Model**: 
   - Get `gemma-2b-it-cpu-int4.bin` (or similar MediaPipe-compatible Gemma weights).
   - Push to device: `adb push gemma.bin /data/local/tmp/gemma.bin`.
3. **Configure Path**: Update `GemmaModelProvider` or DI graph with the model path.
4. **Run**: Deploy `:app:mobile-host` to your device.

---

## Governing Principle

> **Models are replaceable. Tools, app contracts, perception, orchestration, and safety are the product.**