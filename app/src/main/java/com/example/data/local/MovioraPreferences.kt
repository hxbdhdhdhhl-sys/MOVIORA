package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.example.ui.i18n.MovioraLocalization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private val Context.movioraDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "moviora_client_preferences"
)

enum class MovioraQualityPreset(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val descriptionAr: String,
    val maxCapHeight: Int
) {
    AUTO("AUTO", "تلقائي ذكي (Auto)", "Smart Auto", "يتكيف تلقائياً مع سرعة الإنترنت لحظة بلحظة دون تقطيع", 2160),
    DATA_SAVER("DATA_SAVER", "توفير البيانات (Data Saver)", "Data Saver", "يقلل استهلاك الباقة حتى 360p/480p للاتصالات المحدودة", 480),
    BALANCED("BALANCED", "متوازن (Balanced HD)", "Balanced HD", "توازن مثالي بين نقاء الصورة واستهلاك البيانات (720p)", 720),
    HIGH("HIGH", "جودة عالية (Full HD)", "High Quality", "دقة سينمائية كاملة 1080p عند توفرها في المصدر", 1080),
    ULTRA("ULTRA", "فائقة (Ultra 4K / HDR)", "Ultra Quality", "أقصى دقة متاحة في المصدر الأصلي حتى 4K وHDR", 2160);

    companion object {
        fun fromId(id: String): MovioraQualityPreset =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

data class UserClientSettings(
    val languageCode: String = MovioraLocalization.detectDefaultLanguageCode(),
    val themeMode: String = "dark", // dark | light | vip_gold | vip_velvet
    val qualityPreset: MovioraQualityPreset = MovioraQualityPreset.AUTO,
    val wifiDefaultMaxHeight: Int = 2160,
    val mobileDefaultMaxHeight: Int = 720,
    val autoPlayNextEpisode: Boolean = true,
    val preferredSubtitleLang: String = "ar",
    val subtitleFontSizeSp: Int = 18,
    val subtitleColorHex: String = "#FFFFFF",
    val subtitleSyncOffsetMs: Long = 0L,
    val notifNewEpisodes: Boolean = true,
    val notifNewReleases: Boolean = true,
    val notifRecommendations: Boolean = true,
    val notifSubscriptionAlerts: Boolean = true
)

class MovioraPreferences(private val context: Context) {

    private val appContext = context.applicationContext

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val QUALITY_PRESET = stringPreferencesKey("quality_preset")
        val WIFI_MAX_HEIGHT = intPreferencesKey("wifi_max_height")
        val MOBILE_MAX_HEIGHT = intPreferencesKey("mobile_max_height")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val SUBTITLE_LANG = stringPreferencesKey("subtitle_lang")
        val SUBTITLE_SIZE_SP = intPreferencesKey("subtitle_size_sp")
        val SUBTITLE_COLOR_HEX = stringPreferencesKey("subtitle_color_hex")
        val SUBTITLE_OFFSET_MS = longPreferencesKey("subtitle_offset_ms")
        val NOTIF_EPISODES = booleanPreferencesKey("notif_episodes")
        val NOTIF_RELEASES = booleanPreferencesKey("notif_releases")
        val NOTIF_RECOMMENDATIONS = booleanPreferencesKey("notif_recommendations")
        val NOTIF_SUBSCRIPTION = booleanPreferencesKey("notif_subscription")
    }

    val settingsFlow: Flow<UserClientSettings> = appContext.movioraDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val defaultLang = MovioraLocalization.detectDefaultLanguageCode()
            UserClientSettings(
                languageCode = prefs[Keys.LANGUAGE_CODE] ?: defaultLang,
                themeMode = prefs[Keys.THEME_MODE] ?: "dark",
                qualityPreset = MovioraQualityPreset.fromId(prefs[Keys.QUALITY_PRESET] ?: "AUTO"),
                wifiDefaultMaxHeight = prefs[Keys.WIFI_MAX_HEIGHT] ?: 2160,
                mobileDefaultMaxHeight = prefs[Keys.MOBILE_MAX_HEIGHT] ?: 720,
                autoPlayNextEpisode = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
                preferredSubtitleLang = prefs[Keys.SUBTITLE_LANG] ?: defaultLang,
                subtitleFontSizeSp = (prefs[Keys.SUBTITLE_SIZE_SP] ?: 18).coerceIn(13, 30),
                subtitleColorHex = prefs[Keys.SUBTITLE_COLOR_HEX] ?: "#FFFFFF",
                subtitleSyncOffsetMs = (prefs[Keys.SUBTITLE_OFFSET_MS] ?: 0L).coerceIn(-10_000L, 10_000L),
                notifNewEpisodes = prefs[Keys.NOTIF_EPISODES] ?: true,
                notifNewReleases = prefs[Keys.NOTIF_RELEASES] ?: true,
                notifRecommendations = prefs[Keys.NOTIF_RECOMMENDATIONS] ?: true,
                notifSubscriptionAlerts = prefs[Keys.NOTIF_SUBSCRIPTION] ?: true
            )
        }

    suspend fun setLanguage(code: String) {
        appContext.movioraDataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun setThemeMode(mode: String) {
        appContext.movioraDataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setQualityPreset(preset: MovioraQualityPreset) {
        appContext.movioraDataStore.edit { it[Keys.QUALITY_PRESET] = preset.id }
    }

    suspend fun setWifiMaxHeight(height: Int) {
        appContext.movioraDataStore.edit { it[Keys.WIFI_MAX_HEIGHT] = height }
    }

    suspend fun setMobileMaxHeight(height: Int) {
        appContext.movioraDataStore.edit { it[Keys.MOBILE_MAX_HEIGHT] = height }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        appContext.movioraDataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setSubtitlePreferences(
        lang: String,
        fontSizeSp: Int,
        colorHex: String,
        syncOffsetMs: Long
    ) {
        appContext.movioraDataStore.edit {
            it[Keys.SUBTITLE_LANG] = lang
            it[Keys.SUBTITLE_SIZE_SP] = fontSizeSp.coerceIn(13, 30)
            it[Keys.SUBTITLE_COLOR_HEX] = colorHex
            it[Keys.SUBTITLE_OFFSET_MS] = syncOffsetMs.coerceIn(-10_000L, 10_000L)
        }
    }

    suspend fun setNotificationPreferences(
        episodes: Boolean,
        releases: Boolean,
        recommendations: Boolean,
        subscription: Boolean
    ) {
        appContext.movioraDataStore.edit {
            it[Keys.NOTIF_EPISODES] = episodes
            it[Keys.NOTIF_RELEASES] = releases
            it[Keys.NOTIF_RECOMMENDATIONS] = recommendations
            it[Keys.NOTIF_SUBSCRIPTION] = subscription
        }
    }

    suspend fun calculateCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        dirSize(appContext.cacheDir)
    }

    @OptIn(ExperimentalCoilApi::class)
    suspend fun clearAppCache(): Long = withContext(Dispatchers.IO) {
        val before = dirSize(appContext.cacheDir)
        runCatching {
            val loader = Coil.imageLoader(appContext)
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        }
        appContext.cacheDir.listFiles()?.forEach { child ->
            runCatching { child.deleteRecursively() }
        }
        before
    }

    private fun dirSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { dirSize(it) } ?: 0L
    }
}
