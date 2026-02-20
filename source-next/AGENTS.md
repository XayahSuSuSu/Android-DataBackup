# Repository Guidelines

## Project Structure & Module Organization
This repository is a multi-module Android project managed with Gradle Kotlin DSL.
- `app/`: main Android application (Compose UI, services, AIDL, resources, translations).
- `hiddenapi/`: wrapper library for hidden Android APIs used by the app.
- `native/`: JNI/CMake native layer plus Kotlin bindings (`src/main/jni`, `src/main/kotlin`).
- `gradle/libs.versions.toml`: centralized dependency and SDK versions.

Source code lives under each module’s `src/main`, local JVM tests under `src/test`, and instrumentation tests under `src/androidTest`.

## Build, Test, and Development Commands
Use the Gradle wrapper from repository root:
- `./gradlew assembleDebug`: builds debug artifacts for all modules.
- `./gradlew :app:installDebug`: installs the app to a connected device/emulator.
- `./gradlew test`: runs JVM unit tests (`src/test`) across modules.
- `./gradlew connectedAndroidTest`: runs instrumentation tests on a connected device.
- `./gradlew lint`: runs Android lint checks.

## Coding Style & Naming Conventions
- Language stack: Kotlin + Android (plus C/C++ in `native/src/main/jni`).
- Follow Kotlin conventions: 4-space indentation, `PascalCase` for types, `camelCase` for functions/properties, package names lowercase.
- Keep feature classes descriptive and domain-oriented (examples: `BackupService`, `BackupConfig`, `TarWrapper`).
- Resource naming should stay Android-standard snake_case (examples: `ic_archive_restore.xml`, `values-zh-rCN/strings.xml`).

## Testing Guidelines
- Unit tests use JUnit4 (`ExampleUnitTest.kt` pattern).
- Instrumentation tests use AndroidX JUnit/Espresso (`ExampleInstrumentedTest.kt` pattern).
- Add/extend tests when changing backup logic, root-service behavior, database models, or JNI boundaries.
- Prefer module-scoped test runs while iterating (example: `./gradlew :native:testDebugUnitTest`).

## Commit & Pull Request Guidelines
Git history shows conventional-style subjects for feature work (for example, `[Next] feat: ...`) plus automated translation commits.
- Prefer concise, imperative commit messages; include scope/tag when helpful.
- Link issues in commit/PR text when applicable (example: `(#453)`).
- PRs should include: change summary, affected modules (`app`/`hiddenapi`/`native`), test evidence (`./gradlew test`, device test notes), and screenshots for UI changes.
