package com.scholarvault.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CitationStyle {
    APA, MLA, CHICAGO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitationGeneratorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedStyle by remember { mutableStateOf(CitationStyle.APA) }
    var expandedStyle by remember { mutableStateOf(false) }

    var authorFirst by remember { mutableStateOf("") }
    var authorLast by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var dateAccessed by remember { mutableStateOf("") }

    val generatedCitation by remember {
        derivedStateOf {
            generateCitation(selectedStyle, authorFirst, authorLast, title, publisher, year, url, dateAccessed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Citation Generator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Text("Select Style", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            
            ExposedDropdownMenuBox(
                expanded = expandedStyle,
                onExpandedChange = { expandedStyle = !expandedStyle }
            ) {
                OutlinedTextField(
                    value = selectedStyle.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStyle) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expandedStyle,
                    onDismissRequest = { expandedStyle = false }
                ) {
                    CitationStyle.values().forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.name) },
                            onClick = {
                                selectedStyle = style
                                expandedStyle = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Source Details", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = authorFirst,
                    onValueChange = { authorFirst = it },
                    label = { Text("Author First Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = authorLast,
                    onValueChange = { authorLast = it },
                    label = { Text("Author Last Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title of Source") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = publisher,
                onValueChange = { publisher = it },
                label = { Text("Publisher / Container") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year Published") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = dateAccessed,
                    onValueChange = { dateAccessed = it },
                    label = { Text("Date Accessed (Optional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                )
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (generatedCitation.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Generated Citation", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Citation", generatedCitation)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = generatedCitation,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "Fill in the details above to generate a citation.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontStyle = FontStyle.Italic
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

fun generateCitation(style: CitationStyle, first: String, last: String, title: String, publisher: String, year: String, url: String, dateAccessed: String): String {
    val f = first.trim()
    val l = last.trim()
    val t = title.trim()
    val p = publisher.trim()
    val y = year.trim()
    val u = url.trim()
    val da = dateAccessed.trim()

    if (t.isEmpty() && l.isEmpty()) return ""

    val authorStr = when {
        l.isNotEmpty() && f.isNotEmpty() -> "$l, $f."
        l.isNotEmpty() -> "$l."
        f.isNotEmpty() -> "$f."
        else -> ""
    }

    return when (style) {
        CitationStyle.APA -> {
            val dateStr = if (y.isNotEmpty()) " ($y)." else " (n.d.)."
            val titlePart = if (t.isNotEmpty()) " $t." else ""
            val pubPart = if (p.isNotEmpty()) " $p." else ""
            val urlPart = if (u.isNotEmpty()) " $u" else ""
            
            buildString {
                if (authorStr.isNotEmpty()) append(authorStr)
                append(dateStr)
                append(titlePart)
                append(pubPart)
                append(urlPart)
            }.trim().replace("..", ".")
        }
        CitationStyle.MLA -> {
            val titlePart = if (t.isNotEmpty()) " \"$t.\"" else ""
            val pubPart = if (p.isNotEmpty()) " $p," else ""
            val yearPart = if (y.isNotEmpty()) " $y." else ""
            val urlPart = if (u.isNotEmpty()) " $u." else ""
            val accPart = if (da.isNotEmpty()) " Accessed $da." else ""
            
            buildString {
                if (authorStr.isNotEmpty()) append(authorStr)
                append(titlePart)
                append(pubPart)
                append(yearPart)
                append(urlPart)
                append(accPart)
            }.trim().replace("..", ".")
        }
        CitationStyle.CHICAGO -> {
            val titlePart = if (t.isNotEmpty()) " \"$t.\"" else ""
            val pubPart = if (p.isNotEmpty()) " $p," else ""
            val yearPart = if (y.isNotEmpty()) " $y." else ""
            val urlPart = if (u.isNotEmpty()) " $u." else ""
            val accPart = if (da.isNotEmpty()) " Accessed $da." else ""

            buildString {
                if (authorStr.isNotEmpty()) append(authorStr)
                append(titlePart)
                append(pubPart)
                append(yearPart)
                append(urlPart)
                if(accPart.isNotEmpty() && u.isNotEmpty()) append(accPart)
            }.trim().replace("..", ".")
        }
    }
}
