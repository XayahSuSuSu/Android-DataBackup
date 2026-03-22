# Animation in Jetpack Compose

Reference: `androidx/compose/animation/animation/src/commonMain/kotlin/androidx/compose/animation/`

## State-Based Animations

### animate*AsState

Animate individual properties by targeting a value. The animation starts when the value changes.

```kotlin
val size by animateDpAsState(
    targetValue = if (isExpanded) 200.dp else 100.dp,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "size"
)

Box(modifier = Modifier.size(size))
```

Common variants:

```kotlin
animateColorAsState(targetValue = Color.Blue)
animateFloatAsState(targetValue = 1f)
animateIntAsState(targetValue = 100)
animateOffsetAsState(targetValue = Offset(10f, 20f))
```

Each automatically handles coroutines and recomposition. Use the `label` parameter for debugging.

## AnimatedVisibility

Controls appear/disappear animations with enter and exit transitions.

```kotlin
var visible by remember { mutableStateOf(true) }

AnimatedVisibility(visible = visible) {
    Text("Hello!")
}

// Trigger
Button(onClick = { visible = !visible }) { Text("Toggle") }
```

### Enter/Exit Transitions

```kotlin
AnimatedVisibility(
    visible = visible,
    enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
    exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
) {
    Text("Animated!")
}
```

Built-in transitions:
- `slideInVertically`, `slideOutVertically`
- `slideInHorizontally`, `slideOutHorizontally`
- `expandVertically`, `shrinkVertically`
- `expandHorizontally`, `shrinkHorizontally`
- `fadeIn`, `fadeOut`
- `scaleIn`, `scaleOut`
- Combine with `+`: `slideInVertically() + fadeIn()`

### Advanced: Custom animation specs

```kotlin
AnimatedVisibility(
    visible = visible,
    enter = slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight },
        animationSpec = spring()
    ),
    exit = slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(durationMillis = 300)
    )
) {
    Box(Modifier.fillMaxWidth().height(100.dp).background(Color.Blue))
}
```

## AnimatedContent

Replace content with smooth transitions.

```kotlin
var count by remember { mutableStateOf(0) }

AnimatedContent(targetState = count) { target ->
    Text(text = "Count: $target")
}

Button(onClick = { count++ }) { Text("Increment") }
```

### Custom transitionSpec

```kotlin
AnimatedContent(
    targetState = count,
    transitionSpec = {
        slideInVertically(initialOffsetY = { it }) with slideOutVertically(targetOffsetY = { -it })
    }
) { target ->
    Text("$target")
}
```

Use `with` to specify exit and enter together. This runs exits and entries simultaneously.

### Sequencing transitions

```kotlin
AnimatedContent(
    targetState = count,
    transitionSpec = {
        slideInVertically(initialOffsetY = { it }) with slideOutVertically(targetOffsetY = { -it }) using SizeTransform(clip = false)
    }
) { target ->
    Text(
        "Count: $target",
        modifier = Modifier.fillMaxWidth()
    )
}
```

`SizeTransform` animates container size smoothly during content changes.

## Crossfade

Simple content swap with fade effect.

```kotlin
var showFirst by remember { mutableStateOf(true) }

Crossfade(targetState = showFirst) { state ->
    if (state) {
        Text("First")
    } else {
        Text("Second")
    }
}
```

Lightweight alternative to `AnimatedContent` for simple visibility toggles.

## updateTransition

Coordinate multiple animated values with a single state.

```kotlin
var expanded by remember { mutableStateOf(false) }
val transition = updateTransition(targetState = expanded)

val size by transition.animateDp { if (it) 200.dp else 100.dp }
val color by transition.animateColor { if (it) Color.Blue else Color.Red }

Box(
    modifier = Modifier
        .size(size)
        .background(color)
        .clickable { expanded = !expanded }
)
```

All animations run in sync, controlled by a single state change. Useful for complex components with multiple animated properties.

## rememberInfiniteTransition

Create looping animations.

```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "infinite")

val alpha by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000),
        repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
)

Text("Pulsing", modifier = Modifier.alpha(alpha))
```

Runs continuously until the composable is removed. Perfect for loading states, pulsing indicators.

## Animatable

Imperative animation control in coroutines. Use for fine-grained control.

```kotlin
val animatable = remember { Animatable(0f) }

LaunchedEffect(trigger) {
    animatable.animateTo(
        targetValue = 100f,
        animationSpec = spring()
    )
}

Box(Modifier.graphicsLayer(translationX = animatable.value))
```

Useful for responding to gestures or complex conditions:

```kotlin
val animatable = remember { Animatable(0f) }

LaunchedEffect(Unit) {
    animatable.animateTo(targetValue = 360f, animationSpec = tween(2000))
}

Box(
    Modifier
        .size(100.dp)
        .background(Color.Blue)
        .graphicsLayer(rotationZ = animatable.value)
)
```

## Animation Specifications

### spring — Realistic, physics-based

```kotlin
val size by animateDpAsState(
    targetValue = 200.dp,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
)
```

- `dampingRatio`: `NoBouncy` (1f), `LowBouncy` (0.75f), `MediumBouncy` (0.5f), `HighBouncy` (0.2f)
- `stiffness`: `Low`, `Medium`, `High`

Use for interactive feedback, familiar to users.

### tween — Time-based

```kotlin
val color by animateColorAsState(
    targetValue = Color.Blue,
    animationSpec = tween(durationMillis = 500, easing = EaseInOutCubic)
)
```

Easing functions: `EaseInQuad`, `EaseOutQuad`, `EaseInOutQuad`, `LinearEasing`, `FastOutSlowInEasing`.

Predictable timing, good for sequential animations.

### keyframes — Frame-by-frame control

```kotlin
val position by animateFloatAsState(
    targetValue = 100f,
    animationSpec = keyframes {
        0f at 0 using EaseInQuad
        50f at 150 using EaseOutQuad
        100f at 300
    }
)
```

Define exact values at specific timestamps. Use for complex choreography.

## Automatic Size Animation

### animateContentSize

Smoothly animate Box size when content changes.

```kotlin
var expanded by remember { mutableStateOf(false) }

Box(
    modifier = Modifier
        .animateContentSize()
        .background(Color.Blue)
        .clickable { expanded = !expanded }
) {
    Column {
        Text("Header")
        if (expanded) {
            Text("Expanded content...")
        }
    }
}
```

No need for explicit `AnimatedVisibility` or layout transitions. Handles the container automatically.

## Layout Animation in LazyLists

### animateItem — Replaces animateItemPlacement

Animate item appearance, removal, and reordering.

```kotlin
LazyColumn {
    items(items, key = { it.id }) { item ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateItem()
                .padding(8.dp)
                .background(Color.Gray)
        ) {
            Text(item.name)
        }
    }
}
```

Automatically animates:
- New items sliding in
- Removed items sliding out
- Reordered items moving to new positions

Called on items in Lazy layouts (LazyColumn, LazyRow, LazyVerticalGrid).

## Shared Element Transitions

Animate elements across screen boundaries.

```kotlin
@Composable
fun FirstScreen() {
    val sharedContentState = rememberSharedContentState(key = "image")

    Image(
        painter = painterResource(id = R.drawable.cat),
        contentDescription = null,
        modifier = Modifier.sharedBounds(
            sharedContentState = sharedContentState,
            animatedVisibilityScope = this
        )
    )
}

@Composable
fun SecondScreen() {
    val sharedContentState = rememberSharedContentState(key = "image")

    Image(
        painter = painterResource(id = R.drawable.cat),
        contentDescription = null,
        modifier = Modifier.sharedBounds(
            sharedContentState = sharedContentState,
            animatedVisibilityScope = this
        )
    )
}
```

Coordinate transitions across navigation or dialog boundaries. Both elements must use the same `key`.

## Performance: graphicsLayer for Transforms

Animate transforms using `graphicsLayer` instead of layout changes.

```kotlin
// ✅ Correct: Uses GPU-accelerated graphicsLayer
val offset by animateFloatAsState(targetValue = 100f)
Box(modifier = Modifier.graphicsLayer(translationX = offset))

// ❌ Avoid: Causes recomposition and relayout
val offset by animateFloatAsState(targetValue = 100f)
Box(modifier = Modifier.offset(x = offset.dp))
```

Use `graphicsLayer` for:
- Translation (`translationX`, `translationY`)
- Rotation (`rotationX`, `rotationY`, `rotationZ`)
- Scale (`scaleX`, `scaleY`)
- Alpha (opacity)

## Anti-Patterns

### Don't: Animate visibility with if

```kotlin
// ❌ Anti-pattern
@Composable
fun MyScreen() {
    if (visible) {
        Text("Content") // Jumps in/out without animation
    }
}

// ✅ Correct
@Composable
fun MyScreen() {
    AnimatedVisibility(visible = visible) {
        Text("Content")
    }
}
```

### Don't: Create Animatable in composition

```kotlin
// ❌ Anti-pattern
@Composable
fun MyScreen() {
    val animatable = Animatable(0f) // Recreated every recomposition!

    LaunchedEffect(Unit) {
        animatable.animateTo(100f)
    }
}

// ✅ Correct
@Composable
fun MyScreen() {
    val animatable = remember { Animatable(0f) } // Preserved across recompositions

    LaunchedEffect(Unit) {
        animatable.animateTo(100f)
    }
}
```

### Don't: Animate in composition phase

```kotlin
// ❌ Anti-pattern
@Composable
fun MyScreen() {
    var position by remember { mutableStateOf(0f) }
    position = position + 10f // Infinite recomposition loop!
}

// ✅ Correct
@Composable
fun MyScreen() {
    var position by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        repeat(10) {
            position += 10f
            delay(16)
        }
    }
}
```

### Don't: Forget label parameter

```kotlin
// ❌ Anti-pattern (harder to debug)
val size by animateDpAsState(targetValue = 100.dp)

// ✅ Correct
val size by animateDpAsState(
    targetValue = 100.dp,
    label = "box_size"
)
```

Labels help with debugging layout inspector and animation inspection tools.
