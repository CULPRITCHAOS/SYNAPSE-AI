# APPPACK_V0

## Purpose

`AppPack` is the canonical registration contract for any app that wants to integrate with Synapse.

It exists to solve a real problem:

- Synapse cannot scale if every new app requires hardcoded integration logic.
- Android does not allow reliable, privacy-friendly broad scanning of all installed apps on modern Android by default.
- MCP cleanly separates **tools** (model-controlled actions) from **resources** (application-driven context), but MCP by itself does not define the Android-specific app identity, install state, permission, and exported-entrypoint rules that Synapse needs.

So `AppPack` is a **mobile-native contract** that borrows the best parts of MCP and JSON Schema, then adds Android-specific identity and integration fields.

---

## Foundation

`AppPack` is based on these design facts:

1. MCP tools are **model-controlled** actions exposed with a name, description, and `inputSchema`.
2. MCP resources are **application-driven** context exposed through stable URIs and optionally subscriptions.
3. MCP requires capability negotiation during initialization and expects clients/servers to use only negotiated capabilities.
4. JSON Schema 2020-12 is a strong, current schema base for validating typed JSON contracts.
5. Android package visibility is restricted on Android 11+, so Synapse should prefer **explicit registration** over device-wide discovery.
6. Android components with intent filters must explicitly set `android:exported` on Android 12+.
7. Verified Android App Links are optional but, if used, require `https`/`http` intent filters, `android:autoVerify="true"`, and a valid `assetlinks.json` association.

Implication:

`AppPack` should define:
- app identity
- Android integration surface
- capabilities requested
- tools
- resources
- events
- state snapshots
- policy hints
- auth metadata
- compatibility metadata
- optional deep-link / App Link metadata

---

## Relationship to MCP

`AppPack` is **MCP-inspired**, not a copy of MCP.

### Borrow directly
- tool definitions
- resource definitions
- capability declarations
- schema-first argument contracts
- subscription concepts for changing resources

### Add for Android / Synapse
- package identity
- install/runtime compatibility
- app category
- exported entrypoint declarations
- optional App Links metadata
- app-specific capability requests
- state snapshot declarations
- local auth metadata
- trust/signature placeholders

### Do not include in V0
- a full standalone MCP transport definition
- dynamic prompt template support as a first-class section
- arbitrary code hooks
- unrestricted background automation declarations

Prompts can come later if the platform actually needs them.

---

## Design Principles

### 1. Explicit registration over scanning
Because Android limits package visibility by default, Synapse should not depend on broad installed-app enumeration as its core discovery model. Connected apps should register themselves or be added through explicit user flows.

### 2. Contracts over inference
The model should not guess what an app can do. The app should declare:
- what tools exist
- what resources exist
- what events exist
- what state snapshots exist
- what permissions/capabilities are required

### 3. Stable identity
Every app pack must have a stable app identity that survives app restarts, model swaps, and schema updates.

### 4. Typed schemas everywhere
Tool arguments, event payloads, and state snapshots must use JSON Schema, not vague free-form text blobs.

### 5. Least privilege
Apps must request only the capabilities they need, and Synapse must treat those requests as requests—not automatic grants.

### 6. Export only intentional entrypoints
Any Android activity/service/receiver surfaced through `AppPack` must correspond to an intentionally exported integration surface, not an internal component the app forgot to hide.

---

## Module Placement

```text
core/apppack-api/
```

The client-side SDK should also depend on this contract:

```text
sdk/synapse-client-sdk/
```

---

## Top-Level Shape

```json
{
  "schemaId": "synapse.apppack/v0",
  "schemaVersion": "0.1.0",
  "appId": "com.example.game",
  "displayName": "Example Game",
  "appVersion": "1.4.2",
  "developer": {
    "name": "Example Studio",
    "website": "https://example.com",
    "contactEmail": "support@example.com"
  },
  "category": "game",
  "android": {},
  "compatibility": {},
  "capabilitiesRequested": [],
  "tools": [],
  "resources": [],
  "events": [],
  "stateSnapshots": [],
  "examples": [],
  "policies": {},
  "auth": null,
  "signature": null
}
```

---

## Versioning Rules

### `schemaId`
Stable schema family identifier.

For V0:

```json
"schemaId": "synapse.apppack/v0"
```

### `schemaVersion`
SemVer version for the actual schema revision.

For V0, use Semantic Versioning rules:
- bump **MAJOR** for incompatible changes
- bump **MINOR** for backward-compatible additions
- bump **PATCH** for backward-compatible fixes/clarifications

Since this is still early design, `0.y.z` is appropriate.

### `appVersion`
The app’s own version, not the pack schema version.

---

## Required Top-Level Fields

### `schemaId`
String. Required.

### `schemaVersion`
String. Required. Must be SemVer-like.

### `appId`
String. Required.

Rules:
- stable across releases
- globally unique within Synapse registry
- SHOULD default to the Android package name unless there is a strong reason not to

### `displayName`
String. Required.

Human-readable name shown in Synapse UI.

### `appVersion`
String. Required.

### `developer`
Object. Required.

### `category`
Enum string. Required.

Allowed initial values:
- `game`
- `productivity`
- `communication`
- `iot`
- `accessibility`
- `media`
- `devtool`
- `utility`
- `other`

### `android`
Object. Required.

### `compatibility`
Object. Required.

### `capabilitiesRequested`
Array. Required.

### `tools`
Array. Required. May be empty.

### `resources`
Array. Required. May be empty.

### `events`
Array. Required. May be empty.

### `stateSnapshots`
Array. Required. May be empty.

### `policies`
Object. Required.

---

## `developer` Object

```json
{
  "name": "Example Studio",
  "website": "https://example.com",
  "contactEmail": "support@example.com"
}
```

Rules:
- `name` required
- `website` optional
- `contactEmail` optional

---

## `android` Object

This section exists because Synapse is an Android host, not a generic server registry.

```json
{
  "packageName": "com.example.game",
  "minSdk": 28,
  "targetSdk": 36,
  "entrypoints": [],
  "appLinks": [],
  "queriesRequired": [],
  "permissionsUsed": []
}
```

### `packageName`
Required.

Rules:
- must match the installed app package identity Synapse sees during registration
- should be treated as the authoritative Android package identity

### `minSdk`
Required integer.

### `targetSdk`
Required integer.

### `entrypoints`
Optional array describing intentionally exposed Android integration surfaces.

Example:

```json
[
  {
    "type": "activity",
    "className": ".synapse.ConnectActivity",
    "exported": true,
    "intentFilters": [
      {
        "actions": ["com.synapse.REGISTER_APP"],
        "categories": ["android.intent.category.DEFAULT"]
      }
    ]
  }
]
```

Rules:
- only include entrypoints intentionally meant for Synapse integration
- do not include internal-only components
- if an entrypoint declares intent filters, `exported` must be explicit

### `appLinks`
Optional array for verified Android App Links.

Example:

```json
[
  {
    "host": "example.com",
    "pathPrefix": "/synapse",
    "verified": true
  }
]
```

Rules:
- use only for real website-backed deep links
- if `verified=true`, app should actually support Android App Links verification
- this section is optional and should not be used as a fake generic routing mechanism

### `queriesRequired`
Optional array.

Used only if the app genuinely requires package visibility declarations to interact with specific other packages.

### `permissionsUsed`
Optional array.

Documents Android permissions relevant to the integration surface.

---

## `compatibility` Object

```json
{
  "minSynapseVersion": "0.1.0",
  "targetSynapseVersion": "0.1.0",
  "requiredFeatures": ["app_registry", "tool_dispatch"],
  "optionalFeatures": ["resource_subscriptions"]
}
```

Rules:
- `minSynapseVersion` required
- `targetSynapseVersion` required
- `requiredFeatures` required, may be empty
- `optionalFeatures` optional

This section exists so Synapse can reject packs that assume features the current host does not support.

---

## `capabilitiesRequested`

Capabilities are explicit permission-like requests from the app to Synapse.

```json
[
  {
    "name": "inventory_control",
    "reason": "Allows Synapse to equip and use items",
    "dangerous": false
  }
]
```

Rules:
- each capability name must be unique within the pack
- capability requests are not grants
- every tool that requires a capability must reference a declared capability name

Suggested initial capability names:
- `inventory_control`
- `character_movement`
- `quest_tracking`
- `task_management`
- `notifications`
- `scene_activation`
- `sensor_reading`
- `screen_read`
- `screen_control`
- `file_access`
- `network_access`
- `location_access`
- `camera_access`
- `microphone_access`

---

## `tools`

Tools are model-callable actions.

This section intentionally mirrors MCP’s tool model.

```json
[
  {
    "name": "equip_item",
    "description": "Equip an item from the player inventory",
    "inputSchema": {
      "type": "object",
      "properties": {
        "itemId": {
          "type": "string",
          "description": "Inventory item identifier"
        }
      },
      "required": ["itemId"],
      "additionalProperties": false
    },
    "outputSchema": {
      "type": "object",
      "properties": {
        "equipped": { "type": "boolean" }
      },
      "required": ["equipped"],
      "additionalProperties": false
    },
    "requiredCapabilities": ["inventory_control"],
    "dangerous": false,
    "requiresConfirmation": false,
    "idempotent": false,
    "timeoutMs": 5000,
    "tags": ["inventory", "combat"]
  }
]
```

Rules:
- `name` required and unique
- `description` required
- `inputSchema` required and must be valid JSON Schema
- `outputSchema` optional but strongly recommended
- `requiredCapabilities` required, may be empty
- `dangerous` required
- `requiresConfirmation` required
- `timeoutMs` optional with host default fallback

### Tool naming rules

Bad:
- `do_action`
- `run`
- `execute`

Good:
- `equip_item`
- `create_task`
- `activate_scene`

### Tool schema rules

- use `type: object` for tool input
- use `required`
- set `additionalProperties: false` unless flexibility is truly needed
- include human-readable descriptions for fields
- prefer enums where the domain is bounded

---

## `resources`

Resources are application-driven context surfaces.

This section intentionally mirrors MCP’s resource model.

```json
[
  {
    "uri": "synapse-app://com.example.game/inventory",
    "name": "Inventory",
    "description": "Current player inventory",
    "mimeType": "application/json",
    "subscribable": true,
    "freshnessTtlMs": 5000,
    "readSchema": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array"
        }
      },
      "required": ["items"],
      "additionalProperties": false
    }
  }
]
```

Rules:
- `uri` required and unique
- prefer custom `synapse-app://` URIs for app-local resources
- do not misuse `https://` for host-local resources that the client cannot fetch directly itself
- `readSchema` optional but strongly recommended for JSON resources

### Resource URI convention

For V0, prefer:

```text
synapse-app://{appId}/{resourceName}
```

Examples:
- `synapse-app://com.example.game/inventory`
- `synapse-app://com.example.tasks/active-project`

---

## `events`

Events are transient updates emitted from the app to Synapse.

```json
[
  {
    "name": "player_died",
    "description": "Player character died",
    "payloadSchema": {
      "type": "object",
      "properties": {
        "cause": { "type": "string" },
        "timestamp": { "type": "integer" }
      },
      "required": ["cause", "timestamp"],
      "additionalProperties": false
    },
    "priority": "high",
    "retention": "session"
  }
]
```

Rules:
- `name` required and unique
- `payloadSchema` required
- `priority` required
- `retention` required

Allowed priorities:
- `low`
- `normal`
- `high`
- `critical`

Allowed retentions:
- `ephemeral`
- `short`
- `session`
- `audit`

---

## `stateSnapshots`

State snapshots are bounded, current-state views that Synapse can query or cache.

```json
[
  {
    "name": "combat_state",
    "description": "Current combat summary",
    "schema": {
      "type": "object",
      "properties": {
        "hp": { "type": "integer" },
        "mana": { "type": "integer" },
        "inCombat": { "type": "boolean" }
      },
      "required": ["hp", "mana", "inCombat"],
      "additionalProperties": false
    },
    "maxAgeMs": 1000
  }
]
```

Rules:
- `name` required and unique
- `schema` required
- `maxAgeMs` required

Use state snapshots for bounded current state, not append-only event history.

---

## `examples`

Examples are optional routing/training hints.

```json
[
  {
    "userIntent": "equip my best weapon",
    "preferredTool": "equip_item",
    "argumentExampleJson": "{\"itemId\":\"sword_01\"}"
  }
]
```

Rules:
- examples are hints, not authority
- examples must reference valid declared tools
- examples should be concise and realistic

---

## `policies`

Policy hints let the app declare intended operating boundaries.

```json
{
  "allowBackgroundCalls": false,
  "denyWhenDeviceLocked": true,
  "requireUserPresenceForDangerousTools": true,
  "maxCallsPerMinute": 30
}
```

Rules:
- policy hints are advisory until adopted by Synapse security policy
- app-declared policy cannot weaken host security policy
- host policy wins on conflict

---

## `auth`

Optional section describing how the app expects Synapse to authenticate requests.

For V0, keep this simple.

```json
{
  "mode": "none"
}
```

Allowed initial values:
- `none`
- `bearer`
- `api_key`

Notes:
- V0 does not require OAuth-level complexity for same-device integrations
- if HTTP-based integrations are added later, this section can expand

---

## `signature`

Optional placeholder for future trust metadata.

```json
{
  "algorithm": "sha256",
  "digest": "...",
  "signer": "Example Studio"
}
```

V0 rules:
- optional
- host may ignore it initially
- field exists now so the format can evolve without structural churn later

---

## Validation Rules

An `AppPack` is invalid if any of the following are true:

1. missing required top-level fields
2. invalid `schemaVersion`
3. duplicate tool names
4. duplicate resource URIs
5. duplicate event names
6. duplicate state snapshot names
7. tool references undeclared capabilities
8. invalid JSON Schema objects
9. `android.packageName` does not match registered package identity
10. dangerous tool omits explicit `dangerous` / `requiresConfirmation` declarations
11. invalid enum values in category / priority / retention / auth mode

---

## Registration Flow

### Recommended V0 flow

1. App invokes Synapse registration entrypoint.
2. App sends `AppPack`.
3. Synapse validates schema and identity.
4. Synapse stores pack in registry with status `pending`.
5. User reviews requested capabilities and app metadata.
6. User approves or rejects.
7. Approved pack becomes active and routable.

### Why explicit registration

This is better than blind discovery because Android package visibility is restricted by default and because explicit user approval creates a cleaner trust boundary.

---

## Hard Rules

1. `AppPack` is a declaration, not a capability grant.
2. Synapse must not trust undeclared tools/resources/events/state.
3. Synapse must not trust tool annotations or metadata as security truth.
4. Exported Android entrypoints must be intentional and explicit.
5. Resource URIs must be stable.
6. Event payloads and tool arguments must be schema-validatable.
7. App Links metadata must only be used when the app actually owns and verifies the host.

---

## Definition of Done

`APPPACK_V0.md` is good enough for implementation when:

- top-level shape is fixed
- required fields are fixed
- validation rules are fixed
- SDK and host can both depend on the same contract types
- one demo game app and one demo productivity app can both be represented without schema hacks

---

## First Follow-On Types to Implement

After this doc, the repo should define:

- `AppPackV0`
- `DeveloperInfo`
- `AndroidIntegrationSpec`
- `CapabilityRequest`
- `AppToolDef`
- `AppResourceDef`
- `AppEventDef`
- `StateSnapshotDef`
- `AppPolicyHints`
- `AppAuthSpec`
- `AppSignatureSpec`
- `AppPackValidationResult`
