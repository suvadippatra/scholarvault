package com.scholarvault.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

@Composable
fun SimpleMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 16.sp
) {
    val mediaPattern = remember { Regex("""!\[(.*?)\]\((.*?)\)""") }
    val matches = remember(text) { mediaPattern.findAll(text).toList() }

    if (matches.isEmpty()) {
        SimpleMarkdownTextSegment(text, modifier, color, fontSize)
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var lastIdx = 0
            for (match in matches) {
                // Text before match
                val preText = text.substring(lastIdx, match.range.first)
                if (preText.isNotEmpty()) {
                    SimpleMarkdownTextSegment(preText, Modifier.fillMaxWidth(), color, fontSize)
                }

                // Media block
                val altText = match.groupValues[1]
                val uriStr = match.groupValues[2]

                if (altText.equals("Audio", ignoreCase = true) || uriStr.endsWith(".mp3", ignoreCase = true) || uriStr.endsWith(".wav", ignoreCase = true) || uriStr.endsWith(".m4a", ignoreCase = true) || uriStr.contains("audio", ignoreCase = true)) {
                    AudioPlayerBlock(uriStr, altText)
                } else {
                    ImageBlock(uriStr, altText)
                }

                lastIdx = match.range.last + 1
            }

            // Remaining text
            if (lastIdx < text.length) {
                val postText = text.substring(lastIdx)
                if (postText.isNotEmpty()) {
                    SimpleMarkdownTextSegment(postText, Modifier.fillMaxWidth(), color, fontSize)
                }
            }
        }
    }
}

@Composable
fun ImageBlock(url: String, altText: String) {
    var hasError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasError) {
            val imageModel = remember(url) {
                if (url.startsWith("/") && !url.startsWith("content://") && !url.startsWith("file://")) {
                    java.io.File(url)
                } else {
                    url
                }
            }
            coil.compose.AsyncImage(
                model = imageModel,
                contentDescription = altText,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
                onError = { hasError = true }
            )
        }
        if (hasError) {
            Text(
                "📷 Attached Image: $url",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Text(
                altText.ifBlank { "Image" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun AudioPlayerBlock(url: String, name: String) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(url) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop Audio" else "Play Audio",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        try {
                            if (isPlaying) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                isPlaying = false
                            } else {
                                mediaPlayer = android.media.MediaPlayer().apply {
                                    setDataSource(context, android.net.Uri.parse(url))
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        isPlaying = false
                                        release()
                                        mediaPlayer = null
                                    }
                                }
                                isPlaying = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(context, "Cannot play audio file: verify URL/permissions", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "Attached Audio Track" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = url.substringAfterLast("/").take(40),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            if (isPlaying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun SimpleMarkdownTextSegment(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 16.sp
) {
    val annotatedString = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("```", i) -> {
                    val end = text.indexOf("```", i + 3)
                    if (end != -1) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = MaterialTheme.colorScheme.surfaceVariant, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(text.substring(i + 3, end))
                        }
                        i = end + 3
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("- [ ] ", i) && (i == 0 || text[i - 1] == '\n') -> {
                    append("☐ ")
                    i += 6
                }
                text.startsWith("- [x] ", i) && (i == 0 || text[i - 1] == '\n') -> {
                    append("☑ ")
                    i += 6
                }
                text.startsWith("> ", i) && (i == 0 || text[i - 1] == '\n') -> {
                    val end = text.indexOf('\n', i)
                    val endAct = if (end != -1) end else text.length
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))) {
                        append("▎" + text.substring(i + 1, endAct))
                    }
                    i = endAct
                }
                text.startsWith("---", i) && (i == 0 || text[i - 1] == '\n') -> {
                    val end = text.indexOf('\n', i)
                    val endAct = if (end != -1) end else text.length
                    if (text.substring(i, endAct).trim() == "---") {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.outline)) {
                            append("─────────────────────────")
                        }
                        i = endAct
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = MaterialTheme.colorScheme.surfaceVariant)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("#", i) && (i == 0 || text[i - 1] == '\n') -> {
                    var level = 0
                    var j = i
                    while (j < text.length && text[j] == '#') {
                        level++
                        j++
                    }
                    if (j < text.length && text[j] == ' ') {
                        val end = text.indexOf('\n', j)
                        val endAct = if (end != -1) end else text.length
                        val headerText = text.substring(j + 1, endAct)
                        val headerSize = when (level) {
                            1 -> 24.sp
                            2 -> 22.sp
                            3 -> 20.sp
                            else -> 18.sp
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = headerSize)) {
                            append(headerText)
                        }
                        i = endAct
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("[", i) -> {
                    val bracketEnd = text.indexOf("]", i + 1)
                    if (bracketEnd != -1 && bracketEnd + 1 < text.length && text[bracketEnd + 1] == '(') {
                        val parenEnd = text.indexOf(")", bracketEnd + 2)
                        if (parenEnd != -1) {
                            val linkText = text.substring(i + 1, bracketEnd)
                            val linkUrl = text.substring(bracketEnd + 2, parenEnd)
                            
                            val startPos = this.length
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                                append(linkText)
                            }
                            addStringAnnotation(tag = "URL", annotation = linkUrl, start = startPos, end = this.length)
                            
                            i = parenEnd + 1
                            continue
                        }
                    }
                    append(text[i])
                    i++
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }

    val uriHandler = LocalUriHandler.current
    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(color = color, fontSize = fontSize, lineHeight = fontSize * 1.5f),
        onClick = { offset ->
            annotatedString.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                try {
                    uriHandler.openUri(annotation.item)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )
}
