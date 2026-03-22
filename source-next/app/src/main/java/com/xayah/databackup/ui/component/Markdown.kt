package com.xayah.databackup.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

private val HeadingRegex = Regex("^#{1,6}\\s+.*")
private val DividerRegex = Regex("^(-{3,}|\\*{3,}|_{3,})$")
private val UnorderedListRegex = Regex("^(\\s*)([-*+])\\s+(.*)$")
private val OrderedListRegex = Regex("^(\\s*)(\\d+[.])\\s+(.*)$")
private val TaskListRegex = Regex("^\\[([ xX])]\\s+(.*)$")

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Blank -> Spacer(modifier = Modifier.height(4.dp))
                is MarkdownBlock.CodeBlock -> MarkdownCodeBlock(code = block.code)
                is MarkdownBlock.Divider -> HorizontalDivider()
                is MarkdownBlock.Heading -> MarkdownHeading(level = block.level, text = block.text)
                is MarkdownBlock.ListItem -> MarkdownListItem(
                    marker = block.marker,
                    text = block.text,
                    indentLevel = block.indentLevel,
                    checked = block.checked
                )
                is MarkdownBlock.Paragraph -> MarkdownText(text = block.text)
                is MarkdownBlock.Quote -> MarkdownQuote(text = block.text)
            }
        }
    }
}

@Composable
private fun MarkdownHeading(
    level: Int,
    text: String,
) {
    val textStyle = when (level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    Text(
        text = text,
        style = textStyle,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun MarkdownListItem(
    marker: String,
    text: String,
    indentLevel: Int,
    checked: Boolean?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 16).dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = when (checked) {
                true -> "[x]"
                false -> "[ ]"
                null -> marker
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Box(modifier = Modifier.weight(1f)) {
            MarkdownText(text = text)
        }
    }
}

@Composable
private fun MarkdownQuote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
        )
        Box(modifier = Modifier.weight(1f)) {
            MarkdownText(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MarkdownCodeBlock(code: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        text = code,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun MarkdownText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val colorScheme = MaterialTheme.colorScheme
    val annotatedText = remember(
        text,
        color,
        colorScheme.primary,
        colorScheme.onSurface,
        colorScheme.surfaceContainerHigh
    ) {
        buildMarkdownAnnotatedString(
            text = text,
            textColor = color,
            linkColor = colorScheme.primary,
            codeColor = colorScheme.onSurface,
            codeBackground = colorScheme.surfaceContainerHigh
        )
    }
    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium,
        color = color
    )
}

private fun buildMarkdownAnnotatedString(
    text: String,
    textColor: Color,
    linkColor: Color,
    codeColor: Color,
    codeBackground: Color,
): AnnotatedString {
    return buildAnnotatedString {
        appendInlineMarkdown(
            text = text,
            textColor = textColor,
            linkColor = linkColor,
            codeColor = codeColor,
            codeBackground = codeBackground
        )
    }
}

private sealed interface MarkdownBlock {
    data object Blank : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data class ListItem(
        val marker: String,
        val text: String,
        val indentLevel: Int,
        val checked: Boolean?,
    ) : MarkdownBlock

    data object Divider : MarkdownBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = mutableListOf<String>()
    var index = 0

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraphBuffer.joinToString("\n").trim())
            paragraphBuffer.clear()
        }
    }

    while (index < lines.size) {
        val rawLine = lines[index]
        val line = rawLine.trimEnd()

        when {
            line.startsWith("```") -> {
                flushParagraph()
                index++
                val codeLines = mutableListOf<String>()
                while (index < lines.size && lines[index].trimEnd().startsWith("```").not()) {
                    codeLines += lines[index]
                    index++
                }
                blocks += MarkdownBlock.CodeBlock(codeLines.joinToString("\n"))
                if (index < lines.size) index++
            }
            line.isBlank() -> {
                flushParagraph()
                blocks += MarkdownBlock.Blank
                index++
            }
            HeadingRegex.matches(line) -> {
                flushParagraph()
                val level = line.takeWhile { it == '#' }.length.coerceIn(1, 6)
                blocks += MarkdownBlock.Heading(level = level, text = line.drop(level).trim())
                index++
            }
            DividerRegex.matches(line) -> {
                flushParagraph()
                blocks += MarkdownBlock.Divider
                index++
            }
            line.trimStart().startsWith(">") -> {
                flushParagraph()
                val quoteLines = mutableListOf<String>()
                while (index < lines.size && lines[index].trimStart().startsWith(">")) {
                    quoteLines += lines[index].trimStart().removePrefix(">").trimStart()
                    index++
                }
                blocks += MarkdownBlock.Quote(quoteLines.joinToString("\n"))
            }
            else -> {
                val listItem = parseListItemBlock(line)
                if (listItem != null) {
                    flushParagraph()
                    blocks += listItem
                    index++
                } else {
                    paragraphBuffer += line.trim()
                    index++
                }
            }
        }
    }

    flushParagraph()
    return blocks
}

private fun parseListItemBlock(line: String): MarkdownBlock.ListItem? {
    val unorderedMatch = UnorderedListRegex.find(line)
    if (unorderedMatch != null) {
        val indentLevel = unorderedMatch.groupValues[1].length / 2
        val content = unorderedMatch.groupValues[3]
        val taskMatch = TaskListRegex.find(content)
        return MarkdownBlock.ListItem(
            marker = unorderedMatch.groupValues[2],
            text = taskMatch?.groupValues?.get(2) ?: content,
            indentLevel = indentLevel,
            checked = taskMatch?.groupValues?.get(1)?.equals("x", ignoreCase = true)
        )
    }

    val orderedMatch = OrderedListRegex.find(line)
    if (orderedMatch != null) {
        return MarkdownBlock.ListItem(
            marker = orderedMatch.groupValues[2],
            text = orderedMatch.groupValues[3],
            indentLevel = orderedMatch.groupValues[1].length / 2,
            checked = null
        )
    }

    return null
}

private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    textColor: Color,
    linkColor: Color,
    codeColor: Color,
    codeBackground: Color,
) {
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("\\", index) && index + 1 < text.length -> {
                append(text[index + 1])
                index += 2
            }
            text.regionMatches(index, "http://", 0, 7) || text.regionMatches(index, "https://", 0, 8) -> {
                val end = text.indexOfAny(charArrayOf(' ', '\n', '\t'), startIndex = index).let { pos ->
                    if (pos == -1) text.length else pos
                }
                val url = text.substring(index, end).trimEnd('.', ',', ';', ':', ')')
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = androidx.compose.ui.text.TextLinkStyles(
                            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                        )
                    )
                ) {
                    append(url)
                }
                index += url.length
            }
            text.startsWith("[", index) -> {
                val labelEnd = text.indexOf(']', startIndex = index + 1)
                if (labelEnd > index && labelEnd + 1 < text.length && text[labelEnd + 1] == '(') {
                    val urlEnd = text.indexOf(')', startIndex = labelEnd + 2)
                    if (urlEnd > labelEnd + 1) {
                        val label = text.substring(index + 1, labelEnd)
                        val url = text.substring(labelEnd + 2, urlEnd)
                        withLink(
                            LinkAnnotation.Url(
                                url = url,
                                styles = androidx.compose.ui.text.TextLinkStyles(
                                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                                )
                            )
                        ) {
                            appendInlineMarkdown(label, textColor, linkColor, codeColor, codeBackground)
                        }
                        index = urlEnd + 1
                        continue
                    }
                }
                append(text[index])
                index++
            }
            text.startsWith("`", index) -> {
                val end = text.indexOf('`', startIndex = index + 1)
                if (end > index) {
                    pushStyle(
                        SpanStyle(
                            color = codeColor,
                            background = codeBackground,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                    continue
                }
                append(text[index])
                index++
            }
            text.startsWith("***", index) || text.startsWith("___", index) -> {
                val delimiter = text.substring(index, index + 3)
                val end = text.indexOf(delimiter, startIndex = index + 3)
                if (end > index) {
                    pushStyle(
                        SpanStyle(
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic
                        )
                    )
                    appendInlineMarkdown(text.substring(index + 3, end), textColor, linkColor, codeColor, codeBackground)
                    pop()
                    index = end + 3
                    continue
                }
                append(text[index])
                index++
            }
            text.startsWith("**", index) || text.startsWith("__", index) -> {
                val delimiter = text.substring(index, index + 2)
                val end = text.indexOf(delimiter, startIndex = index + 2)
                if (end > index) {
                    pushStyle(SpanStyle(color = textColor, fontWeight = FontWeight.SemiBold))
                    appendInlineMarkdown(text.substring(index + 2, end), textColor, linkColor, codeColor, codeBackground)
                    pop()
                    index = end + 2
                    continue
                }
                append(text[index])
                index++
            }
            text.startsWith("~~", index) -> {
                val end = text.indexOf("~~", startIndex = index + 2)
                if (end > index) {
                    pushStyle(SpanStyle(color = textColor, textDecoration = TextDecoration.LineThrough))
                    appendInlineMarkdown(text.substring(index + 2, end), textColor, linkColor, codeColor, codeBackground)
                    pop()
                    index = end + 2
                    continue
                }
                append(text[index])
                index++
            }
            text.startsWith("*", index) || text.startsWith("_", index) -> {
                val delimiter = text[index]
                val end = text.indexOf(delimiter, startIndex = index + 1)
                if (end > index) {
                    pushStyle(SpanStyle(color = textColor, fontStyle = FontStyle.Italic))
                    appendInlineMarkdown(text.substring(index + 1, end), textColor, linkColor, codeColor, codeBackground)
                    pop()
                    index = end + 1
                    continue
                }
                append(text[index])
                index++
            }
            else -> {
                append(text[index])
                index++
            }
        }
    }
}
