package com.scholarvault.ui.tools.pdf_inverter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.scholarvault.ui.Screen

@Composable
fun GlobalProgressBanner(navController: NavController, isBottomBarVisible: Boolean) {
    val processingState by PdfProcessingRepository.processingState.collectAsState()

    if (processingState is PdfProcessingState.Processing) {
        val pState = processingState as PdfProcessingState.Processing
        val bottomPadding = if (isBottomBarVisible) 80.dp else 16.dp
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        navController.navigate(Screen.PdfColorInverter.route) {
                            launchSingleTop = true
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { pState.progress / 100f },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Inverting ${pState.fileName} — ${pState.progress}%",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
