# Accessibility Reference

## Semantics — Exposing UI to Accessibility Services

Semantics describe UI elements to screen readers, voice commands, and testing tools. Compose generates semantics automatically for built-in components, but custom views require manual annotation.

```kotlin
// Built-in components have semantics
Button(onClick = { }) { Text("Click me") }  // Auto-announces as button

// Custom composables need explicit semantics
Box(
    modifier = Modifier.semantics {
        role = Role.Button
        onClick(label = "Activate") { true }
    }
) {
    Text("Custom button")
}
```

**Source**: `androidx/compose/ui/semantics/`

---

## contentDescription — Labels for Accessibility

Every meaningful visual element needs a label for screen readers.

### When to Set contentDescription

**DO**: Images, icons, buttons without text, decorative with meaningful purpose

```kotlin
// Icon in a button: set contentDescription
Button(onClick = { }) {
    Icon(Icons.Default.Settings, contentDescription = "App settings")
}

// Text-only button: contentDescription auto-generated
Button(onClick = { }) { Text("Save") }

// Standalone image: always set description
Image(
    painter = painterResource(R.drawable.product),
    contentDescription = "Product photo: wireless headphones"
)
```

### When to Set contentDescription = null

**Purely decorative images** that convey no information:

```kotlin
// Decorative divider line
Divider(modifier = Modifier.padding(vertical = 8.dp))

// Purely decorative background
Box(
    modifier = Modifier
        .background(Color.Gray)
        .size(100.dp)
) {
    Image(
        painter = painterResource(R.drawable.background_pattern),
        contentDescription = null  // Purely decorative
    )
}

// Icon next to label: skip icon description
Row {
    Icon(
        painter = painterResource(R.drawable.verified),
        contentDescription = null,  // Label below describes it
        tint = Color.Green
    )
    Text("Verified")
}
```

Omitting `contentDescription` or using `null` tells the screen reader to skip the element.

---

## Modifier.semantics — Merging and Overriding

### Default Merging

By default, child semantics merge with parents:

```kotlin
// Child text is included in parent semantics
Column(modifier = Modifier.semantics { heading() }) {
    Text("Section Title")  // Screen reader: "Section Title, heading"
}
```

### Clearing and Setting Semantics

Use `clearAndSetSemantics` to override all child semantics:

```kotlin
// Screen reader ignores children, announces custom label
Box(
    modifier = Modifier.clearAndSetSemantics {
        contentDescription = "Custom audio player with play/pause"
    }
) {
    Icon(Icons.Default.PlayArrow, contentDescription = null)
    Text("00:30")  // Not read
    Icon(Icons.Default.VolumeUp, contentDescription = null)  // Not read
}
```

### Merging Disabled

Prevent children from merging:

```kotlin
Box(
    modifier = Modifier.semantics(mergeDescendants = false) {
        heading()
    }
) {
    Text("Heading")  // Announced separately, not merged
}
```

---

## Touch Target Sizing

Minimum touch target is 48dp × 48dp per Material Design and WCAG guidelines.

```kotlin
// Small button without sufficient touch target
Button(modifier = Modifier.size(32.dp)) { }  // TOO SMALL

// Proper touch target
Button(
    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
) { }

// Alternative: add padding
Box(
    modifier = Modifier
        .size(32.dp)
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
) {
    Icon(Icons.Default.Edit, contentDescription = "Edit")
}

// For clickable elements without Button
Box(
    modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable { /* ... */ }
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    contentAlignment = Alignment.Center
) {
    Icon(Icons.Default.Close, contentDescription = "Close")
}
```

---

## Headings — Screen Reader Navigation

Headings allow users to navigate by section. Set heading semantic:

```kotlin
// Level 1 heading
Text(
    text = "Products",
    modifier = Modifier.semantics { heading() },
    style = MaterialTheme.typography.headlineLarge
)

// Subheading (no formal levels in Compose; use structure)
Text(
    text = "New Arrivals",
    modifier = Modifier.semantics { heading() },
    style = MaterialTheme.typography.headlineMedium
)
```

Screen readers announce "heading" and allow jumping between sections. Use headings to structure content logically, not for styling.

---

## Custom Actions

Allow screen readers to trigger complex interactions:

```kotlin
@Composable
fun SlideToUnlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(label = "Unlock") {
                        unlock()
                        true
                    },
                    CustomAccessibilityAction(label = "Emergency call") {
                        emergencyCall()
                        true
                    }
                )
            }
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Text("Slide to Unlock", color = Color.White)
    }
}
```

Custom actions appear in the accessibility menu. Avoid for standard interactions (Button, Checkbox).

---

## Traversal Order

Control screen reader navigation order explicitly when needed:

```kotlin
// Default: top-to-bottom, left-to-right
Row {
    Button(onClick = { }) { Text("First") }
    Button(onClick = { }) { Text("Second") }
}

// Custom order (right-to-left)
Row {
    Button(
        onClick = { },
        modifier = Modifier.semantics { traversalIndex = 1f }
    ) { Text("Read Second") }
    Button(
        onClick = { },
        modifier = Modifier.semantics { traversalIndex = 0f }
    ) { Text("Read First") }
}

// Group items as single traversal unit
Column(
    modifier = Modifier.semantics(mergeDescendants = false) {
        isTraversalGroup = true
    }
) {
    Text("Label")
    Text("Value")
}
```

Use `traversalIndex` sparingly. Good structure is usually sufficient.

---

## State Descriptions

Inform users of component state (enabled/disabled, checked/unchecked):

```kotlin
@Composable
fun AccessibleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .semantics {
                this.contentDescription = label
                this.stateDescription = if (checked) "Checked" else "Unchecked"
                role = Role.Checkbox
            }
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
    ) {
        Icon(
            if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}
```

Screen reader announces: "Label, Checkbox, Checked" or "Label, Checkbox, Unchecked".

---

## Live Regions

Announce dynamic content changes without requiring interaction:

```kotlin
@Composable
fun LiveMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Assertive  // Polite or Assertive
        }
    )
}

// Usage
var status by remember { mutableStateOf("Loading...") }

LaunchedEffect(Unit) {
    delay(2000)
    status = "Done loading"  // Screen reader announces immediately
}

LiveMessage(message = status)
```

**Assertive**: Interrupts current speech. Use for critical updates (error, alert).
**Polite**: Queues after current speech. Use for status updates.

---

## Testing Semantics

Use Compose Test APIs to verify accessibility:

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun testContentDescription() {
    composeTestRule.setContent {
        Icon(
            Icons.Default.Settings,
            contentDescription = "App settings"
        )
    }

    composeTestRule
        .onNodeWithContentDescription("App settings")
        .assertIsDisplayed()
}

@Test
fun testHeading() {
    composeTestRule.setContent {
        Text("Main Title", modifier = Modifier.semantics { heading() })
    }

    composeTestRule
        .onNode(isHeading())
        .assertIsDisplayed()
        .assertTextEquals("Main Title")
}

@Test
fun testTouchTarget() {
    composeTestRule.setContent {
        Button(modifier = Modifier.size(32.dp)) { Text("Too small") }
    }

    composeTestRule
        .onNodeWithText("Too small")
        .assertHeightIsAtLeast(48.dp)  // Fails: 32.dp < 48.dp
}

@Test
fun testCustomAction() {
    composeTestRule.setContent {
        Box(
            modifier = Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Unlock") { true }
                )
            }
        )
    }

    composeTestRule
        .onNode(hasCustomAccessibilityAction("Unlock"))
        .performCustomAccessibilityAction("Unlock")
}
```

---

## Anti-Patterns

### Decorative Images Without null contentDescription

```kotlin
// DON'T: Screen reader reads useless description
Image(
    painter = painterResource(R.drawable.separator_line),
    contentDescription = "Line"  // No value
)

// DO
Image(
    painter = painterResource(R.drawable.separator_line),
    contentDescription = null
)
```

### Clickable Without Semantics

```kotlin
// DON'T: No semantic role or description
Box(
    modifier = Modifier
        .clickable { deleteItem() }
        .size(32.dp)
) {
    Icon(Icons.Default.Delete, contentDescription = null)
}

// DO
Box(
    modifier = Modifier
        .clickable { deleteItem() }
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .semantics {
            role = Role.Button
            contentDescription = "Delete item"
        },
    contentAlignment = Alignment.Center
) {
    Icon(Icons.Default.Delete, contentDescription = null)
}
```

### Hardcoded Content Descriptions Without Context

```kotlin
// DON'T: Generic, doesn't describe purpose
Icon(Icons.Default.Star, contentDescription = "Icon")

// DO: Specific
Icon(Icons.Default.Star, contentDescription = "Add to favorites")
```

### Missing Heading Structure

```kotlin
// DON'T: No navigation structure
Column {
    Text("Section 1")
    Text("Section 2")
    Text("Section 3")
}

// DO
Column {
    Text("Section 1", modifier = Modifier.semantics { heading() })
    Text("Section 2", modifier = Modifier.semantics { heading() })
    Text("Section 3", modifier = Modifier.semantics { heading() })
}
```

---

## Resources

- **Compose Accessibility**: https://developer.android.com/develop/ui/compose/accessibility
- **Material Design Accessibility**: https://m3.material.io/foundations/accessible-design
- **WCAG 2.1**: https://www.w3.org/WAI/WCAG21/quickref/
- **Testing A11y in Compose**: https://developer.android.com/develop/ui/compose/testing#a11y
