package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    lineHeight: androidx.compose.ui.unit.TextUnit = 20.sp
) {
    val lines = remember(markdown) { markdown.lines() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                trimmed.startsWith("### ") -> {
                    val content = trimmed.removePrefix("### ").trim()
                    Text(
                        text = buildAnnotatedStringWithMarkdown(content, color),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    val content = trimmed.removePrefix("## ").trim()
                    Text(
                        text = buildAnnotatedStringWithMarkdown(content, color),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    val content = trimmed.removePrefix("# ").trim()
                    Text(
                        text = buildAnnotatedStringWithMarkdown(content, color),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ") -> {
                    val bulletText = trimmed.substring(2).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = buildAnnotatedStringWithMarkdown(bulletText, color),
                            fontSize = 14.sp,
                            lineHeight = lineHeight,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val match = Regex("^(\\d+\\.)\\s+(.*)").find(trimmed)
                    val prefix = match?.groupValues?.getOrNull(1) ?: "1."
                    val content = match?.groupValues?.getOrNull(2) ?: trimmed
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$prefix ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = buildAnnotatedStringWithMarkdown(content, color),
                            fontSize = 14.sp,
                            lineHeight = lineHeight,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = buildAnnotatedStringWithMarkdown(trimmed, color),
                        fontSize = 14.sp,
                        lineHeight = lineHeight,
                        color = color
                    )
                }
            }
        }
    }
}

fun buildAnnotatedStringWithMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            // Check for bold **text**
            if (cursor + 1 < length && text[cursor] == '*' && text[cursor + 1] == '*') {
                val nextIndex = text.indexOf("**", cursor + 2)
                if (nextIndex != -1) {
                    val boldContent = text.substring(cursor + 2, nextIndex)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                        append(boldContent)
                    }
                    cursor = nextIndex + 2
                    continue
                }
            }

            // Check for inline code `text`
            if (text[cursor] == '`') {
                val nextIndex = text.indexOf('`', cursor + 1)
                if (nextIndex != -1) {
                    val codeContent = text.substring(cursor + 1, nextIndex)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            background = defaultColor.copy(alpha = 0.12f)
                        )
                    ) {
                        append(" $codeContent ")
                    }
                    cursor = nextIndex + 1
                    continue
                }
            }

            // Check for italic *text* or _text_
            if (text[cursor] == '*' || text[cursor] == '_') {
                val delimiter = text[cursor]
                val nextIndex = text.indexOf(delimiter, cursor + 1)
                if (nextIndex != -1 && nextIndex > cursor + 1) {
                    val italicContent = text.substring(cursor + 1, nextIndex)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(italicContent)
                    }
                    cursor = nextIndex + 1
                    continue
                }
            }

            // Regular character
            append(text[cursor])
            cursor++
        }
    }
}
