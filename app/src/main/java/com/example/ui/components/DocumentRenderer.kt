package com.scholarvault.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DocumentAttachment(val name: String, val uri: String)

@Composable
fun DocumentPreviewDialog(attachment: DocumentAttachment, onDismiss: () -> Unit, cardBg: Color, textColor: Color) {
    val context = LocalContext.current
    val uri = android.net.Uri.parse(attachment.uri)
    var isImage by remember(uri) { mutableStateOf(false) }
    var isPdf by remember(uri) { mutableStateOf(false) }
    var launchedIntent by remember(uri) { mutableStateOf(false) }
    
    LaunchedEffect(uri) {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val fileName = attachment.name.lowercase()
        isPdf = mimeType.contains("pdf") || fileName.endsWith(".pdf") || attachment.uri.lowercase().contains(".pdf")
        isImage = mimeType.startsWith("image/") || fileName.endsWith(".jpg") || fileName.endsWith(".png") || mimeType.contains("jpeg")
        
        if (isPdf && !launchedIntent) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setPackage("com.google.android.apps.docs") // Google Drive PDF Viewer
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(fallbackIntent, "Open PDF"))
                } catch (e2: Exception) {
                     // Unable to open
                }
            }
            launchedIntent = true
            onDismiss()
        }
    }

    if (isPdf) {
        return // Handled by Intent
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        ElevatedCard(
            modifier = Modifier.fillMaxSize(0.95f).padding(vertical = 24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = attachment.name, 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = textColor, 
                        maxLines = 1, 
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                }
                Divider(color = textColor.copy(alpha = 0.1f))
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    if (isImage) {
                        ImageRendererView(uri = uri)
                    } else {
                        Text("Unsupported Format", color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun ImageRendererView(uri: android.net.Uri) {
   val context = LocalContext.current
   var bitmap by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
   var error by remember(uri) { mutableStateOf<String?>(null) }
   
   var scale by remember { mutableFloatStateOf(1f) }
   var offset by remember { mutableStateOf(Offset.Zero) }
   val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
       scale = (scale * zoomChange).coerceIn(1f, 5f)
       if (scale > 1f) {
           offset += offsetChange
       } else {
           offset = Offset.Zero
       }
   }
   
   LaunchedEffect(uri) {
       withContext(Dispatchers.IO) {
           try {
               val decoded = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                   val source = if (uri.scheme == "file" && uri.path != null) {
                       android.graphics.ImageDecoder.createSource(java.io.File(uri.path!!))
                   } else {
                       android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                   }
                   android.graphics.ImageDecoder.decodeBitmap(source) { dec, _, _ ->
                       dec.allocator = android.graphics.ImageDecoder.ALLOCATOR_DEFAULT
                   }
               } else {
                   val inputStream = if (uri.scheme == "file" && uri.path != null) java.io.FileInputStream(java.io.File(uri.path!!)) else context.contentResolver.openInputStream(uri)
                   val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                   inputStream?.close()
                   bmp
               }
               bitmap = decoded?.asImageBitmap()
               if (decoded == null) error = "Could not decode image"
           } catch(e: Exception) {
               error = e.localizedMessage
           }
       }
   }
   if (bitmap != null) {
       Image(
           bitmap = bitmap!!, 
           contentDescription = null, 
           modifier = Modifier
               .fillMaxSize()
               .graphicsLayer(
                   scaleX = scale,
                   scaleY = scale,
                   translationX = offset.x,
                   translationY = offset.y
               )
               .transformable(state = transformableState)
       )
   } else if (error != null) {
       Text("Error loading image: $error", color = Color.Red)
   } else {
       CircularProgressIndicator()
   }
}

