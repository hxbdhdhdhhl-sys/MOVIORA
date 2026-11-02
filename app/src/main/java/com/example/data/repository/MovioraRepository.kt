package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.data.catalog.MovioraArchiveCatalog
import com.example.data.catalog.MovioraGlobalCatalog
import com.example.data.local.db.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MovioraRepository(
    private val dao: MovioraDao,
    private val activeLocalUserId: String = DEFAULT_LOCAL_USER_ID
) {

    constructor(context: Context) : this(
        dao = MovioraDatabase.getInstance(context.applicationContext).movioraDao(),
        activeLocalUserId = DEFAULT_LOCAL_USER_ID
    )

    companion object {
        const val DEFAULT_LOCAL_USER_ID = "local_moviora_owner"
    }

    // --- Local User Profile & Device Session Initialization ---

    suspend fun ensureUserProfileAndSession(): Result<UserProfile> = runCatching {
        val now = System.currentTimeMillis()
        val uid = activeLocalUserId
        val existing = dao.getUserProfileOnce(uid)

        val profileEntity = if (existing == null) {
            val fresh = UserProfileEntity(
                id = uid,
                userId = uid,
                displayName = "مشترك MOVIORA (وضع التجربة الداخلي)",
                email = "internal@moviora.local",
                emailVerified = true,
                photoUrl = "",
                preferredLanguage = "ar",
                preferredSubtitleLang = "ar",
                themeMode = "dark",
                notificationsEnabled = true,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            dao.upsertUserProfile(fresh)
            fresh
        } else {
            existing
        }

        val sessionId = ("android_" + Build.MODEL.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")).take(64)
        dao.upsertSession(
            UserSessionEntity(
                id = sessionId,
                userId = uid,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android Preview Device" }.take(100),
                platform = "Android ${Build.VERSION.RELEASE} (وضع داخلي مستقل)".take(60),
                isActive = true,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )

        if (dao.countNotifications() == 0) {
            dao.upsertNotification(
                NotificationEntity(
                    id = "notif_welcome_local",
                    targetUid = "ALL",
                    category = "new_release",
                    titleAr = "مرحباً بك في MOVIORA (الوضع الداخلي المستقل)",
                    bodyAr = "جميع أقسام المكتبة العالمية، الأرشيف السوفيتي والروسي، المشغل السينمائي، القوائم، ولوحة الإدارة متاحة الآن للتجربة المباشرة بدون الحاجة لتسجيل دخول.",
                    mediaId = "",
                    createdAtEpochMs = now
                )
            )
        }

        profileEntity.toDomain()
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        dao.observeUserProfile(userId).map { it?.toDomain() }

    suspend fun updateUserProfile(
        displayName: String,
        photoUrl: String,
        preferredLanguage: String,
        preferredSubtitleLang: String,
        themeMode: String,
        notificationsEnabled: Boolean
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val uid = activeLocalUserId
        val existing = dao.getUserProfileOnce(uid)
        val safePhoto = photoUrl.trim().takeIf { it.isEmpty() || it.startsWith("https://") } ?: ""
        dao.upsertUserProfile(
            UserProfileEntity(
                id = uid,
                userId = uid,
                displayName = displayName.trim().take(80).ifBlank { "مشترك MOVIORA" },
                email = existing?.email ?: "internal@moviora.local",
                emailVerified = true,
                photoUrl = safePhoto,
                preferredLanguage = preferredLanguage.take(12),
                preferredSubtitleLang = preferredSubtitleLang.take(12),
                themeMode = themeMode,
                notificationsEnabled = notificationsEnabled,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
    }

    fun observeSessions(userId: String): Flow<List<UserSession>> =
        dao.observeSessions(userId).map { list -> list.map { it.toDomain() } }

    suspend fun revokeSession(sessionId: String): Result<Unit> = runCatching {
        dao.deleteSession(sessionId)
    }

    suspend fun deleteAccountData(): Result<Unit> = resetLocalAppData()

    suspend fun resetLocalAppData(): Result<Unit> = runCatching {
        val uid = activeLocalUserId
        dao.clearFavoritesForUser(uid)
        dao.clearWatchHistoryForUser(uid)
        dao.clearSearchHistoryForUser(uid)
        dao.clearWatchlistsForUser(uid)
        dao.clearReviewsForUser(uid)
        dao.deleteSubscription(uid)
        dao.clearPaymentsForUser(uid)
        dao.clearAllMediaOverrides()
        dao.clearAllCustomEpisodes()
        dao.clearAllCustomSeasons()
        dao.clearAllVideoSources()
        dao.clearAllSubtitleSources()
        ensureUserProfileAndSession()
    }

    // --- Catalogue Observation & Local Admin Overrides ---

    fun observeCatalogue(): Flow<List<MediaItem>> =
        dao.observePublishedMediaOverrides().map { overrides ->
            val localItems = overrides.map { it.toDomain() }
            val localById = localItems.associateBy { it.id }
            val merged = LinkedHashMap<String, MediaItem>()

            for (base in MovioraGlobalCatalog.allBaselineMedia) {
                val overrideItem = localById[base.id]
                if (overrideItem != null) {
                    if (!overrideItem.isDeleted) merged[base.id] = overrideItem
                } else {
                    merged[base.id] = base
                }
            }
            for (custom in localItems) {
                if (!custom.isDeleted && !merged.containsKey(custom.id)) {
                    merged[custom.id] = custom
                }
            }
            merged.values.toList()
        }

    fun observeSeasons(seriesId: String): Flow<List<Season>> =
        dao.observeSeasons(seriesId).map { entities ->
            if (entities.isNotEmpty()) {
                entities.map { it.toDomain() }.sortedBy { it.seasonNumber }
            } else {
                val (defaultSeasons, _) = MovioraArchiveCatalog.generateDefaultSeasonsAndEpisodes(
                    MovioraGlobalCatalog.allBaselineMedia.filter { it.id == seriesId }
                )
                defaultSeasons.sortedBy { it.seasonNumber }
            }
        }

    fun observeEpisodes(seriesId: String): Flow<List<Episode>> =
        dao.observeEpisodes(seriesId).map { entities ->
            val customMap = entities.map { it.toDomain() }
                .associateBy { "${it.seasonNumber}_${it.episodeNumber}" }
            val (_, defaultEps) = MovioraArchiveCatalog.generateDefaultSeasonsAndEpisodes(
                MovioraGlobalCatalog.allBaselineMedia.filter { it.id == seriesId }
            )
            val combined = LinkedHashMap<String, Episode>()
            for (ep in defaultEps) combined["${ep.seasonNumber}_${ep.episodeNumber}"] = ep
            for ((k, ep) in customMap) combined[k] = ep
            combined.values.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
        }

    fun observeVideoSources(): Flow<List<VideoSource>> =
        dao.observeVideoSources().map { list -> list.map { it.toDomain() } }

    fun observeSubtitleSources(): Flow<List<SubtitleSource>> =
        dao.observeSubtitleSources().map { list -> list.map { it.toDomain() } }

    fun observePeople(): Flow<List<Person>> =
        dao.observePeople().map { entities ->
            val map = LinkedHashMap<String, Person>()
            MovioraGlobalCatalog.baselinePeople.forEach { map[it.id] = it }
            entities.forEach { map[it.id] = it.toDomain() }
            map.values.toList()
        }

    // --- Local Admin Operations ---

    suspend fun checkIsAdmin(): Boolean = true

    suspend fun seedCatalogueIfAdminAndEmpty(): Result<Int> = runCatching {
        if (dao.countMediaOverrides() > 0) return@runCatching 0
        val now = System.currentTimeMillis()
        val entities = MovioraGlobalCatalog.allBaselineMedia.map { item ->
            val docId = item.id.ifBlank { "media_${UUID.randomUUID().toString().take(8)}" }
            item.toEntity(resolvedId = docId, createdMs = item.year.toLong() * 1_000_000L, updatedMs = now)
        }
        dao.upsertMediaOverrides(entities)
        logAdminAudit("SEED_CATALOGUE", "media_overrides", "تمت تهيئة قاعدة البيانات المحلية بـ ${entities.size} عملاً مرخصاً وأرشيفياً")
        entities.size
    }

    suspend fun saveMediaItemAsAdmin(item: MediaItem): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val docId = item.id.takeIf { it.isNotBlank() } ?: "media_$now"
        val existing = dao.getMediaOverrideOnce(docId)
        val createdMs = existing?.createdAtEpochMs ?: now
        dao.upsertMediaOverride(item.copy(id = docId).toEntity(resolvedId = docId, createdMs = createdMs, updatedMs = now))
        logAdminAudit(
            action = if (existing != null) "UPDATE_MEDIA" else "CREATE_MEDIA",
            targetId = docId,
            details = if (existing != null) "تحديث بيانات العمل: ${item.titleAr}" else "إضافة عمل جديد: ${item.titleAr}"
        )
        docId
    }

    suspend fun deleteMediaItemAsAdmin(mediaId: String, mediaItem: MediaItem?): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val existing = dao.getMediaOverrideOnce(mediaId)
        val baseMatch = MovioraGlobalCatalog.allBaselineMedia.any { it.id == mediaId }
        if (!baseMatch && existing != null) {
            dao.deleteMediaOverride(mediaId)
        } else {
            val template = mediaItem ?: existing?.toDomain()
            if (template != null) {
                dao.upsertMediaOverride(
                    template.copy(id = mediaId, isDeleted = true).toEntity(
                        resolvedId = mediaId,
                        createdMs = existing?.createdAtEpochMs ?: now,
                        updatedMs = now
                    )
                )
            }
        }
        logAdminAudit("DELETE_MEDIA", mediaId, "حذف أو إخفاء العمل: $mediaId")
    }

    suspend fun addEpisodeAsAdmin(episode: Episode): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val epId = "${episode.seriesId}_s${episode.seasonNumber}_e${episode.episodeNumber}"
        val seasonId = "${episode.seriesId}_s${episode.seasonNumber}"
        val existingSeason = dao.getSeasonOnce(seasonId)
        if (existingSeason == null) {
            dao.upsertSeason(
                SeasonEntity(
                    id = seasonId,
                    seriesId = episode.seriesId,
                    seasonNumber = episode.seasonNumber,
                    titleAr = "الموسم ${episode.seasonNumber}",
                    titleEn = "Season ${episode.seasonNumber}",
                    year = 2025,
                    episodesCount = episode.episodeNumber,
                    isUpcoming = false,
                    isPublished = true,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
        }
        dao.upsertEpisode(
            EpisodeEntity(
                id = epId,
                seriesId = episode.seriesId,
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
                titleAr = episode.titleAr,
                titleEn = episode.titleEn,
                descriptionAr = episode.descriptionAr,
                thumbnailUrl = episode.thumbnailUrl,
                durationSeconds = episode.durationSeconds,
                streamUrl = episode.streamUrl,
                streamProtocol = episode.streamProtocol,
                subtitleUrlAr = episode.subtitleUrlAr,
                subtitleUrlEn = episode.subtitleUrlEn,
                isPublished = true,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        logAdminAudit("UPSERT_EPISODE", epId, "حفظ الحلقة ${episode.episodeNumber} للموسم ${episode.seasonNumber}")
        epId
    }

    suspend fun attachLicensedVideoSourceAsAdmin(
        targetId: String,
        targetType: String,
        url: String,
        protocol: String,
        maxQualityLabel: String,
        licenseName: String,
        allowOffline: Boolean
    ): Result<String> = runCatching {
        require(url.startsWith("https://")) { "يجب أن يبدأ رابط البث المرخص بـ https://" }
        val now = System.currentTimeMillis()
        val sourceId = "src_${targetId}_${protocol.lowercase()}"
        val existing = dao.getVideoSourceOnce(sourceId)
        dao.upsertVideoSource(
            VideoSourceEntity(
                id = sourceId,
                targetId = targetId,
                targetType = targetType,
                url = url.trim(),
                protocol = protocol,
                maxQualityLabel = maxQualityLabel,
                licenseName = licenseName.trim(),
                allowOffline = allowOffline,
                isPublished = true,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
        logAdminAudit("ATTACH_STREAM", targetId, "ربط مصدر بث مرخص ($protocol) للهدف $targetId")
        sourceId
    }

    suspend fun attachSubtitleSourceAsAdmin(
        targetId: String,
        languageCode: String,
        languageLabel: String,
        url: String,
        format: String = "VTT"
    ): Result<String> = runCatching {
        require(url.startsWith("https://")) { "يجب أن يبدأ رابط ملف الترجمة بـ https://" }
        val now = System.currentTimeMillis()
        val subId = "sub_${targetId}_${languageCode.lowercase()}"
        val existing = dao.getSubtitleSourceOnce(subId)
        dao.upsertSubtitleSource(
            SubtitleSourceEntity(
                id = subId,
                targetId = targetId,
                languageCode = languageCode,
                languageLabel = languageLabel,
                url = url.trim(),
                format = format,
                isPublished = true,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
        logAdminAudit("ATTACH_SUBTITLE", targetId, "ربط ترجمة ($languageLabel) للعمل $targetId")
        subId
    }

    suspend fun broadcastNotificationAsAdmin(
        targetUid: String,
        category: String,
        titleAr: String,
        bodyAr: String,
        mediaId: String = ""
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val id = "notif_$now"
        dao.upsertNotification(
            NotificationEntity(
                id = id,
                targetUid = targetUid.ifBlank { "ALL" },
                category = category,
                titleAr = titleAr.trim().take(180),
                bodyAr = bodyAr.trim().take(1000),
                mediaId = mediaId,
                createdAtEpochMs = now
            )
        )
        logAdminAudit("SEND_NOTIFICATION", id, "إرسال إشعار: $titleAr")
    }

    suspend fun grantVipAsAdmin(
        targetUserId: String,
        planId: String,
        durationDays: Int
    ): Result<Unit> = runCatching {
        val resolvedUid = targetUserId.ifBlank { activeLocalUserId }
        val now = System.currentTimeMillis()
        val expires = now + durationDays * 86_400_000L
        val existing = dao.getSubscriptionOnce(resolvedUid)
        dao.upsertSubscription(
            SubscriptionEntity(
                id = resolvedUid,
                userId = resolvedUid,
                planId = planId,
                status = "active",
                provider = "admin_grant",
                purchaseToken = "admin_grant_${now}_$resolvedUid",
                priceDisplay = "مفعّل محلياً من الإدارة",
                startsAtEpochMs = now,
                expiresAtEpochMs = expires,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
        logAdminAudit("GRANT_VIP", resolvedUid, "تفعيل باقة $planId لمدة $durationDays يوماً")
    }

    suspend fun addAdminRole(targetUid: String, email: String, role: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val resolvedUid = targetUid.ifBlank { activeLocalUserId }
        dao.upsertAdminRecord(
            AdminRecordEntity(
                id = resolvedUid,
                adminUid = resolvedUid,
                email = email.trim(),
                role = role,
                grantedBy = activeLocalUserId,
                createdAtEpochMs = now
            )
        )
        logAdminAudit("GRANT_ADMIN", resolvedUid, "منح صلاحية $role للمستخدم $email")
    }

    private suspend fun logAdminAudit(action: String, targetId: String, details: String) {
        val now = System.currentTimeMillis()
        val id = "audit_${now}_${UUID.randomUUID().toString().take(5)}"
        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = id,
                adminUid = activeLocalUserId,
                action = action.take(80),
                targetId = targetId.take(128),
                details = details.take(1000),
                createdAtEpochMs = now
            )
        )
    }

    // --- Favorites, Watch History, Search History, Watchlists ---

    fun observeFavorites(userId: String): Flow<List<FavoriteItem>> =
        dao.observeFavorites(userId).map { list -> list.map { it.toDomain() } }

    suspend fun toggleFavorite(mediaId: String, titleAr: String): Result<Boolean> = runCatching {
        val uid = activeLocalUserId
        val favId = "${uid}_$mediaId"
        val existing = dao.getFavoriteOnce(favId)
        if (existing != null) {
            dao.deleteFavorite(favId)
            false
        } else {
            dao.upsertFavorite(
                FavoriteEntity(
                    id = favId,
                    userId = uid,
                    mediaId = mediaId,
                    titleAr = titleAr.take(200),
                    createdAtEpochMs = System.currentTimeMillis()
                )
            )
            true
        }
    }

    fun observeWatchHistory(userId: String): Flow<List<WatchHistoryEntry>> =
        dao.observeWatchHistory(userId).map { list -> list.map { it.toDomain() } }

    suspend fun saveWatchProgress(
        mediaId: String,
        episodeId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        positionMs: Long,
        durationMs: Long
    ): Result<Unit> = runCatching {
        val uid = activeLocalUserId
        val now = System.currentTimeMillis()
        val docId = "${uid}_${episodeId.ifBlank { mediaId }}"
        val safeDuration = durationMs.coerceAtLeast(1L)
        val safePos = positionMs.coerceIn(0L, safeDuration)
        val completed = safeDuration > 5_000L && (safePos.toDouble() / safeDuration.toDouble()) >= 0.92
        val existing = dao.getWatchHistoryOnce(docId)
        dao.upsertWatchHistory(
            WatchHistoryEntity(
                id = docId,
                userId = uid,
                mediaId = mediaId,
                episodeId = episodeId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                positionMs = safePos,
                durationMs = safeDuration,
                completed = completed,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
    }

    fun observeSearchHistory(userId: String): Flow<List<SearchHistoryEntry>> =
        dao.observeSearchHistory(userId).map { list -> list.map { it.toDomain() } }

    suspend fun recordSearchQuery(query: String): Result<Unit> = runCatching {
        val cleaned = query.trim().take(120)
        if (cleaned.length < 2) return@runCatching
        val uid = activeLocalUserId
        val id = "${uid}_q_${cleaned.lowercase().hashCode().toUInt().toString(16)}"
        dao.upsertSearchHistory(
            SearchHistoryEntity(
                id = id,
                userId = uid,
                query = cleaned,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearSearchHistory(itemId: String? = null): Result<Unit> = runCatching {
        if (itemId != null) {
            dao.deleteSearchHistoryItem(itemId)
        } else {
            dao.clearSearchHistoryForUser(activeLocalUserId)
        }
    }

    fun observeMyWatchlists(userId: String): Flow<List<Watchlist>> =
        dao.observeMyWatchlists(userId).map { list -> list.map { it.toDomain() } }

    fun observePublicWatchlists(): Flow<List<Watchlist>> =
        dao.observePublicWatchlists().map { list -> list.map { it.toDomain() } }

    suspend fun createWatchlist(
        name: String,
        description: String,
        isPublic: Boolean,
        ownerName: String
    ): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val listId = "wl_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        dao.upsertWatchlist(
            WatchlistEntity(
                id = listId,
                userId = activeLocalUserId,
                ownerName = ownerName.ifBlank { "مشترك MOVIORA" }.take(80),
                name = name.trim().take(100),
                description = description.trim().take(500),
                isPublic = isPublic,
                mediaIds = emptyList(),
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        listId
    }

    suspend fun toggleMediaInWatchlist(listId: String, mediaId: String): Result<Unit> = runCatching {
        val existing = dao.getWatchlistOnce(listId) ?: return@runCatching
        val current = existing.mediaIds
        val updated = if (current.contains(mediaId)) current - mediaId else (current + mediaId).take(100)
        dao.upsertWatchlist(existing.copy(mediaIds = updated, updatedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun deleteWatchlist(listId: String): Result<Unit> = runCatching {
        dao.deleteWatchlist(listId)
    }

    // --- Reviews, Community Follows, Reports & Support ---

    fun observeApprovedReviewsForMedia(mediaId: String): Flow<List<Review>> =
        dao.observeApprovedReviewsForMedia(mediaId).map { list -> list.map { it.toDomain() } }

    suspend fun submitOrUpdateReview(
        mediaId: String,
        authorName: String,
        rating: Int,
        comment: String
    ): Result<Unit> = runCatching {
        val uid = activeLocalUserId
        val now = System.currentTimeMillis()
        val reviewId = "${uid}_$mediaId"
        val existing = dao.getReviewOnce(reviewId)
        dao.upsertReview(
            ReviewEntity(
                id = reviewId,
                userId = uid,
                authorName = authorName.ifBlank { "مشاهد MOVIORA" }.take(80),
                mediaId = mediaId,
                rating = rating.coerceIn(1, 5),
                comment = comment.trim().take(2000),
                status = "approved",
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
    }

    suspend fun submitReport(targetId: String, targetType: String, reason: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        dao.upsertReport(
            ReportEntity(
                id = reportId,
                reporterId = activeLocalUserId,
                targetId = targetId,
                targetType = targetType,
                reason = reason.trim().take(1000),
                status = "open",
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
    }

    fun observeFollows(userId: String): Flow<List<Follow>> =
        dao.observeFollows(userId).map { list -> list.map { it.toDomain() } }

    suspend fun toggleFollowUser(targetUserId: String, targetName: String): Result<Boolean> = runCatching {
        val uid = activeLocalUserId
        require(uid != targetUserId) { "لا يمكنك متابعة حسابك نفسه" }
        val id = "${uid}_$targetUserId"
        val existing = dao.getFollowOnce(id)
        if (existing != null) {
            dao.deleteFollow(id)
            false
        } else {
            dao.upsertFollow(
                FollowEntity(
                    id = id,
                    followerId = uid,
                    targetUserId = targetUserId,
                    targetName = targetName.take(80),
                    createdAtEpochMs = System.currentTimeMillis()
                )
            )
            true
        }
    }

    // --- Subscriptions, Instant Local VIP Preview, Billing & Notifications ---

    fun observeSubscription(userId: String): Flow<Subscription?> =
        dao.observeSubscription(userId).map { it?.toDomain() }

    fun observePayments(userId: String): Flow<List<Payment>> =
        dao.observePayments(userId).map { list -> list.map { it.toDomain() } }

    suspend fun activateLocalVipForTesting(
        planId: String,
        priceDisplay: String,
        durationDays: Int
    ): Result<Unit> = runCatching {
        val uid = activeLocalUserId
        val now = System.currentTimeMillis()
        val expires = now + durationDays * 86_400_000L
        val orderId = "LOCAL-VIP-${now.toString().takeLast(6)}"
        dao.upsertPayment(
            PaymentEntity(
                id = "pay_$orderId",
                userId = uid,
                planId = planId,
                amountDisplay = "$priceDisplay (تفعيل تجريبي داخلي)",
                currency = "USD",
                orderId = orderId,
                purchaseToken = "local_token_$now",
                status = "verified",
                createdAtEpochMs = now
            )
        )
        val existing = dao.getSubscriptionOnce(uid)
        dao.upsertSubscription(
            SubscriptionEntity(
                id = uid,
                userId = uid,
                planId = planId,
                status = "active",
                provider = "internal_preview",
                purchaseToken = "local_token_$now",
                priceDisplay = priceDisplay,
                startsAtEpochMs = now,
                expiresAtEpochMs = expires,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
    }

    suspend fun deactivateLocalVip(): Result<Unit> = runCatching {
        dao.deleteSubscription(activeLocalUserId)
    }

    suspend fun recordVerifiedGooglePlayPurchase(
        planId: String,
        priceDisplay: String,
        currency: String,
        orderId: String,
        purchaseToken: String
    ): Result<Unit> = runCatching {
        require(purchaseToken.length >= 6) { "رمز شراء Google Play غير صالح" }
        val uid = activeLocalUserId
        val now = System.currentTimeMillis()
        val days = when (planId) {
            "vip_yearly" -> 365L
            "vip_quarterly" -> 90L
            else -> 30L
        }
        val expires = now + days * 86_400_000L
        val payId = "pay_${orderId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}".take(96)
        dao.upsertPayment(
            PaymentEntity(
                id = payId,
                userId = uid,
                planId = planId,
                amountDisplay = priceDisplay,
                currency = currency,
                orderId = orderId,
                purchaseToken = purchaseToken,
                status = "verified",
                createdAtEpochMs = now
            )
        )
        val existing = dao.getSubscriptionOnce(uid)
        dao.upsertSubscription(
            SubscriptionEntity(
                id = uid,
                userId = uid,
                planId = planId,
                status = "active",
                provider = "google_play",
                purchaseToken = purchaseToken,
                priceDisplay = priceDisplay,
                startsAtEpochMs = now,
                expiresAtEpochMs = expires,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
    }

    suspend fun cancelSubscriptionAutoRenew(): Result<Unit> = runCatching {
        val uid = activeLocalUserId
        val existing = dao.getSubscriptionOnce(uid) ?: return@runCatching
        dao.upsertSubscription(existing.copy(status = "cancelled", updatedAtEpochMs = System.currentTimeMillis()))
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        dao.observeNotifications(userId).map { list -> list.map { it.toDomain() } }

    suspend fun submitSupportTicket(
        subject: String,
        message: String,
        isVipPriority: Boolean
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val id = "tkt_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        dao.upsertSupportTicket(
            SupportTicketEntity(
                id = id,
                userId = activeLocalUserId,
                subject = subject.trim().take(160),
                message = message.trim().take(3000),
                isVipPriority = isVipPriority,
                status = "open",
                adminReply = "",
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
    }

    suspend fun logPlaybackError(mediaId: String, errorCode: String, errorMessage: String) {
        val now = System.currentTimeMillis()
        val id = "err_${now}_${UUID.randomUUID().toString().take(4)}"
        dao.insertPlaybackError(
            PlaybackErrorEntity(
                id = id,
                userId = activeLocalUserId,
                mediaId = mediaId.take(128).ifBlank { "unknown" },
                errorCode = errorCode.take(80).ifBlank { "PLAYER_ERR" },
                errorMessage = errorMessage.take(1000).ifBlank { "Playback failure" },
                createdAtEpochMs = now
            )
        )
    }

    // --- Admin Lists (for Local Admin Console) ---

    fun observeOpenReportsAsAdmin(): Flow<List<Report>> =
        dao.observeAllReports().map { list -> list.map { it.toDomain() } }

    suspend fun resolveReportAsAdmin(report: Report, newStatus: String, hideReviewId: String? = null): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val existing = dao.getReportOnce(report.id)
        if (existing != null) {
            dao.upsertReport(existing.copy(status = newStatus, updatedAtEpochMs = now))
        }
        if (hideReviewId != null) {
            val rev = dao.getReviewOnce(hideReviewId)
            if (rev != null) {
                dao.upsertReview(rev.copy(status = "hidden", updatedAtEpochMs = now))
            }
        }
        logAdminAudit("RESOLVE_REPORT", report.id, "معالجة البلاغ بحالة: $newStatus")
    }

    fun observePlaybackErrorsAsAdmin(): Flow<List<PlaybackErrorLog>> =
        dao.observePlaybackErrors().map { list -> list.map { it.toDomain() } }

    fun observeAuditLogsAsAdmin(): Flow<List<AdminAuditLog>> =
        dao.observeAuditLogs().map { list -> list.map { it.toDomain() } }

    fun observeAllUsersAsAdmin(): Flow<List<UserProfile>> =
        dao.observeAllUsers().map { list -> list.map { it.toDomain() } }

    fun observeAllSubscriptionsAsAdmin(): Flow<List<Subscription>> =
        dao.observeAllSubscriptions().map { list -> list.map { it.toDomain() } }
}
