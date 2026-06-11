package com.scholarvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.scholarvault.ui.theme.LocalThemeController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val isDark = LocalThemeController.current.isDarkTheme
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FA)
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color.LightGray else Color.DarkGray

    Scaffold(
        topBar = {
            com.scholarvault.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "About App",
                showProfileIcon = false,
                showSearchBar = false
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Identity
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("ScholarVault", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("Secure Offline-First Academic Vault", fontSize = 14.sp, color = subTextColor)
            }

            Text(
                "ScholarVault is designed to simplify academic and professional lives by centralizing research documents, academic courses, credentials, and reminders in one highly secure, completely offline-first environment.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // --- SECURITY SECTION ---
            AboutSection(
                icon = Icons.Default.Security,
                title = "Why ScholarVault is Secure",
                description = "• 100% Offline-First Architecture: Your documents, academic courses, and secret wallet cards never leave your physical device. The application does not use external tracking services, analytic telemetry, or remote servers.\n\n" +
                              "• Hardware Keystore Isolation: Sensitive wallet keys, custom credentials, and secure certificates are guarded using military-grade AES-256 encryption. Encryption keys are securely bound to the Android Hardware Keystore, providing optimal resilience against memory dumps.\n\n" +
                              "• Room Secure Sandboxing: System schedules and schema registries rest in a highly-optimized local SQLite database managed by Room. The database is entirely sandboxed inside standard Android private storage limits.",
                cardColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.5f),
                textColor = textColor,
                subTextColor = subTextColor,
                iconTint = MaterialTheme.colorScheme.secondary
            )

            Text("Core Subsystems & Features", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)

            AboutSection(
                icon = Icons.Default.CurrencyExchange,
                title = "Local Transactions & Utility Billing",
                description = "Manage finances directly within ScholarVault:\n" +
                              "• Multi-Account Ledger: Track total balances, income, and expenses across an unlimited number of accounts securely.\n" +
                              "• Electricity & Utility Bill Tracker: Specialized calculators for utilities, such as reading input boxes and tiered unit charge formulas computed dynamically.\n" +
                              "• Link Receipts locally: Scan, capture or connect files from ScholarVault directly to the transaction summary pane.",
                cardColor = cardColor, textColor = textColor, subTextColor = subTextColor, iconTint = MaterialTheme.colorScheme.primary
            )

            AboutSection(
                icon = Icons.Default.Description,
                title = "High-Performance PDF Viewer V2",
                description = "Fully rebuilt PDF document subsystem featuring:\n" +
                              "• Multiple Reading Modes: Choose between Standard Vertical Scrolling, Horizontal Paging, or full 3D Page Flip animation mode configured with real-world underdamped physics easing formulas.\n" +
                              "• Instant Document Query & Search: Match matching strings instantly within multi-page documents with contextual visual highlight overlays and match backstack counters.\n" +
                              "• Document Annotation & Notes: Attach interactive custom rich-text notes and research cards directly linked to specific PDFs, saved locally and persistent across reading sessions.\n" +
                              "• Multi-Scale Zoom State: Enhanced dual-pointer zoom translation mechanics with matrix stabilization to ensure lightning-fast pinch-to-zoom without rendering lag.",
                cardColor = cardColor, textColor = textColor, subTextColor = subTextColor, iconTint = MaterialTheme.colorScheme.primary
            )

            AboutSection(
                icon = Icons.Default.Folder,
                title = "Nested Document Organization",
                description = "A powerful, local file repository system:\n" +
                              "• Multi-Level Directories: Organize files into hierarchical folders for seamless course organization.\n" +
                              "• File Import & Exporting: Share documents externally or securely import from system providers like Google Drive into local sandboxed storage.\n" +
                              "• Quick Filters: Tag search elements for academic papers, transcripts, or notes to fetch entries instantly.\n" +
                              "• Inline Metadata Tracking: Automatically parse size, extension, checksum values, and date stamps for optimal disk utilization.",
                cardColor = cardColor, textColor = textColor, subTextColor = subTextColor, iconTint = MaterialTheme.colorScheme.primary
            )

            AboutSection(
                icon = Icons.Default.Wallet,
                title = "Privacy-Focused Credentials Wallet",
                description = "Keep crucial IDs easily accessible, completely protected:\n" +
                              "• Specialized Template Schemes: Add student cards, professional certificates, driver's licenses, and passports with customized schemas.\n" +
                              "• Biometric Lock Screen: Option to overlay PIN codes or native biometric dialogs prior to displaying active wallet card details.\n" +
                              "• Customizable Fields: Attach unlimited custom textual or numeric key-value properties to individual cards.\n" +
                              "• Document Association: Safely link scanned attachment images or PDF files to their respective wallet index cards.",
                cardColor = cardColor, textColor = textColor, subTextColor = subTextColor, iconTint = MaterialTheme.colorScheme.primary
            )

            AboutSection(
                icon = Icons.Default.School,
                title = "Academics timeline",
                description = "Track your complete professional or educational journey:\n" +
                              "• Chronological Timelines: Organize entries by semester or academic periods with specific, responsive visual nodes.\n" +
                              "• Course & CGPA Estimator: Log courses, credits, grades, and compute cumulative grade points dynamically with a dedicated offline predictor.\n" +
                              "• Certificate Binding: Connect proof-of-completion files directly to specific timeline courses.",
                cardColor = cardColor, textColor = textColor, subTextColor = subTextColor, iconTint = MaterialTheme.colorScheme.primary
            )

            AboutSection(
                icon = Icons.Default.Dashboard,
                title = "Dashboard & Scheduling",
                description = "Stay updated on upcoming exams or deliverables:\n" +
                              "• Centralized Feed: Displays quick stats summaries, latest secure cache sizes, and dynamic motivational quote feeds.\n" +
                              "• Native Android Alarms: Set reminders that integrate with system-level alarm managers to fire notifications even if the app operates in standby mode.",
                cardColor = cardColor, textColor = textColor, subTextColor = subTextColor, iconTint = MaterialTheme.colorScheme.primary
            )

            Text("Built-In Utilities", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)

            AboutSection(
                icon = Icons.Default.Build,
                title = "Academic Toolbox & Helpers",
                description = "ScholarVault comes with extensive standalone utilities completely offline:\n" +
                              "• Note Taking & Flashcards: Powerful markdown composer for drafting ideas and a spaced-repetition flashcard system for active recall.\n" +
                              "• Focus Timer: Pomodoro-styled timer module specifically crafted to maintain focus streaks.\n" +
                              "• Citations & Password Generation: Instantly create APA/MLA compliant links or produce cryptographically secure randomized passwords.\n" +
                              "• Image Resizer & DPI Converter: Crop, scale, resize, and prepare document signature PNGs with a custom background removal threshold engine.\n" +
                              "• Image to PDF Creator: Snap scans or select gallery bitmaps and package them instantly into structured, print-ready PDF attachments.\n" +
                              "• PDF Visual Dark Inverter: Swap black text on white backgrounds into eyesafe dark mode readers on the fly.\n" +
                              "• N-Up PPT-to-PDF Layout: Consolidate 2, 4, or 6 slides onto a single physical page, generating an optimized N-up document to save printing costs.\n" +
                              "• Calculators & Converters: CGPA scaling calculators, unit length/weight/temperature converter, scientific computation.\n" +
                              "• Secure Backup & Data Export: Save down your complete vaults and physical data files as an encrypted archive for safe keeping.",
                cardColor = cardColor, textColor = textColor, subTextColor = subTextColor, iconTint = MaterialTheme.colorScheme.primary
            )

            Divider(color = subTextColor.copy(alpha = 0.2f))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Developed with ❤️ by @suvadippatra", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Version 1.0.0 (Build 1)", fontSize = 12.sp, color = subTextColor)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AboutSection(
    icon: ImageVector, 
    title: String, 
    description: String, 
    cardColor: Color, 
    textColor: Color, 
    subTextColor: Color, 
    iconTint: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                Spacer(modifier = Modifier.height(6.dp))
                Text(description, fontSize = 14.sp, color = subTextColor, lineHeight = 20.sp)
            }
        }
    }
}

