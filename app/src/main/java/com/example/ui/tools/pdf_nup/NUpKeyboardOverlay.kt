package com.scholarvault.ui.tools.pdf_nup

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp

@Composable
fun NUpKeyboardOverlay(
    isVisible: Boolean,
    onHide: () -> Unit,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Keyboard", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    IconButton(onClick = onHide) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Keyboard")
                    }
                }

                val keys = listOf(
                    listOf("7", "8", "9", "÷", "⌫"),
                    listOf("4", "5", "6", "×", "-"),
                    listOf("1", "2", "3", ",", "+"),
                    listOf("C", "0", ".", "SPACE")
                )
                
                val buttonSpacing = 6.dp
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Column(
                        modifier = Modifier.widthIn(max = if (isTablet) 400.dp else Dp.Unspecified).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(buttonSpacing)
                    ) {
                        keys.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                            ) {
                                row.forEach { btn ->
                                    val isAction = btn in listOf("C", "⌫", "SPACE")
                                    val isOperator = btn in listOf("÷", "×", "-", "+", ",")
                                    val buttonColor = when {
                                        isOperator -> MaterialTheme.colorScheme.secondaryContainer
                                        isAction -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                    val contentColor = when {
                                        isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
                                        isAction -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    val weight = if (row.size == 4 && btn == "SPACE") 2f else 1f
                                    Box(
                                        modifier = Modifier
                                            .weight(weight)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(buttonColor)
                                            .clickable { onKeyPress(btn) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (btn == "SPACE") "␣" else btn,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
