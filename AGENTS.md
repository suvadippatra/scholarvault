# Agent Instructions

## UI Layout Rules

1. **Bottom Navigation, FAB, and FAB Menu Lock:**
   - **DO NOT** modify the structural layout, spacing, or styling of the `CustomBottomNavigationBar`, the primary FloatingAction Button (FAB), and the `FabMenuOverlay` in `app/src/main/java/com/example/ui/MainScreen.kt`.
   - The navigation bar background explicitly relies on a `Transparent` outer container with the background drawn into `bottomNavShape` inside its own box.
   - The FAB menu overlay is absolutely positioned above the FAB using `.navigationBarsPadding()` and `.padding(bottom = paddingBottom + 40.dp)`. This exact padding formula prevents the menu from falling out of position.
   - These components are considered **frozen**. Any future updates to the UI should leave these elements untouched to preserve visual integrity.

## Performance & Animation Rules

1. **Staggered Animations (Top-to-Bottom Loading)**:
   - When introducing delay popups or list renderings, load items staggered from top to bottom (e.g., using `animateItemPlacement` or coroutine delays combined with `AnimatedVisibility`).
2. **Text First, Defer Heavy Loads**:
   - Ensure text-based strings and primary structural elements load instantly.
   - Avoid parsing files, rendering image bitmaps, reading raw byte buffers, or loading extensive nested attachments synchronously inside a core Composition loop. Defer them to when explicitly requested (e.g. user toggles a preview icon).
3. **Toggleable Animation State**:
   - Complex animations must check a boolean state (e.g. `animationsEnabled`) before executing. The application will soon integrate a Settings toggle for this exact property. All major UI animations should have a fallback `else` path that provides instant layout loading if the boolean is false.
4. **Fluidity in Data Handling**:
   - Heavy parsing (e.g., converting JSON Strings of attachments) within Composables MUST leverage `remember` and `derivedStateOf`. Do not parse strings to JSON at 60fps on recomposition.
5. **Compressed Typography**:
   - When injecting custom fonts for a professional student look, employ Jetpack Compose's Downloadable Fonts via Google Fonts (`androidx.compose.ui.text.googlefonts`) instead of dropping `.ttf` files into raw assets, ensuring high-quality scaling with virtually zero impact on the APK size footprint.

## Typography & Styling Rules

1. **Text Size & Style Consistency**:
   - Apply consistent typography globally using Material 3 guidelines.
   - **Readable Minimums**: Text CANNOT be too small. The absolute minimum text size for any readable context must be `12.sp`. Secondary/help text should be `14.sp`, and body/content text `16.sp` or larger.
   - **Title & Heading Consistency**: Large hero titles should be `32.sp` to `36.sp`, standard screen titles `24.sp` to `28.sp`, section titles `18.sp` to `20.sp`.
   - Every page (including Profile) must use the same universal `FontFamily` and font styles.
   - **Line Spacing**: Set readable line heights (`lineHeight`) aligning with standard Material typography recommendations.

2. **Container & Theming**:
   - Use global Material 3 theme properties consistently. Rely on `MaterialTheme.colorScheme` standard colors, rather than manually hardcoded color hexes.
   - Consistently apply container shapes, rounding (e.g., `12.dp` or `16.dp`), padding, and shadowing across all screens.
   - **Custom Theming**: MUST maintain and prioritize the existing custom dark/light color scheme variations. Do not replace them with generic default color palettes.

## Navigation & Page Structure Rules

1. **Sub-page Header & Navigation**:
   - In **ANY** page except the main bottom-nav pages (Dashboard, Academic, Documents, Tools):
     - You MUST show a **back button** (`Up` navigation) instead of the sidebar/drawer menu button.
     - The Bottom Navigation Bar must be **hidden in an animated way** (leveraging `AnimatedVisibility` or state like the Profile page does).
   - **Search Bar & Profile Pic Visibility**: Whether the top search bar or the profile picture is rendered on these sub-pages is strictly determined by explicit user request. If the user does not explicitly say to keep them, hide them or do what the user requests.