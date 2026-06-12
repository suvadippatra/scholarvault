package com.scholarvault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInstructionsSheet(
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val textColor = if (isDark) Color.White else Color.Black

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFBFCFD),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Profile Guide",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            
            InstructionItem(
                icon = Icons.Default.Security,
                title = "Privacy First",
                description = "Your sensitive data (Aadhaar, PAN) is encrypted via AES-256 in the SecurityVault and never leaves your device.",
                textColor = textColor
            )
            
            InstructionItem(
                icon = Icons.Default.Sync,
                title = "Wallet Cross-Linking",
                description = "Adding an ID or document here can automatically synchronize with your digital Wallet, keeping everything unified.",
                textColor = textColor
            )
            
            InstructionItem(
                icon = Icons.Default.DonutLarge,
                title = "Completeness Score",
                description = "A gamified arc indicates your profile's fullness. Filling out your details helps auto-fill college apps seamlessly.",
                textColor = textColor
            )
            
            InstructionItem(
                icon = Icons.Default.Edit,
                title = "Editing & View Modes",
                description = "Tap the pencil icon to toggle into Edit Mode. The interface transforms into a formal entry tool with specialized date and numeric keyboards.",
                textColor = textColor
            )
            
            InstructionItem(
                icon = Icons.Default.Crop,
                title = "Image Cropper",
                description = "Instantly crop and rotate your profile picture or signature with the built-in slider and bounding box editor.",
                textColor = textColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InstructionItem(
    icon: ImageVector,
    title: String,
    description: String,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp).padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = textColor)
            Text(description, fontSize = 15.sp, color = textColor.copy(alpha = 0.8f), lineHeight = 22.sp)
        }
    }
}
