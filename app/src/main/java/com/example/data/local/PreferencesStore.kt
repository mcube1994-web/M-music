package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AiEqMode
import com.example.domain.model.AppearanceSkin
import com.example.domain.model.NavidromeConfig
import com.example.domain.model.VisualizerMode
import com.example.domain.model.VisualizerPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.m3DataStore by preferencesDataStore(name = "m3_player_prefs")

class PreferencesStore(private val context: Context) {

    private object Keys {
        val OPENING_SWEEP_ENABLED = booleanPreferencesKey("opening_sweep_enabled")
        val VISUALIZER_MODE = stringPreferencesKey("visualizer_mode")
        val VISUALIZER_PALETTE = stringPreferencesKey("visualizer_palette")
        val APPEARANCE_SKIN = stringPreferencesKey("appearance_skin")
        val AI_EQ_MODE = stringPreferencesKey("ai_eq_mode")
        val AI_AUTO_EQ_ENABLED = booleanPreferencesKey("ai_auto_eq_enabled")
        val NAVIDROME_URL = stringPreferencesKey("navidrome_url")
        val NAVIDROME_USER = stringPreferencesKey("navidrome_user")
        val NAVIDROME_PASS = stringPreferencesKey("navidrome_pass")
        val SPATIALIZER_EFFECT_ENABLED = booleanPreferencesKey("spatializer_effect_enabled")
    }

    val openingSweepEnabled: Flow<Boolean> = context.m3DataStore.data.map { prefs ->
        prefs[Keys.OPENING_SWEEP_ENABLED] ?: true
    }

    val visualizerMode: Flow<VisualizerMode> = context.m3DataStore.data.map { prefs ->
        val raw = prefs[Keys.VISUALIZER_MODE]
        runCatching { if (raw != null) VisualizerMode.valueOf(raw) else VisualizerMode.VIZZ_MESH_GRID }
            .getOrDefault(VisualizerMode.VIZZ_MESH_GRID)
    }

    val visualizerPalette: Flow<VisualizerPalette> = context.m3DataStore.data.map { prefs ->
        val raw = prefs[Keys.VISUALIZER_PALETTE]
        runCatching { if (raw != null) VisualizerPalette.valueOf(raw) else VisualizerPalette.CYBER_CYAN }
            .getOrDefault(VisualizerPalette.CYBER_CYAN)
    }

    val appearanceSkin: Flow<AppearanceSkin> = context.m3DataStore.data.map { prefs ->
        val raw = prefs[Keys.APPEARANCE_SKIN]
        runCatching { if (raw != null) AppearanceSkin.valueOf(raw) else AppearanceSkin.LEVYRA_DEEPSOUND }
            .getOrDefault(AppearanceSkin.LEVYRA_DEEPSOUND)
    }

    val aiAutoEqEnabled: Flow<Boolean> = context.m3DataStore.data.map { prefs ->
        prefs[Keys.AI_AUTO_EQ_ENABLED] ?: true
    }

    val selectedEqMode: Flow<AiEqMode> = context.m3DataStore.data.map { prefs ->
        val raw = prefs[Keys.AI_EQ_MODE]
        runCatching { if (raw != null) AiEqMode.valueOf(raw) else AiEqMode.AUTO_DETECT }
            .getOrDefault(AiEqMode.AUTO_DETECT)
    }

    val spatializerEffectEnabled: Flow<Boolean> = context.m3DataStore.data.map { prefs ->
        prefs[Keys.SPATIALIZER_EFFECT_ENABLED] ?: true
    }

    val navidromeConfig: Flow<NavidromeConfig> = context.m3DataStore.data.map { prefs ->
        val url = prefs[Keys.NAVIDROME_URL].orEmpty()
        val user = prefs[Keys.NAVIDROME_USER].orEmpty()
        val pass = prefs[Keys.NAVIDROME_PASS].orEmpty()
        NavidromeConfig(
            serverUrl = url,
            username = user,
            password = pass,
            isConnected = url.isNotBlank() && user.isNotBlank(),
            statusMessage = if (url.isNotBlank()) "Saved server: $url" else "Not configured"
        )
    }

    suspend fun setOpeningSweepEnabled(enabled: Boolean) {
        context.m3DataStore.edit { it[Keys.OPENING_SWEEP_ENABLED] = enabled }
    }

    suspend fun setVisualizerMode(mode: VisualizerMode) {
        context.m3DataStore.edit { it[Keys.VISUALIZER_MODE] = mode.name }
    }

    suspend fun setVisualizerPalette(palette: VisualizerPalette) {
        context.m3DataStore.edit { it[Keys.VISUALIZER_PALETTE] = palette.name }
    }

    suspend fun setAppearanceSkin(skin: AppearanceSkin) {
        context.m3DataStore.edit { it[Keys.APPEARANCE_SKIN] = skin.name }
    }

    suspend fun setAiAutoEqEnabled(enabled: Boolean) {
        context.m3DataStore.edit { it[Keys.AI_AUTO_EQ_ENABLED] = enabled }
    }

    suspend fun setEqMode(mode: AiEqMode) {
        context.m3DataStore.edit { it[Keys.AI_EQ_MODE] = mode.name }
    }

    suspend fun setSpatializerEffectEnabled(enabled: Boolean) {
        context.m3DataStore.edit { it[Keys.SPATIALIZER_EFFECT_ENABLED] = enabled }
    }

    suspend fun saveNavidromeConfig(url: String, user: String, pass: String) {
        context.m3DataStore.edit {
            it[Keys.NAVIDROME_URL] = url.trim().trimEnd('/')
            it[Keys.NAVIDROME_USER] = user.trim()
            it[Keys.NAVIDROME_PASS] = pass
        }
    }
}
