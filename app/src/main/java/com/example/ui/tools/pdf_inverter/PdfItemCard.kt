package com.scholarvault.ui.tools.pdf_inverter

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PdfItemCard(
    item: PdfItem,
    onRemove: () -> Unit,
    onNameChange: (String) -> Unit,
    onPagesChange: (String) -> Unit,
    onModeChange: (InvertMode) -> Unit,
    onPreview: (Uri, InvertMode?, String, ((InvertMode) -> Unit)?) -> Unit,
    onSaveToApp: () -> Unit,
    onSaveToDownloads: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${item.pageCount} pages", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, "Remove")
                }
            }
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = item.newName,
                onValueChange = onNameChange,
                label = { Text("Output Filename") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = item.pagesToInvertStr,
                onValueChange = onPagesChange,
                label = { Text("Pages to Invert (e.g. 1-2, 5, 8-)") },
                placeholder = { Text("all") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            
            var expandedMode by remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedMode = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Mode: ${item.mode.label}",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, "", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = expandedMode, onDismissRequest = { expandedMode = false }) {
                        InvertMode.values().forEach { m ->
                            DropdownMenuItem(text = { 
                                Column {
                                    Text(m.label, fontWeight = FontWeight.Bold)
                                    Text(m.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }, onClick = { 
                                onModeChange(m)
                                expandedMode = false 
                            })
                        }
                    }
                }
                
                Spacer(Modifier.width(8.dp))
                
                TextButton(
                    onClick = { onPreview(item.uri, item.mode, item.pagesToInvertStr, onModeChange) },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Preview, "Preview", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Interactive Preview", fontSize = 13.sp)
                }
            }
            
            if (item.resultUri != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onPreview(item.resultUri, null, "", null) }) {
                        Icon(Icons.Default.Preview, "Preview")
                    }
                    IconButton(onClick = onSaveToDownloads) {
                        Icon(Icons.Default.Save, "Save")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = onPrint) {
                        Icon(Icons.Default.Print, "Print")
                    }
                }
            }
        }
    }
}
