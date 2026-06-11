package com.scholarvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.theme.MyApplicationTheme

data class MenuItem(
    val title: String,
    val icon: ImageVector? = null,
    val tint: Color = Color.Unspecified,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun CustomPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<MenuItem>
) {
    val themeController = LocalThemeController.current
    val isDark = themeController.isDarkTheme
    val themeMode = themeController.themeMode
    val menuBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD0D0D0)
    val itemBgColor = if (isDark) Color(0xFF333333) else Color(0xFFF2F2F2)
    val textColor = if (isDark) Color.White else Color.Black

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 0.dp),
        modifier = Modifier
            .background(menuBgColor, RoundedCornerShape(20.dp))
            .padding(12.dp)
            .width(200.dp)
    ) {
        MyApplicationTheme(themeMode = themeMode) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEach { item ->
                    Surface(
                        color = itemBgColor,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { 
                                onDismissRequest()
                                item.onClick()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (item.isDestructive) Color.Red else if (item.tint == Color.Unspecified) textColor else item.tint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Medium,
                                color = if (item.isDestructive) Color.Red else textColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
