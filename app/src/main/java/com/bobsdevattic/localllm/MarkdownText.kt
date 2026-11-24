package com.bobsdevattic.localllm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * A small, dependency-free Markdown renderer covering the subset that language
 * models typically emit: headings, bold/italic, inline code, fenced code
 * blocks, bullet and numbered lists, block quotes, and horizontal rules.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Code -> CodeBlock(block.text)
                is MdBlock.Heading -> Text(inlineMarkdown(block.text), style = headingStyle(block.level))
                is MdBlock.Bullet -> MarkdownListRow("•", block.text)
                is MdBlock.Numbered -> MarkdownListRow("${block.number}.", block.text)
                is MdBlock.Quote -> Text(
                    inlineMarkdown(block.text),
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 10.dp)
                )
                is MdBlock.Rule -> HorizontalDivider()
                is MdBlock.Paragraph -> Text(inlineMarkdown(block.text))
            }
        }
    }
}

@Composable
private fun MarkdownListRow(marker: String, text: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("$marker ", style = MaterialTheme.typography.bodyLarge)
        Text(inlineMarkdown(text), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            code,
            modifier = Modifier.padding(10.dp),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun headingStyle(level: Int) = when (level) {
    1 -> MaterialTheme.typography.titleLarge
    2 -> MaterialTheme.typography.titleMedium
    else -> MaterialTheme.typography.titleSmall
}.copy(fontWeight = FontWeight.Bold)

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Numbered(val number: Int, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Rule : MdBlock
}

private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
private val bulletRegex = Regex("^\\s*[-*+]\\s+(.*)$")
private val numberedRegex = Regex("^\\s*(\\d+)[.)]\\s+(.*)$")
private val ruleRegex = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")

private fun parseMarkdownBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks += MdBlock.Paragraph(paragraph.toString().trim())
        paragraph.setLength(0)
    }

    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        val fence = line.trimStart()
        if (fence.startsWith("```")) {
            flushParagraph()
            val code = StringBuilder()
            index++
            while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                if (code.isNotEmpty()) code.append("\n")
                code.append(lines[index])
                index++
            }
            index++ // skip closing fence
            blocks += MdBlock.Code(code.toString())
            continue
        }
        when {
            line.isBlank() -> flushParagraph()
            ruleRegex.matches(line) -> {
                flushParagraph()
                blocks += MdBlock.Rule
            }
            headingRegex.matches(line) -> {
                flushParagraph()
                val match = headingRegex.find(line)!!
                blocks += MdBlock.Heading(match.groupValues[1].length, match.groupValues[2].trim())
            }
            numberedRegex.matches(line) -> {
                flushParagraph()
                val match = numberedRegex.find(line)!!
                blocks += MdBlock.Numbered(match.groupValues[1].toIntOrNull() ?: 1, match.groupValues[2].trim())
            }
            bulletRegex.matches(line) -> {
                flushParagraph()
                blocks += MdBlock.Bullet(bulletRegex.find(line)!!.groupValues[1].trim())
            }
            line.trimStart().startsWith(">") -> {
                flushParagraph()
                blocks += MdBlock.Quote(line.trimStart().removePrefix(">").trim())
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(" ")
                paragraph.append(line.trim())
            }
        }
        index++
    }
    flushParagraph()
    return blocks
}

private val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")

// Inline formatting: inline code, bold, italic, and links.
private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val rest = text.substring(i)
        when {
            rest.startsWith("`") -> {
                val end = text.indexOf('`', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            rest.startsWith("**") || rest.startsWith("__") -> {
                val marker = rest.substring(0, 2)
                val end = text.indexOf(marker, i + 2)
                if (end > i) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            (rest.startsWith("*") || rest.startsWith("_")) -> {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            rest.startsWith("[") -> {
                val match = linkRegex.find(rest)
                if (match != null && match.range.first == 0) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(match.groupValues[1])
                    }
                    i += match.range.last + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}
