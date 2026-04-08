# EVENT_STATE_BUS_V0

## Purpose

`EventStateBus` is the canonical runtime contract for how Synapse ingests, stores, publishes, subscribes to, and replays app/device/host updates.

It exists because Synapse needs to handle **two fundamentally different kinds of information**:

1. **Current state**
   - what is true right now
   - examples: inventory state, active quest, current thermostat mode, current task list summary

2. **Transient events**
   - things that happened over time
   - examples: player died, task completed, scene activated, sensor triggered

If these get mixed into one sloppy stream, Synapse will become unreliable fast.

---

## Foundation

This design is grounded in current official docs:

1. MCP resources are **application-driven** context surfaces, identified by URIs, and can optionally support `subscribe` for change notifications and `listChanged` for changes to the available-resource list. MCP defines `resources/list`, `resources/read`, `resources/subscribe`, `notifications/resources/updated`, and `notifications/resources/list_changed`. MCP also says servers **must validate resource URIs**, should implement access controls for sensitive resources, and should check permissions before operations. citeturn669705view0turn963417view4

2. MCP lifecycle requires capability negotiation at initialization and says both parties should **only use capabilities that were successfully negotiated**. citeturn669705view6

3. MCP transports define resumability and redelivery concepts using **event IDs**, **`Last-Event-ID`**, and **session IDs** (`Mcp-Session-Id`). It also says a server must not broadcast the same message across multiple streams and can use resumability to mitigate message loss. citeturn963417view0turn963417view3

4. Android’s Flow guidance says `StateFlow` is a **state-holder observable flow** that emits the current and new state updates and is a good fit for observable mutable state; `SharedFlow` emits values to **multiple consumers** and is a configurable generalization useful for broadcast-style events. citeturn606902view0turn606902view1

5. Android app architecture guidance recommends **unidirectional data flow** where state flows down and events flow up. Android also recommends a **single source of truth** for each data type, exposing immutable data and centralizing mutations. Repositories are the entry points to the data layer, and higher layers should not access raw data sources directly. citeturn606902view2turn230953search6turn230953search0

Implication:

- Synapse should treat **state snapshots/resources** differently from **transient events**.
- Synapse should expose current state through **state-holder streams** and transient updates through **broadcast event streams**.
- The bus needs **subscriptions**, **resource updates**, **list-changed notifications**, **replay cursors**, and **session-aware delivery**.
- Durable state and replayable events need a **single source of truth**, not ad hoc in-memory mutation. citeturn230953search4turn230953search7

---

## Core Design Principle

> **State is not an event. Events are not resources. Resources are not transport sessions.**

Synapse needs all four concepts, and they must stay separate:

- **Event** = something happened
- **State snapshot** = current value of a bounded state surface
- **Resource** = an addressable context surface, often read on demand
- **Session / stream** = a delivery channel with ordering and resumability rules

---

## Relationship to Other Repo Contracts

### `APPPACK_V0`
Declares resources, events, and state snapshots that an app may expose.

### `TOOL_CONTRACT_V0`
Tools may emit events and mutate state.

### `MODEL_PROVIDER_V0`
Providers consume normalized resources/state and may trigger tool calls that cause updates.

### `SECURITY_POLICY_V0`
Security policy decides whether reads/subscriptions/replays are allowed.

### `ORCHESTRATION_LOOP.md`
The orchestrator reads from the bus to assemble context and publish results.

---

## Module Placement

```text
core/event-api/
core/storage/
core/orchestrator/
feature/connected-apps/
feature/perception/
```

Recommended main implementation module:

```text
core/event-api/
```

Bus persistence should live behind repositories in the data layer, not inside UI or provider code. Android’s architecture guidance recommends repositories as the entry points and a single source of truth for each data type. citeturn230953search0turn230953search6

---

## Bus Capabilities

Inspired by MCP resource capabilities, Synapse should support these bus-level features:

```kotlin
data class EventStateBusCapabilities(
    val resourceSubscribe: Boolean,
    val resourceListChanged: Boolean,
    val eventReplay: Boolean,
    val sessionResumption: Boolean,
    val stateSnapshots: Boolean
)
```

### Meaning

- `resourceSubscribe`: clients may subscribe to changes for a specific resource or snapshot
- `resourceListChanged`: clients may be notified when resource inventory changes
- `eventReplay`: clients may replay events from a cursor
- `sessionResumption`: clients may resume a broken subscription stream from a cursor/session token
- `stateSnapshots`: current-value snapshot reads and streams are supported

### Rule

Following MCP lifecycle rules, Synapse should only use capabilities that were actually negotiated/allowed for the connection or integration surface. citeturn669705view6

---

## Main Runtime Types

### EventEnvelope

```kotlin
data class EventEnvelope(
    val eventId: String,
    val appId: String?,
    val source: EventSource,
    val name: String,
    val payloadJson: String,
    val priority: EventPriority,
    val retention: RetentionClass,
    val emittedAtEpochMs: Long,
    val correlationId: String? = null,
    val causationId: String? = null,
    val schemaVersion: String? = null
)
```

### EventSource

```kotlin
enum class EventSource {
    HOST,
    CONNECTED_APP,
    TOOL_EXECUTION,
    PERCEPTION,
    SYSTEM,
    USER_ACTION
}
```

### EventPriority

```kotlin
enum class EventPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}
```

### RetentionClass

```kotlin
enum class RetentionClass {
    EPHEMERAL,
    SHORT,
    SESSION,
    AUDIT
}
```

### StateSnapshotRecord

```kotlin
data class StateSnapshotRecord(
    val snapshotId: String,
    val appId: String?,
    val name: String,
    val uri: String,
    val stateJson: String,
    val schemaVersion: String? = null,
    val maxAgeMs: Long,
    val updatedAtEpochMs: Long
)
```

### ResourceRecord

```kotlin
data class ResourceRecord(
    val uri: String,
    val appId: String?,
    val name: String,
    val mimeType: String?,
    val subscribable: Boolean,
    val freshnessTtlMs: Long?,
    val listVersion: Long
)
```

### BusCursor

```kotlin
data class BusCursor(
    val streamId: String,
    val lastEventId: String?,
    val issuedAtEpochMs: Long
)
```

### BusSession

```kotlin
data class BusSession(
    val sessionId: String,
    val clientId: String,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long?
)
```

---

## State vs Event Mapping

This is the critical behavioral rule:

### Use `StateSnapshotRecord` when
- the value represents **current truth**
- a late subscriber should immediately get the latest value
- old intermediate updates do not all need to be replayed

Examples:
- current quest
- current room temperature
- current inventory summary
- current active project summary

### Use `EventEnvelope` when
- the time-ordered occurrence matters
- consumers may need replay/history
- the event should not overwrite prior events

Examples:
- quest completed
- item consumed
- door unlocked
- sensor triggered

### Use `ResourceRecord` when
- Synapse or a connected component needs an addressable URI-based context surface
- contents may be read on demand
- the resource may or may not be subscribable

Examples:
- `synapse-app://com.example.game/inventory`
- `synapse-app://com.example.tasks/active-project`

This follows MCP’s model: resources are addressable URI-based context, with optional subscriptions and list changes. citeturn669705view0

---

## Flow Mapping for Android

### Current state → `StateFlow`

Android explicitly says `StateFlow` is a state-holder observable flow that emits the current and new state updates and is a good fit for observable mutable state. citeturn606902view0

So Synapse should expose state snapshots like this:

```kotlin
interface StateSnapshotStream<T> {
    val state: StateFlow<T>
}
```

### Transient events → `SharedFlow`

Android says `SharedFlow` emits values to all consumers and is a configurable generalization of `StateFlow`. citeturn606902view1

So Synapse should expose transient event streams like this:

```kotlin
interface EventStream {
    val events: SharedFlow<EventEnvelope>
}
```

### Why this split is correct

- `StateFlow` always has a current value
- `SharedFlow` fits fan-out delivery for multiple subscribers
- using one primitive for both would be sloppy and semantically wrong

---

## Unidirectional Data Flow

Android recommends UDF where state flows down and events flow up. citeturn606902view2

For Synapse, that means:

1. apps, tools, perception, and system surfaces **emit events upward** into the bus
2. repositories normalize and persist them
3. state reducers/projectors produce current snapshots/resources
4. orchestrator and UI read immutable state/resources **downward**

This keeps the bus from becoming a random bidirectional mutation mess.

---

## Single Source of Truth

Android’s architecture guidance says each data type should have a single source of truth and repositories should be the entry point to data. citeturn230953search6turn230953search0

So for Synapse:

- persistent event log is the source of truth for replayable events
- latest durable state snapshot store is the source of truth for current state
- in-memory hot streams are caches/projections, not authoritative storage

### Persistence recommendation

- small config/session metadata: DataStore
- larger structured replay/state data: Room or equivalent durable DB

Android explicitly positions DataStore for small datasets and Room for larger/complex data. citeturn230953search7

---

## Subscription Model

Synapse should support two subscription types:

### 1. Resource subscription
Aligned with MCP `resources/subscribe`.

```kotlin
data class ResourceSubscription(
    val sessionId: String,
    val uri: String,
    val createdAtEpochMs: Long
)
```

Behavior:
- subscriber receives `ResourceUpdated` notifications when the resource changes
- subscriber may optionally pull the latest value via `readResource(uri)`

### 2. Event stream subscription
Synapse-native extension for replayable transient events.

```kotlin
data class EventSubscription(
    val sessionId: String,
    val filter: EventFilter,
    val replayFrom: BusCursor? = null
)
```

### `EventFilter`

```kotlin
data class EventFilter(
    val appIds: Set<String> = emptySet(),
    val names: Set<String> = emptySet(),
    val priorities: Set<EventPriority> = emptySet(),
    val minimumRetention: RetentionClass? = null
)
```

---

## Update Notifications

Borrow MCP semantics here.

### Resource updated

```kotlin
data class ResourceUpdated(
    val uri: String,
    val updatedAtEpochMs: Long
)
```

Equivalent conceptually to MCP `notifications/resources/updated`. citeturn669705view0

### Resource list changed

```kotlin
data object ResourceListChanged
```

Equivalent conceptually to MCP `notifications/resources/list_changed`. citeturn669705view0

### State snapshot updated

```kotlin
data class StateSnapshotUpdated(
    val snapshotId: String,
    val uri: String,
    val updatedAtEpochMs: Long
)
```

### Event emitted

```kotlin
data class EventEmitted(
    val eventId: String,
    val name: String,
    val appId: String?,
    val emittedAtEpochMs: Long
)
```

---

## Replay, Ordering, and Resumption

MCP transport guidance gives useful rules here even though Synapse’s internal bus is not literally MCP transport:

- resumability should use event IDs/cursors
- replay must not duplicate messages across unrelated streams
- sessions should be logically scoped and resumable with a session identifier / last event ID pattern. citeturn963417view0turn963417view3

### V0 rules

1. every `EventEnvelope` gets a unique `eventId`
2. event ordering is guaranteed only **within a stream partition**
3. replay is cursor-based, not timestamp-only
4. replay windows depend on `RetentionClass`
5. resumption resumes **the same logical stream**, not a different stream

### Stream partition recommendation

For V0, partition by:
- `appId` + logical stream type

Examples:
- `com.example.game:events`
- `com.example.tasks:events`
- `host:system`

This keeps ordering and replay simple.

---

## Resource URI Rules

MCP says resources are uniquely identified by URIs and servers must validate them. It also recommends using `https://` only when the client can fetch directly from the web on its own; otherwise use another/custom scheme. citeturn669705view0turn963417view4

So for Synapse V0:

### Preferred local URI convention

```text
synapse-app://{appId}/{resourceName}
```

Examples:
- `synapse-app://com.example.game/inventory`
- `synapse-app://com.example.tasks/active-project`

### Hard rules

- validate all URIs
- do not use `https://` unless the client can actually fetch that resource directly itself
- use stable URIs so subscriptions do not thrash

---

## Bus Ingestion Pipeline

Every inbound event/state/resource update should pass through this pipeline:

1. source identity validation
2. schema validation
3. authorization / capability check
4. normalization
5. persistence to source of truth if applicable
6. projection into hot streams (`StateFlow` / `SharedFlow`)
7. notification to subscribers
8. audit receipt emission if required

---

## Event Retention Policy

### `EPHEMERAL`
- live-delivery only
- no durable replay

### `SHORT`
- short durable replay window
- intended for recent-context recovery

### `SESSION`
- retained for the logical session lifecycle
- replayable within that session window

### `AUDIT`
- persisted durably
- replayable and inspectable

This lets the bus avoid treating all events as equally important.

---

## State Snapshot Freshness

Every snapshot should declare `maxAgeMs`.

Rules:
- stale snapshot may still be returned, but it must be marked stale
- orchestrator can prefer refresh before heavy planning if staleness matters
- subscription update should refresh the state holder as soon as a new value arrives

---

## Security Rules

1. Validate all resource URIs. MCP explicitly requires this. citeturn963417view4
2. Apply access controls to sensitive resources. MCP recommends this. citeturn963417view4
3. Check resource/state/event permissions before reads/subscriptions. MCP recommends permission checks before operations. citeturn963417view4
4. Do not expose undeclared resources/events/state from apps.
5. Do not allow subscriptions or replay on capabilities that were not negotiated/allowed. MCP lifecycle says only successfully negotiated capabilities should be used. citeturn669705view6
6. Do not broadcast identical server messages across multiple unrelated streams; use stream-specific delivery and replay. MCP transport explicitly forbids broadcasting the same message across multiple connected streams. citeturn963417view0

---

## Definitions of Done

`EVENT_STATE_BUS_V0.md` is implementation-ready when:

- event model is fixed
- state snapshot model is fixed
- resource model is fixed
- subscription model is fixed
- replay/cursor rules are fixed
- retention classes are fixed
- one demo game app and one demo productivity app can both publish state and events without schema hacks

---

## Immediate Follow-On Types to Implement

After this doc, the repo should define:

- `EventEnvelope`
- `EventSource`
- `EventPriority`
- `RetentionClass`
- `StateSnapshotRecord`
- `ResourceRecord`
- `BusCursor`
- `BusSession`
- `ResourceSubscription`
- `EventSubscription`
- `EventFilter`
- `ResourceUpdated`
- `ResourceListChanged`
- `StateSnapshotUpdated`
- `EventEmitted`
- `EventStateBusCapabilities`
