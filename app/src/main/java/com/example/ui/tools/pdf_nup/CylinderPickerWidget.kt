package com.scholarvault.ui.tools.pdf_nup

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CylinderPickerWidget(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    currentValue: Int,
    range: IntRange = 1..12,
    onValueSelected: (Int) -> Unit
) {
    if (!isVisible) return

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
    ) {
        val initialIndex = maxOf(0, currentValue - range.first)
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
        
        val itemsList = listOf(-1, -2) + range.toList() + listOf(-3, -4) 
        
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .collect { isScrolling ->
                    if (!isScrolling) {
                        val layoutInfo = listState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        val viewportCenter = layoutInfo.viewportEndOffset / 2
                        
                        val closestItem = visibleItems.minByOrNull {
                            Math.abs((it.offset + it.size / 2) - viewportCenter)
                        }
                        
                        if (closestItem != null) {
                            val selectedValue = itemsList[closestItem.index]
                            if (selectedValue in range && selectedValue != currentValue) {
                                onValueSelected(selectedValue)
                            }
                        }
                    }
                }
        }

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(itemsList) { itemValue ->
                    val isRealItem = itemValue in range
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRealItem) {
                            val isSelected = itemValue == currentValue
                            Text(
                                text = itemValue.toString(),
                                fontSize = if (isSelected) 24.sp else 18.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            // Highlight bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            )
        }
    }
}
