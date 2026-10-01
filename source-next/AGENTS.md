# Repository Guidelines

## Jetpack Compose
For all Compose/Android UI tasks, use the relevant instructions under
`.codex/skills/jetpack-compose/` before answering or editing code. Current
Compose skills include:
- `.codex/skills/jetpack-compose/adaptive/SKILL.md`
- `.codex/skills/jetpack-compose/migration/migrate-xml-views-to-jetpack-compose/SKILL.md`
- `.codex/skills/jetpack-compose/theming/styles/SKILL.md`

When a selected skill references files in its `references/` directory, consult
the relevant reference files before acting.

## Project Structure & Module Organization
This repository is a multi-module Android project managed with Gradle Kotlin DSL.
- `app/`: main Android application (Compose UI, services, AIDL, resources, translations).
- `hiddenapi/`: wrapper library for hidden Android APIs used by the app.
- `native/`: JNI/CMake native layer plus Kotlin bindings (`src/main/jni`, `src/main/kotlin`).
- `gradle/libs.versions.toml`: centralized dependency and SDK versions.

Source code lives under each module’s `src/main`. Temporary JVM tests may use `src/test`, and temporary instrumentation tests may use
`src/androidTest`; remove their classes and dedicated fixtures before delivery.

## Architecture & Responsibilities
Organize code by responsibility and use business-oriented names. Use Flow for observable state, Arrow optics for nested state updates,
and domain Helpers for concrete operations.

- Keep the three existing Gradle modules. Organize application code by responsibility, with backend subpackages only for backend-specific work.
- `data`: repositories for data access, selections, and process orchestration. Name them after their actual scope, such as `RusticRepository`
  for repository/snapshot management and `ArchiveBackupProcessRepository` for the archive backup workflow.
- `entity`: shared business models, grouped into `backup`, `restore`, and `rustic` where appropriate. Keep Room models in `database.entity`.
  Rustic manifests and snapshot wire models remain backend-specific; shared selection and restore models must not depend on them.
- `service.backup` and `service.restore`: Helpers that perform concrete operations. Put backend-specific implementations in `archive` or `rustic`.
  Share Android installation/provider operations when they do not depend on the storage backend.
- `rootservice`: privileged service lifecycle, Binder entry points, and Binder callback/parcel conversion. Preserve the process and permission
  under which each operation executes; moving a class must not move an operation to another process.
- `feature`: screens, ViewModels, and presentation state grouped by feature. Keep backend-specific backup progress pages in sibling `archive`
  and `rustic` packages. `ui.component` contains components shared by screens.
- `util`: cross-feature utilities, not backup/restore orchestration. `native` contains Kotlin Wrappers, JNI bridges, and native implementations.
- Introduce an interface only at a real substitution boundary, such as `RestoreHelper`. Do not add forwarding-only Coordinator/Gateway/Provider
  layers. Prefer a concrete Repository or domain Helper with an explicit responsibility.
- Keep small, closely related types together; keep page-only UiState near its ViewModel. Avoid catch-all `Models.kt` files or one file per trivial type.

## Build, Test, and Development Commands
Use the Gradle wrapper from repository root:
- `./gradlew assembleDebug`: builds debug artifacts for all modules.
- `./gradlew :app:installDebug`: installs the app to a connected device/emulator.
- `./gradlew test`: runs temporary JVM unit tests (`src/test`) across modules when present.
- `./gradlew connectedAndroidTest`: runs temporary instrumentation tests on a connected device when present.
- `./gradlew lint`: runs Android lint checks.
- `cargo test --locked --manifest-path native/src/main/jni/rustic/Cargo.toml`: runs the owned Rustic bridge's Rust tests.

## Coding Style & Naming Conventions
- Language stack: Kotlin + Android (plus C/C++ in `native/src/main/jni`).
- Follow Kotlin conventions: 4-space indentation, `PascalCase` for types, `camelCase` for functions/properties, package names lowercase.
- Use a maximum line width of 150 characters.
- Keep feature classes descriptive and domain-oriented (examples: `BackupService`, `BackupConfig`, `TarWrapper`).
- Resource naming should stay Android-standard snake_case (examples: `ic_archive_restore.xml`, `values-zh-rCN/strings.xml`).
- Use `mFoo` for stored instance dependencies and internal held objects, `_foo` for mutable Flow backing fields, and camelCase for public
  properties, ordinary parameters, and local variables. For example: `private val mRusticRepo: RusticRepository`,
  `private val _state = MutableStateFlow(...)`, and `val state: StateFlow<...> = _state.asStateFlow()`.
- Give public state an explicit type and expose read-only Flow/StateFlow. Keep mutable state private and update it through named operations.
- Use descriptive verbs: `get` for current values, `load` for loading state, `read` for raw input, `serialize`/`deserialize` for format conversion,
  and `start`/`restore` for execution. Name boolean properties with `is`, `has`, or `can` where appropriate.
- Expression bodies suit simple accessors and transformations; use block bodies for multi-step operations. Prefer readable control flow over
  chained side effects. Preserve Arrow optics for nested updates and ordinary `copy` for simple updates.
- Prefer `runCatching` over `try/catch` when behavior is equivalent and the code remains clear; use `onFailure`, `getOrElse`, or `getOrThrow`
  as appropriate. Preserve exception handling boundaries and rethrow `CancellationException` before fallback or failure handling in coroutine code.
  Keep `try/finally` for guaranteed cleanup and `try/catch` when distinct exception branches are clearer.
- Use a class/object `TAG` and `LogHelper` consistently. Comments should explain contracts, reasons, cleanup, or Android version differences;
  retain useful platform source references. Do not log repository passwords, private record contents, or whole restore requests.
- Keep existing serialized enum values, resource names, and platform symbols unchanged. Rust uses Rust naming and formatting conventions;
  Android API stubs, vendored libraries, generated files, and upstream-derived code follow their source conventions.

## State, Backends & Compatibility
- ViewModels adapt repository state to presentation and own UI actions; Helpers perform concrete work. Preserve `BaseViewModel` and existing
  task lifetimes. Do not silently move tasks to background services or change cancellation semantics during structural refactors.
- Rethrow coroutine cancellation. Preserve task serialization and report completed, skipped, failed, and unprocessed items distinctly.
- Each restore navigation entry owns its own `RestoreRepository`, shared only with its child destinations. Never register its selection or
  pending request as a global singleton. Prepare an immutable execution request and consume it once; recreation must not replay device writes.
- Common restore requests, selections, progress, and inventory use backend-neutral types. Encapsulate Rustic repository credentials and snapshot
  identity in a typed source. Convert Rustic wire metadata at the backend boundary; do not import Rustic wire models into shared restore UI.
- Add backend implementations only when functional. Do not advertise Archive restore through a no-op implementation or invent a shared
  snapshot API that assumes Archive has Rustic's capabilities.
- Preserve configuration keys, JSON names and enum strings, manifest versions, directory layouts, Room schemas, and snapshot IDs. Pin wire names
  explicitly when renaming serialized properties. A source/package rename must not change stored backups.
- Update Kotlin native declarations, JNI exported names/class descriptors, linker export maps, call sites, and JNI tests together. Ensure export
  maps are linker dependencies so incremental builds relink after a symbol rename. Preserve AIDL signatures and
  parcel contracts unless the requested change explicitly requires a protocol change.
- Preserve restore order, user IDs, partial failure handling, ownership/permission/SELinux repair, and cleanup on both success and failure.
  Record unrelated defects separately instead of mixing behavior changes into a structural refactor.

## Testing Guidelines
- Test classes are temporary verification tools only. After verification passes, remove them and their dedicated fixtures and helpers before
  delivery. Do not retain test classes in the final source tree or commits, including example tests and temporary test classes in production packages.
- Keep a concise record of the checks and results. If a check fails or cannot run, record the unresolved limitation; cleanup does not mean it passed.
- Temporary JVM tests use JUnit4; temporary instrumentation tests use AndroidX JUnit/Espresso.
- Create temporary tests when changing backup logic, root-service behavior, database models, or JNI boundaries.
- Prefer module-scoped test runs while iterating (example: `./gradlew :native:testDebugUnitTest`).
- Before moving backup/restore logic, cover format compatibility, category/app-part selection, request generation and one-time consumption,
  session isolation, cancellation, progress/results, cache invalidation, source mapping, and cleanup where affected.
- Run module unit tests, `assembleDebug`, lint, and the Rust tests after cross-layer changes. JNI renames also require Native instrumentation
  tests when a device is available. Test destructive restore workflows only with disposable test data/devices.
- Temporary Root integration tests require an authorized debug app on a rooted test device. Do not count unavailable or skipped checks as passes.
- Preserve screen layout and navigation behavior during package moves. Check affected screen previews/navigation and report unavailable device
  or screenshot validation explicitly; a successful compilation is not evidence of a device restore round trip.

## Commit & Pull Request Guidelines
Git history shows conventional-style subjects for feature work (for example, `[Next] feat: ...`) plus automated translation commits.
- Prefer concise, imperative commit messages; include scope/tag when helpful.
- Link issues in commit/PR text when applicable (example: `(#453)`).
- PRs should include: change summary, affected modules (`app`/`hiddenapi`/`native`), test evidence (`./gradlew test`, device test notes), and screenshots for UI changes.
