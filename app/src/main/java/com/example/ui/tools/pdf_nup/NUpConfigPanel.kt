package com.scholarvault.ui.tools.pdf_nup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NUpConfigPanel(
    config: NUpConfig,
    onConfigChange: (NUpConfig) -> Unit,
    previewContent: (@Composable () -> Unit)? = null,
    onPreviewClick: () -> Unit,
    pageNumbersContent: (@Composable () -> Unit)? = null,
    isMobile: Boolean = false,
    itemsEmpty: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Page Numbers section at the top of controls
        if (pageNumbersContent != null) {
            pageNumbersContent()
        }

        // Processing Mode Status Indicator
        val modeLabel = when(config.processingMode) {
            ProcessingMode.MERGE_ALL -> "Merge All Documents"
            ProcessingMode.PARALLEL_BATCH -> "Process Files in Parallel"
            ProcessingMode.GRID_REPEAT -> "Photo Grid Repeat (Copies)"
        }
        val modeDesc = when(config.processingMode) {
            ProcessingMode.MERGE_ALL -> "Combines all selected pages sequentially into a single PDF sheet grid."
            ProcessingMode.PARALLEL_BATCH -> "Processes each uploaded page as an independent PDF sheet grid."
            ProcessingMode.GRID_REPEAT -> "Repeats each source page multiple times to fill the sheet grid (excellent for photo printing)."
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Mode: $modeLabel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = modeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Responsive Layout: Single unified layout that shows rows, columns, and reading order up to down on the left, and graphics preview on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp), // Increased from 280.dp to 330.dp to prevent parameter cutting
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Controls: Row, Column, Reading Order cards stacked up-to-down in a single Column
            Column(
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Item 1: Rows
                ConfigCard(title = "Rows", modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NumberStepper(
                            value = config.rows,
                            fieldKey = "rows",
                            onValueChange = { if (it in 1..20) onConfigChange(config.copy(rows = it)) }
                        )
                    }
                }
                
                // Item 2: Columns
                ConfigCard(title = "Columns", modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NumberStepper(
                            value = config.columns,
                            fieldKey = "columns",
                            onValueChange = { if (it in 1..20) onConfigChange(config.copy(columns = it)) }
                        )
                    }
                }
                
                // Item 3: Reading Order - styled with plenty of vertical height
                ConfigCard(
                    title = "Order",
                    modifier = Modifier.weight(1.3f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clickable {
                                    val newOrder = if (config.arrangementOrder == ArrangementOrder.LTR) ArrangementOrder.RTL else ArrangementOrder.LTR
                                    onConfigChange(config.copy(arrangementOrder = newOrder))
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            val isLtr = config.arrangementOrder == ArrangementOrder.LTR
                            Text("Left", fontSize = 12.sp)
                            Text(if (isLtr) " → " else " ← ", fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)
                            Text("Right", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clickable {
                                    val nextFlow = when (config.flowLayout) {
                                        FlowLayout.HORIZONTAL_TTB -> FlowLayout.HORIZONTAL_BTT
                                        FlowLayout.HORIZONTAL_BTT -> FlowLayout.VERTICAL_TTB
                                        FlowLayout.VERTICAL_TTB -> FlowLayout.VERTICAL_BTT
                                        FlowLayout.VERTICAL_BTT -> FlowLayout.HORIZONTAL_TTB
                                    }
                                    onConfigChange(config.copy(flowLayout = nextFlow))
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            val flowText = when (config.flowLayout) {
                                FlowLayout.VERTICAL_TTB -> "⬇️ Vertical Flow"
                                FlowLayout.VERTICAL_BTT -> "⬆️ Vertical Flow"
                                FlowLayout.HORIZONTAL_TTB -> "➡️ Horizontal"
                                FlowLayout.HORIZONTAL_BTT -> "⬅️ Horizontal"
                            }
                            Text(flowText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            
            // Right Preview with matching height
            if (previewContent != null) {
                Box(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                ) {
                    previewContent()
                }
            }
        }

        // Page Size & Dimensions (arranged as a single row with dimensions shifted to the right)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left controls: Page Size label, size selector, and rotate button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Page Size", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val sizes = listOf(CanvasSize.LETTER, CanvasSize.A4, CanvasSize.A3, CanvasSize.LEGAL, CanvasSize.CUSTOM)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .clickable {
                            val currentIndex = sizes.indexOf(config.canvasSize)
                            val nextIndex = (currentIndex + 1) % sizes.size
                            onConfigChange(config.copy(canvasSize = sizes[nextIndex]))
                        }
                ) {
                    val sizeLabel = when (config.canvasSize) {
                        CanvasSize.A4 -> "A4"
                        CanvasSize.A3 -> "A3"
                        CanvasSize.LETTER -> "Letter"
                        CanvasSize.LEGAL -> "Legal"
                        CanvasSize.CUSTOM -> "Custom"
                    }
                    Text(
                        text = sizeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            if (config.canvasSize == CanvasSize.CUSTOM) {
                                val currentW = config.customWidthPoints
                                val currentH = config.customHeightPoints
                                onConfigChange(
                                    config.copy(
                                        customWidthPoints = currentH,
                                        customHeightPoints = currentW,
                                        orientation = PageOrientation.AUTO
                                    )
                                )
                            } else {
                                val nextOrientation = if (config.orientation == PageOrientation.LANDSCAPE) {
                                    PageOrientation.PORTRAIT
                                } else {
                                    PageOrientation.LANDSCAPE
                                }
                                onConfigChange(
                                    config.copy(orientation = nextOrientation)
                                )
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rotate Dimensions", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Right controls: Dimensions input boxes shifted to the right side (converted from/to mm)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val wStr = (config.actualCanvasWidthPoints / 2.8346456f).toInt().toString()
                val hStr = (config.actualCanvasHeightPoints / 2.8346456f).toInt().toString()

                DimensionInput(
                    value = wStr,
                    fieldKey = "canvas_width",
                    onValueChange = {
                        val wMm = it.toFloatOrNull()
                        if (wMm != null) {
                            val wPts = wMm * 2.8346456f
                            onConfigChange(config.copy(canvasSize = CanvasSize.CUSTOM, customWidthPoints = wPts))
                        }
                    }
                )
                Text("×", fontSize = 12.sp)
                DimensionInput(
                    value = hStr,
                    fieldKey = "canvas_height",
                    onValueChange = {
                         val hMm = it.toFloatOrNull()
                         if (hMm != null) {
                             val hPts = hMm * 2.8346456f
                             onConfigChange(config.copy(canvasSize = CanvasSize.CUSTOM, customHeightPoints = hPts))
                         }
                    }
                )
                Text("mm", fontSize = 12.sp)
            }
        }

        // Spacing, Outline, Border in dynamic FlowRow on mobile screen to prevent mm cutoff, or adaptive on desktop
        if (isMobile) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Spacing
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Spacing", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    DimensionInput(
                        value = config.innerSpacingDp.toInt().toString(),
                        fieldKey = "spacing_mobile",
                        onValueChange = {
                             val s = it.toFloatOrNull()
                             if (s != null) onConfigChange(config.copy(innerSpacingDp = s))
                        }
                    )
                    Text("mm", fontSize = 12.sp)
                }

                // Outline
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Outline", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    Checkbox(
                        checked = config.drawBorder,
                        onCheckedChange = { onConfigChange(config.copy(drawBorder = it)) },
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Border - can elegantly flow to next line when space is short
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Border", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.clickable { 
                            onConfigChange(config.copy(isBorderIndependent = !config.isBorderIndependent)) 
                        },
                        color = if (config.isBorderIndependent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (!config.isBorderIndependent) {
                        DimensionInput(
                            value = config.borderTopWidthDp.toInt().toString(),
                            fieldKey = "border_mobile",
                            onValueChange = {
                                val b = it.toFloatOrNull()
                                if (b != null) onConfigChange(config.copy(
                                    borderTopWidthDp = b,
                                    borderBottomWidthDp = b,
                                    borderLeftWidthDp = b,
                                    borderRightWidthDp = b
                                ))
                            }
                        )
                        Text("mm", fontSize = 12.sp)
                    } else {
                        Text(
                            text = "[Indep.]", 
                            fontSize = 11.sp, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { 
                                onConfigChange(config.copy(isBorderIndependent = false)) 
                            }
                        )
                    }
                }
            }
        } else {
            // Desktop layout uses FlowRow to adapt elegantly to varying sidebar widths
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Spacing
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Spacing", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    DimensionInput(
                        value = config.innerSpacingDp.toInt().toString(),
                        fieldKey = "spacing_desktop",
                        onValueChange = {
                             val s = it.toFloatOrNull()
                             if (s != null) onConfigChange(config.copy(innerSpacingDp = s))
                        }
                    )
                    Text("mm", fontSize = 13.sp)
                }

                // Outline
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Outline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Checkbox(
                        checked = config.drawBorder,
                        onCheckedChange = { onConfigChange(config.copy(drawBorder = it)) },
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Border
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Border", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { 
                            onConfigChange(config.copy(isBorderIndependent = !config.isBorderIndependent)) 
                        },
                        color = if (config.isBorderIndependent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (!config.isBorderIndependent) {
                        DimensionInput(
                            value = config.borderTopWidthDp.toInt().toString(),
                            fieldKey = "border_desktop",
                            onValueChange = {
                                val b = it.toFloatOrNull()
                                if (b != null) onConfigChange(config.copy(
                                    borderTopWidthDp = b,
                                    borderBottomWidthDp = b,
                                    borderLeftWidthDp = b,
                                    borderRightWidthDp = b
                                ))
                            }
                        )
                        Text("mm", fontSize = 13.sp)
                    } else {
                        Text(
                            text = "[Indep.]", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { 
                                onConfigChange(config.copy(isBorderIndependent = false)) 
                            }
                        )
                    }
                }
            }
        }

        // Independent Borders subweights displayed below if toggled
        androidx.compose.animation.AnimatedVisibility(visible = config.isBorderIndependent) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Independent Borders (mm)", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Top", fontSize = 11.sp)
                            DimensionInput(value = config.borderTopWidthDp.toInt().toString(), fieldKey = "border_top", onValueChange = { val b = it.toFloatOrNull(); if (b!=null) onConfigChange(config.copy(borderTopWidthDp = b))})
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bot", fontSize = 11.sp)
                            DimensionInput(value = config.borderBottomWidthDp.toInt().toString(), fieldKey = "border_bot", onValueChange = { val b = it.toFloatOrNull(); if (b!=null) onConfigChange(config.copy(borderBottomWidthDp = b))})
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Left", fontSize = 11.sp)
                            DimensionInput(value = config.borderLeftWidthDp.toInt().toString(), fieldKey = "border_left", onValueChange = { val b = it.toFloatOrNull(); if (b!=null) onConfigChange(config.copy(borderLeftWidthDp = b))})
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Right", fontSize = 11.sp)
                            DimensionInput(value = config.borderRightWidthDp.toInt().toString(), fieldKey = "border_right", onValueChange = { val b = it.toFloatOrNull(); if (b!=null) onConfigChange(config.copy(borderRightWidthDp = b))})
                        }
                    }
                }
            }
        }

        // Fitting & Preview Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fitting", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val fits = PageFit.values()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable {
                        val curr = fits.indexOf(config.pageFit)
                        val next = (curr + 1) % fits.size
                        onConfigChange(config.copy(pageFit = fits[next]))
                    }
                ) {
                    Text(
                        text = config.pageFit.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            Surface(
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (itemsEmpty) {
                            android.widget.Toast.makeText(
                                context,
                                "No documents loaded to preview.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onPreviewClick()
                        }
                    }
            ) {
                Text(
                    text = "Preview",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth()
                )
            }
        }

        // Output File Rename Box - at the end of configurations in NUpConfigPanel, NOT locked to the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Text(
                text = "Output File Name",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = config.outputFileName,
                onValueChange = { onConfigChange(config.copy(outputFileName = it)) },
                placeholder = { Text("e.g. NUp_Document") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
fun ConfigControls(config: NUpConfig, onConfigChange: (NUpConfig) -> Unit, modifier: Modifier = Modifier) {
    // Rows
    ConfigCard(title = "Rows", modifier = modifier) {
        NumberStepper(
            value = config.rows,
            fieldKey = "rows",
            onValueChange = { if (it in 1..20) onConfigChange(config.copy(rows = it)) }
        )
    }

    // Columns
    ConfigCard(title = "Columns", modifier = modifier) {
        NumberStepper(
            value = config.columns,
            fieldKey = "columns",
            onValueChange = { if (it in 1..20) onConfigChange(config.copy(columns = it)) }
        )
    }

    // Reading Order
    ConfigCard(title = "Order", modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clickable {
                        val newOrder = if (config.arrangementOrder == ArrangementOrder.LTR) ArrangementOrder.RTL else ArrangementOrder.LTR
                        onConfigChange(config.copy(arrangementOrder = newOrder))
                    }
                    .padding(2.dp)
            ) {
                val isLtr = config.arrangementOrder == ArrangementOrder.LTR
                Text("Left", fontSize = 13.sp)
                Text(if (isLtr) "→" else "←", fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
                Text("Right", fontSize = 13.sp)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clickable {
                        val nextFlow = when (config.flowLayout) {
                            FlowLayout.HORIZONTAL_TTB -> FlowLayout.HORIZONTAL_BTT
                            FlowLayout.HORIZONTAL_BTT -> FlowLayout.VERTICAL_TTB
                            FlowLayout.VERTICAL_TTB -> FlowLayout.VERTICAL_BTT
                            FlowLayout.VERTICAL_BTT -> FlowLayout.HORIZONTAL_TTB
                        }
                        onConfigChange(config.copy(flowLayout = nextFlow))
                    }
                    .padding(2.dp)
            ) {
                val flowText = when (config.flowLayout) {
                    FlowLayout.VERTICAL_TTB -> "⬇️ Vertically"
                    FlowLayout.VERTICAL_BTT -> "⬆️ Vertically"
                    FlowLayout.HORIZONTAL_TTB -> "➡️ Horizontally"
                    FlowLayout.HORIZONTAL_BTT -> "⬅️ Horizontally"
                }
                Text(flowText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ConfigCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
fun NumberStepper(value: Int, fieldKey: String, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.clickable { onValueChange(value - 1) }.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }
        }
        
        BasicTextFieldWrapper(
            value = value.toString(),
            fieldKey = fieldKey,
            onValueChange = { 
                val newVal = it.toIntOrNull()
                if (newVal != null) onValueChange(newVal)
            },
            modifier = Modifier.width(48.dp)
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.clickable { onValueChange(value + 1) }.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DimensionInput(value: String, fieldKey: String, onValueChange: (String) -> Unit) {
    BasicTextFieldWrapper(
        value = value,
        fieldKey = fieldKey,
        onValueChange = onValueChange,
        modifier = Modifier.width(54.dp)
    )
}

@Composable
fun BasicTextFieldWrapper(value: String, fieldKey: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboardController = LocalNUpKeyboard.current
    val isActive = keyboardController?.activeFieldKey?.value == fieldKey

    Surface(
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent,
        modifier = modifier
            .height(36.dp) // Height increased from 30.dp to 36.dp to prevent numeric cutting
            .clickable {
                keyboardController?.requestKeyboard(fieldKey, value, onValueChange)
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = value,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
