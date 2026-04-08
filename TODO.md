# SYNAPSE-AI TODO

## Immediate Setup
- [ ] Install/update Android Studio stable
- [ ] Install Android SDK platform tools, emulator, command-line tools, and target SDK packages
- [ ] Connect and verify a real Samsung device with ADB (USB or Wi‑Fi)
- [ ] Install Node.js 18+
- [ ] Install Claude Code and run `claude doctor`
- [ ] Open `SYNAPSE-AI` in Android Studio and complete initial Gradle sync

## Sprint 0 — Repo / Module Scaffolding
- [ ] Create `build-logic/`
- [ ] Create `gradle/libs.versions.toml`
- [ ] Create `app/mobile-host`
- [ ] Create `core/common`
- [ ] Create `core/model-api`
- [ ] Create `core/tool-api`
- [ ] Create `core/apppack-api`
- [ ] Create `core/event-api`
- [ ] Create `core/device-profile`
- [ ] Create `core/model-registry`
- [ ] Create `core/runtime-governor`
- [ ] Create `core/security`
- [ ] Create `core/storage`
- [ ] Create `core/orchestrator`
- [ ] Create `core/evals`
- [ ] Create `runtime/local-service`
- [ ] Create `runtime/provider-fake`

## Immediate Kotlin Contract Files
- [ ] `AppPackV0.kt`
- [ ] `ModelProvider.kt`
- [ ] `ToolContract.kt`
- [ ] `EventEnvelope.kt`
- [ ] `OrchestrationTypes.kt`

## First Host Feature Shells
- [ ] `feature/chat/api`
- [ ] `feature/chat/impl`
- [ ] `feature/connected-apps/api`
- [ ] `feature/connected-apps/impl`
- [ ] `feature/settings/api`
- [ ] `feature/settings/impl`

## First Runtime / Eval Steps
- [ ] Implement `provider-fake`
- [ ] Add contract validation tests
- [ ] Add orchestration trace tests
- [ ] Add apppack validation tests
- [ ] Add initial eval dataset structure under `core/evals/`
