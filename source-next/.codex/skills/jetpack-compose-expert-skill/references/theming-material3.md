# Material 3 Theming Reference

## MaterialTheme Basics

`MaterialTheme` is the root provider for design tokens in Compose Material 3. It establishes `colorScheme`, `typography`, and `shapes` across your app.

```kotlin
@Composable
fun MyApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography(),
        shapes = Shapes()
    ) {
        // All descendants access tokens via MaterialTheme
        Scaffold {
            Text("Uses MaterialTheme.typography.bodyLarge")
        }
    }
}
```

**Source**: `androidx/compose/material3/MaterialTheme.kt`

---

## ColorScheme — Light and Dark

A `ColorScheme` bundles 29+ semantic color tokens (primary, secondary, error, surface, etc.).

### Default Light/Dark Schemes

```kotlin
// Light (default)
val lightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC6)
)

// Dark
val darkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6)
)

MaterialTheme(colorScheme = if (isDark) darkColors else lightColors) { ... }
```

### Dynamic Color (Material You)

Android 12+ supports extracting colors from wallpaper. Check `Build.VERSION.SDK_INT` before calling:

```kotlin
val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val context = LocalContext.current
    if (isDark) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
} else {
    if (isDark) darkColorScheme() else lightColorScheme()
}

MaterialTheme(colorScheme = colorScheme) { ... }
```

This requires `android:READ_MEDIA_IMAGES` or context access. `dynamicColorScheme` APIs are in `androidx.compose.material3`.

---

## Typography — Custom Type Scales

`Typography` defines `displayLarge`, `headlineSmall`, `bodyLarge`, `labelSmall`, etc.

### Using Google Fonts

```kotlin
val Poppins = FontFamily(
    Font(R.font.poppins_regular, weight = FontWeight.Normal),
    Font(R.font.poppins_bold, weight = FontWeight.Bold),
    Font(R.font.poppins_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
)

val customTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Poppins,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Poppins,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

MaterialTheme(typography = customTypography) { ... }
```

All M3 type styles follow a 15-level scale. Partial overrides keep defaults for unspecified styles.

---

## Shapes — Corner Radius Customization

`Shapes` defines `extraSmall`, `small`, `medium`, `large`, `extraLarge` corner radii.

```kotlin
val customShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

MaterialTheme(shapes = customShapes) { ... }

// Use in components
Button(
    modifier = Modifier.clip(MaterialTheme.shapes.large)
) { ... }
```

Components automatically use theme shapes via `Surface` and `Card`.

---

## Dark Theme

### isSystemInDarkTheme()

Check system dark mode setting:

```kotlin
@Composable
fun MyApp() {
    val isDark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
        // Content
    }
}
```

### Manual Toggle with Persistence

For user-selectable dark mode:

```kotlin
val darkModeState = rememberSaveable { mutableStateOf(isSystemInDarkTheme()) }

MaterialTheme(colorScheme = if (darkModeState.value) darkColorScheme() else lightColorScheme()) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { darkModeState.value = !darkModeState.value }) {
                Icon(Icons.Default.Settings, "Toggle theme")
            }
        }
    ) {
        // Content
    }
}
```

Persist selection via DataStore or SharedPreferences.

---

## Component-Level Styling

Use `MaterialTheme` tokens for colors, not hardcoded values:

```kotlin
// DO
Text(
    text = "Hello",
    color = MaterialTheme.colorScheme.onSurface,
    style = MaterialTheme.typography.bodyLarge
)

// DON'T
Text(text = "Hello", color = Color.Black, fontSize = 14.sp)
```

Common tokens:
- `primary`, `secondary`, `tertiary` — accent colors
- `surface`, `surfaceVariant` — container backgrounds
- `onPrimary`, `onSurface`, `onError` — text/content on colored backgrounds
- `error`, `errorContainer` — error states

---

## Surface vs Box

**Surface**: styled container with elevation, background from `colorScheme.surface`, respects theme.

```kotlin
Surface(
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth()
) {
    // Has elevation, shadow
    Text("Themed container")
}
```

**Box**: plain container, no theming assumptions.

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
) {
    // Manual styling
    Text("Manual background")
}
```

Use `Surface` for semantic containers (cards, dialogs). Use `Box` for layout grouping.

---

## Scaffold — Layout Integration

`Scaffold` composes `topBar`, `floatingActionButton`, `snackbarHost`, and content with proper padding:

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = { Text("My App") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    },
    floatingActionButton = {
        FloatingActionButton(
            onClick = { /* ... */ },
            containerColor = MaterialTheme.colorScheme.tertiary
        ) {
            Icon(Icons.Default.Add, "Add item")
        }
    },
    snackbarHost = { SnackbarHost(it) }
) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
        // Content respects safe area
    }
}
```

`Scaffold` handles insets and spacing automatically. Don't nest `Scaffold` components.

---

## Extending Theme with CompositionLocal

Add custom design tokens:

```kotlin
data class AppColors(
    val brandPrimary: Color = Color(0xFF6200EE),
    val brandSecondary: Color = Color(0xFF03DAC6),
    val neutral: Color = Color.Gray
)

val LocalAppColors = compositionLocalOf { AppColors() }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val appColors = AppColors()
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme {
            content()
        }
    }
}

// Use custom tokens
@Composable
fun MyButton() {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = LocalAppColors.current.brandPrimary
        )
    ) { }
}
```

---

## Anti-Patterns

### Hardcoding Colors
```kotlin
// DON'T
Text("Hello", color = Color(0xFF000000))

// DO
Text("Hello", color = MaterialTheme.colorScheme.onSurface)
```

### Ignoring Theme in Reusable Components
```kotlin
// DON'T
fun MyCard(content: @Composable () -> Unit) {
    Box(modifier = Modifier.background(Color.White))
}

// DO
fun MyCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        content()
    }
}
```

### Mixing Material 2 and Material 3
Don't import both `androidx.compose.material` and `androidx.compose.material3`. Choose M3 for new projects.

### Not Providing All Theme Parameters
Partial `MaterialTheme` calls may leave descendants with defaults:

```kotlin
// Unsafe if colorScheme varies by locale
MaterialTheme(typography = customTypography) { ... }

// Better
MaterialTheme(
    colorScheme = currentColorScheme,
    typography = currentTypography,
    shapes = currentShapes
) { ... }
```

---

## Resources

- **Material 3 Tokens**: https://m3.material.io/
- **Compose Material3 Docs**: https://developer.android.com/develop/ui/compose/designsystems/material3
- **Dynamic Color**: Requires `androidx.compose.material3:material3` >= 1.1.0 and Android 12+
