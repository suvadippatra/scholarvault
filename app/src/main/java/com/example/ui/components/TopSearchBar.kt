package com.scholarvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.animation.togetherWith
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import com.scholarvault.ui.CachedImage
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.filled.Clear

import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.theme.MyApplicationTheme
import com.scholarvault.ui.viewmodel.ProfileViewModel

val LocalCustomNavigate = androidx.compose.runtime.compositionLocalOf<(String) -> Unit> { {} }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSearchBar(
    onOpenDrawer: () -> Unit,
    isBackButton: Boolean = false,
    title: String? = null,
    searchQuery: String = "", // Kept for backwards compatibility but we use ViewModel's
    onSearchChange: (String) -> Unit = {},
    onNavigate: ((String) -> Unit)? = null,
    showProfileIcon: Boolean = true,
    showSearchBar: Boolean = true,
    viewModel: ProfileViewModel = viewModel(),
    actions: @Composable RowScope.() -> Unit = {}
) {
    val globalOnNavigate = LocalCustomNavigate.current
    val resolvedOnNavigate = onNavigate ?: globalOnNavigate
    val theme = LocalThemeController.current
    val isDark = theme.isDarkTheme

    val textColorPrimary = if (isDark) Color.White else Color.Black
    val textColorSecondary = if (isDark) Color(0xFFAAAAAA) else Color.DarkGray
    val searchBg = if (isDark) Color(0xFF333333) else Color(0xFFD6D6D6)

    var showProfileMenu by remember { mutableStateOf(false) }
    
    val profileFlow by viewModel.profileStream.collectAsState()
    val profile = profileFlow?.profile

    val vmSearchQuery by viewModel.searchQuery.collectAsState()
    var queryState by remember { mutableStateOf(searchQuery) }

    LaunchedEffect(searchQuery) {
        if (searchQuery != queryState) {
            queryState = searchQuery
        }
    }

    LaunchedEffect(vmSearchQuery) {
        if (searchQuery.isEmpty() && vmSearchQuery != queryState) {
            queryState = vmSearchQuery
        }
    }

    val currentOnSearchChange = { newQuery: String ->
        queryState = newQuery
        viewModel.updateSearchQuery(newQuery)
        onSearchChange(newQuery)
    }

    val focusRequester = remember { FocusRequester() }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 960

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .height(48.dp)
            .padding(horizontal = if (isTablet) 32.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onOpenDrawer) {
            if (theme.animationsEnabled) {
                androidx.compose.animation.AnimatedContent(
                    targetState = isBackButton,
                    label = "iconConv",
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                         androidx.compose.animation.scaleIn(initialScale = 0.8f)) togetherWith (
                           androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                           androidx.compose.animation.scaleOut(targetScale = 0.8f)
                        )
                    }
                ) { back ->
                    Icon(
                        imageVector = if (back) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                        contentDescription = if (back) "Back" else "Menu",
                        tint = textColorPrimary
                    )
                }
            } else {
                androidx.compose.animation.Crossfade(targetState = isBackButton, label = "iconFade", animationSpec = androidx.compose.animation.core.tween(if (theme.animationsEnabled) 300 else 0)) { back ->
                    Icon(
                        imageVector = if (back) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                        contentDescription = if (back) "Back" else "Menu",
                        tint = textColorPrimary
                    )
                }
            }
        }

        if (title != null) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColorPrimary,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        } else {
            if (theme.animationsEnabled) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showSearchBar,
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    ) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    ) + androidx.compose.animation.fadeOut(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isEditing) {
                        TextField(
                            value = queryState,
                            onValueChange = currentOnSearchChange,
                            placeholder = { Text("Search...", color = textColorSecondary, fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = searchBg,
                                unfocusedContainerColor = searchBg,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textColorPrimary,
                                unfocusedTextColor = textColorPrimary,
                                cursorColor = textColorPrimary
                            ),
                            maxLines = 1,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 20.sp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    currentOnSearchChange("")
                                    isEditing = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = textColorPrimary
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(searchBg)
                                .clickable { isEditing = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (queryState.isEmpty()) "Search..." else queryState,
                                color = if (queryState.isEmpty()) textColorSecondary else textColorPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                if (!showSearchBar) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                if (showSearchBar) {
                    if (isEditing) {
                        TextField(
                            value = queryState,
                            onValueChange = currentOnSearchChange,
                            placeholder = { Text("Search...", color = textColorSecondary, fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = searchBg,
                                unfocusedContainerColor = searchBg,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textColorPrimary,
                                unfocusedTextColor = textColorPrimary,
                                cursorColor = textColorPrimary
                            ),
                            maxLines = 1,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 20.sp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    currentOnSearchChange("")
                                    isEditing = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = textColorPrimary
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(searchBg)
                                .clickable { isEditing = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (queryState.isEmpty()) "Search..." else queryState,
                                color = if (queryState.isEmpty()) textColorSecondary else textColorPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        actions()
        if (showProfileIcon) {
            Box {
                IconButton(onClick = { showProfileMenu = true }) {
                    if (profile?.profilePicUri != null) {
                        Box(modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape)) {
                            CachedImage(uri = Uri.parse(profile.profilePicUri))
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(32.dp),
                            tint = textColorPrimary
                        )
                    }
                }
                if (showProfileMenu) {
                    androidx.compose.ui.window.Popup(
                        alignment = Alignment.TopEnd,
                        offset = androidx.compose.ui.unit.IntOffset(0, 0),
                        onDismissRequest = { showProfileMenu = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                    ) {
                        val isDark = theme.isDarkTheme
                        val menuBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD0D0D0)
                        val itemBgColor = if (isDark) Color(0xFF333333) else Color(0xFFF2F2F2)
                        val textColor = if (isDark) Color.White else Color.Black

                        MyApplicationTheme(themeMode = theme.themeMode) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = menuBgColor,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .width(220.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    // Header with Icon
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(top = 18.dp)) {
                                            val fullName = listOf(profile?.firstName, profile?.middleName, profile?.lastName).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")
                                            Text(if(fullName.isNotBlank()) fullName else "Student", fontWeight = FontWeight.Normal, color = textColor, fontSize = 16.sp, textAlign = TextAlign.End, maxLines = 1)
                                            val email = profile?.email
                                            if (!email.isNullOrBlank()) {
                                                Text(email, color = textColor, fontSize = 12.sp, textAlign = TextAlign.End, maxLines = 1)
                                            }
                                        }
                                        IconButton(
                                            onClick = { showProfileMenu = false },
                                        ) {
                                            if (profile?.profilePicUri != null) {
                                                Box(modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape)) {
                                                    CachedImage(uri = Uri.parse(profile.profilePicUri))
                                                }
                                            } else {
                                                Icon(
                                                    Icons.Default.AccountCircle,
                                                    contentDescription = "Profile",
                                                    modifier = Modifier.size(32.dp),
                                                    tint = textColor
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                        listOf("View Profile", "Export CV", "About Dev.", "Settings").forEach { itemTitle ->
                                            Surface(
                                                color = itemBgColor,
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable { 
                                                        showProfileMenu = false 
                                                        if (itemTitle == "View Profile") {
                                                            resolvedOnNavigate("profile")
                                                        } else if (itemTitle == "Settings") {
                                                            resolvedOnNavigate("settings")
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    itemTitle,
                                                    modifier = Modifier.padding(vertical = 12.dp),
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.Normal,
                                                    color = textColor,
                                                    fontSize = 14.sp
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
    }
    
    // Global search overlay (renders safely outside the row layout)
    GlobalSearchOverlay(
        searchQuery = queryState,
        onDismiss = { currentOnSearchChange("") }, // Clear query on selection to dismiss popup
        onNavigate = resolvedOnNavigate
    )
}
