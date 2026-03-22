# CompositionLocals: Implicit Data Passing in Jetpack Compose

CompositionLocals provide a way to pass data implicitly down the composition tree without threading it through every function parameter. They're analogous to SwiftUI's `@Environment`.

## What Are CompositionLocals?

A CompositionLocal is a slot in the composition that holds a value accessible to any descendant composable without explicit parameter passing. Values are provided using `CompositionLocalProvider` and accessed via `current`.

```kotlin
val localAppTheme = compositionLocalOf { "Light" }

@Composable
fun MyScreen() {
  CompositionLocalProvider(localAppTheme provides "Dark") {
    DescendantComposable() // Can access "Dark" via localAppTheme.current
  }
}

@Composable
fun DescendantComposable() {
  Text(localAppTheme.current) // Reads "Dark"
}
```

**Source:** `androidx/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/CompositionLocal.kt`

## compositionLocalOf vs staticCompositionLocalOf

The key difference is **when recomposition is triggered** when a value changes.

### compositionLocalOf
Causes recomposition of all descendants when the value changes. Use when children genuinely depend on the value.

```kotlin
val LocalUserPreferences = compositionLocalOf { UserPreferences() }
```

**Recomposition behavior:** All consumers recompose.

### staticCompositionLocalOf
No recomposition of descendants; only the direct reader is affected. Use when you're **confident descendants don't depend on updates**, or updates are infrequent.

```kotlin
val LocalAppVersion = staticCompositionLocalOf { "1.0.0" }
```

**⚠️ Pitfall:** If a child reads `LocalAppVersion.current` and expects updates, you'll get stale data. Only use for truly static configuration.

### compositionLocalWithComputedDefaultOf
Introduced for computed default values. The lambda is called each time the value is read when no provider is active.

```kotlin
val LocalResources = compositionLocalWithComputedDefaultOf { context.resources }
```

This is more efficient than `compositionLocalOf { lazy { ... } }` because it avoids capturing state unnecessarily.

## Built-In CompositionLocals

The Compose runtime and UI libraries provide standard locals:

| Local | Type | Purpose |
|-------|------|---------|
| `LocalContext` | `Context` | Android Context (requires AndroidCompositionLocals) |
| `LocalConfiguration` | `Configuration` | Screen size, orientation, density |
| `LocalDensity` | `Density` | Pixel density for dp/px conversion |
| `LocalLayoutDirection` | `LayoutDirection` | LTR/RTL directionality |
| `LocalView` | `View` | Underlying Android View (if available) |
| `LocalLifecycleOwner` | `LifecycleOwner` | Activity/Fragment lifecycle |
| `LocalSavedStateRegistryOwner` | `SavedStateRegistryOwner` | For state persistence |

**Source:** `androidx/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidCompositionLocals.android.kt`

```kotlin
@Composable
fun MyComposable() {
  val context = LocalContext.current
  val density = LocalDensity.current
  val config = LocalConfiguration.current

  Text("Screen width: ${config.screenWidthDp}dp")
}
```

## Providing Values with CompositionLocalProvider

Provide one or multiple local values:

```kotlin
// Single local
CompositionLocalProvider(LocalUserPreferences provides user) {
  Content()
}

// Multiple locals
CompositionLocalProvider(
  LocalUserPreferences provides user,
  LocalTheme provides darkTheme,
  LocalLanguage provides "en"
) {
  Content()
}
```

Values are **scoped** to descendants only:

```kotlin
CompositionLocalProvider(LocalUserPreferences provides userA) {
  ComponentA() // Sees userA
  CompositionLocalProvider(LocalUserPreferences provides userB) {
    ComponentB() // Sees userB (overrides)
  }
  ComponentC() // Sees userA (original)
}
```

## Creating Custom CompositionLocals

Create locals at top level, outside composable functions:

```kotlin
data class AppTheme(val isDark: Boolean, val colors: Colors)

val LocalAppTheme = compositionLocalOf<AppTheme> {
  error("AppTheme not provided")
}

// For nullable defaults
val LocalOptionalUser = compositionLocalOf<User?> { null }
```

**When to create a CompositionLocal:**
- Value is needed by many descendants
- Threading it as a parameter creates "prop drilling"
- Value is configuration-like (theme, locale, permissions)

**When NOT to use CompositionLocal:**
- Only 1–2 levels of composables need it → use parameters
- Value changes frequently and children need precise control → use State/ViewModel
- It's a dependency that should be testable → prefer parameters or dependency injection

## Testing with CompositionLocals

Provide test doubles to avoid real implementations:

```kotlin
@Composable
fun MyScreen() {
  val user = LocalUserRepository.current
  Text(user.name)
}

// In test
@Test
fun testMyScreen() {
  composeRule.setContent {
    CompositionLocalProvider(
      LocalUserRepository provides FakeUserRepository(User("Test User"))
    ) {
      MyScreen()
    }
  }
  composeRule.onNodeWithText("Test User").assertExists()
}
```

## Anti-Patterns

### ✗ Using CompositionLocal as Generic Dependency Injection
```kotlin
// Bad: obscures dependencies, hard to test
val LocalEverything = compositionLocalOf { AppContainer() }

@Composable
fun MyScreen() {
  val container = LocalEverything.current
  val repo = container.userRepo
  val cache = container.cache
}
```

**Better:** Provide specific locals or pass dependencies as parameters.

### ✗ Reading LocalContext Repeatedly
```kotlin
// Inefficient: reads on every recomposition
@Composable
fun MyComposable() {
  val context = LocalContext.current // Reading repeatedly
  // ...
}
```

**Better:** Read once outside the lambda or cache in remember:

```kotlin
@Composable
fun MyComposable() {
  val context = LocalContext.current
  val effect = remember(context) { /* use context */ }
}
```

### ✗ Storing Mutable State in CompositionLocal
```kotlin
// Bad: state changes won't trigger recomposition properly
val LocalCounter = compositionLocalOf { mutableStateOf(0) }
```

**Better:** Store the State in a parent composable and provide the value, not the State:

```kotlin
val LocalCount = compositionLocalOf { 0 }

@Composable
fun Parent() {
  var count by remember { mutableStateOf(0) }
  CompositionLocalProvider(LocalCount provides count) {
    Child()
  }
}
```

## Key Takeaways

1. Use `compositionLocalOf` for values that children read and depend on updates
2. Use `staticCompositionLocalOf` only for truly static values
3. Prefer parameters over CompositionLocals unless you have significant nesting
4. Always provide a sensible error default or nullable type
5. Test by providing fake implementations via `CompositionLocalProvider`
6. CompositionLocals are not a replacement for proper architecture — use them for configuration and environment data, not general dependency injection
