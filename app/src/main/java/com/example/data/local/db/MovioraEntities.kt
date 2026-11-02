package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.data.model.*

class MovioraRoomConverters {
    private val delimiter = "\u001F"

    @TypeConverter
    fun fromStringList(items: List<String>?): String =
        items?.filter { it.isNotBlank() }?.joinToString(delimiter) ?: ""

    @TypeConverter
    fun toStringList(serialized: String?): List<String> =
        if (serialized.isNullOrBlank()) emptyList()
        else serialized.split(delimiter).filter { it.isNotBlank() }
}

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val displayName: String,
    val email: String,
    val emailVerified: Boolean,
    val photoUrl: String,
    val preferredLanguage: String,
    val preferredSubtitleLang: String,
    val themeMode: String,
    val notificationsEnabled: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        userId = userId,
        displayName = displayName,
        email = email,
        emailVerified = emailVerified,
        photoUrl = photoUrl,
        preferredLanguage = preferredLanguage,
        preferredSubtitleLang = preferredSubtitleLang,
        themeMode = themeMode,
        notificationsEnabled = notificationsEnabled,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val deviceModel: String,
    val platform: String,
    val isActive: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): UserSession = UserSession(
        id = id,
        userId = userId,
        deviceModel = deviceModel,
        platform = platform,
        isActive = isActive,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "media_overrides")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val titleAr: String,
    val titleOriginal: String,
    val titleEn: String,
    val titleRu: String,
    val kind: String,
    val year: Int,
    val country: String,
    val originalLanguage: String,
    val synopsisAr: String,
    val synopsisEn: String,
    val synopsisRu: String,
    val genres: List<String>,
    val director: String,
    val cast: List<String>,
    val posterUrl: String,
    val backdropUrl: String,
    val accentColorHex: String,
    val seasonsCount: Int,
    val episodesCount: Int,
    val durationMinutes: Int,
    val rating: Double,
    val viewsCount: Long,
    val availableSubtitles: List<String>,
    val availableAudio: List<String>,
    val streamStatus: String,
    val streamUrl: String,
    val streamProtocol: String,
    val licenseInfo: String,
    val isVipExclusive: Boolean,
    val isDownloadAllowed: Boolean,
    val archiveSection: String,
    val warConflict: String,
    val historicalEra: String,
    val historicalAccuracyNote: String,
    val upcomingSeasonNote: String,
    val isPublished: Boolean,
    val isDeleted: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): MediaItem = MediaItem(
        id = id,
        titleAr = titleAr,
        titleOriginal = titleOriginal,
        titleEn = titleEn,
        titleRu = titleRu,
        kind = kind,
        year = year,
        country = country,
        originalLanguage = originalLanguage,
        synopsisAr = synopsisAr,
        synopsisEn = synopsisEn,
        synopsisRu = synopsisRu,
        genres = genres,
        director = director,
        cast = cast,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        accentColorHex = accentColorHex,
        seasonsCount = seasonsCount,
        episodesCount = episodesCount,
        durationMinutes = durationMinutes,
        rating = rating,
        viewsCount = viewsCount,
        availableSubtitles = availableSubtitles,
        availableAudio = availableAudio,
        streamStatus = streamStatus,
        streamUrl = streamUrl,
        streamProtocol = streamProtocol,
        licenseInfo = licenseInfo,
        isVipExclusive = isVipExclusive,
        isDownloadAllowed = isDownloadAllowed,
        archiveSection = archiveSection,
        warConflict = warConflict,
        historicalEra = historicalEra,
        historicalAccuracyNote = historicalAccuracyNote,
        upcomingSeasonNote = upcomingSeasonNote,
        isPublished = isPublished,
        isDeleted = isDeleted,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun MediaItem.toEntity(
    resolvedId: String = id,
    createdMs: Long = if (createdAtEpochMs > 0L) createdAtEpochMs else System.currentTimeMillis(),
    updatedMs: Long = System.currentTimeMillis()
): MediaItemEntity = MediaItemEntity(
    id = resolvedId,
    titleAr = titleAr,
    titleOriginal = titleOriginal,
    titleEn = titleEn,
    titleRu = titleRu,
    kind = kind,
    year = year,
    country = country,
    originalLanguage = originalLanguage,
    synopsisAr = synopsisAr,
    synopsisEn = synopsisEn,
    synopsisRu = synopsisRu,
    genres = genres,
    director = director,
    cast = cast,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    accentColorHex = accentColorHex,
    seasonsCount = seasonsCount,
    episodesCount = episodesCount,
    durationMinutes = durationMinutes,
    rating = rating,
    viewsCount = viewsCount,
    availableSubtitles = availableSubtitles,
    availableAudio = availableAudio,
    streamStatus = streamStatus,
    streamUrl = streamUrl,
    streamProtocol = streamProtocol,
    licenseInfo = licenseInfo,
    isVipExclusive = isVipExclusive,
    isDownloadAllowed = isDownloadAllowed,
    archiveSection = archiveSection,
    warConflict = warConflict,
    historicalEra = historicalEra,
    historicalAccuracyNote = historicalAccuracyNote,
    upcomingSeasonNote = upcomingSeasonNote,
    isPublished = isPublished,
    isDeleted = isDeleted,
    createdAtEpochMs = createdMs,
    updatedAtEpochMs = updatedMs
)

@Entity(tableName = "seasons")
data class SeasonEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val titleAr: String,
    val titleEn: String,
    val year: Int,
    val episodesCount: Int,
    val isUpcoming: Boolean,
    val isPublished: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): Season = Season(
        id = id,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        titleAr = titleAr,
        titleEn = titleEn,
        year = year,
        episodesCount = episodesCount,
        isUpcoming = isUpcoming,
        isPublished = isPublished,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val streamUrl: String,
    val streamProtocol: String,
    val subtitleUrlAr: String,
    val subtitleUrlEn: String,
    val isPublished: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): Episode = Episode(
        id = id,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        titleAr = titleAr,
        titleEn = titleEn,
        descriptionAr = descriptionAr,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        streamUrl = streamUrl,
        streamProtocol = streamProtocol,
        subtitleUrlAr = subtitleUrlAr,
        subtitleUrlEn = subtitleUrlEn,
        isPublished = isPublished,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "video_sources")
data class VideoSourceEntity(
    @PrimaryKey val id: String,
    val targetId: String,
    val targetType: String,
    val url: String,
    val protocol: String,
    val maxQualityLabel: String,
    val licenseName: String,
    val allowOffline: Boolean,
    val isPublished: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): VideoSource = VideoSource(
        id = id,
        targetId = targetId,
        targetType = targetType,
        url = url,
        protocol = protocol,
        maxQualityLabel = maxQualityLabel,
        licenseName = licenseName,
        allowOffline = allowOffline,
        isPublished = isPublished,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "subtitle_sources")
data class SubtitleSourceEntity(
    @PrimaryKey val id: String,
    val targetId: String,
    val languageCode: String,
    val languageLabel: String,
    val url: String,
    val format: String,
    val isPublished: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): SubtitleSource = SubtitleSource(
        id = id,
        targetId = targetId,
        languageCode = languageCode,
        languageLabel = languageLabel,
        url = url,
        format = format,
        isPublished = isPublished,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val nameRu: String,
    val role: String,
    val birthYear: String,
    val country: String,
    val bioAr: String,
    val photoUrl: String,
    val notableWorks: List<String>,
    val isPublished: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): Person = Person(
        id = id,
        nameAr = nameAr,
        nameEn = nameEn,
        nameRu = nameRu,
        role = role,
        birthYear = birthYear,
        country = country,
        bioAr = bioAr,
        photoUrl = photoUrl,
        notableWorks = notableWorks,
        isPublished = isPublished,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val mediaId: String,
    val titleAr: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): FavoriteItem = FavoriteItem(
        id = id,
        userId = userId,
        mediaId = mediaId,
        titleAr = titleAr,
        createdAtEpochMs = createdAtEpochMs
    )
}

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val mediaId: String,
    val episodeId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): WatchHistoryEntry = WatchHistoryEntry(
        id = id,
        userId = userId,
        mediaId = mediaId,
        episodeId = episodeId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        positionMs = positionMs,
        durationMs = durationMs,
        completed = completed,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val query: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): SearchHistoryEntry = SearchHistoryEntry(
        id = id,
        userId = userId,
        query = query,
        createdAtEpochMs = createdAtEpochMs
    )
}

@Entity(tableName = "watchlists")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val ownerName: String,
    val name: String,
    val description: String,
    val isPublic: Boolean,
    val mediaIds: List<String>,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): Watchlist = Watchlist(
        id = id,
        userId = userId,
        ownerName = ownerName,
        name = name,
        description = description,
        isPublic = isPublic,
        mediaIds = mediaIds,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val authorName: String,
    val mediaId: String,
    val rating: Int,
    val comment: String,
    val status: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): Review = Review(
        id = id,
        userId = userId,
        authorName = authorName,
        mediaId = mediaId,
        rating = rating,
        comment = comment,
        status = status,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey val id: String,
    val followerId: String,
    val targetUserId: String,
    val targetName: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): Follow = Follow(
        id = id,
        followerId = followerId,
        targetUserId = targetUserId,
        targetName = targetName,
        createdAtEpochMs = createdAtEpochMs
    )
}

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val planId: String,
    val status: String,
    val provider: String,
    val purchaseToken: String,
    val priceDisplay: String,
    val startsAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): Subscription = Subscription(
        id = id,
        userId = userId,
        planId = planId,
        status = status,
        provider = provider,
        purchaseToken = purchaseToken,
        priceDisplay = priceDisplay,
        startsAtEpochMs = startsAtEpochMs,
        expiresAtEpochMs = expiresAtEpochMs,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val planId: String,
    val amountDisplay: String,
    val currency: String,
    val orderId: String,
    val purchaseToken: String,
    val status: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): Payment = Payment(
        id = id,
        userId = userId,
        planId = planId,
        amountDisplay = amountDisplay,
        currency = currency,
        orderId = orderId,
        purchaseToken = purchaseToken,
        status = status,
        createdAtEpochMs = createdAtEpochMs
    )
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val targetUid: String,
    val category: String,
    val titleAr: String,
    val bodyAr: String,
    val mediaId: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): AppNotification = AppNotification(
        id = id,
        targetUid = targetUid,
        category = category,
        titleAr = titleAr,
        bodyAr = bodyAr,
        mediaId = mediaId,
        createdAtEpochMs = createdAtEpochMs
    )
}

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val targetId: String,
    val targetType: String,
    val reason: String,
    val status: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): Report = Report(
        id = id,
        reporterId = reporterId,
        targetId = targetId,
        targetType = targetType,
        reason = reason,
        status = status,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val subject: String,
    val message: String,
    val isVipPriority: Boolean,
    val status: String,
    val adminReply: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun toDomain(): SupportTicket = SupportTicket(
        id = id,
        userId = userId,
        subject = subject,
        message = message,
        isVipPriority = isVipPriority,
        status = status,
        adminReply = adminReply,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

@Entity(tableName = "playback_errors")
data class PlaybackErrorEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val mediaId: String,
    val errorCode: String,
    val errorMessage: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): PlaybackErrorLog = PlaybackErrorLog(
        id = id,
        userId = userId,
        mediaId = mediaId,
        errorCode = errorCode,
        errorMessage = errorMessage,
        createdAtEpochMs = createdAtEpochMs
    )
}

@Entity(tableName = "admin_records")
data class AdminRecordEntity(
    @PrimaryKey val id: String,
    val adminUid: String,
    val email: String,
    val role: String,
    val grantedBy: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): AdminRecord = AdminRecord(
        id = id,
        adminUid = adminUid,
        email = email,
        role = role,
        grantedBy = grantedBy,
        createdAtEpochMs = createdAtEpochMs
    )
}

@Entity(tableName = "admin_audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey val id: String,
    val adminUid: String,
    val action: String,
    val targetId: String,
    val details: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): AdminAuditLog = AdminAuditLog(
        id = id,
        adminUid = adminUid,
        action = action,
        targetId = targetId,
        details = details,
        createdAtEpochMs = createdAtEpochMs
    )
}
