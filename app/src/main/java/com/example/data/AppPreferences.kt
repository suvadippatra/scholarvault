package com.scholarvault.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore by preferencesDataStore(name = "scholar_viewer_prefs")

object PrefKeys {
    val PDF_VIEWER_MODE = stringPreferencesKey("pdf_viewer_mode")
    // "google_drive" | "custom"  — default: "custom"
    val PDF_FLIP_ANIMATION = booleanPreferencesKey("pdf_flip_anim")
    val PDF_SCROLL_DIRECTION = stringPreferencesKey("pdf_scroll_dir")
    // "horizontal" | "vertical"
    val PDF_FIT_MODE = stringPreferencesKey("pdf_fit_mode")
    // "fit" | "grid"
    val THEME_MODE = stringPreferencesKey("theme_mode")
    // "system" | "light" | "dark"
    val AUDIO_FORMAT = stringPreferencesKey("audio_format")
    val RECORDING_PREFIX = stringPreferencesKey("recording_prefix")
    val LOW_SIZE_RECORDING = booleanPreferencesKey("low_size_recording")
    val SCANNER_ENGINE = stringPreferencesKey("scanner_engine")
}

class AppPreferences(private val context: Context) {
    
    val themeMode: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.THEME_MODE] ?: "system" }

    val audioFormat: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.AUDIO_FORMAT] ?: "m4a" }

    val recordingPrefix: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.RECORDING_PREFIX] ?: "Recording_" }

    val lowSizeRecording: Flow<Boolean> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.LOW_SIZE_RECORDING] ?: false }

    val pdfViewerMode: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.PDF_VIEWER_MODE] ?: "custom_v2" }

    val scannerEngine: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.SCANNER_ENGINE] ?: "custom" }

    val pdfFlipAnimation: Flow<Boolean> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.PDF_FLIP_ANIMATION] ?: false }

    val pdfScrollDirection: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.PDF_SCROLL_DIRECTION] ?: "vertical" }

    val pdfFitMode: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.PDF_FIT_MODE] ?: "fit" }

    fun getPdfReadingProgress(filePath: String): Flow<Int> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[intPreferencesKey("progress_$filePath")] ?: 0 }

    suspend fun setPdfReadingProgress(filePath: String, pageIndex: Int) {
        context.appPrefsDataStore.edit { it[intPreferencesKey("progress_$filePath")] = pageIndex }
    }

    suspend fun setPdfViewerMode(mode: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.PDF_VIEWER_MODE] = mode }
    }

    suspend fun setPdfFlipAnimation(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[PrefKeys.PDF_FLIP_ANIMATION] = enabled }
    }

    suspend fun setPdfScrollDirection(dir: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.PDF_SCROLL_DIRECTION] = dir }
    }

    suspend fun setPdfFitMode(mode: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.PDF_FIT_MODE] = mode }
    }

    suspend fun resetTransientViewerState() {
        context.appPrefsDataStore.edit { prefs ->
            prefs[PrefKeys.PDF_FIT_MODE] = "fit"
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.THEME_MODE] = mode }
    }

    suspend fun setAudioFormat(format: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.AUDIO_FORMAT] = format }
    }

    suspend fun setRecordingPrefix(prefix: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.RECORDING_PREFIX] = prefix }
    }

    suspend fun setLowSizeRecording(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[PrefKeys.LOW_SIZE_RECORDING] = enabled }
    }

    suspend fun setScannerEngine(engine: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.SCANNER_ENGINE] = engine }
    }
}
