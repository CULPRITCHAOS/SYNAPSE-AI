# ANDROID_STUDIO_SETUP_V0

## Purpose

This document defines the practical local setup for developing `SYNAPSE-AI`.

The intended workflow is:
- **Android Studio** for Android SDK, Gradle sync, builds, emulator/device runs, debugging, and Logcat
- **Claude Code** as a terminal coding assistant for scaffolding, refactors, repetitive edits, and repo-wide implementation help

This is the correct split because Android Studio is the actual Android development environment, while Claude Code is a terminal-based coding tool rather than an Android IDE.

---

## Baseline Setup Decision

Use **both**:

### Android Studio
Use for:
- creating/opening the Android project
- SDK installation and management
- Gradle sync
- emulator/device deployment
- debugging
- Logcat inspection
- running instrumented tests

Android’s current install and run docs make Android Studio the normal path for setup, running, debugging, and emulator/device use. The install guide says the emulator is the preferred method for testing across configurations, while the hardware-device guide says you should still always test on a real device before release. citeturn554003search2turn252429search0turn252429search1

### Claude Code
Use for:
- writing Kotlin/Gradle files
- scaffolding modules
- generating interfaces/types/tests
- refactors
- repetitive repo-wide edits
- documentation updates

Anthropic’s docs say Claude Code is a terminal tool installed with Node.js 18+ using npm. citeturn554003search4turn554003search5

---

## Required Local Tools

## 1. Android Studio
Install the latest **stable** Android Studio.

Why:
- Android’s install page says to start with the latest version and run the setup wizard to install recommended SDK packages. It also notes that cloud/service integrations are only available on the latest stable channel and major versions released in the previous 10 months. citeturn554003search2

### Recommended usage
- use Android Studio for project import and Gradle sync
- let the setup wizard install recommended SDK components
- use the Device Manager / Running Devices window for emulator/device management

---

## 2. Android SDK components
At minimum install:
- Android SDK Platform for the target compile SDK
- Android SDK Build-Tools
- Android SDK Platform-Tools
- Android Emulator
- command-line tools

Why:
- Android Studio’s install flow installs recommended SDK packages, and the emulator/ADB docs rely on SDK platform tools and emulator tooling. citeturn554003search2turn554003search0turn252429search0

---

## 3. JDK / Gradle runtime
Use the Android Studio-managed JDK/Gradle path unless there is a specific reason not to.

Why:
- Android’s Java/Gradle docs say Studio uses its configured JDK to run Gradle, and current Studio releases now support Gradle Daemon JVM criteria for simpler JDK management in new projects. Android also recommends explicitly specifying the Java toolchain version for build consistency. citeturn957213search6turn957213search7turn957213search5

### Practical rule
- avoid random machine-specific JDK drift
- keep Android Studio Gradle JDK and terminal `JAVA_HOME` aligned when possible for consistency. citeturn957213search6

---

## 4. Node.js
Install **Node.js 18+**.

Why:
- Claude Code requires Node.js 18+ according to Anthropic’s setup docs. citeturn554003search4turn554003search5

---

## 5. Claude Code
Install with:

```bash
npm install -g @anthropic-ai/claude-code
```

Anthropic explicitly says not to use `sudo npm install -g` because it can create permission and security issues. citeturn554003search4turn554003search5

After install, run:

```bash
claude doctor
```

Anthropic recommends it to verify the installation type/setup. citeturn554003search5

---

## Recommended First-Time Setup Order

## Step 1 — Install Android Studio
- install latest stable Android Studio
- run setup wizard
- install recommended SDK packages

Reference: Android Studio install guide. citeturn554003search2

## Step 2 — Verify SDK / emulator tooling
Check that these work from terminal after setup:

```bash
adb version
emulator -list-avds
```

Why:
- Android’s emulator docs use `emulator -list-avds` and ADB/device docs use `adb devices` as the normal verification path. citeturn554003search0turn252429search0

## Step 3 — Set up a real device
For your Samsung phone:
- enable developer options
- enable USB debugging or wireless debugging
- pair the device in Android Studio if using Wi‑Fi

Android’s device guide explicitly says to always test on a real device before release, and documents both USB and Wi‑Fi pairing/debugging. citeturn252429search0

## Step 4 — Install Node.js 18+
- verify with `node -v`

## Step 5 — Install Claude Code
- install globally with npm
- authenticate
- run `claude doctor`

## Step 6 — Clone and open `SYNAPSE-AI`
- clone repo locally
- open root project in Android Studio
- let Gradle sync
- fix any missing SDK prompts from Studio

## Step 7 — Create the multi-module Android project shell
Use Android Studio as the source of truth for:
- Gradle settings
- modules
- namespaces
- SDK versions
- run/debug configurations

Use Claude Code to help generate:
- module build files
- Kotlin interfaces/types
- repetitive scaffolding
- TODO implementation passes

---

## Emulator vs Real Device

### Emulator
Use for:
- fast UI iteration
- different API levels and screen sizes
- quick smoke tests

Android’s install page says the emulator is the preferred method for testing across device configurations. citeturn554003search2

### Real device
Use for:
- actual runtime behavior
- local AI performance checks
- device permissions and sensor behavior
- end-to-end validation before trusting the app

Android’s hardware-device guide explicitly says to always test on a real Android device before releasing to users. citeturn252429search0

### Synapse-specific rule
For local-AI/provider benchmarks, trust the **real device** over the emulator.

That is consistent with Google’s Android on-device LLM guidance and Android’s general real-device recommendation for final validation. citeturn252429search0turn554003search2

---

## Android Studio Project Conventions for This Repo

When the project shell is created in Studio, configure it to match the repo docs:

- regular multi-module Android project
- version catalog in `gradle/libs.versions.toml`
- shared build logic in `build-logic/`
- `app/mobile-host` as composition root
- `core/*`, `runtime/*`, `feature/*`, `sdk/*`, `integration/*` modules per `REPO_MODULE_SCAFFOLDING_V0.md`

Android build docs recommend version catalogs for shared dependencies in multi-module builds. citeturn554003search1turn554003search3

---

## Minimal Terminal Checks

After setup, these should work:

```bash
adb version
adb devices
emulator -list-avds
./gradlew tasks
./gradlew test
```

On Windows, use `gradlew.bat` instead of `./gradlew` where appropriate.

`adb devices` and `emulator -list-avds` align with Android’s official device/emulator docs. citeturn252429search0turn554003search0

---

## Debugging Workflow

Use Android Studio for:
- breakpoints
- variable inspection
- attaching/running the debugger
- Logcat
- selecting debug build variants

Android’s debugger docs describe using the debuggable build variant and Studio’s Debug window/tooling for this workflow. citeturn252429search1

---

## Recommended Immediate Next Actions

1. install/update Android Studio stable
2. install recommended SDK packages
3. connect your Samsung phone with USB or Wi‑Fi debugging
4. install Node.js 18+
5. install Claude Code
6. clone/open `SYNAPSE-AI`
7. scaffold the Gradle/module structure in Android Studio
8. start with the first Kotlin contract files from the repo todo list

---

## Definition of Done

The local environment is ready when:
- Android Studio opens the repo and completes Gradle sync
- the SDK/platform tools are installed
- `adb devices` sees your phone or emulator
- at least one emulator or real device can run a debug build
- Claude Code is installed and passes `claude doctor`
- the repo is ready for the first Kotlin contract scaffolding sprint
