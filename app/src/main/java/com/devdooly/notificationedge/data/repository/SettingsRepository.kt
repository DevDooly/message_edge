package com.devdooly.notificationedge.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.model.EdgeSide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "notification_edge_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private const val SYNC_PREFS_NAME = "notification_edge_sync_prefs"
        private const val KEY_LAUNCH_DIRECT = "launch_direct_to_panel"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private object PreferencesKeys {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val EDGE_SIDE = intPreferencesKey("edge_side")
        val HANDLE_POS_RATIO = floatPreferencesKey("handle_pos_ratio")
        val HANDLE_WIDTH_DP = intPreferencesKey("handle_width_dp")
        val HANDLE_HEIGHT_DP = intPreferencesKey("handle_height_dp")
        val HANDLE_COLOR = longPreferencesKey("handle_color")
        val HANDLE_ALPHA = floatPreferencesKey("handle_alpha")
        val HANDLE_VISIBLE = booleanPreferencesKey("handle_visible")
        val LAUNCH_DIRECT_TO_PANEL = booleanPreferencesKey("launch_direct_to_panel")
        val PANEL_WIDTH_DP = intPreferencesKey("panel_width_dp")
        val AUTO_DISMISS_ON_OPEN = booleanPreferencesKey("auto_dismiss_on_open")
        val EDGE_LIGHTING_ENABLED = booleanPreferencesKey("edge_lighting_enabled")
        val EDGE_LIGHTING_DURATION_MS = longPreferencesKey("edge_lighting_duration_ms")
        val EDGE_LIGHTING_COLOR = longPreferencesKey("edge_lighting_color")
        val EDGE_LIGHTING_CORNER_RADIUS_DP = intPreferencesKey("edge_lighting_corner_radius_dp")
        val SELECTED_FONT = stringPreferencesKey("selected_font")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val PAUSE_MEDIA_ON_OPEN = booleanPreferencesKey("pause_media_on_open")
        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")
        val DISCOVERED_APP_PACKAGES = stringSetPreferencesKey("discovered_app_packages")
        val BLOCKED_KEYWORDS = stringSetPreferencesKey("blocked_keywords")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isServiceEnabled = prefs[PreferencesKeys.SERVICE_ENABLED] ?: true,
            edgeSide = if ((prefs[PreferencesKeys.EDGE_SIDE] ?: 1) == 0) EdgeSide.LEFT else EdgeSide.RIGHT,
            handlePositionRatio = prefs[PreferencesKeys.HANDLE_POS_RATIO] ?: 0.5f,
            handleWidthDp = prefs[PreferencesKeys.HANDLE_WIDTH_DP] ?: 8,
            handleHeightDp = prefs[PreferencesKeys.HANDLE_HEIGHT_DP] ?: 110,
            handleColor = prefs[PreferencesKeys.HANDLE_COLOR] ?: 0xFF82D8D0,
            handleAlpha = prefs[PreferencesKeys.HANDLE_ALPHA] ?: 0.75f,
            isHandleVisible = prefs[PreferencesKeys.HANDLE_VISIBLE] ?: true,
            launchDirectToPanel = prefs[PreferencesKeys.LAUNCH_DIRECT_TO_PANEL] ?: true,
            panelWidthDp = prefs[PreferencesKeys.PANEL_WIDTH_DP] ?: 280,
            autoDismissOnOpen = prefs[PreferencesKeys.AUTO_DISMISS_ON_OPEN] ?: true,
            isEdgeLightingEnabled = prefs[PreferencesKeys.EDGE_LIGHTING_ENABLED] ?: true,
            edgeLightingDurationMs = prefs[PreferencesKeys.EDGE_LIGHTING_DURATION_MS] ?: 3000L,
            edgeLightingColor = prefs[PreferencesKeys.EDGE_LIGHTING_COLOR] ?: 0xFF82D8D0,
            edgeLightingCornerRadiusDp = prefs[PreferencesKeys.EDGE_LIGHTING_CORNER_RADIUS_DP] ?: 32,
            selectedFont = prefs[PreferencesKeys.SELECTED_FONT] ?: "default",
            hapticFeedbackEnabled = prefs[PreferencesKeys.HAPTIC_ENABLED] ?: true,
            pauseMediaOnOpen = prefs[PreferencesKeys.PAUSE_MEDIA_ON_OPEN] ?: true,
            excludedPackages = prefs[PreferencesKeys.EXCLUDED_PACKAGES] ?: emptySet(),
            discoveredAppPackages = prefs[PreferencesKeys.DISCOVERED_APP_PACKAGES] ?: emptySet(),
            blockedKeywords = prefs[PreferencesKeys.BLOCKED_KEYWORDS] ?: emptySet()
        )
    }

    suspend fun updateServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SERVICE_ENABLED] = enabled }
    }

    suspend fun updateEdgeSide(side: EdgeSide) {
        context.dataStore.edit { it[PreferencesKeys.EDGE_SIDE] = if (side == EdgeSide.LEFT) 0 else 1 }
    }

    suspend fun updateHandlePositionRatio(ratio: Float) {
        context.dataStore.edit { it[PreferencesKeys.HANDLE_POS_RATIO] = ratio.coerceIn(0.1f, 0.9f) }
    }

    suspend fun updateHandleWidthDp(width: Int) {
        context.dataStore.edit { it[PreferencesKeys.HANDLE_WIDTH_DP] = width.coerceIn(4, 30) }
    }

    suspend fun updateHandleHeightDp(height: Int) {
        val snapped = (height / 5) * 5
        context.dataStore.edit { it[PreferencesKeys.HANDLE_HEIGHT_DP] = snapped.coerceIn(40, 250) }
    }

    suspend fun updateHandleColor(color: Long) {
        context.dataStore.edit { it[PreferencesKeys.HANDLE_COLOR] = color }
    }

    suspend fun updateHandleAlpha(alpha: Float) {
        context.dataStore.edit { it[PreferencesKeys.HANDLE_ALPHA] = alpha.coerceIn(0.0f, 1.0f) }
    }

    suspend fun updateHandleVisible(visible: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HANDLE_VISIBLE] = visible }
    }

    suspend fun updateLaunchDirectToPanel(direct: Boolean) {
        context.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LAUNCH_DIRECT, direct)
            .apply()
        context.dataStore.edit { it[PreferencesKeys.LAUNCH_DIRECT_TO_PANEL] = direct }
    }

    fun isLaunchDirectToPanelSync(): Boolean {
        val sp = context.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_LAUNCH_DIRECT, true)
    }

    suspend fun updatePanelWidthDp(width: Int) {
        val snapped = (width / 5) * 5
        context.dataStore.edit { it[PreferencesKeys.PANEL_WIDTH_DP] = snapped.coerceIn(220, 360) }
    }

    suspend fun updateAutoDismissOnOpen(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_DISMISS_ON_OPEN] = enabled }
    }

    suspend fun updateEdgeLightingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.EDGE_LIGHTING_ENABLED] = enabled }
    }

    suspend fun updateEdgeLightingColor(color: Long) {
        context.dataStore.edit { it[PreferencesKeys.EDGE_LIGHTING_COLOR] = color }
    }

    suspend fun updateEdgeLightingCornerRadiusDp(radius: Int) {
        context.dataStore.edit { it[PreferencesKeys.EDGE_LIGHTING_CORNER_RADIUS_DP] = radius.coerceIn(0, 60) }
    }

    suspend fun updateSelectedFont(fontId: String) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_FONT] = fontId }
    }

    suspend fun updateHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun updatePauseMediaOnOpen(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PAUSE_MEDIA_ON_OPEN] = enabled }
    }

    suspend fun setPackageExcluded(packageName: String, excluded: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.EXCLUDED_PACKAGES] ?: emptySet()
            prefs[PreferencesKeys.EXCLUDED_PACKAGES] = if (excluded) {
                current + packageName
            } else {
                current - packageName
            }
        }
    }

    suspend fun addDiscoveredPackage(packageName: String) {
        if (packageName.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.DISCOVERED_APP_PACKAGES] ?: emptySet()
            if (!current.contains(packageName)) {
                prefs[PreferencesKeys.DISCOVERED_APP_PACKAGES] = current + packageName
            }
        }
    }

    suspend fun clearDiscoveredPackages() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DISCOVERED_APP_PACKAGES] = emptySet()
        }
    }

    suspend fun addBlockedKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.BLOCKED_KEYWORDS] ?: emptySet()
            prefs[PreferencesKeys.BLOCKED_KEYWORDS] = current + trimmed
        }
    }

    suspend fun removeBlockedKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.BLOCKED_KEYWORDS] ?: emptySet()
            prefs[PreferencesKeys.BLOCKED_KEYWORDS] = current - trimmed
        }
    }
}
