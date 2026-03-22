# Jetpack Compose State Management Reference

## State Fundamentals

State in Compose is observable data that triggers recomposition when changed.

### Creating State

Use type-specific state holders for efficiency:

```kotlin
// General-purpose state (Any type)
val name = mutableStateOf("Alice")

// Primitive specializations (avoid boxing)
val count = mutableIntStateOf(0)
val progress = mutableFloatStateOf(0.5f)
val enabled = mutableStateOf(true)  // Boolean has no specialization
```

**Pitfall:** Using `mutableStateOf<Int>()` instead of `mutableIntStateOf()` causes unnecessary boxing on every read/write. Primitive specializations are located in `androidx.compose.runtime` (source: `State.kt`).

## remember vs rememberSaveable

Both associate state with a composition key, but differ in persistence scope.

### remember
- Lives for the composition's lifetime
- Lost on process death, configuration changes, back navigation
- Best for UI state: selection, expanded/collapsed, scroll position

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

### rememberSaveable
- Survives process death and configuration changes
- Uses `Bundle`-compatible types by default (String, Int, Boolean, etc.)
- For custom types, provide a `Saver` or use `@Parcelize`
- Best for data that represents user input or navigation state

```kotlin
@Composable
fun SearchScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    // survives configuration change
}

// Custom type requires explicit Saver
data class User(val id: Int, val name: String)
val userSaver = Saver<User, String>(
    save = { "${it.id}:${it.name}" },
    restore = { parts -> User(parts.split(":")[0].toInt(), parts.split(":")[1]) }
)
var user by rememberSaveable(stateSaver = userSaver) { mutableStateOf(User(1, "Alice")) }
```

**Pitfall:** Assuming `rememberSaveable` works with all types. Custom classes need explicit `Saver` or `@Parcelize`. See `SaveableStateRegistry` in `androidx.compose.runtime.saveable`.

## State Hoisting

Move state up to a parent composable to enable reusability and testing.

### Stateful vs Stateless Pattern

```kotlin
// ❌ Stateful version (tightly coupled)
@Composable
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }) { Text(count.toString()) }
}

// ✅ Stateless version (reusable, testable)
@Composable
fun Counter(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Button(onClick = { onCountChange(count + 1) }) { Text(count.toString()) }
}

// ✅ Wrapper composable (provides state, uses stateless child)
@Composable
fun StatefulCounter() {
    var count by remember { mutableIntStateOf(0) }
    Counter(count = count, onCountChange = { count = it })
}
```

**Rule:** Push state as high as needed, but no higher. If only one child needs state, keep it there. If multiple children or parents need it, hoist up.

## derivedStateOf

Computes a value from existing state, recomputing only when dependencies change.

```kotlin
// ❌ Wrong: recomputes on every recomposition
val isEven = count % 2 == 0

// ✅ Correct: recomputes only when count changes
val isEven = derivedStateOf { count % 2 == 0 }
```

**When to use:**
- Expensive computations from state (e.g., filtering, sorting lists)
- Combining multiple state values
- Creating intermediate state for conditional logic

```kotlin
@Composable
fun UserList(users: List<User>, filterText: String) {
    val filteredUsers = derivedStateOf {
        users.filter { it.name.contains(filterText, ignoreCase = true) }
    }

    LazyColumn {
        items(filteredUsers.value.size) { index ->
            UserRow(filteredUsers.value[index])
        }
    }
}
```

**Pitfall:** Using `derivedStateOf` for cheap operations (String concatenation, simple conditions) adds overhead. Only use when the computation is non-trivial.

**Pitfall:** Accessing `.value` in a lambda passed to a child composable doesn't create a dependency. Use `snapshotFlow` for callbacks.

## snapshotFlow

Converts Compose state to Kotlin Flow for side effects and external APIs.

```kotlin
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .debounce(500)
            .distinctUntilChanged()
            .collect { viewModel.search(it) }
    }
}
```

**Key behaviors:**
- Emits initial value, then only on changes
- Works with derivedStateOf, collections, and nested state
- Runs in the composition's coroutine scope (launched via `LaunchedEffect`)

**Pitfall:** Accessing state directly in a `LaunchedEffect` doesn't track changes:
```kotlin
// ❌ Won't re-run when query changes
LaunchedEffect(Unit) {
    viewModel.search(query)  // Capture at launch time only
}

// ✅ Re-runs when query changes
LaunchedEffect(query) {
    viewModel.search(query)
}
```

## SnapshotStateList and SnapshotStateMap

Observable collections that trigger recomposition on structural changes.

```kotlin
val items = remember { mutableStateListOf<Item>() }
items.add(Item(1, "First"))
items[0] = Item(1, "Updated")
items.removeAt(0)

val map = remember { mutableStateMapOf<String, String>() }
map["key"] = "value"  // Triggers recomposition
```

**Important:** Changes to list contents trigger recomposition, but changes to list *elements* (if they're mutable objects) do not.

```kotlin
data class Item(val id: Int, var name: String)

val items = remember { mutableStateListOf(Item(1, "First")) }

// ✅ Triggers recomposition (list structure changed)
items[0] = Item(1, "Updated")

// ❌ Does NOT trigger recomposition (object mutated in-place)
items[0].name = "Updated"  // Mutated but list reference unchanged

// ✅ Correct: use copy() or mutableStateOf for nested state
items[0] = items[0].copy(name = "Updated")
```

See source: `androidx.compose.runtime.snapshots` for collection implementation.

## @Stable and @Immutable Annotations

These annotations help the compiler optimize recomposition (strong skipping mode).

### @Immutable
- All public fields are read-only primitives or other `@Immutable` types
- Instances never change after construction
- Compiler can skip recomposition if parameter unchanged

```kotlin
@Immutable
data class User(val id: Int, val name: String)
```

### @Stable
- Implements structural equality (`equals`)
- Public properties are read-only or observable
- Changes are always notified to Compose (through state objects)
- Weaker guarantee than `@Immutable`, but suitable for types with observable state

```kotlin
@Stable
class UserViewModel {
    val userName: State<String> = mutableStateOf("")
    val isLoading: State<Boolean> = mutableStateOf(false)

    // Observable state, not direct properties
}
```

**Pitfall:** Not annotating data classes used as parameters. Unannotated types are assumed unstable, triggering unnecessary recompositions.

```kotlin
// ❌ Treated as unstable, causes recomposition
class Config(val title: String, val color: Color)

// ✅ Properly annotated
@Immutable
class Config(val title: String, val color: Color)
```

## Strong Skipping Mode

In Compose 1.6+, strong skipping mode applies stricter recomposition logic.

**What changed:**
- Composables skip recomposition if *all* parameters have unchanged identity and value
- Unannotated parameter types are treated as unstable (always recompose)
- `@Stable` and `@Immutable` annotations are now critical for performance
- Lambda parameters always cause recomposition (they're new instances)

**Enable strong skipping:**
```gradle
composeOptions {
    kotlinCompilerExtensionVersion = "1.5.4+"  // enables by default
}
```

**Practical impact:**
```kotlin
// ❌ These create new instances, always recompose child
@Composable
fun Parent() {
    Child(title = buildString { append("Title") })
    Child(config = Config(...))  // Unstable type
}

// ✅ Cache instances
@Composable
fun Parent() {
    val title = remember { "Title" }
    val config = remember { Config(...) }
    Child(title = title)
    Child(config = config)
}
```

## State in ViewModels: StateFlow vs Compose State

### StateFlow (Recommended for ViewModel)
- Survives composition recomposition and configuration changes
- Works with lifecycle (`collectAsStateWithLifecycle`)
- Thread-safe, works across layers

```kotlin
class UserViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}

@Composable
fun UserScreen(viewModel: UserViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> SuccessScreen((uiState as UiState.Success).data)
        is UiState.Error -> ErrorScreen((uiState as UiState.Error).message)
    }
}
```

### Compose State (For UI-only state)
- Use for temporary, UI-local state
- Don't hoist to ViewModel
- Lost on back navigation

```kotlin
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    var showFilters by remember { mutableStateOf(false) }  // UI-only
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    SearchUI(
        results = searchResults,
        showFilters = showFilters,
        onToggleFilters = { showFilters = !showFilters }
    )
}
```

**Key difference:** `collectAsStateWithLifecycle()` (in `androidx.lifecycle:lifecycle-runtime-compose`) collects only when the composable is in a STARTED state, avoiding memory leaks.

## Common Anti-Patterns

### State in Local Variables
```kotlin
// ❌ Lost on recomposition
@Composable
fun Counter() {
    var count = 0  // Reset to 0 on every recomposition
    Button(onClick = { count++ }) { Text(count.toString()) }
}

// ✅ Correct
@Composable
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }) { Text(count.toString()) }
}
```

### Reading State in Wrong Scope
```kotlin
// ❌ Reads happen inside lambda; changes don't re-launch effect
var count by remember { mutableIntStateOf(0) }
LaunchedEffect(Unit) {
    while (true) {
        delay(1000)
        println(count)  // Always prints 0
    }
}

// ✅ Pass state to LaunchedEffect key
LaunchedEffect(count) {
    println("Count changed: $count")
}
```

### Creating State in Lambdas
```kotlin
// ❌ Creates new state on every call
val onButtonClick = {
    val newValue = remember { mutableStateOf(0) }  // ERROR: Can't call remember in lambda
}

// ✅ Create state at composition level
var value by remember { mutableIntStateOf(0) }
val onButtonClick = { value++ }
```

---

**Source references:** `androidx.compose.runtime.State`, `androidx.compose.runtime.saveable`, `androidx.lifecycle.runtime.compose`
