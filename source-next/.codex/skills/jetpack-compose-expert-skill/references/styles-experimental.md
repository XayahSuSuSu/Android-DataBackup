# Compose Styles API (Experimental)

> `@ExperimentalFoundationStyleApi` — `androidx.compose.foundation:foundation:1.11.0-alpha06`
>
> AOSP source: `compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/style/`

## What is the Styles API?

A declarative, state-driven styling system for Compose. Instead of manually chaining modifiers
and `animateXAsState` calls for every interaction state, you declare all visual states in a
single `Style { }` block. The framework handles state detection, property interpolation, and
animation automatically.

### Before vs After

```kotlin
// BEFORE — 15+ lines of imperative state wiring
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val bgColor by animateColorAsState(
    if (isPressed) Color.DarkBlue else Color.Blue
)
val scale by animateFloatAsState(
    if (isPressed) 0.95f else 1f
)
Box(
    Modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .background(bgColor, RoundedCornerShape(16.dp))
        .clip(RoundedCornerShape(16.dp))
        .clickable(interactionSource = interactionSource, indication = null) { }
        .padding(16.dp)
)

// AFTER — Declarative. Done.
val style = Style {
    background(Color.Blue)
    shape(RoundedCornerShape(16.dp))
    contentPadding(16.dp)
    pressed(Style {
        animate(Style {
            background(Color.DarkBlue)
            scale(0.95f)
        })
    })
}
Box(Modifier.styleable(styleState = styleState, style = style))
```

The mental shift: **stop telling Compose how to animate between states**. Declare what each
state looks like — the framework interpolates.

---

## The Three Pieces

### 1. `Style { }` — Declare Visual States

A `Style` is a `fun interface`. You use the builder DSL:

```kotlin
val cardStyle = Style {
    // Base properties (always applied)
    background(Color(0xFFF5F5F5))
    shape(RoundedCornerShape(12.dp))
    contentPadding(16.dp)

    // State overrides — only applied when state is active
    selected(Style {
        animate(Style {
            background(Color.Blue.copy(alpha = 0.15f))
            borderWidth(2.dp)
            borderColor(Color.Blue)
        })
    })

    disabled(Style {
        background(Color(0xFFE0E0E0))
        contentColor(Color.Gray)  // no animate = instant snap
    })
}
```

`animate(Style { })` wraps properties that should interpolate smoothly.
Without `animate`, state changes snap immediately.

### 2. `MutableStyleState` — Drive State

```kotlin
// For toggle states (checked, selected, enabled) — set explicitly:
val styleState = remember { MutableStyleState(MutableInteractionSource()) }
styleState.isChecked = isChecked
styleState.isSelected = isSelected
styleState.isEnabled = isEnabled

// For interaction states (pressed, hovered, focused) — share interactionSource:
val interactionSource = remember { MutableInteractionSource() }
val styleState = remember { MutableStyleState(interactionSource) }
// isPressed, isHovered, isFocused auto-track from shared interactionSource
```

### 3. `Modifier.styleable()` — Apply to Any Composable

```kotlin
Box(
    Modifier
        .styleable(styleState = styleState, style = cardStyle)
        .clickable(interactionSource = interactionSource, indication = null) { }
)
```

Background renders, shape clips, borders draw, transforms apply, text properties propagate
to children via CompositionLocal. All animated.

---

## CRITICAL: Alpha06 Auto-Detection is Broken

**`styleable(style = myStyle)` without an explicit `styleState` does NOT detect interaction
states from sibling modifiers.** This is the single biggest trap in alpha06.

### What DOESN'T work:

```kotlin
// Compiles. Renders base style. State changes are SILENT.
Box(
    Modifier
        .styleable(style = myStyle)           // no styleState!
        .toggleable(value = isChecked, ...)   // style never sees this
)
```

### What DOES work:

**Pattern A — Toggle states (checked, selected, enabled):**
```kotlin
val styleState = remember { MutableStyleState(MutableInteractionSource()) }
styleState.isChecked = isChecked  // YOU drive the state

Box(
    Modifier
        .styleable(styleState = styleState, style = myStyle)
        .clickable { isChecked = !isChecked }
)
```

**Pattern B — Interaction states (pressed, hovered, focused):**
```kotlin
val interactionSource = remember { MutableInteractionSource() }
val styleState = remember { MutableStyleState(interactionSource) }

Box(
    Modifier
        .styleable(styleState = styleState, style = myStyle)
        .clickable(
            interactionSource = interactionSource,  // same instance!
            indication = null,
        ) { }
)
```

**Pattern C — Both (toggle button with press feedback):**
```kotlin
val interactionSource = remember { MutableInteractionSource() }
val styleState = remember { MutableStyleState(interactionSource) }
styleState.isChecked = isChecked  // explicit for toggle

Box(
    Modifier
        .styleable(styleState = styleState, style = myStyle)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
        ) { isChecked = !isChecked }
)
```

**Rule: always pass `styleState` to `styleable()`.**

---

## Text Property Propagation Gotcha

`contentColor()`, `fontSize()`, `fontWeight()`, `letterSpacing()`, `textDecoration()`, and
other text properties propagate to ALL child composables inside the styleable box via
`CompositionLocal` (`LocalContentColor`, `LocalTextStyle`).

### Problem:
```kotlin
// fontSize(28.sp) from the Style applies to BOTH texts!
Box(Modifier.styleable(style = Style { fontSize(28.sp) })) {
    Text("Title")      // 28sp
    Text("Subtitle")   // also 28sp — overlap!
}
```

### Fix:
Use a single `Text` inside styled boxes when style sets text properties. Move
descriptions outside the styleable scope:

```kotlin
Text("Description goes here")  // outside the styled box
Box(Modifier.styleable(style = gradientStyle)) {
    Text("Title Only")  // only this gets styled
}
```

---

## Verified Properties (alpha06, tested on device)

| Property | Works? | Notes |
|----------|--------|-------|
| `background(Color)` | Yes | Fills behind content |
| `background(Brush)` | Yes | Gradient backgrounds |
| `shape(Shape)` | Yes | Clips content + background |
| `contentPadding(Dp)` | Yes | Inner padding |
| `borderWidth(Dp) + borderColor(Color)` | Yes | Must set both |
| `scale(Float)` | Yes | graphicsLayer transform |
| `rotationZ(Float)` | Yes | graphicsLayer rotation |
| `translationX/Y(Float)` | Yes | graphicsLayer offset |
| `alpha(Float)` | Yes | Opacity |
| `contentColor(Color)` | Yes | Propagates to child Text/Icon |
| `contentBrush(Brush)` | Yes | Gradient text |
| `fontSize(TextUnit)` | Yes | Propagates to children |
| `fontWeight(FontWeight)` | Yes | Propagates to children |
| `letterSpacing(TextUnit)` | Yes | Propagates to children |
| `textDecoration(TextDecoration)` | Yes | Underline, strikethrough |
| `animate(Style { })` | Yes | Smooth spring interpolation |
| `dropShadow(Shadow)` | No | `Shadow` constructor is internal |

---

## Style Composition

Styles compose with `.then()` — later styles override earlier ones per-property:

```kotlin
val base = Style {
    background(Color.Blue)
    shape(RoundedCornerShape(12.dp))
    contentPadding(16.dp)
}

val elevated = Style {
    borderWidth(2.dp)
    borderColor(Color.LightGray)
    scale(1.02f)
}

val dark = Style {
    background(Color(0xFF1E1E2E))  // overrides base's background
    contentColor(Color.White)
}

// Chained:
val composed = base.then(elevated).then(dark)

// Factory (equivalent):
val composed = Style(base, elevated, dark)
```

---

## Building Reusable Components

The Styles API maps to Compose's component conventions. The `style` parameter becomes
first-class, like `modifier`:

```kotlin
// 1. Defaults object — theme-aware default style
@OptIn(ExperimentalFoundationStyleApi::class)
object StyledChipDefaults {
    @Composable
    fun style(): Style {
        val bg = MaterialTheme.colorScheme.secondaryContainer
        val fg = MaterialTheme.colorScheme.onSecondaryContainer
        return Style {
            background(bg)
            shape(RoundedCornerShape(8.dp))
            contentPadding(horizontal = 16.dp, vertical = 8.dp)
            contentColor(fg)
            pressed(Style {
                animate(Style { scale(0.95f) })
            })
        }
    }
}

// 2. Component — style as parameter with default
@Composable
fun StyledChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: Style = StyledChipDefaults.style(),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = remember { MutableStyleState(interactionSource) }

    Box(
        modifier = modifier
            .styleable(styleState = styleState, style = style)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// 3. Usage — default, custom, or composed
StyledChip(onClick = {}) { Text("Default") }

StyledChip(
    onClick = {},
    style = Style {
        background(Color.Teal)
        shape(CircleShape)
        contentColor(Color.Black)
    },
) { Text("Custom") }

StyledChip(
    onClick = {},
    style = StyledChipDefaults.style().then(Style {
        borderWidth(2.dp)
        borderColor(Color.Teal)
    }),
) { Text("Composed") }
```

---

## Theme Integration

`StyleScope` extends `CompositionLocalAccessorScope`, so Style blocks can read
`MaterialTheme` values at resolution time:

```kotlin
@Composable
fun ThemedButton() {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surface = MaterialTheme.colorScheme.surface

    val style = Style {
        background(primary)
        contentColor(onPrimary)
        shape(RoundedCornerShape(12.dp))
        contentPadding(16.dp)
        pressed(Style {
            animate(Style {
                background(surface)
                contentColor(primary)
                scale(0.95f)
            })
        })
    }
    // When theme changes (dark/light), style re-resolves automatically
}
```

Capture theme colors in a `@Composable` scope, use in the Style builder. Theme switches
update all styled elements instantly.

---

## Architecture: How It Works

The API lives in 7 source files under `androidx.compose.foundation.style`:

| File | Purpose |
|------|---------|
| `Style.kt` | `fun interface Style` + composition operators |
| `StyleScope.kt` | ~50 property functions |
| `StyleState.kt` | `StyleState` interface + `MutableStyleState` |
| `StyleModifier.kt` | `Modifier.styleable()` implementation |
| `StyleAnimations.kt` | `animate()` blocks |
| `ResolvedStyle.kt` | Property resolution with bitset flagging |
| `ExperimentalFoundationStyleApi.kt` | Opt-in annotation |

### Two-Node System

`Modifier.styleable()` inserts two modifier nodes:

- **`StyleOuterNode`** — Layout (padding, sizing), drawing (background, border, shape),
  transforms (scale, rotation, translation, alpha). Can invalidate at draw layer only when
  transform/draw properties change — no recomposition.

- **`StyleInnerNode`** — Content padding and text style propagation. Sets `LocalContentColor`,
  `LocalTextStyle`, etc. so child `Text` and `Icon` composables pick up styled colors/fonts.

### Bitset-Based Property Tracking

`ResolvedStyle` uses bitset flags for ~50 properties. On state change:

1. Only the delta between old and new resolved properties is computed
2. Drawing-only changes (background, border, alpha) → **draw-only invalidation** (skips layout + composition)
3. Layout changes (padding, sizing) → layout invalidation
4. Text changes (contentColor, fontSize) → composition invalidation (updates CompositionLocals)

A press animation changing only `scale` and `background` never triggers recomposition.

---

## All StyleScope Properties

### Layout
- `contentPadding(Dp)`, `contentPadding(horizontal, vertical)`, `contentPadding(start, top, end, bottom)`
- `externalPadding(Dp)` and same variants
- `width(Dp)`, `height(Dp)`, `size(Dp)`, `size(width, height)`
- `minWidth/minHeight/maxWidth/maxHeight(Dp)`
- `fillWidth()`, `fillHeight()`, `fillSize()`

### Drawing
- `background(Color)`, `background(Brush)`
- `foreground(Color)`, `foreground(Brush)`
- `shape(Shape)`
- `borderWidth(Dp)`, `borderColor(Color)`, `borderBrush(Brush)`
- `border(width, color)`, `border(width, brush)`

### Transforms
- `scale(Float)`, `scaleX(Float)`, `scaleY(Float)`
- `rotationX(Float)`, `rotationY(Float)`, `rotationZ(Float)`
- `translationX(Float)`, `translationY(Float)`, `translation(x, y)`
- `alpha(Float)`, `clip(Boolean)`, `zIndex(Float)`
- `transformOrigin(TransformOrigin)`

### Text & Content
- `contentColor(Color)`, `contentBrush(Brush)`
- `fontSize(TextUnit)`, `fontWeight(FontWeight)`, `fontStyle(FontStyle)`
- `letterSpacing(TextUnit)`, `lineHeight(TextUnit)`
- `textDecoration(TextDecoration)`, `fontFamily(FontFamily)`
- `textAlign(TextAlign)`, `textDirection(TextDirection)`
- `textStyle(TextStyle)`, `textIndent(TextIndent)`
- `baselineShift(BaselineShift)`, `lineBreak(LineBreak)`
- `hyphens(Hyphens)`, `fontSynthesis(FontSynthesis)`

### Shadows (internal constructor in alpha06)
- `dropShadow(Shadow)`, `innerShadow(Shadow)`

### State Functions
- `pressed(Style)`, `hovered(Style)`, `focused(Style)`
- `selected(Style)`, `checked(Style)`, `disabled(Style)`

### Animation
- `animate(Style)` — default spring
- `animate(spec: AnimationSpec<Float>, Style)` — custom spec
- `animate(toSpec, fromSpec, Style)` — asymmetric enter/exit

### Composition
- `Style.then(other: Style)` — chain (later overrides)
- `Style(style1, style2)` — merge factory
- `Style(vararg styles)` — merge multiple

---

## Common Pitfalls

1. **Forgetting `styleState`** — the #1 bug. Style renders but never reacts to state.
2. **Not sharing `interactionSource`** — pressed/hovered/focused won't track without it.
3. **Multiple Text children in styled box** — all inherit fontSize/fontWeight/contentColor.
4. **Using `toggleable()` / `selectable()`** — they create their own interactionSource internally. Use `.clickable()` and set state explicitly on `MutableStyleState`.
5. **Missing `@OptIn(ExperimentalFoundationStyleApi::class)`** — required on all usages.
6. **Trying to use `dropShadow()`** — `Shadow` constructor is internal in alpha06, won't compile.
7. **No `indication = null` on clickable** — without it you get default ripple on top of your styled feedback.
