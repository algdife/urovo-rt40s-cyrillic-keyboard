# Urovo RT40S Cyrillic Toggle — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build, sign, and ship `cyr-toggle-1.0.0-release.apk` — an Android Accessibility Service that intercepts hardware-keypad letter presses on the Urovo RT40S, toggles Latin↔Russian on Shift+Space, and inserts Russian phonetic Cyrillic characters into any focused text field across all apps.

**Architecture:** Single-package Kotlin Android app. AccessibilityService with `canRequestFilterKeyEvents` consumes letter key events in RU mode and injects Cyrillic via `ACTION_SET_TEXT` (with clipboard `ACTION_PASTE` fallback). EN mode is full pass-through.

**Tech Stack:** Kotlin 1.9+, Android Gradle Plugin 8.2+, AndroidX, JUnit 4 (pure JVM tests for `KeyMap`), Robolectric 4.11 (`ModeStore`), JDK 17, minSdk 28, targetSdk 34. No third-party runtime dependencies.

**Spec:** `docs/superpowers/specs/2026-04-26-urovo-cyrillic-keyboard-design.md` — all section references below are to that spec.

---

## File structure

```
C:/AutomateIt/Urovo/
├── settings.gradle.kts
├── build.gradle.kts                                     (project root)
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew, gradlew.bat
├── README.md                                            (IT-staff build/install)
├── INSTALL_RU.md                                        (warehouse-worker, Russian)
├── keystore/cyr-toggle.keystore                         (gitignored; generated locally)
├── keystore/keystore.properties                         (gitignored; signing config)
├── .gitignore
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── kotlin/ru/urovo/cyrtoggle/
        │   │   ├── KeyMap.kt
        │   │   ├── Mode.kt
        │   │   ├── ModeStore.kt
        │   │   ├── TextInjector.kt
        │   │   ├── AccessibilityTextInjector.kt
        │   │   ├── Toaster.kt
        │   │   ├── CyrToggleAccessibilityService.kt
        │   │   ├── BootReceiver.kt
        │   │   └── SettingsActivity.kt
        │   └── res/
        │       ├── xml/accessibility_service_config.xml
        │       ├── layout/activity_settings.xml
        │       ├── values/strings.xml
        │       └── values/themes.xml
        └── test/kotlin/ru/urovo/cyrtoggle/
            ├── KeyMapTest.kt
            └── ModeStoreTest.kt
```

**Decomposition rationale:** `KeyMap` is pure data so it lives alone and gets exhaustive JVM tests. `ModeStore` wraps `SharedPreferences` and is testable with Robolectric. `TextInjector` is an interface so the service depends on an abstraction; the concrete `AccessibilityTextInjector` is the only Android-coupled piece and is exercised manually on device.

---

This plan is broken into **15 tasks**. Each task ends with a commit. Run them in order.

The plan body follows in subsequent sections. See the companion files committed alongside this plan:

- `2026-04-26-urovo-cyrillic-keyboard-tasks-01-04.md` — Tasks 1-4 (scaffold, KeyMap, Mode, ModeStore)
- `2026-04-26-urovo-cyrillic-keyboard-tasks-05-09.md` — Tasks 5-9 (TextInjector, Toaster, accessibility XML, service, BootReceiver)
- `2026-04-26-urovo-cyrillic-keyboard-tasks-10-15.md` — Tasks 10-15 (manifest, SettingsActivity, build, test, instructions)

(I'm splitting due to output limits — the plan content is identical to a single file, just chunked.)
