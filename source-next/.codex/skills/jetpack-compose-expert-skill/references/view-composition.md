# Jetpack Compose View Composition Reference

## Composable Function Naming Conventions

Names communicate intent. Follow these patterns consistently.

### Nouns (UI Components)
- PascalCase, describe *what* the composable displays
- Used for UI widgets, screens, layout building blocks

```kotlin
@Composable
fun Button(...)  // Displays a button

@Composable
fun UserCard(user: User)  // Displays a user card

@Composable
fun LoginScreen()  // Displays a login screen

@Composable
fun CheckboxWithLabel(...)  // Displays a checkbox with label
```

### Verbs (Side Effects / Effects)
- PascalCase, describe *what action* happens
- Used for composables that don't emit UI, only perform side effects

```kotlin
@Composable
fun LaunchedEffect(...)  // Launches a coroutine

@Composable
fun DisposableEffect(...)  // Manages resource lifecycle

@Composable
fun SideEffect(...)  // Performs a side effect
```

### Anti-Pattern: Ambiguous Names
```kotlin
// ❌ Unclear if this is a UI component or effect
@Composable
fun HandleLogin(...)

// ✅ Explicit
@Composable
fun LoginScreen(...)  // Displays UI

@Composable
fun PerformLogin(...)  // Side effect function (if truly a side effect)
```

## The Slot Pattern

Accept `@Composable` lambda parameters to create flexible, reusable containers.

### Basic Slot Pattern
```kotlin
@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color.White)
            .padding(16.dp)
    ) {
        content()
    }
}

// Usage
Card {
    Text("Hello")
    Image(...)
}
```

### Multiple Slots
```kotlin
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(modifier = modifier.padding(16.dp)) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            title()
            subtitle?.invoke()
        }
        trailing?.invoke()
    }
}

// Usage
ListItem(
    icon = { Icon(Icons.Default.Person, "") },
    title = { Text("Alice") },
    subtitle = { Text("Online") },
    trailing = { Badge() }
)
```

**Key principle:** Slots accept `@Composable` lambdas, not pre-composed values. This ensures composition is deferred and scope-aware.

```kotlin
// ❌ Wrong: passes composed value
fun CustomLayout(content: String) { ... }

// ✅ Correct: passes composition lambda
fun CustomLayout(content: @Composable () -> Unit) { ... }
```

Source: Material 3 composables in `androidx.compose.material3` use slots extensively.

## Extracting Composables

Know when to extract and when to keep composables together.

### Extract When
- **Reused in multiple places:** DRY principle
- **Single responsibility:** Composable handles one concern
- **Easier to test:** Small, focused unit
- **Performance:** Enables skipping and independent recomposition

```kotlin
// ❌ Before: god composable
@Composable
fun UserProfile(user: User) {
    Column {
        // Header
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(user.photo)
            Text(user.name, style = MaterialTheme.typography.headlineSmall)
            IconButton({ /* edit */ }) { Icon(Icons.Default.Edit, "") }
        }

        // Stats
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(user.followers, "Followers")
            StatItem(user.following, "Following")
            StatItem(user.posts, "Posts")
        }

        // Bio
        Text(user.bio, style = MaterialTheme.typography.bodyMedium)
    }
}

// ✅ After: extracted composables
@Composable
fun UserProfile(user: User) {
    Column {
        UserProfileHeader(user)
        UserStats(user)
        UserBio(user.bio)
    }
}

@Composable
private fun UserProfileHeader(user: User) { ... }

@Composable
private fun UserStats(user: User) { ... }

@Composable
private fun UserBio(bio: String) { ... }
```

### Don't Extract When
- **Single use:** Extraction adds indirection without benefit
- **Tightly coupled logic:** Would require passing many parameters
- **Too small:** Single `Text()` or `Icon()` doesn't need extraction

```kotlin
// ❌ Over-extraction: trivial wrapper
@Composable
private fun UserName(name: String) {
    Text(name, style = MaterialTheme.typography.headlineSmall)
}

// ✅ Keep inline if only used once
@Composable
fun UserProfile(user: User) {
    Text(user.name, style = MaterialTheme.typography.headlineSmall)
}
```

## Stateful vs Stateless Composables

Structure composables as a stateless layer with optional stateful wrapper.

### Pattern: Stateless + Wrapper

```kotlin
// ✅ Stateless composable (reusable, testable)
@Composable
fun ToggleButton(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    text: String
) {
    Button(
        onClick = { onToggle(!isEnabled) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) Color.Blue else Color.Gray
        )
    ) {
        Text(text)
    }
}

// ✅ Stateful wrapper (manages state, uses stateless child)
@Composable
fun StatefulToggleButton(text: String = "Toggle") {
    var isEnabled by remember { mutableStateOf(false) }
    ToggleButton(
        isEnabled = isEnabled,
        onToggle = { isEnabled = it },
        text = text
    )
}

// Usage: choose based on need
@Composable
fun MyScreen() {
    // Use stateless when caller manages state
    var featureEnabled by remember { mutableStateOf(false) }
    ToggleButton(featureEnabled, { featureEnabled = it }, "Feature")

    // Use stateful wrapper for isolated state
    StatefulToggleButton("Local Toggle")
}
```

**Advantage:** Caller can test and reuse `ToggleButton` without mocking state. `StatefulToggleButton` provides convenience for simple cases.

## Preview Annotations

Use previews for rapid UI development and regression testing.

### @Preview
Basic preview of a single composable.

```kotlin
@Preview
@Composable
fun UserCardPreview() {
    UserCard(user = User(1, "Alice"))
}

// Multiple previews
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun UserCardPreviews() {
    UserCard(user = User(1, "Alice"))
}
```

### @PreviewLightDark
Automatically generates light and dark theme previews.

```kotlin
@PreviewLightDark
@Composable
fun UserCardPreview() {
    MyTheme {
        UserCard(user = User(1, "Alice"))
    }
}
```

### @PreviewFontScale
Shows how composable responds to different font sizes.

```kotlin
@Preview(fontScale = 0.8f, name = "Small Fonts")
@Preview(fontScale = 1f, name = "Normal Fonts")
@Preview(fontScale = 1.2f, name = "Large Fonts")
@Composable
fun TextPreview() {
    Text("This is text")
}
```

### @PreviewScreenSizes
Tests multiple screen dimensions.

```kotlin
@Preview(device = Devices.PHONE, name = "Phone")
@Preview(device = Devices.TABLET, name = "Tablet")
@Preview(device = Devices.FOLDABLE, name = "Foldable")
@Composable
fun ResponsiveLayoutPreview() {
    ResponsiveLayout()
}
```

Source: `androidx.compose.ui.tooling.preview`

## CompositionLocal

Provide implicit parameters without threading them through the hierarchy.

### When to Use
- **Theming:** Pass `Colors`, `Typography` implicitly
- **Navigation:** Access from deep in composable tree
- **Locale/Strings:** Avoid passing through every composable

```kotlin
// ✅ Define at top level
val LocalUser = staticCompositionLocalOf<User?> { null }

@Composable
fun App(user: User) {
    CompositionLocalProvider(LocalUser provides user) {
        MainContent()
    }
}

// ✅ Access deep in tree without passing through every composable
@Composable
fun UserGreeting() {
    val user = LocalUser.current
    Text("Hello, ${user?.name}")
}
```

### When NOT to Use
- **Configuration params:** If only 1-2 levels deep, pass directly
- **Frequently changing values:** Can cause unnecessary recomposition
- **Dependencies:** Use dependency injection at ViewModel level

```kotlin
// ❌ Over-use: should pass `title` directly
val LocalTitle = staticCompositionLocalOf<String> { "" }

@Composable
fun Parent() {
    CompositionLocalProvider(LocalTitle provides "My Title") {
        Child()
    }
}

// ✅ Just pass it
@Composable
fun Parent(title: String) {
    Child(title = title)
}

@Composable
fun Child(title: String) { ... }
```

**Types:**
- `staticCompositionLocalOf`: Value rarely changes, default is a constant
- `compositionLocalOf`: Value changes frequently, default is computed

## Composable Return Values

**Never return values from composables.** Use callbacks instead.

```kotlin
// ❌ Wrong: composables don't return values
@Composable
fun UserInput(): String {
    var text by remember { mutableStateOf("") }
    return text  // Can't do this
}

// ✅ Correct: callback pattern
@Composable
fun UserInput(onUserInput: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = {
            text = it
            onUserInput(it)  // Notify parent
        }
    )
}

// Usage
@Composable
fun FormScreen() {
    UserInput(onUserInput = { input -> /* handle */ })
}
```

**Rationale:** Composables are executed during composition, which happens at unpredictable times and may be skipped or reordered.

## Screen-Level Composables

Structure screens as a thin ViewModel integration layer above pure composables.

### Recommended Pattern
```kotlin
// ✅ Screen composable: connects ViewModel
@Composable
fun UserDetailsScreen(
    viewModel: UserDetailsViewModel = hiltViewModel(),
    userId: String
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    UserDetailsContent(
        uiState = uiState,
        onRetry = { viewModel.loadUser(userId) }
    )
}

// ✅ Content composable: pure (testable, reusable)
@Composable
private fun UserDetailsContent(
    uiState: UiState,
    onRetry: () -> Unit
) {
    when (uiState) {
        is UiState.Loading -> LoadingUI()
        is UiState.Success -> SuccessUI(uiState.user)
        is UiState.Error -> ErrorUI(uiState.message, onRetry)
    }
}

// ✅ Composable for preview/testing
@Preview
@Composable
private fun UserDetailsContentPreview() {
    UserDetailsContent(
        uiState = UiState.Success(User(1, "Alice")),
        onRetry = {}
    )
}
```

**Benefits:**
- Public screen composable integrates ViewModel
- Private content composable is pure, testable, previewable
- Clear separation: UI logic (public) vs rendering (private)

**Anti-pattern:** Passing ViewModel to child composables. Keep it at screen level only.

```kotlin
// ❌ Couples child to ViewModel
@Composable
fun UserCard(viewModel: UserViewModel) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    Text(user.name)
}

// ✅ Pass only the data
@Composable
fun UserCard(user: User) {
    Text(user.name)
}
```

## Reusability Guidelines

Design composables to be configurable without over-parameterization.

### Configuration via Parameters
```kotlin
// ✅ Expose what varies, hide what doesn't
@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Internal: fixed styling, padding, etc.
    Box(
        modifier = modifier
            .background(Color.White)
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        content()
    }
}
```

### Avoid Parameter Explosion
```kotlin
// ❌ Too many parameters, hard to use
@Composable
fun Button(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    cornerRadius: Dp,
    padding: PaddingValues,
    elevation: Dp,
    ...
)

// ✅ Sensible defaults, style objects
@Composable
fun Button(
    text: String,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    onClick: () -> Unit
) { ... }

// Or: use Material composables with built-in styles
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ...
) { ... }  // Material3 Button has reasonable defaults
```

## Common Anti-Patterns

### God Composables
```kotlin
// ❌ Does too much
@Composable
fun Dashboard() {
    // Header
    Box { /* 20 lines */ }

    // List
    LazyColumn {
        items(items) { /* 15 lines */ }
    }

    // Footer
    Box { /* 15 lines */ }

    // Dialogs, side effects, state management...
}

// ✅ Extract and delegate
@Composable
fun Dashboard() {
    Column {
        DashboardHeader()
        DashboardContent()
        DashboardFooter()
    }
}
```

### Deep Nesting
```kotlin
// ❌ Hard to read and debug
@Composable
fun LoginScreen() {
    Box { Column { Row { Card { Box { Text { ... } } } } } }
}

// ✅ Intermediate variables and extraction
@Composable
fun LoginScreen() {
    val form = loginFormState()
    Column {
        LoginForm(form)
        LoginButton(form)
    }
}
```

### Passing ViewModel to Children
```kotlin
// ❌ Violates composition boundaries
@Composable
fun ParentScreen(viewModel: ParentViewModel) {
    ChildCard(viewModel = viewModel)  // Don't do this
}

// ✅ Extract data, pass to child
@Composable
fun ParentScreen(viewModel: ParentViewModel) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    ChildCard(data = data)
}
```

---

**Source references:** `androidx.compose.material3`, `androidx.compose.ui.tooling.preview`, `androidx.compose.runtime.CompositionLocal`
