package com.example.data.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val emailVerified: Boolean = false,
    val photoUrl: String = "",
    val preferredLanguage: String = "ar",
    val preferredSubtitleLang: String = "ar",
    val themeMode: String = "dark",
    val notificationsEnabled: Boolean = true,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class PublicProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val isVip: Boolean = false,
    val isPublic: Boolean = true,
    val followersCount: Long = 0L,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class MediaItem(
    val id: String = "",
    val titleAr: String = "",
    val titleOriginal: String = "",
    val titleEn: String = "",
    val titleRu: String = "",
    val kind: String = "series", // movie | series | documentary
    val year: Int = 2024,
    val country: String = "",
    val originalLanguage: String = "Русский",
    val synopsisAr: String = "",
    val synopsisEn: String = "",
    val synopsisRu: String = "",
    val genres: List<String> = emptyList(),
    val director: String = "",
    val cast: List<String> = emptyList(),
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val accentColorHex: String = "#3B2660",
    val seasonsCount: Int = 1,
    val episodesCount: Int = 1,
    val durationMinutes: Int = 50,
    val rating: Double = 8.5,
    val viewsCount: Long = 0L,
    val availableSubtitles: List<String> = listOf("العربية", "English", "Русский"),
    val availableAudio: List<String> = listOf("الأصلي"),
    val streamStatus: String = "ARCHIVE_METADATA_ONLY", // LICENSED_ACTIVE | ARCHIVE_METADATA_ONLY
    val streamUrl: String = "",
    val streamProtocol: String = "NONE", // HLS | DASH | MP4 | NONE
    val licenseInfo: String = "",
    val isVipExclusive: Boolean = false,
    val isDownloadAllowed: Boolean = false,
    val archiveSection: String = "", // SOVIET | RUSSIAN_WAR | WWI | WWII | COLD_WAR | ROME | EMPIRES | EUROPEAN | MILITARY | DOCS
    val warConflict: String = "",
    val historicalEra: String = "",
    val historicalAccuracyNote: String = "",
    val upcomingSeasonNote: String = "",
    val isPublished: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
) {
    fun localizedTitle(langCode: String): String = when (langCode) {
        "ar" -> titleAr.ifBlank { titleEn.ifBlank { titleOriginal } }
        "ru" -> titleRu.ifBlank { titleOriginal.ifBlank { titleEn } }
        else -> titleEn.ifBlank { titleOriginal.ifBlank { titleAr } }
    }

    fun localizedSynopsis(langCode: String): String = when (langCode) {
        "ar" -> synopsisAr.ifBlank { synopsisEn }
        "ru" -> synopsisRu.ifBlank { synopsisEn.ifBlank { synopsisAr } }
        else -> synopsisEn.ifBlank { synopsisAr }
    }
}

data class Season(
    val id: String = "",
    val seriesId: String = "",
    val seasonNumber: Int = 1,
    val titleAr: String = "",
    val titleEn: String = "",
    val year: Int = 2024,
    val episodesCount: Int = 1,
    val isUpcoming: Boolean = false,
    val isPublished: Boolean = true,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class Episode(
    val id: String = "",
    val seriesId: String = "",
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val titleAr: String = "",
    val titleEn: String = "",
    val descriptionAr: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 2700,
    val streamUrl: String = "",
    val streamProtocol: String = "NONE",
    val subtitleUrlAr: String = "",
    val subtitleUrlEn: String = "",
    val isPublished: Boolean = true,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class Person(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val nameRu: String = "",
    val role: String = "actor", // actor | director | producer | actor_director
    val birthYear: String = "",
    val country: String = "",
    val bioAr: String = "",
    val photoUrl: String = "",
    val notableWorks: List<String> = emptyList(),
    val isPublished: Boolean = true,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class VideoSource(
    val id: String = "",
    val targetId: String = "",
    val targetType: String = "movie", // movie | episode
    val url: String = "",
    val protocol: String = "HLS", // HLS | DASH | MP4
    val maxQualityLabel: String = "1080p",
    val licenseName: String = "Public Domain / Licensed",
    val allowOffline: Boolean = false,
    val isPublished: Boolean = true,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class SubtitleSource(
    val id: String = "",
    val targetId: String = "",
    val languageCode: String = "ar",
    val languageLabel: String = "العربية",
    val url: String = "",
    val format: String = "VTT",
    val isPublished: Boolean = true,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class FavoriteItem(
    val id: String = "",
    val userId: String = "",
    val mediaId: String = "",
    val titleAr: String = "",
    val createdAtEpochMs: Long = 0L
)

data class WatchHistoryEntry(
    val id: String = "",
    val userId: String = "",
    val mediaId: String = "",
    val episodeId: String = "",
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val positionMs: Long = 0L,
    val durationMs: Long = 1L,
    val completed: Boolean = false,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class SearchHistoryEntry(
    val id: String = "",
    val userId: String = "",
    val query: String = "",
    val createdAtEpochMs: Long = 0L
)

data class UserSession(
    val id: String = "",
    val userId: String = "",
    val deviceModel: String = "",
    val platform: String = "Android",
    val isActive: Boolean = true,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class Watchlist(
    val id: String = "",
    val userId: String = "",
    val ownerName: String = "",
    val name: String = "",
    val description: String = "",
    val isPublic: Boolean = false,
    val mediaIds: List<String> = emptyList(),
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class Review(
    val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val mediaId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val status: String = "approved", // approved | hidden
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class Follow(
    val id: String = "",
    val followerId: String = "",
    val targetUserId: String = "",
    val targetName: String = "",
    val createdAtEpochMs: Long = 0L
)

data class Subscription(
    val id: String = "",
    val userId: String = "",
    val planId: String = "vip_monthly", // vip_monthly | vip_quarterly | vip_yearly
    val status: String = "active", // active | cancelled | expired
    val provider: String = "internal_preview", // internal_preview | google_play | admin_grant
    val purchaseToken: String = "",
    val priceDisplay: String = "",
    val startsAtEpochMs: Long = 0L,
    val expiresAtEpochMs: Long = 0L,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
) {
    val isCurrentlyActive: Boolean
        get() = status == "active" && expiresAtEpochMs > System.currentTimeMillis()
}

data class Payment(
    val id: String = "",
    val userId: String = "",
    val planId: String = "",
    val amountDisplay: String = "",
    val currency: String = "USD",
    val orderId: String = "",
    val purchaseToken: String = "",
    val status: String = "verified",
    val createdAtEpochMs: Long = 0L
)

data class AppNotification(
    val id: String = "",
    val targetUid: String = "ALL",
    val category: String = "new_release",
    val titleAr: String = "",
    val bodyAr: String = "",
    val mediaId: String = "",
    val createdAtEpochMs: Long = 0L
)

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val targetId: String = "",
    val targetType: String = "review",
    val reason: String = "",
    val status: String = "open", // open | resolved | dismissed
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val message: String = "",
    val isVipPriority: Boolean = false,
    val status: String = "open",
    val adminReply: String = "",
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class PlaybackErrorLog(
    val id: String = "",
    val userId: String = "",
    val mediaId: String = "",
    val errorCode: String = "",
    val errorMessage: String = "",
    val createdAtEpochMs: Long = 0L
)

data class AdminRecord(
    val id: String = "",
    val adminUid: String = "",
    val email: String = "",
    val role: String = "super_admin",
    val grantedBy: String = "",
    val createdAtEpochMs: Long = 0L
)

data class AdminAuditLog(
    val id: String = "",
    val adminUid: String = "",
    val action: String = "",
    val targetId: String = "",
    val details: String = "",
    val createdAtEpochMs: Long = 0L
)

data class HistoricalTimelineEvent(
    val id: String,
    val yearLabel: String,
    val eraCategory: String,
    val titleAr: String,
    val titleEn: String,
    val summaryAr: String,
    val factVsDramaNoteAr: String,
    val linkedMediaIds: List<String>
)

data class HistoricalTheaterMap(
    val id: String,
    val titleAr: String,
    val eraAr: String,
    val regionAr: String,
    val strategicSummaryAr: String,
    val keyFronts: List<Pair<String, Pair<Float, Float>>>,
    val linkedMediaIds: List<String>
)
