package com.scholarvault.ui.tools.pdf_nup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NUpHelpSheet(
    onDismissRequest: () -> Unit,
    currentOrder: ArrangementOrder
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Text(
                text = "How N-Up Works & Capabilities",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "N-Up is a highly flexible tool that allows you to combine multiple pages, images, or documents into structured grid layouts. Ideal for saving paper on handouts, creating custom booklets, or printing multi-photo sheets.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Processing Modes (Triple Dot Menu):",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "• Merge All Files (Default):\n" +
                       "  Combines selected pages across all loaded documents sequentially into a single output PDF document. Perfect for stitching multi-chapter notes or slides onto a unified sheet layout.\n\n" +
                       "• Process in Parallel:\n" +
                       "  Processes each loaded document or image independently, creating separate corresponding N-Up files. Great for batch-processing several distinct items at once with the same layout rules.\n\n" +
                       "• Photo Grid Repeat:\n" +
                       "  Duplicates each individual source page or photo multiple times to fill an entire sheet matrix grid. This is specially designed for photo printing, badges, or label layouts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Photo Printing & Custom DPI:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "• Why Multi-Copy Matrix Layouts?\n" +
                       "  When printing passport photos, standard wallet-sized prints, custom stickers, ID cards, or name tags, you often need multiple copies of a single image printed on a high-quality sheet. Photo Grid Repeat automates this seamlessly.\n\n" +
                       "• High Resolution & Accuracy:\n" +
                       "  Using custom DPI settings (e.g., 300 DPI, 600 DPI) ensures your raster images and photos translate into sharp, pixel-perfect physical prints without blurring or dynamic resolution degradation. Setting the correct Page Fit and DPI preserves fine details and exact physical dimensions on your photo paper.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Key Layout Parameters:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "• Rows & Columns: Defines the grid layout (e.g., 2 columns × 2 rows puts 4 pages on each sheet).\n" +
                       "• Reading Order: Controls flow directions (Left-to-Right vs Right-to-Left, Horizontally vs Vertically).\n" +
                       "• Spacing & Border: Adjusts inner spacing gaps and adds configurable bordering.\n" +
                       "• Fitting: Scales pages dynamically (Stretch, Full Width, Full Height, or Auto-fit ratio maintainers).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Arrangement Diagram Examples",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ArrangementDiagram(ArrangementOrder.LTR, selected = currentOrder == ArrangementOrder.LTR)
                ArrangementDiagram(ArrangementOrder.RTL, selected = currentOrder == ArrangementOrder.RTL)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ArrangementDiagram(order: ArrangementOrder, selected: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(100.dp, 140.dp)) {
            drawRect(color = bgColor)
            drawRect(color = borderColor, style = Stroke(width = 2.dp.toPx()))
            
            val cols = 2
            val rows = 2
            val cellW = size.width / cols
            val cellH = size.height / rows
            
            val textStyle = TextStyle(fontSize = cellH.toSp() * 0.4f, color = textColor)
            
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = c * cellW
                    val y = r * cellH
                    drawRect(
                        color = borderColor, 
                        topLeft = Offset(x, y), 
                        size = Size(cellW, cellH), 
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    val index = if (order == ArrangementOrder.LTR) {
                        r * cols + c + 1
                    } else {
                        r * cols + (cols - 1 - c) + 1
                    }
                    
                    val txt = index.toString()
                    val measurement = textMeasurer.measure(txt, style = textStyle)
                    drawText(
                        textLayoutResult = measurement,
                        topLeft = Offset(
                            x + (cellW - measurement.size.width) / 2f,
                            y + (cellH - measurement.size.height) / 2f
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (order == ArrangementOrder.LTR) "Left to Right" else "Right to Left",
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
