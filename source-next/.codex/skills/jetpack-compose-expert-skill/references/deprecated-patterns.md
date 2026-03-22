# Deprecated Patterns & API Migrations in Jetpack Compose

This guide covers major API changes and deprecations in Compose's evolution. Each section shows the old pattern → new approach with migration notes.

---

## String-Based Routes → Type-Safe `@Serializable` Routes

**Old (pre-2.8):**
```kotlin
NavHost(navController, startDestination = "home") {
    composable("home") { HomeScreen() }
    composable("details/{id}") { backStackEntry ->
        DetailsScreen(id = backStackEntry.arguments?.getString("id"))
    }
}
```

**New (Navigation 2.8+):**
```kotlin
@Serializable data class Home
@Serializable data class Details(val id: String)

NavHost(navController, startDestination = Home) {
    composable<Home> { HomeScreen() }
    composable<Details> { backStackEntry ->
        val args: Details = backStackEntry.toRoute()
        DetailsScreen(id = args.id)
    }
}
```

**Migration notes:** Type-safe routes eliminate string typos and runtime crashes. Requires `kotlinx-serialization` plugin and `navigation-compose:2.8.0+`. Encode complex objects using custom serializers.

---

## `accompanist-systemuicontroller` → `enableEdgeToEdge()`

**Old:**
```kotlin
val systemUiController = rememberSystemUiController()
systemUiController.setSystemBarsColor(
    color = Color.Transparent,
    darkIcons = false
)
```

**New (Compose 1.7+):**
```kotlin
enableEdgeToEdge()
// In Activity.onCreate() before setContent {}
```

**Migration notes:** Built-in since Compose 1.7. Automatically handles status bar, navigation bar, and IME behind content. Remove `accompanist-systemuicontroller` dependency entirely.

---

## `accompanist-permissions` → `activity-compose`

**Old:**
```kotlin
val permissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
```

**New:**
```kotlin
val permissionState = rememberPermissionState(android.Manifest.permission.CAMERA) {
    // callback when permission changes
}
```

**Migration notes:** Functionality moved to `androidx.activity:activity-compose`. The API is largely identical; main change is dependency update. For multiple permissions, use `rememberMultiplePermissionsState()`.

---

## `accompanist-pager` → `HorizontalPager`/`VerticalPager`

**Old:**
```kotlin
val pagerState = rememberPagerState()
HorizontalPager(count = items.size, state = pagerState) { page ->
    PageContent(items[page])
}
```

**New (Foundation):**
```kotlin
val pagerState = rememberPagerState(pageCount = { items.size })
HorizontalPager(state = pagerState) { page ->
    PageContent(items[page])
}
```

**Migration notes:** Native Pager in `foundation:1.6+` replaces accompanist. Removes external dependency. State initialization slightly different; pass lambda for dynamic page counts.

---

## `accompanist-swiperefresh` → `PullToRefreshBox`

**Old:**
```kotlin
SwipeRefresh(state = rememberSwipeRefreshState(isRefreshing), onRefresh = { load() }) {
    LazyColumn { items(data) { item -> ItemRow(item) } }
}
```

**New (Material3):**
```kotlin
PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { load() }) {
    LazyColumn { items(data) { item -> ItemRow(item) } }
}
```

**Migration notes:** `PullToRefreshBox` in `material3:1.2+` is the official replacement. Cleaner API. Remove `accompanist-swiperefresh` dependency.

---

## `accompanist-flowlayout` → `FlowRow`/`FlowColumn`

**Old:**
```kotlin
FlowRow(mainAxisSize = SizeMode.Expand) {
    items.forEach { item -> Chip(text = item) }
}
```

**New (Foundation):**
```kotlin
FlowRow(modifier = Modifier.fillMaxWidth()) {
    items.forEach { item -> Chip(text = item) }
}
```

**Migration notes:** FlowRow/FlowColumn in `foundation:1.6+`. API simplified; use standard modifiers instead of `SizeMode`. Better performance and less memory overhead.

---

## `LazyColumn { animateItemPlacement() }` → `LazyColumn { animateItem() }`

**Old:**
```kotlin
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemRow(item.name, Modifier.animateItemPlacement())
    }
}
```

**New:**
```kotlin
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemRow(item.name, Modifier.animateItem())
    }
}
```

**Migration notes:** `animateItem()` is the modern API (Compose 1.7+). Returns animation state for finer control. `animateItemPlacement()` still works but is superseded.

---

## `Modifier.composed` Pattern → `Modifier.Node` API

**Old:**
```kotlin
fun Modifier.myModifier(value: Int) = composed {
    val state = remember { mutableStateOf(value) }
    Modifier.fillMaxWidth().padding(8.dp)
}
```

**New:**
```kotlin
fun Modifier.myModifier(value: Int) = this.then(
    Modifier
        .fillMaxWidth()
        .padding(8.dp)
)
// Or for complex state:
class MyModifierNode(val value: Int) : ModifierNodeElement<MyNodeImpl>() {
    override fun create() = MyNodeImpl(value)
    override fun update(node: MyNodeImpl) { node.value = value }
}
private class MyNodeImpl(var value: Int) : Modifier.Node
```

**Migration notes:** `composed {}` incurs overhead; avoid if no `remember` calls needed. For stateful modifiers, prefer `ModifierNode` API (Compose 1.8+). Benchmark before migrating existing code.

---

## Primitive State Optimization: `mutableStateOf(0)` → `mutableIntStateOf(0)`

**Old:**
```kotlin
var count by remember { mutableStateOf(0) }
var temperature by remember { mutableStateOf(37.5f) }
```

**New:**
```kotlin
var count by remember { mutableIntStateOf(0) }
var temperature by remember { mutableFloatStateOf(37.5f) }
```

**Migration notes:** Primitive-specific functions (`mutableIntStateOf`, `mutableFloatStateOf`, `mutableLongStateOf`) avoid boxing. Negligible performance impact in UI code but best practice since Compose 1.4+.

---

## `collectAsState()` → `collectAsStateWithLifecycle()`

**Old:**
```kotlin
val state by viewModel.uiState.collectAsState()
```

**New:**
```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

**Migration notes:** `collectAsStateWithLifecycle()` (Compose 1.6+) respects lifecycle—automatically stops collecting when activity is paused. Prevents memory leaks and redundant work. Requires `androidx.lifecycle:lifecycle-runtime-compose`.

---

## `@ExperimentalMaterial3Api` Graduation

**Old:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
fun MyScreen() {
    DatePicker(state = rememberDatePickerState())
}
```

**New (Compose 1.8+, Material3 1.3+):**
```kotlin
fun MyScreen() {
    DatePicker(state = rememberDatePickerState())
}
```

**Migration notes:** DatePicker, TimePicker, ExposedDropdownMenuBox, and SearchBar graduated to stable in Material3 1.3+. Remove `@OptIn` annotations. APIs are stable—safe for production use.

---

## `Scaffold` Padding Enforcement

**Old (problematic):**
```kotlin
Scaffold(topBar = { TopAppBar() }) {
    LazyColumn { items(data) { item -> ItemRow(item) } }
}
```

**New (required since 1.6+):**
```kotlin
Scaffold(topBar = { TopAppBar() }) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
        items(data) { item -> ItemRow(item) }
    }
}
```

**Migration notes:** Must use `innerPadding` parameter since Compose 1.6. Ignoring it causes content overlap under system bars. The compiler enforces this now—old pattern won't compile.

---

## Material 2 → Material 3 Migration

**Old (Material):**
```kotlin
Button(onClick = { }) { Text("Click") }
TextField(value = text, onValueChange = { text = it })
Surface(color = MaterialTheme.colors.primary) { /* */ }
```

**New (Material3):**
```kotlin
Button(onClick = { }) { Text("Click") }  // Same signature
TextField(value = text, onValueChange = { text = it })  // Same signature
Surface(color = MaterialTheme.colorScheme.primary) { /* */ }
```

**Migration notes:** Most Composables are API-compatible. Main changes: `colors` → `colorScheme`, new shape system, updated ripple defaults. Use Compose BOM to align Material3 versions.

---

## `WindowInsets` & Edge-to-Edge

**Old:**
```kotlin
Surface(modifier = Modifier.systemBarsPadding()) { /* */ }
```

**New (API 35+ default edge-to-edge):**
```kotlin
Surface(modifier = Modifier.padding(WindowInsets.systemBars.asPaddingValues())) { /* */ }
// Or use enableEdgeToEdge() in Activity—handles automatically
```

**Migration notes:** Edge-to-edge is default on Android 15+. System bar colors are managed by `enableEdgeToEdge()`. Use `WindowInsets.safeDrawing` for notch-aware layouts. Deprecate manual `systemBarsPadding()` calls.

---

## `ObservableState` Pattern Changes

**Old:**
```kotlin
@Composable
fun observe(state: ObservableState): State<T> = produceState(state.value) {
    state.onChange { value = it }
}
```

**New:**
```kotlin
@Composable
fun <T> ObservableState<T>.asState(): State<T> = produceState(this.value) {
    snapshotFlow { value }.collect { value = it }
}
```

**Migration notes:** `snapshotFlow {}` is preferred over direct listeners (Compose 1.6+). Integrates better with Compose's snapshot system. Use `distinctUntilChanged()` to avoid redundant recompositions.
