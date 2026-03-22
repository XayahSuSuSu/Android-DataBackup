# Jetpack Compose Side Effects Reference

Compose is declarative, but apps must interact with the imperative world: launch coroutines, register listeners, manage resources. Side effects are the bridge. Understanding when and how to use them is essential for correctness.

## The Effect Mental Model

Compose recomposes when state changes. Effects are blocks of code that run outside the normal composition and recomposition cycle:

- **Composition**: Calculate the UI tree
- **Side effects**: Run imperative code (coroutines, callbacks, lifecycle events)
- **Layout**: Measure and position elements
- **Drawing**: Render to screen

Effects run *after* composition succeeds. If composition fails, the effect doesn't run.

```kotlin
@Composable
fun MyScreen() {
    // This runs during composition
    val state = remember { mutableStateOf("initial") }

    // This runs AFTER composition, and only when 'state.value' changes
    LaunchedEffect(state.value) {
        println("State changed to: ${state.value}")
    }

    // This runs after every composition (use sparingly)
    SideEffect {
        println("Recomposition happened")
    }

    // This runs when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            println("Composable is leaving composition")
        }
    }

    Button(onClick = { state.value = "updated" }) {
        Text(state.value)
    }
}
```

## SideEffect — After Every Successful Composition

`SideEffect` runs after *every* successful composition. It has no cleanup, no keys, and always executes.

```kotlin
@Composable
fun MyComposable() {
    var clickCount by remember { mutableStateOf(0) }

    // Runs after every recomposition
    SideEffect {
        println("Recomposed! Click count: $clickCount")
    }

    Button(onClick = { clickCount++ }) {
        Text("Clicks: $clickCount")
    }
}
```

### Use Cases

- Synchronizing Compose state with external systems (e.g., Analytics logging)
- Updating non-Compose UI elements
- One-way synchronization where cleanup isn't needed

```kotlin
@Composable
fun TrackScreenView(screenName: String) {
    SideEffect {
        Analytics.logScreenView(screenName)
    }
}
```

**Do:** Use for simple, stateless synchronization.
**Don't:** Use for resource allocation (use `DisposableEffect` instead).

Source: `compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Effects.kt`

## LaunchedEffect(key) — Coroutines Scoped to Composition

`LaunchedEffect` launches a coroutine in a scope tied to the composable's lifecycle. The coroutine is cancelled if the key changes or the composable leaves composition.

```kotlin
@Composable
fun DataLoader(userId: String) {
    var data by remember { mutableStateOf<String?>(null) }

    // Coroutine runs when userId changes or composable enters composition
    LaunchedEffect(userId) {
        data = loadData(userId)  // suspend function
    }

    Text(data ?: "Loading...")
}
```

### Key Selection

```kotlin
// Key = Unit: runs once when composable enters composition, never cancels/restarts
LaunchedEffect(Unit) {
    setupOnce()
}

// Key = specific value: reruns whenever the value changes
var userId by remember { mutableStateOf("user1") }
LaunchedEffect(userId) {
    loadUserData(userId)  // reruns when userId changes
}

// Multiple keys: reruns if ANY key changes
LaunchedEffect(userId, postId) {
    loadUserAndPost(userId, postId)
}

// No key parameter (not recommended): equivalent to Unit
LaunchedEffect {
    setupOnce()
}
```

### Common Mistake: Wrong Key Selection

```kotlin
// Don't: Key changes every recomposition (creates infinite loop)
@Composable
fun BadKeySelection() {
    var count by remember { mutableStateOf(0) }
    val randomKey = Random.nextInt()  // Changes every recomposition!

    LaunchedEffect(randomKey) {
        count++  // This launches infinitely
    }

    Text("Count: $count")
}

// Do: Use stable keys that represent the data you depend on
@Composable
fun GoodKeySelection(userId: String) {
    var userData by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(userId) {
        userData = fetchUser(userId)
    }

    Text(userData?.name ?: "Loading...")
}
```

### Cancellation Behavior

```kotlin
@Composable
fun ResourceUser(shouldLoad: Boolean) {
    LaunchedEffect(shouldLoad) {
        if (shouldLoad) {
            val resource = acquireResource()
            try {
                delay(5000)  // Long operation
                processResource(resource)
            } finally {
                resource.close()  // Runs even if cancelled
            }
        }
    }
}

// If shouldLoad becomes false, the LaunchedEffect coroutine is cancelled.
// The finally block ensures cleanup.
```

## DisposableEffect(key) — For Cleanup

`DisposableEffect` runs after composition and requires a cleanup function (onDispose). Use for listeners, registrations, and resources.

```kotlin
@Composable
fun LocationListener(context: Context) {
    DisposableEffect(context) {
        val listener = LocationListener { location ->
            println("Location: $location")
        }
        // Register listener
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0,
            0f,
            listener
        )

        // Cleanup: unregister listener
        onDispose {
            locationManager.removeUpdates(listener)
        }
    }
}
```

### Common Pattern: Lifecycle Events

```kotlin
@Composable
fun ScreenWithLifecycle() {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> println("Screen resumed")
                Lifecycle.Event.ON_PAUSE -> println("Screen paused")
                else -> {}
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}
```

**Do:** Use `DisposableEffect` for every resource you allocate.
**Don't:** Forget the `onDispose` block (resource leaks result).

Source: `compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Effects.kt`

## rememberCoroutineScope — Launching from Event Handlers

`rememberCoroutineScope` provides a coroutine scope tied to the composable. Use it to launch coroutines from event handlers (clicks, gestures).

```kotlin
@Composable
fun ButtonWithAsync() {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf("") }

    Button(
        onClick = {
            // Launch coroutine from click handler
            scope.launch {
                result = fetchData()
            }
        }
    ) {
        Text("Fetch")
    }

    Text(result)
}
```

### Do vs Don't

```kotlin
// Don't: regular function scope doesn't work
@Composable
fun BadAsync() {
    var result by remember { mutableStateOf("") }

    Button(
        onClick = {
            runBlocking {  // Blocks UI thread!
                result = fetchData()
            }
        }
    ) {
        Text("Fetch")
    }
}

// Do: use rememberCoroutineScope
@Composable
fun GoodAsync() {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf("") }

    Button(
        onClick = {
            scope.launch {
                result = fetchData()
            }
        }
    ) {
        Text("Fetch")
    }
}
```

## rememberUpdatedState — Capturing Latest Values

Long-running effects need the latest value of frequently-changing state, but you don't want to restart the effect on every change.

```kotlin
// Don't: effect restarts when callback changes
@Composable
fun BadCallback(onSuccess: (String) -> Unit) {
    LaunchedEffect(onSuccess) {  // Restarts whenever onSuccess changes!
        val result = expensiveOperation()
        onSuccess(result)
    }
}

// Do: use rememberUpdatedState to capture latest without restarting
@Composable
fun GoodCallback(onSuccess: (String) -> Unit) {
    val updatedOnSuccess = rememberUpdatedState(onSuccess)

    LaunchedEffect(Unit) {
        val result = expensiveOperation()
        updatedOnSuccess.value(result)
    }
}
```

### Another Example: Animations

```kotlin
@Composable
fun AnimateWithCallback(
    shouldAnimate: Boolean,
    onAnimationEnd: () -> Unit
) {
    val updatedCallback = rememberUpdatedState(onAnimationEnd)
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(shouldAnimate) {
        if (shouldAnimate) {
            while (progress < 1f) {
                progress += 0.1f
                delay(16)
            }
            updatedCallback.value()  // Call latest callback without restarting
        }
    }
}
```

## produceState — Converting Non-Compose State to Compose State

`produceState` converts imperative state sources (callbacks, flows, coroutines) into Compose state.

```kotlin
@Composable
fun UserData(userId: String): State<User?> = produceState<User?>(initialValue = null) {
    value = fetchUser(userId)

    // Optional: for lifecycle cleanup
    snapshotFlow { userId }.collect { newUserId ->
        value = fetchUser(newUserId)
    }
}

// Usage
@Composable
fun UserScreen(userId: String) {
    val user by UserData(userId)
    Text(user?.name ?: "Loading...")
}
```

### Integration with Flows

```kotlin
@Composable
fun <T> Flow<T>.collectAsState(initial: T): State<T> = produceState(initial) {
    collect { value = it }
}

// Usage
@Composable
fun ObserveFlow(dataFlow: Flow<String>) {
    val data by dataFlow.collectAsState(initial = "")
    Text(data)
}
```

## Effect Ordering and Lifecycle

Effects execute in declaration order after composition:

```kotlin
@Composable
fun EffectOrder() {
    println("1. Composition")

    SideEffect {
        println("4. Side effect (after every composition)")
    }

    LaunchedEffect(Unit) {
        println("3. Launched effect (async, but scheduled)")
        delay(100)
        println("5. After delay in launched effect")
    }

    DisposableEffect(Unit) {
        println("2. Disposable effect setup (after composition)")

        onDispose {
            println("6. Cleanup when leaving composition")
        }
    }

    println("End of composition body")
}

// Output order (approximate):
// 1. Composition
// End of composition body
// 2. Disposable effect setup (after composition)
// 3. Launched effect (async, but scheduled)
// 4. Side effect (after every composition)
// 5. After delay in launched effect
// [... later when composable leaves ...]
// 6. Cleanup when leaving composition
```

## Common Mistakes

### Using LaunchedEffect(Unit) When Key Should Change

```kotlin
// Don't: effect runs once, never updates
@Composable
fun BadSearch(query: String) {
    var results by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        results = search(query)  // Only runs once!
    }

    Text("Results: ${results.size}")
}

// Do: use query as key
@Composable
fun GoodSearch(query: String) {
    var results by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(query) {
        results = search(query)  // Reruns when query changes
    }

    Text("Results: ${results.size}")
}
```

### Forgetting Cleanup in DisposableEffect

```kotlin
// Don't: memory leak
@Composable
fun BadListener(context: Context) {
    DisposableEffect(Unit) {
        val listener = MyListener()
        context.registerListener(listener)
        // Missing: onDispose { context.unregisterListener(listener) }
    }
}

// Do: always clean up
@Composable
fun GoodListener(context: Context) {
    DisposableEffect(Unit) {
        val listener = MyListener()
        context.registerListener(listener)

        onDispose {
            context.unregisterListener(listener)
        }
    }
}
```

### Capturing Mutable State Directly

```kotlin
// Don't: stale state in effect
@Composable
fun BadCapture() {
    var count by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(1000)
        println(count)  // May be stale!
    }

    Button(onClick = { count++ }) { Text("Click") }
}

// Do: use rememberUpdatedState or include in key
@Composable
fun GoodCapture() {
    var count by remember { mutableStateOf(0) }

    val updatedCount = rememberUpdatedState(count)
    LaunchedEffect(Unit) {
        delay(1000)
        println(updatedCount.value)  // Always current
    }

    Button(onClick = { count++ }) { Text("Click") }
}
```

---

**Summary:** Effects bridge declarative Compose with imperative systems. Master key selection in `LaunchedEffect`, always cleanup in `DisposableEffect`, use `rememberUpdatedState` for long-running effects that need fresh values, and prefer effect-based patterns over manual lifecycle management.
