# APPPACK_STYLE_GUIDE_V0

## Purpose

This guide defines what makes a **good Synapse AppPack**.

`APPPACK_V0.md` defines the contract shape.  
This file defines the style, quality bar, and review rules.

This guide exists because future Tool Foundry, AppPack Critic, and fine-tuning workflows need consistent examples. If AppPacks are inconsistent, the model will learn inconsistent behavior.

Do not fine-tune on AppPack examples until this guide exists and the examples are reviewed against it.

---

## Relationship to Other Docs

This guide sits under the current implementation spine:

- `docs/APPPACK_V0.md` defines the schema.
- `docs/TOOL_CONTRACT_V0.md` defines tool execution contracts.
- `docs/EVENT_STATE_BUS_V0.md` defines events, resources, state snapshots, and subscriptions.
- `docs/SECURITY_POLICY_V0.md` defines private-build security gates.
- `docs/EVAL_PLAN_V0.md` defines how correctness is measured.

This guide is a **style/rubric layer** used for:

- manually authoring good AppPacks
- reviewing AppPacks
- generating Tool Foundry proposals
- labeling future fine-tuning data
- creating eval cases

---

## Core Principle

A good AppPack is boring, explicit, typed, and narrow.

Bad AppPacks are vague, magical, broad, and permission-hungry.

The host should never need to guess what an app means.

---

## AppPack Quality Checklist

A good AppPack must answer these questions clearly:

1. What app is this?
2. What capabilities does it request, and why?
3. What tools can Synapse call?
4. What resources can Synapse read?
5. What state snapshots represent current truth?
6. What events tell Synapse something changed?
7. Which actions mutate state?
8. Which actions need confirmation?
9. Which state is required before mutation?
10. How can this integration be tested?

If the pack cannot answer these, it is not ready.

---

## Naming Rules

## App ID

Use stable Android-style app IDs.

Good:

```text
com.synapse.demo.game
com.example.inventory
com.rob.stocktracker
```

Bad:

```text
game
inventory
myapp
```

---

## Tool Names

Tool names must be namespaced and action-specific.

Use:

```text
{appId}.{verb}_{object}
```

Good:

```text
com.synapse.demo.game.get_stats
com.synapse.demo.game.use_potion
com.example.inventory.add_sale
com.example.inventory.adjust_stock
host.flashlight.set_state
```

Bad:

```text
get
run
do_action
use
flashlight
update
```

### Why

Tool names become part of the model's action vocabulary. Vague names cause bad routing and collisions.

---

## Resource Names

Resources describe readable context.

Use nouns.

Good:

```text
inventory
active_project
player_status
quest_log
```

Bad:

```text
get_inventory
read_status
fetch_data
```

If it starts with a verb, it may be a tool, not a resource.

---

## Event Names

Events describe something that happened.

Use past-tense or change-oriented names.

Good:

```text
player_damaged
inventory_changed
quest_completed
task_created
sale_recorded
```

Bad:

```text
player_status
inventory
get_event
update
```

If it represents current truth, it is probably state/resource, not an event.

---

## Tool Description Rules

Tool descriptions should be short, concrete, and operational.

Good:

```text
Uses one healing potion in the demo game. Returns updated HP and remaining potion count.
```

Bad:

```text
Helps the player.
```

Good:

```text
Records a sale for one inventory item and decrements stock by the sold quantity.
```

Bad:

```text
Manages inventory.
```

---

## Description Format

Use this pattern:

```text
[Verb] [specific object] [scope/context]. Returns [specific output].
```

Examples:

```text
Reads the current player HP and potion count from the demo game. Returns hp and potions.
```

```text
Uses one healing potion in the demo game. Returns updated hp and remaining potions.
```

```text
Adds one sale record for a merch item. Returns updated stock and sale id.
```

---

## Tool vs Resource vs State vs Event

This is the most important style distinction.

## Use a Tool when

The operation performs an action or mutation.

Examples:

- use potion
- equip item
- record sale
- create task
- activate scene
- send message

## Use a Resource when

Synapse needs addressable context it can read.

Examples:

- inventory list
- quest log
- active project
- product catalog

## Use a State Snapshot when

Synapse needs current truth.

Examples:

- player_status: hp, potions, inCombat
- stock_status: item counts
- device_status: on/off/temperature

## Use an Event when

Something happened and ordering/history matters.

Examples:

- player_damaged
- potion_used
- sale_recorded
- task_completed

---

## Common Classification Mistakes

### Mistake: state as tool

Bad:

```json
{
  "tool": "com.synapse.demo.game.get_current_hp"
}
```

Better:

```json
{
  "stateSnapshot": "player_status"
}
```

A read tool can exist as fallback, but live state should be a state snapshot.

---

### Mistake: event as state

Bad:

```json
{
  "stateSnapshot": "player_damaged"
}
```

Better:

```json
{
  "event": "player_damaged"
}
```

Damage is something that happened. Current HP is state.

---

### Mistake: mutation as resource

Bad:

```json
{
  "resource": "use_potion"
}
```

Better:

```json
{
  "tool": "com.synapse.demo.game.use_potion"
}
```

Using a potion mutates state.

---

## Input Schema Rules

Tool inputs must be the smallest useful schema.

Good:

```json
{
  "type": "object",
  "properties": {
    "quantity": {
      "type": "integer",
      "minimum": 1,
      "maximum": 10,
      "description": "Number of potions to use."
    }
  },
  "required": ["quantity"],
  "additionalProperties": false
}
```

Bad:

```json
{
  "type": "object",
  "properties": {
    "data": { "type": "string" }
  }
}
```

---

## Schema Rules

1. Top-level input schema should be an object.
2. Use concrete types.
3. Use enums for bounded values.
4. Use minimum/maximum for numeric bounds.
5. Use `required` explicitly.
6. Prefer `additionalProperties: false`.
7. Do not use `string` blobs for structured data.

---

## Output Schema Rules

Every mutation tool should define an output schema.

Good:

```json
{
  "type": "object",
  "properties": {
    "success": { "type": "boolean" },
    "hp": { "type": "integer" },
    "potions": { "type": "integer" }
  },
  "required": ["success", "hp", "potions"],
  "additionalProperties": false
}
```

Bad:

```json
{
  "type": "string"
}
```

The host needs structured outputs for receipts, state updates, evals, and deterministic user responses.

---

## Capability Rules

Capabilities should be narrow.

Good:

```text
inventory_read
inventory_mutate
player_status_read
healing_item_use
```

Bad:

```text
full_access
admin
control_everything
all_game_tools
```

---

## Capability Request Format

A good capability request includes:

- name
- class
- reason
- default grant recommendation
- risky actions if any

Example:

```json
{
  "name": "healing_item_use",
  "class": "APP_MUTATION",
  "reason": "Allows Synapse to use healing potions when the user asks for healing.",
  "dangerous": false
}
```

---

## Mutation Tool Rules

Mutation tools require stricter metadata.

Every mutation tool should define:

- side effect class
- confirmation policy
- required capabilities
- required state source if applicable
- output schema
- whether dry-run is supported
- whether repeated calls are allowed

Example:

```json
{
  "name": "com.synapse.demo.game.use_potion",
  "sideEffectClass": "APP_STATE_MUTATION",
  "confirmationPolicy": "HOST_POLICY_DECIDES",
  "requiresFreshState": true,
  "requiredStateSnapshots": ["player_status"],
  "supportsDryRun": false,
  "repeatable": true,
  "maxCallsPerRequest": 5
}
```

---

## State Before Mutation

State-dependent mutation tools must not execute from `StateSource.NONE`.

Good flow:

```text
Fresh player_status snapshot exists
→ potions > 0
→ use_potion allowed
```

Fallback flow:

```text
No fresh state
→ call get_stats
→ potions > 0
→ use_potion allowed
```

Bad flow:

```text
Tool exists
→ model says use it
→ execute
```

---

## Repeated Mutation Rules

Repeated mutation tools must re-check actual tool results or state between calls.

Good:

```text
requestedQuantity = 2
get_stats returns potions=2
use_potion #1 returns hp=110, potions=1
use_potion #2 returns hp=160, potions=0
final response: You used 2 potions. Your HP is now 160.
```

Bad:

```text
get_stats returns potions=2
model writes prose saying it will use both
host blindly calls use_potion twice without checking results
```

---

## Dry-Run Rules

Dry-run is optional but must be explicit.

For tools with side effects:

- if the app supports dry-run, declare it
- if it does not, declare `supportsDryRun: false`
- do not pretend Synapse can simulate side effects without app cooperation

Good:

```json
{
  "supportsDryRun": true,
  "dryRunDescription": "Validates whether stock can be decremented without recording a sale."
}
```

Bad:

```json
{
  "supportsDryRun": true
}
```

with no actual dry-run behavior.

---

## Confirmation Policy Rules

Suggested defaults:

- read-only tool: `NONE`
- app-state mutation: `HOST_POLICY_DECIDES`
- destructive mutation: `EVERY_TIME`
- external effect: `EVERY_TIME`
- user-defined mutation: `EVERY_TIME`

Examples:

```text
get_stats → NONE
use_potion → HOST_POLICY_DECIDES
delete_save_file → EVERY_TIME
send_message → EVERY_TIME
record_sale → FIRST_USE or HOST_POLICY_DECIDES
```

---

## Game Rule Metadata

If a game has unusual rules, declare them.

Example:

```json
{
  "gameRules": {
    "allowOverheal": true,
    "potionHealAmount": 50,
    "maxPotionUsesPerRequest": 5
  }
}
```

Do not force Synapse or the model to assume normal game conventions.

If HP can exceed normal-looking values, say so.

---

## Good Dragon Quest Example

```json
{
  "appId": "com.synapse.demo.game",
  "displayName": "Dragon Quest Demo",
  "capabilitiesRequested": [
    {
      "name": "player_status_read",
      "class": "READ_ONLY",
      "reason": "Allows Synapse to read current HP and potion count."
    },
    {
      "name": "healing_item_use",
      "class": "APP_MUTATION",
      "reason": "Allows Synapse to use healing potions when requested."
    }
  ],
  "stateSnapshots": [
    {
      "name": "player_status",
      "description": "Current player HP and potion count.",
      "maxAgeMs": 10000,
      "schema": {
        "type": "object",
        "properties": {
          "hp": { "type": "integer" },
          "potions": { "type": "integer" }
        },
        "required": ["hp", "potions"],
        "additionalProperties": false
      }
    }
  ],
  "tools": [
    {
      "name": "com.synapse.demo.game.get_stats",
      "description": "Reads the current player HP and potion count. Returns hp and potions.",
      "sideEffectClass": "READ_ONLY",
      "requiredCapabilities": ["player_status_read"]
    },
    {
      "name": "com.synapse.demo.game.use_potion",
      "description": "Uses one healing potion. Returns updated hp and remaining potions.",
      "sideEffectClass": "APP_STATE_MUTATION",
      "requiredCapabilities": ["healing_item_use"],
      "requiresFreshState": true,
      "requiredStateSnapshots": ["player_status"],
      "repeatable": true,
      "supportsDryRun": false
    }
  ]
}
```

---

## Bad Dragon Quest Example

```json
{
  "appId": "game",
  "tools": [
    {
      "name": "do_action",
      "description": "Does stuff in the game.",
      "inputSchema": {
        "type": "object",
        "properties": {
          "data": { "type": "string" }
        }
      },
      "requiredCapabilities": ["full_access"]
    }
  ]
}
```

Why it is bad:

- appId is not stable/namespaced
- tool name is vague
- description is useless
- input schema is an unstructured blob
- capability is too broad
- side effects are unclear
- no state requirements
- no output schema
- no confirmation/dry-run policy

---

## AppPack Critic Rubric

Score each AppPack from 0–2 on each axis.

### 1. Identity clarity
- 0: unclear app identity
- 1: mostly clear
- 2: stable appId/package/displayName/version

### 2. Tool naming
- 0: vague/colliding names
- 1: partially namespaced
- 2: fully namespaced and action-specific

### 3. Schema quality
- 0: vague blobs or missing schemas
- 1: usable but loose
- 2: typed, bounded, required fields, no unnecessary extras

### 4. State/resource/event separation
- 0: mixed concepts
- 1: mostly correct
- 2: clear separation

### 5. Mutation safety
- 0: mutation tools lack policy/state requirements
- 1: partial safeguards
- 2: side effects, confirmation, state requirements, outputs defined

### 6. Capability discipline
- 0: broad/full access
- 1: somewhat narrow
- 2: least-privilege capabilities with clear reasons

### 7. Testability
- 0: cannot be tested cleanly
- 1: partial test paths
- 2: happy path and negative path are obvious

### Passing bar

For training-data quality, target:

```text
minimum total score: 11 / 14
no zero in Mutation safety
no zero in Tool naming
no zero in Schema quality
```

---

## Fine-Tuning Data Rule

No AppPack example should enter curated training data until it:

1. passes this style guide
2. has a reviewed expected tool plan
3. has a reviewed expected final response
4. includes state source assumptions
5. includes at least one negative-path case
6. has a pass/fail label

Raw traces are not training data.

Reviewed examples are training data.

---

## Tool Foundry Developer Mode vs User Mode

## Developer Mode

For app creators who control the app.

Can generate:

- AppPack
- SDK integration plan
- tools
- resources
- state snapshots
- events
- dry-run endpoints
- eval cases
- mock fixtures

This is the strongest mode.

## User Mode

For users integrating apps they did not build.

More constrained:

- deep links
- intents
- accessibility/macros
- user-confirmed routines
- no privileged internal state unless app cooperates
- no guaranteed dry-run

Do not conflate these modes.

---

## Final Rule

A good AppPack makes Synapse smarter by making the app easier to reason about.

A bad AppPack makes the model guess.

Do not make the model guess.
