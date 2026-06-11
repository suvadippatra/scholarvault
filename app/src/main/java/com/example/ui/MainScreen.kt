// Path: app/src/main/java/com.scholarvault/ui/MainScreen.kt
package com.scholarvault.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.scholarvault.ui.tools.focustimer.FocusTimerScreen
import com.scholarvault.ui.theme.LocalThemeController
import com.scholarvault.ui.viewmodel.AcademicViewModel
import com.scholarvault.ui.viewmodel.ProfileViewModel
import com.scholarvault.ui.viewmodel.DocumentViewModel
import com.scholarvault.ui.viewmodel.DashboardViewModel
import com.scholarvault.ui.viewmodel.TransactionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Academics : Screen("academics", "Academics", Icons.Default.School)
    object Documents : Screen("documents", "Documents", Icons.Default.Folder)
    object ViewWallet : Screen("view_wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Trash : Screen("trash", "Trash", Icons.Default.Delete)
    object WalletCardDetail : Screen("wallet_card/{cardId}", "Card Detail", Icons.Default.CreditCard) {
        fun createRoute(cardId: Int) = "wallet_card/$cardId"
    }
    object EditWalletCard : Screen("edit_wallet_card/{categoryId}/{cardId}", "Edit Card", Icons.Default.Edit) {
        fun createRoute(categoryId: Int, cardId: Int = -1) = "edit_wallet_card/$categoryId/$cardId"
    }
    object Tools : Screen("tools", "Tools", Icons.Default.Build)
    object PrePrintingSetup : Screen("pre_printing_setup", "Pre-Print Setup", Icons.Default.Print)
    object ImageResizer : Screen("image_resizer", "Image Resizer", Icons.Default.Image)
    object PdfColorInverter : Screen("pdf_color_inverter", "PDF Color Inverter", Icons.Default.PictureAsPdf)
    object PdfNUp : Screen("pdf_nup", "PDF N-Up", Icons.Default.ViewModule)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object AcademicDetail : Screen("academic_detail/{id}", "Academic Detail", Icons.Default.School) {
        fun createRoute(id: String) = "academic_detail/$id"
    }
    object AddAcademicItem : Screen("add_academic_item/{category}", "Add Academic", Icons.Default.Add) {
        fun createRoute(category: String) = "add_academic_item/$category"
    }
    object EditAcademicItem : Screen("edit_academic_item/{id}", "Edit Academic", Icons.Default.Edit) {
        fun createRoute(id: String) = "edit_academic_item/$id"
    }
    object Viewer : Screen("viewer/{fileType}/{filePath}/{fileName}", "Viewer", Icons.Default.Fullscreen) {
        fun createRoute(fileType: String, filePath: String, fileName: String): String {
            val encodedPath = android.util.Base64.encodeToString((if (filePath.isEmpty()) "empty" else filePath).toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val encodedName = android.util.Base64.encodeToString(fileName.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            return "viewer/$fileType/$encodedPath/$encodedName"
        }
    }
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object PasswordGenerator : Screen("password_generator", "Password Generator", Icons.Default.Password)
    object FocusTimer : Screen("focus_timer", "Focus Timer", Icons.Default.Timer)
    object CgpaCalculator : Screen("cgpa_calculator", "CGPA Calculator", Icons.Default.Calculate)
    object UnitConverter : Screen("unit_converter", "Unit Converter", Icons.Default.SyncAlt)
    object CitationGenerator : Screen("citation_generator", "Citation Generator", Icons.Default.FormatQuote)
    object FlashcardsDecks : Screen("flashcards_decks", "Flashcards", Icons.Default.Style)
    object FlashcardDeckDetail : Screen("flashcard_deck_detail/{deckId}", "Deck Detail", Icons.Default.Flip) {
        fun createRoute(deckId: String) = "flashcard_deck_detail/$deckId"
    }
    object FlashcardStudy : Screen("flashcard_study/{deckId}", "Study Deck", Icons.Default.PlayArrow) {
        fun createRoute(deckId: String) = "flashcard_study/$deckId"
    }
    object QuickNoteFull : Screen("quick_note_full", "Take Notes", Icons.Default.NoteAlt)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.List)
    object Reminders : Screen("reminders", "Reminders", Icons.Default.Notifications)
    object NotesList : Screen("notes_list", "Notes", Icons.Default.Notes)
    object NotesComposer : Screen("notes_composer/{noteId}", "Note Composer", Icons.Default.EditNote) {
        fun createRoute(noteId: String) = "notes_composer/$noteId"
    }
    object About : Screen("about", "About", Icons.Default.Info)
    object CompressImage : Screen("compress_image", "Compress Image", Icons.Default.Compress)
    object ImageToPdf : Screen("image_to_pdf", "Image to PDF", Icons.Default.PictureAsPdf)
    object SecureBackup : Screen("secure_backup", "Secure Backup", Icons.Default.Backup)
    object DataExport : Screen("data_export", "Data Export", Icons.Default.ImportExport)
    object DocumentScanner : Screen("document_scanner", "Document Scanner", Icons.Default.DocumentScanner)
    object RecentGeneratedMedia : Screen("recent_generated_media", "Recent Generated Media", Icons.Default.History)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.CurrencyExchange)
}

val BottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Academics,
    Screen.Documents,
    Screen.Tools
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AcademicViewModel, docViewModel: DocumentViewModel) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    var showToolChooser by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            com.scholarvault.ui.tools.SharedData.pendingUris.collect { uris ->
                if (uris.isNotEmpty()) {
                    if (com.scholarvault.ui.tools.SharedData.navigateToPrePrint.value) {
                        showToolChooser = false
                        navController.navigate(Screen.PrePrintingSetup.route) { launchSingleTop = true }
                        com.scholarvault.ui.tools.SharedData.navigateToPrePrint.value = false
                    } else {
                        showToolChooser = true
                    }
                }
            }
        }
        launch {
            com.scholarvault.ui.tools.SharedData.pendingPdfUri.collect { uri ->
                if (uri != null) {
                    val contextLocal = context
                    val displayName = try {
                        var name: String? = null
                        if (uri.scheme == "content") {
                            contextLocal.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                    if (nameIdx != -1) {
                                        name = cursor.getString(nameIdx)
                                    }
                                }
                            }
                        }
                        name ?: uri.lastPathSegment
                    } catch (e: Exception) {
                        null
                    } ?: "External_Document.pdf"

                    // Instantly open the PDF without gateway delay!
                    navController.navigate(Screen.Viewer.createRoute("pdf_external", uri.toString(), displayName))
                    com.scholarvault.ui.tools.SharedData.pendingPdfUri.value = null
                }
            }
        }
    }
    
    if (showToolChooser) {
        val uris = com.scholarvault.ui.tools.SharedData.pendingUris.value
        val isAllImages = uris.isNotEmpty() && uris.all { context.contentResolver.getType(it)?.startsWith("image/") == true }
        
        AlertDialog(
            onDismissRequest = { 
                showToolChooser = false
                com.scholarvault.ui.tools.SharedData.pendingUris.value = emptyList()
            },
            title = { Text("Choose Tool") },
            text = {
                Column {
                    Text("Select a tool for the shared file(s):")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showToolChooser = false
                            navController.navigate(Screen.PrePrintingSetup.route) { launchSingleTop = true }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pre-Printing Setup")
                    }
                    if (uris.isNotEmpty() && (uris.size == 1 || isAllImages)) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                showToolChooser = false
                                navController.navigate(Screen.ImageResizer.route) { launchSingleTop = true }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Image Resizer")
                        }
                    }
                    val isAllPdfs = uris.isNotEmpty() && uris.all {
                        it.toString().endsWith(".pdf", ignoreCase = true) || context.contentResolver.getType(it) == "application/pdf"
                    }
                    if (isAllPdfs) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                showToolChooser = false
                                navController.navigate(Screen.PdfColorInverter.route) { launchSingleTop = true }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("PDF Color Inverter")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    showToolChooser = false
                    com.scholarvault.ui.tools.SharedData.pendingUris.value = emptyList()
                }) { Text("Cancel") }
            }
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isSidebarExpanded by rememberSaveable { mutableStateOf(false) }
    var isFabMenuOpen by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var showZoomedProfilePic by remember { mutableStateOf(false) }
    val isDark = LocalThemeController.current.isDarkTheme
    val sidebarBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD9D9D9)
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 960
    val isWideScreen = configuration.screenWidthDp >= 1200

    val drawerContentBlock: @Composable () -> Unit = {
        ModalDrawerSheet(
            modifier = Modifier.width(if (isTablet) (if (isWideScreen) 320.dp else 280.dp) else 300.dp),
            drawerContainerColor = sidebarBgColor,
            drawerShape = if (isTablet) RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp) else RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
        ) {
            SidebarContent(
                onClose = { scope.launch { drawerState.close() } },
                onProfileClick = { showZoomedProfilePic = true },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    scope.launch { drawerState.close() }
                }
            )
        }
    }

    val screenContent: @Composable () -> Unit = {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val isBottomBarVisible = currentRoute in listOf(
            Screen.Dashboard.route,
            Screen.Academics.route,
            Screen.Documents.route,
            Screen.Tools.route
        )
        
        val scaffoldBgColor = if (isDark) Color(0xFF121212) else Color(0xFFFCFCFC)
        val navBgColor = if (isDark) Color(0xFF2C2C30) else Color(0xFFDCD5E4)
        
        Scaffold(
            containerColor = scaffoldBgColor,
            contentWindowInsets = WindowInsets.navigationBars
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (isBottomBarVisible && !isTablet) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .align(Alignment.BottomCenter)
                            .background(navBgColor)
                    )
                }
                
                val blurRadius by androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (isFabMenuOpen) 16.dp else 0.dp,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
                )
                
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .blur(blurRadius)
                ) {
                    val animationsEnabled = LocalThemeController.current.animationsEnabled
                    val animationDuration = if (isTablet) 300 else 400
                    
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        enterTransition = {
                            if (animationsEnabled) {
                                val isBottomNavRoute = targetState.destination.route in listOf(
                                    Screen.Dashboard.route, Screen.Academics.route, Screen.Documents.route, Screen.Tools.route
                                )
                                val isInitialBottomNav = initialState.destination.route in listOf(
                                    Screen.Dashboard.route, Screen.Academics.route, Screen.Documents.route, Screen.Tools.route
                                )
                                
                                if (isBottomNavRoute && isInitialBottomNav) {
                                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(animationDuration)) +
                                    androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = androidx.compose.animation.core.tween(animationDuration))
                                } else {
                                    slideIntoContainer(
                                        towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = androidx.compose.animation.core.spring()
                                    ) + androidx.compose.animation.fadeIn()
                                }
                            } else androidx.compose.animation.EnterTransition.None
                        },
                        exitTransition = {
                            if (animationsEnabled) {
                                val isBottomNavRoute = targetState.destination.route in listOf(
                                    Screen.Dashboard.route, Screen.Academics.route, Screen.Documents.route, Screen.Tools.route
                                )
                                val isInitialBottomNav = initialState.destination.route in listOf(
                                    Screen.Dashboard.route, Screen.Academics.route, Screen.Documents.route, Screen.Tools.route
                                )
                                
                                if (isBottomNavRoute && isInitialBottomNav) {
                                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(animationDuration))
                                } else {
                                    slideOutOfContainer(
                                        towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = androidx.compose.animation.core.spring()
                                    ) + androidx.compose.animation.fadeOut()
                                }
                            } else androidx.compose.animation.ExitTransition.None
                        },
                        popEnterTransition = {
                            if (animationsEnabled) {
                                slideIntoContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = androidx.compose.animation.core.spring()
                                ) + androidx.compose.animation.fadeIn()
                            } else androidx.compose.animation.EnterTransition.None
                        },
                        popExitTransition = {
                            if (animationsEnabled) {
                                slideOutOfContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = androidx.compose.animation.core.spring()
                                ) + androidx.compose.animation.fadeOut()
                            } else androidx.compose.animation.ExitTransition.None
                        }
                    ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            onOpenDrawer = { 
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            },
                            onNavigate = { route -> navController.navigate(route) },
                            viewModel = dashboardViewModel
                        )
                    }
                    composable("ids_list") {
                        IdsListScreen(onBack = { navController.popBackStack() }, docViewModel = docViewModel)
                    }
                    composable(Screen.Academics.route) {
                        AcademicsScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { 
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            },
                            onNavigate = { route -> navController.navigate(route) },
                            onNavigateToDetail = { id -> navController.navigate(Screen.AcademicDetail.createRoute(id)) },
                            onNavigateToAddItem = { cat -> navController.navigate(Screen.AddAcademicItem.createRoute(cat)) },
                            onNavigateToIdsList = { navController.navigate("ids_list") }
                        )
                    }
                    composable(Screen.AddAcademicItem.route) { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: "Skill-Based"
                        val context = androidx.compose.ui.platform.LocalContext.current
                        com.scholarvault.ui.components.AddAcademicItemScreen(
                            category = category,
                            docViewModel = docViewModel,
                            onBack = { navController.popBackStack() },
                            onSave = { item, semesters, externalUri, externalName, walletDocId -> 
                                scope.launch {
                                    viewModel.insertAcademicItem(item)
                                    semesters.forEach { viewModel.insertSemester(it) }
                                    
                                    var finalDocId = walletDocId
                                    if (externalUri != null && externalName != null) {
                                        var externalSize = 0L
                                        context.contentResolver.query(externalUri, null, null, null, null)?.use { cursor ->
                                            if (cursor.moveToFirst()) {
                                                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                                if (sizeIndex != -1) {
                                                    externalSize = cursor.getLong(sizeIndex)
                                                }
                                            }
                                        }
                                        val docFileToSave = com.scholarvault.data.model.DocumentFile(
                                            name = externalName,
                                            isFolder = false,
                                            parentFolderId = null,
                                            filePath = "",
                                            extension = externalName.substringAfterLast(".", ""),
                                            sizeBytes = externalSize,
                                            isEncrypted = true,
                                            tags = listOf("wallet")
                                        )
                                        val newDocId = docViewModel.insertAttachmentFile(context, docFileToSave, externalUri)
                                        if (newDocId != -1L) {
                                            finalDocId = newDocId.toInt()
                                        }
                                    }
                                    
                                    if (finalDocId != null) {
                                        viewModel.insertAcademicDocumentLink(
                                            com.scholarvault.data.model.AcademicDocumentLink(
                                                academicItemId = item.id,
                                                walletDocumentId = finalDocId.toString(),
                                                linkLabel = "Attached Document"
                                            )
                                        )
                                    }
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                    composable(Screen.AcademicDetail.route) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        AcademicDetailScreen(
                            itemId = id, 
                            viewModel = viewModel, 
                            docViewModel = docViewModel,
                            onNavigateToEdit = { editId ->
                                navController.navigate(Screen.EditAcademicItem.createRoute(editId))
                            },
                            onNavigateToViewer = { route -> navController.navigate(route) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.EditAcademicItem.route) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        val courseState by viewModel.getCourseWithSemesters(id).collectAsState()
                        val item = courseState?.course
                        val semesters = courseState?.semesters ?: emptyList()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        if (item != null) {
                            com.scholarvault.ui.components.EditAcademicItemScreen(
                                item = item,
                                semesters = semesters,
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() },
                                onSave = { updatedItem, updatedSemesters, extUri, extName, existingDocId ->
                                    scope.launch {
                                        viewModel.insertAcademicItem(updatedItem)
                                        // Simple sync: delete old and insert new or rely on Room to handle if IDs match.
                                        // For safety with duration changes, clear first.
                                        viewModel.deleteSemestersForCourse(updatedItem.id)
                                        updatedSemesters.forEach { viewModel.insertSemester(it) }
                                        
                                        var finalDocId = existingDocId
                                        if (extUri != null && extName != null) {
                                            // Handle attachment update logic if needed
                                            val docFileToSave = com.scholarvault.data.model.DocumentFile(
                                                name = extName,
                                                isFolder = false,
                                                parentFolderId = null,
                                                filePath = "",
                                                extension = extName.substringAfterLast(".", ""),
                                                sizeBytes = 0, // Should measure
                                                isEncrypted = true,
                                                tags = listOf("wallet")
                                            )
                                            val newDocId = docViewModel.insertAttachmentFile(context, docFileToSave, extUri)
                                            if (newDocId != -1L) finalDocId = newDocId.toInt()
                                        }
                                        
                                        if (finalDocId != null) {
                                            viewModel.insertAcademicDocumentLink(
                                                com.scholarvault.data.model.AcademicDocumentLink(
                                                    academicItemId = updatedItem.id,
                                                    walletDocumentId = finalDocId.toString(),
                                                    linkLabel = "Updated Document"
                                                )
                                            )
                                        }
                                        navController.popBackStack()
                                    }
                                }
                            )
                        } else {
                            // loading or error state, fallback
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    composable(Screen.Documents.route) {
                        DocumentsScreen(
                            docViewModel = docViewModel,
                            onOpenDrawer = { 
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            },
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }
                    composable(Screen.Trash.route) {
                        TrashScreen(
                            docViewModel = docViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.ViewWallet.route) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val securityManager = remember { com.scholarvault.security.WalletSecurityManager(context) }
                        com.scholarvault.security.WalletAuthenticationWrapper(
                            securityManager = securityManager,
                            onUnlockSuccess = {},
                            onCancel = { navController.popBackStack() }
                        ) {
                            ViewWalletScreen(
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToCategory = { catId -> navController.navigate("view_wallet_category/$catId") },
                                onNavigateToCard = { cardId -> navController.navigate(Screen.WalletCardDetail.createRoute(cardId)) }
                            )
                        }
                    }
                    composable("view_wallet_category/{categoryId}") { backStackEntry ->
                        val catIdStr = backStackEntry.arguments?.getString("categoryId") ?: "-1"
                        val categoryId = catIdStr.toIntOrNull() ?: -1
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val securityManager = remember { com.scholarvault.security.WalletSecurityManager(context) }
                        com.scholarvault.security.WalletAuthenticationWrapper(
                            securityManager = securityManager,
                            onUnlockSuccess = {},
                            onCancel = { navController.popBackStack() }
                        ) {
                            WalletCategoryScreen(
                                categoryId = categoryId,
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToCard = { cardId -> navController.navigate(Screen.WalletCardDetail.createRoute(cardId)) },
                                onAddCard = { catId -> navController.navigate(Screen.EditWalletCard.createRoute(catId, -1)) }
                            )
                        }
                    }
                    composable(Screen.WalletCardDetail.route) { backStackEntry ->
                        val cardIdStr = backStackEntry.arguments?.getString("cardId") ?: "-1"
                        val cardId = cardIdStr.toIntOrNull() ?: -1
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val securityManager = remember { com.scholarvault.security.WalletSecurityManager(context) }
                        com.scholarvault.security.WalletAuthenticationWrapper(
                            securityManager = securityManager,
                            onUnlockSuccess = {},
                            onCancel = { navController.popBackStack() }
                        ) {
                            WalletCardDetailScreen(
                                cardId = cardId,
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() },
                                onEdit = { cId, catId -> navController.navigate(Screen.EditWalletCard.createRoute(catId, cId)) },
                                onNavigateToViewer = { route -> navController.navigate(route) }
                            )
                        }
                    }
                    composable(Screen.EditWalletCard.route) { backStackEntry ->
                        val catIdStr = backStackEntry.arguments?.getString("categoryId") ?: "-1"
                        val cardIdStr = backStackEntry.arguments?.getString("cardId") ?: "-1"
                        val categoryId = catIdStr.toIntOrNull() ?: -1
                        val cardId = cardIdStr.toIntOrNull() ?: -1
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val securityManager = remember { com.scholarvault.security.WalletSecurityManager(context) }
                        com.scholarvault.security.WalletAuthenticationWrapper(
                            securityManager = securityManager,
                            onUnlockSuccess = {},
                            onCancel = { navController.popBackStack() }
                        ) {
                            EditWalletCardScreen(
                                categoryId = categoryId,
                                cardId = cardId,
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable(Screen.Viewer.route) { backStackEntry ->
                        val fileType = backStackEntry.arguments?.getString("fileType") ?: "text"
                        val filePathEnc = backStackEntry.arguments?.getString("filePath") ?: "empty"
                        val fileNameEnc = backStackEntry.arguments?.getString("fileName") ?: "Unknown"
                        
                        val filePath = try {
                            String(android.util.Base64.decode(filePathEnc, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
                        } catch (e: Exception) { "empty" }
                        
                        val fileName = try {
                            String(android.util.Base64.decode(fileNameEnc, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
                        } catch (e: Exception) { "Unknown" }

                        ViewerScreen(
                            fileType = fileType,
                            filePath = if (filePath == "empty") "" else filePath,
                            fileName = fileName,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToWalletSecurity = { navController.navigate("wallet_security") },
                            onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                            onNavigateToAbout = { navController.navigate(Screen.About.route) }
                        )
                    }
                    composable("wallet_security") {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val securityManager = remember { com.scholarvault.security.WalletSecurityManager(context) }
                        com.scholarvault.security.WalletAuthenticationWrapper(
                            securityManager = securityManager,
                            onUnlockSuccess = {},
                            onCancel = { navController.popBackStack() }
                        ) {
                            com.scholarvault.ui.WalletSecurityScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable(Screen.PasswordGenerator.route) {
                        com.scholarvault.ui.tools.PasswordGeneratorScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.FocusTimer.route) {
                        FocusTimerScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.CgpaCalculator.route) {
                        CgpaCalculatorScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.UnitConverter.route) {
                        com.scholarvault.ui.tools.UnitConverterScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.CitationGenerator.route) {
                        com.scholarvault.ui.tools.CitationGeneratorScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.QuickNoteFull.route) {
                        com.scholarvault.ui.tools.quick_note.QuickNoteFullScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.FlashcardsDecks.route) {
                        com.scholarvault.ui.tools.flashcards.FlashcardsDecksScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDeck = { navController.navigate(Screen.FlashcardDeckDetail.createRoute(it)) }
                        )
                    }
                    composable(
                        route = Screen.FlashcardDeckDetail.route,
                        arguments = listOf(navArgument("deckId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val deckId = backStackEntry.arguments?.getString("deckId") ?: ""
                        com.scholarvault.ui.tools.flashcards.FlashcardDeckDetailScreen(
                            deckId = deckId,
                            onBack = { navController.popBackStack() },
                            onNavigateToStudy = { navController.navigate(Screen.FlashcardStudy.createRoute(it)) }
                        )
                    }
                    composable(
                        route = Screen.FlashcardStudy.route,
                        arguments = listOf(navArgument("deckId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val deckId = backStackEntry.arguments?.getString("deckId") ?: ""
                        com.scholarvault.ui.tools.flashcards.FlashcardStudyScreen(
                            deckId = deckId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Tasks.route) {
                        TasksScreen(
                            onBack = { navController.popBackStack() },
                            onNavigate = { route -> navController.navigate(route) },
                            viewModel = dashboardViewModel
                        )
                    }
                    composable(Screen.Reminders.route) {
                        RemindersScreen(
                            onBack = { navController.popBackStack() },
                            onNavigate = { route -> navController.navigate(route) },
                            viewModel = dashboardViewModel
                        )
                    }
                    composable(Screen.Tools.route) {
                        ToolsScreen(
                            onOpenDrawer = { 
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            },
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }
                    composable("calculator") {
                        com.scholarvault.ui.tools.CalculatorScreen(onBack = { navController.popBackStack() })
                    }
                    composable("sound_recorder") {
                        com.scholarvault.ui.tools.SoundRecorderScreen(
                            docViewModel = docViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToViewer = { route -> navController.navigate(route) },
                            onNavigateToRecentRecordings = { navController.navigate("recent_recordings") }
                        )
                    }
                    composable("recent_recordings") {
                        com.scholarvault.ui.tools.RecentRecordingsScreen(
                            docViewModel = docViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToViewer = { route -> navController.navigate(route) }
                        )
                    }
                    composable(Screen.PrePrintingSetup.route) {
                        com.scholarvault.ui.tools.PrePrintingSetupScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.ImageResizer.route) {
                        com.scholarvault.ui.tools.ImageResizerScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.PdfColorInverter.route) {
                        com.scholarvault.ui.tools.pdf_inverter.PdfColorInverterScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.PdfNUp.route) {
                        com.scholarvault.ui.tools.pdf_nup.NUpScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToHistory = { navController.navigate("pdf_nup_history") }
                        )
                    }
                    composable("pdf_nup_history") {
                        com.scholarvault.ui.tools.pdf_nup.NUpHistoryScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Profile.route) {
                        com.scholarvault.ui.ProfileScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.About.route) {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.CompressImage.route) {
                        com.scholarvault.ui.tools.CompressImageScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.ImageToPdf.route) {
                        com.scholarvault.ui.tools.ImageToPdfScreen(
                            onBack = { navController.popBackStack() },
                            docViewModel = docViewModel
                        )
                    }
                    composable(Screen.RecentGeneratedMedia.route) {
                        com.scholarvault.ui.tools.RecentGeneratedMediaScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Transactions.route) {
                        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                val app = context.applicationContext as com.scholarvault.MainApplication
                                return TransactionViewModel(app.database) as T
                            }
                        }
                        val tViewModel: TransactionViewModel = viewModel(factory = factory)
                        TransactionsScreen(navController = navController, viewModel = tViewModel)
                    }
                    composable(Screen.SecureBackup.route) {
                        com.scholarvault.ui.tools.SecureBackupScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.DataExport.route) {
                        com.scholarvault.ui.tools.DataExportScreen(onBack = { navController.popBackStack() })
                    }
                    composable("document_scanner") {
                        com.scholarvault.ui.tools.scanner.ScannerRecentScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToViewer = { route -> navController.navigate(route) },
                            docViewModel = docViewModel
                        )
                    }
                    composable("document_scanner_capture") {
                        com.scholarvault.ui.tools.scanner.ScannerCaptureScreen(
                            onBack = { navController.popBackStack() },
                            docViewModel = docViewModel
                        )
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                if (currentRoute != com.scholarvault.ui.Screen.PdfColorInverter.route) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        com.scholarvault.ui.tools.pdf_inverter.GlobalProgressBanner(
                            navController = navController,
                            isBottomBarVisible = isBottomBarVisible && !isTablet
                        )
                    }
                }

                if (!isTablet) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = if (LocalThemeController.current.animationsEnabled) {
                            androidx.compose.animation.slideInVertically(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                ),
                                initialOffsetY = { it }
                            )
                        } else androidx.compose.animation.EnterTransition.None,
                        exit = if (LocalThemeController.current.animationsEnabled) {
                            androidx.compose.animation.slideOutVertically(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                ),
                                targetOffsetY = { it }
                            )
                        } else androidx.compose.animation.ExitTransition.None,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        CustomBottomNavigationBar(
                            navController = navController,
                            isFabMenuOpen = isFabMenuOpen,
                            onToggleFabMenu = { isFabMenuOpen = !isFabMenuOpen },
                            onNavigate = { isFabMenuOpen = false }
                        )
                    }
                }
                } // Close blurred Box

                androidx.compose.animation.AnimatedVisibility(
                    visible = isFabMenuOpen,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(
                        initialScale = 0.8f,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    ),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(
                        targetScale = 0.8f,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    androidx.activity.compose.BackHandler { isFabMenuOpen = false }
                    FabMenuOverlay(
                        onClose = { isFabMenuOpen = false },
                        onNavigate = { route -> 
                            if (route == "quick_note_widget") {
                                com.scholarvault.ui.tools.SharedData.isQuickNoteWidgetVisible.value = true
                            } else {
                                navController.navigate(route) 
                            }
                        },
                        onUploadClick = { 
                            navController.navigate(Screen.Documents.route)
                            scope.launch { docViewModel.triggerUploadChannel.send(Unit) }
                        },
                        paddingBottom = 88.dp,
                        isTablet = isTablet
                    )
                }

                if (!isTablet) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = if (LocalThemeController.current.animationsEnabled) {
                            androidx.compose.animation.slideInVertically(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                ),
                                initialOffsetY = { it }
                            )
                        } else androidx.compose.animation.EnterTransition.None,
                        exit = if (LocalThemeController.current.animationsEnabled) {
                            androidx.compose.animation.slideOutVertically(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                ),
                                targetOffsetY = { it }
                            )
                        } else androidx.compose.animation.ExitTransition.None,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        TopLevelFab(
                            isFabMenuOpen = isFabMenuOpen,
                            onToggleFabMenu = { isFabMenuOpen = !isFabMenuOpen }
                        )
                    }
                } else {
                    // Desktop FAB - Bottom Right
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { isFabMenuOpen = !isFabMenuOpen },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (isFabMenuOpen) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Menu",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            
            val quickNoteVisible by com.scholarvault.ui.tools.SharedData.isQuickNoteWidgetVisible.collectAsState()
            com.scholarvault.ui.tools.quick_note.QuickNoteWidgetOverlay(
                isWidgetVisible = quickNoteVisible,
                onCloseWidget = { com.scholarvault.ui.tools.SharedData.isQuickNoteWidgetVisible.value = false }
            )
            
            } // Close outer Box
        } // Close Scaffold
    } // Close screenContent lambda

    androidx.compose.runtime.CompositionLocalProvider(
        com.scholarvault.ui.components.LocalCustomNavigate provides { route -> navController.navigate(route) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isDrawerGesturesEnabled = remember(currentRoute) {
            val root = currentRoute?.substringBefore("/") ?: ""
            val blocked = setOf(
                "viewer",
                "pdf_color_inverter",
                "pdf_nup",
                "pre_printing_setup",
                "image_resizer",
                "sound_recorder",
                "focus_timer",
                "cgpa_calculator",
                "unit_converter",
                "password_generator"
            )
            root !in blocked
        }

        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize()) {
                val menuAnimationSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.Dp>(300)
                val sidebarWidth by androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (isSidebarExpanded) (if (isWideScreen) 320.dp else 280.dp) else 72.dp,
                    animationSpec = menuAnimationSpec,
                    label = "SidebarWidth"
                )
                Box(modifier = Modifier.width(sidebarWidth).fillMaxHeight()) {
                    if (isSidebarExpanded) {
                        SidebarContent(
                            onClose = { isSidebarExpanded = false },
                            onProfileClick = { showZoomedProfilePic = true },
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            profileViewModel = profileViewModel
                        )
                    } else {
                        CompactSidebar(
                            onExpand = { isSidebarExpanded = true },
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            currentRoute = currentRoute,
                            profileViewModel = profileViewModel
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    screenContent()
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = drawerContentBlock,
                gesturesEnabled = isDrawerGesturesEnabled,
                content = screenContent
            )
        }

        if (showZoomedProfilePic) {
            val profileFlow by profileViewModel.profileStream.collectAsState()
            val profileData = profileFlow?.profile

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showZoomedProfilePic = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.padding(16.dp).width(300.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Profile Photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showZoomedProfilePic = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(240.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (profileData?.profilePicUri != null) {
                                    CachedImage(
                                        uri = Uri.parse(profileData.profilePicUri),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(160.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        if (showProfileMenu) {
            val profileFlow by profileViewModel.profileStream.collectAsState()
            val profileData = profileFlow?.profile
            val firstName = profileData?.firstName?.takeIf { it.isNotBlank() } ?: "Student"
            val lastName = profileData?.lastName?.takeIf { it.isNotBlank() } ?: ""
            val email = profileData?.email?.takeIf { it.isNotBlank() } ?: ""

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { showProfileMenu = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 16.dp,
                    modifier = Modifier.width(340.dp).clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (profileData?.profilePicUri != null) {
                                    CachedImage(uri = Uri.parse(profileData.profilePicUri))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "$firstName $lastName".trim(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (email.isNotBlank()) {
                            Text(
                                email,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Menu Item 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProfileMenu = false }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Account Settings", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        // Menu Item 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    showProfileMenu = false
                                    navController.navigate(Screen.About.route)
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("About App", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        // Developer Link
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    showProfileMenu = false
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://github.com/suvadippatra")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Developer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Developer: @suvadippatra", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { showProfileMenu = false },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Close", modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Draggable, Resizable Premium PiP Calculator Overlay
        com.scholarvault.ui.tools.FloatingCalculatorOverlay(
            onFullscreenRestore = {
                navController.navigate("calculator")
            }
        )
    }
    }
}

@Composable
fun CustomBottomNavigationBar(navController: NavHostController, isFabMenuOpen: Boolean, onToggleFabMenu: () -> Unit, onNavigate: () -> Unit) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = LocalThemeController.current.isDarkTheme
    
    var rotationTarget by remember { mutableStateOf(0f) }
    var previousIsFabMenuOpen by remember { mutableStateOf(isFabMenuOpen) }
    
    LaunchedEffect(isFabMenuOpen) {
        if (isFabMenuOpen != previousIsFabMenuOpen) {
            rotationTarget += 45f
            previousIsFabMenuOpen = isFabMenuOpen
        }
    }

    val navBgColor = if (isDark) Color(0xFF2C2C30) else Color(0xFFDCD5E4)
    val fabIconColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF4A148C)

    val density = androidx.compose.ui.platform.LocalDensity.current
    val fabRadius = 27f * density.density
    val gap = 6f * density.density
    val cutoutRadius = fabRadius + gap
    val fabCenterY = 0f // FAB center relative to navBar top

    val bottomNavShape = remember(density) {
        object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density
            ): androidx.compose.ui.graphics.Outline {
                if (size.width <= 0f || size.height <= 0f) {
                    return androidx.compose.ui.graphics.Outline.Generic(androidx.compose.ui.graphics.Path())
                }
                
                val path = androidx.compose.ui.graphics.Path().apply {
                    val cornerRadius = kotlin.math.min(24f * density.density, size.height / 2f)
                    val center = size.width / 2f
                    
                    moveTo(0f, cornerRadius)
                    quadraticTo(0f, 0f, cornerRadius, 0f)
                    
                    val dx = if (cutoutRadius > fabCenterY && cutoutRadius > 0f) {
                         val inner = (cutoutRadius * cutoutRadius) - (fabCenterY * fabCenterY)
                         if (inner > 0f) kotlin.math.sqrt(inner) else 0f
                    } else {
                         0f
                    }
                    
                    // Safe guard for layout width
                    if (center - dx > cornerRadius) {
                        lineTo(center - dx, 0f)
                    } else {
                        lineTo(cornerRadius, 0f) // fallback to simple shape
                    }
                    
                    if (cutoutRadius > 0f) {
                        val angleRad = kotlin.math.asin((fabCenterY / cutoutRadius).toDouble().coerceIn(-1.0, 1.0))
                        val angleDeg = Math.toDegrees(angleRad).toFloat()
                        val startAngle = 180f + angleDeg
                        val sweepAngle = -(180f + 2f * angleDeg)
                        
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                left = center - cutoutRadius,
                                top = fabCenterY - cutoutRadius,
                                right = center + cutoutRadius,
                                bottom = fabCenterY + cutoutRadius
                            ),
                            startAngleDegrees = startAngle,
                            sweepAngleDegrees = sweepAngle,
                            forceMoveTo = false
                        )
                    }
                    
                    if (center + dx < size.width - cornerRadius) {
                        lineTo(center + dx, 0f)
                    } else {
                        lineTo(size.width - cornerRadius, 0f)
                    }
                    
                    lineTo(size.width - cornerRadius, 0f)
                    quadraticTo(size.width, 0f, size.width, cornerRadius)
                    
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                return androidx.compose.ui.graphics.Outline.Generic(path)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // The background navigation bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(navBgColor, bottomNavShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NavBarItem(Screen.Dashboard, currentRoute, navController, onNavigate)
                        NavBarItem(Screen.Academics, currentRoute, navController, onNavigate)
                    }
                    
                    Spacer(modifier = Modifier.width(60.dp)) // Space for FAB
    
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NavBarItem(Screen.Documents, currentRoute, navController, onNavigate)
                        NavBarItem(Screen.Tools, currentRoute, navController, onNavigate)
                    }
                }
            }
        }
    }
}

@Composable
fun TopLevelFab(isFabMenuOpen: Boolean, onToggleFabMenu: () -> Unit) {
    var rotationTarget by remember { mutableStateOf(0f) }
    var previousIsFabMenuOpen by remember { mutableStateOf(isFabMenuOpen) }
    
    LaunchedEffect(isFabMenuOpen) {
        if (isFabMenuOpen != previousIsFabMenuOpen) {
            rotationTarget += 45f
            previousIsFabMenuOpen = isFabMenuOpen
        }
    }

    Box(
        modifier = Modifier
            .padding(bottom = 80.dp - 19.dp)
            .size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        val rotation by androidx.compose.animation.core.animateFloatAsState(
            targetValue = rotationTarget, 
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
        )
        FloatingActionButton(
            onClick = onToggleFabMenu,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxSize(),
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Menu",
                modifier = Modifier.size(32.dp).rotate(rotation)
            )
        }
    }
}

@Composable
fun NavBarItem(screen: Screen, currentRoute: String?, navController: NavHostController, onNavigate: () -> Unit) {
    val selected = currentRoute == screen.route
    val isDark = LocalThemeController.current.isDarkTheme
    val selectedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF311B92)
    val unselectedColor = if (isDark) Color(0xFFAAAAAA) else Color.DarkGray
    val color = if (selected) selectedColor else unselectedColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = androidx.compose.ui.Modifier
            .clickable {
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                onNavigate()
            }
            .padding(8.dp)
    ) {
        Icon(screen.icon, contentDescription = screen.title, tint = color)
        Text(screen.title, fontSize = 10.sp, color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ToolSelectionOverlay(
    onClose: () -> Unit,
    onToolsSelected: (List<com.scholarvault.ui.ToolItem>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val allTools = com.scholarvault.ui.AllToolsList
    val filteredTools = remember(searchQuery) {
        val baseTools = allTools.filter { it.route != "quick_note_full" }
        if (searchQuery.isEmpty()) baseTools else baseTools.filter { it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }
    var selectedTools by remember { mutableStateOf(emptySet<com.scholarvault.ui.ToolItem>()) }
    
    val isDark = LocalThemeController.current.isDarkTheme
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (isDark) Color.White else Color.Black)
                }
                
                RoundedCornerShape(24.dp).let { shape ->
                    androidx.compose.material3.TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        placeholder = { Text("Search tools...") },
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = shape
                    )
                }
                
                IconButton(onClick = { onToolsSelected(selectedTools.toList()) }) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm", tint = if (isDark) Color.White else Color.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTools.size) { i ->
                    val tool = filteredTools[i]
                    val isSelected = selectedTools.contains(tool)
                    
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isDark) Color(0xFF2C2C2E) else Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            selectedTools = if (isSelected) selectedTools - tool else selectedTools + tool
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(tool.icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tool.title, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else if (isDark) Color.White else Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FabMenuOverlay(onClose: () -> Unit, onNavigate: (String) -> Unit, onUploadClick: () -> Unit = {}, paddingBottom: Dp, isTablet: Boolean = false) {
    val context = LocalContext.current
    val isDark = LocalThemeController.current.isDarkTheme
    val textColor = if (isDark) Color.White else Color.Black
    val overlayBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFDCDCDC)
    val itemBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val bgAlpha = Color.Transparent

    var isEditingLayout by remember { mutableStateOf(false) }
    var isToolSelectionMode by remember { mutableStateOf(false) }

    val key = "fab_menu_order"
    val defaultItemsList = remember {
        listOf(
            "Upload" to (Icons.Default.CloudUpload to { onUploadClick() }),
            "Wallet" to (Icons.Default.AccountBalanceWallet to { onNavigate(Screen.ViewWallet.route) }),
            "Transaction" to (Icons.Default.CurrencyExchange to { onNavigate(Screen.Transactions.route) }),
            "Notes" to (Icons.Default.Description to { onNavigate(Screen.NotesList.route) }),
            "Capture" to (Icons.Default.DocumentScanner to { onNavigate("document_scanner") }),
            "Record" to (Icons.Default.Mic to { onNavigate("sound_recorder") })
        )
    }

    var itemsOrder by remember {
        val vault = com.scholarvault.util.SecurityVault(context)
        val orderStr = vault.getSecureString(key) ?: "Upload,Wallet,Transaction,Notes,Capture,Record"
        val list = mutableListOf<Pair<String, Pair<ImageVector, () -> Unit>>>()
        val saved = orderStr.split(",")
        saved.forEach { name ->
            val found = defaultItemsList.find { it.first == name }
            if (found != null) {
                list.add(found)
            } else {
                val tool = com.scholarvault.ui.AllToolsList.find { it.title == name }
                if (tool != null) {
                    list.add(tool.title to (tool.icon to { onNavigate(tool.route) }))
                }
            }
        }
        // Fallback for missing/corrupted
        defaultItemsList.forEach { def ->
            if (list.none { it.first == def.first }) {
                list.add(def)
            }
        }
        mutableStateOf(list)
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Save ordering helper
    val saveItemsOrder: (List<Pair<String, Pair<ImageVector, () -> Unit>>>) -> Unit = { list ->
        val names = list.map { it.first }.joinToString(",")
        val vault = com.scholarvault.util.SecurityVault(context)
        vault.saveSecureString(key, names)
    }

    if (isToolSelectionMode) {
        ToolSelectionOverlay(
            onClose = { isToolSelectionMode = false },
            onToolsSelected = { newTools ->
                val updated = itemsOrder.toMutableList()
                newTools.forEach { t ->
                    if (updated.none { it.first == t.title }) {
                        updated.add(t.title to (t.icon to { onNavigate(t.route) }))
                    }
                }
                itemsOrder = updated
                saveItemsOrder(updated)
                isToolSelectionMode = false
            }
        )
        return
    }

    // Capture coordinates in overlay coordinate space
    var overlayContainerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val itemCoords = remember { mutableMapOf<Int, Rect>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgAlpha)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { if (!isEditingLayout) onClose() }
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = paddingBottom + 40.dp) // Lift above FAB
                .padding(horizontal = 16.dp)
                .wrapContentHeight() // Make it shrink to content
                .background(overlayBg, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .onGloballyPositioned { overlayContainerCoordinates = it }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        isToolSelectionMode = true
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = itemBg, contentColor = textColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(0.dp)
                ) { Text("Add Tool", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor) }
                Button(
                    onClick = {
                        isEditingLayout = !isEditingLayout
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditingLayout) MaterialTheme.colorScheme.primary else itemBg,
                        contentColor = if (isEditingLayout) MaterialTheme.colorScheme.onPrimary else textColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(0.dp)
                ) { 
                    Text(
                        if (isEditingLayout) "Done" else "Edit Layout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isEditingLayout) MaterialTheme.colorScheme.onPrimary else textColor
                    ) 
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isEditingLayout) {
                Text(
                    text = "Press & drag any item to rearrange layout",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val displayItems = itemsOrder
                
                val columns = if (isTablet) 5 else 3
                val chunked = displayItems.chunked(columns)
                chunked.forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEachIndexed { colIndex, item ->
                            val flatIndex = rowIndex * columns + colIndex
                            val isItemDragged = draggedIndex == flatIndex

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coords ->
                                        val parent = overlayContainerCoordinates
                                        if (parent != null && parent.isAttached && coords.isAttached) {
                                            val localOffset = parent.localPositionOf(coords, Offset.Zero)
                                            val size = coords.size
                                            itemCoords[flatIndex] = Rect(localOffset, Size(size.width.toFloat(), size.height.toFloat()))
                                        }
                                    }
                                    .graphicsLayer {
                                        if (isItemDragged) {
                                            translationX = dragOffset.x
                                            translationY = dragOffset.y
                                            shadowElevation = 8.dp.toPx()
                                            scaleX = 1.1f
                                            scaleY = 1.1f
                                            alpha = 0.9f
                                        }
                                    }
                                    .pointerInput(isEditingLayout, flatIndex) {
                                        if (!isEditingLayout) return@pointerInput
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedIndex = flatIndex
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount

                                                // Find if our dragged center is over another item
                                                val currentRect = itemCoords[flatIndex]
                                                if (currentRect != null) {
                                                    val currentCenter = currentRect.center + dragOffset
                                                    var targetIdx: Int? = null
                                                    for ((idx, rect) in itemCoords) {
                                                        if (idx != flatIndex && rect.contains(currentCenter)) {
                                                            targetIdx = idx
                                                            break
                                                        }
                                                    }
                                                    if (targetIdx != null && targetIdx < displayItems.size) {
                                                        val targetTitle = displayItems[targetIdx].first
                                                        val draggedTitle = item.first
                                                        
                                                        val newList = itemsOrder.toMutableList()
                                                        val actualDragIdx = newList.indexOfFirst { it.first == draggedTitle }
                                                        val actualTargetIdx = newList.indexOfFirst { it.first == targetTitle }
                                                        
                                                        if (actualDragIdx != -1 && actualTargetIdx != -1) {
                                                            val temp = newList[actualDragIdx]
                                                            newList[actualDragIdx] = newList[actualTargetIdx]
                                                            newList[actualTargetIdx] = temp
                                                            itemsOrder = newList
                                                            saveItemsOrder(newList)
                                                            draggedIndex = targetIdx
                                                            dragOffset = Offset.Zero
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggedIndex = null
                                                dragOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                dragOffset = Offset.Zero
                                            }
                                        )
                                    }
                            ) {
                                FabMenuItem(
                                    title = item.first,
                                    icon = item.second.first,
                                    bg = itemBg,
                                    tc = textColor,
                                    isEditingLayout = isEditingLayout,
                                    isDragged = isItemDragged,
                                    onRemove = {
                                        val newList = itemsOrder.toMutableList()
                                        newList.removeAll { it.first == item.first }
                                        itemsOrder = newList
                                        saveItemsOrder(newList)
                                    },
                                    onClick = {
                                        if (!isEditingLayout) {
                                            onClose()
                                            item.second.second()
                                        }
                                    }
                                )
                            }
                        }
                        if (rowItems.size < columns) {
                            for (k in 0 until (columns - rowItems.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FabMenuItem(
    title: String,
    icon: ImageVector,
    bg: Color,
    tc: Color,
    modifier: Modifier = Modifier,
    isEditingLayout: Boolean = false,
    isDragged: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val isDark = LocalThemeController.current.isDarkTheme
    val borderStroke = if (isEditingLayout) {
        BorderStroke(1.dp, if (isDragged) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f))
    } else {
        null
    }
    
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditingLayout) { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp).align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = tc)
                    if (isEditingLayout) {
                        Box(
                            modifier = Modifier
                                .offset(x = 12.dp, y = (-12).dp)
                                .size(14.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = tc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isEditingLayout && onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = tc.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SidebarContent(onClose: () -> Unit, onProfileClick: () -> Unit, onNavigate: (String) -> Unit, profileViewModel: ProfileViewModel = viewModel()) {
    val isDark = LocalThemeController.current.isDarkTheme
    val textColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.White else Color.Black
    val sbBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD1D1D1)
    
    val profileFlow by profileViewModel.profileStream.collectAsState()
    val profile = profileFlow?.profile
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(sbBg)
            .statusBarsPadding()
            .padding(vertical = 16.dp)
    ) {
        // --- PINNED HEADER ---
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(bottomEnd = 16.dp, topEnd = 16.dp))
                        .background(if (isDark) Color(0xFF4A4A4A) else Color(0xFFD0C3B5))
                        .padding(12.dp)
                        .clickable { onClose() }
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Close", tint = iconColor, modifier = Modifier.size(28.dp))
                }
                val themeController = LocalThemeController.current
                IconButton(
                    onClick = { 
                        themeController.setThemeMode(if (isDark) "light" else "dark")
                    },
                    modifier = Modifier
                        .background(if (isDark) Color(0xFF333333) else Color.White, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        if (isDark) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                        contentDescription = "Theme",
                        tint = if(isDark) Color.Yellow else Color.DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(if (profile?.profilePicUri != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .clip(CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profile?.profilePicUri != null) {
                        CachedImage(uri = Uri.parse(profile.profilePicUri))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val fullName = listOf(profile?.firstName, profile?.middleName, profile?.lastName).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")
                Text(if(fullName.isNotBlank()) fullName else "Student", fontSize = 22.sp, fontWeight = FontWeight.Normal, color = textColor)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- SCROLLABLE LIST ---
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DrawerItem("View Profile") { onNavigate(Screen.Profile.route) }
            DrawerItem("Dashboard") { onNavigate(Screen.Dashboard.route) }
            DrawerItem("Academics") { onNavigate(Screen.Academics.route) }
            DrawerItem("Documents") { onNavigate(Screen.Documents.route) }
            DrawerItem("Tools") { onNavigate(Screen.Tools.route) }
            DrawerItem("Recent Generated Media") { onNavigate(Screen.RecentGeneratedMedia.route) }
            DrawerItem("About App") { onNavigate(Screen.About.route) }
            DrawerItem("Exam Notifications") { }

            Spacer(modifier = Modifier.height(24.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            DrawerItem("Developer: @suvadippatra") { 
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/suvadippatra")
                    )
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }
            DrawerItem("Settings") { onNavigate(Screen.Settings.route) }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DrawerItem(title: String, onClick: () -> Unit) {
    val isDark = LocalThemeController.current.isDarkTheme
    val textColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE0E0E0)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}

@Composable
fun CompactSidebar(
    onExpand: () -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String?,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val isDark = LocalThemeController.current.isDarkTheme
    val sbBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD1D1D1)
    val iconColor = if (isDark) Color.White else Color.Black
    val profileFlow by profileViewModel.profileStream.collectAsState()
    val profile = profileFlow?.profile

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(72.dp)
            .background(sbBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                // Expand Menu Button
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF4A4A4A) else Color(0xFFD0C3B5))
                        .size(44.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Expand Menu", tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Short Rounded profile Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (profile?.profilePicUri != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable { onNavigate(Screen.Profile.route) },
                contentAlignment = Alignment.Center
            ) {
                if (profile?.profilePicUri != null) {
                    CachedImage(uri = Uri.parse(profile.profilePicUri))
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom compact rail icons corresponding to the main pages
            val items = listOf(
                Screen.Dashboard,
                Screen.Academics,
                Screen.Documents,
                Screen.Tools
            )
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                IconButton(
                    onClick = { onNavigate(screen.route) },
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .background(
                            if (isSelected) (if (isDark) Color(0xFF4A4A4A) else Color(0xFFD0C3B5)) else Color.Transparent,
                            androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .size(44.dp)
                ) {
                    Icon(screen.icon, contentDescription = screen.title, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Bottom section of compact sidebar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Theme Toggle
            val themeController = LocalThemeController.current
            IconButton(
                onClick = { themeController.setThemeMode(if (isDark) "light" else "dark") },
                modifier = Modifier
                    .background(if (isDark) Color(0xFF333333) else Color.White, androidx.compose.foundation.shape.CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    if (isDark) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                    contentDescription = "Theme",
                    tint = if(isDark) Color.Yellow else Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Settings Icon
            val isSettingsSelected = currentRoute == Screen.Settings.route
            IconButton(
                onClick = { onNavigate(Screen.Settings.route) },
                modifier = Modifier
                    .background(
                        if (isSettingsSelected) (if (isDark) Color(0xFF4A4A4A) else Color(0xFFD0C3B5)) else Color.Transparent,
                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = iconColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}




