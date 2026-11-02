package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovioraDao {

    // --- User Profiles & Sessions ---
    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    fun observeUserProfile(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getUserProfileOnce(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles ORDER BY updatedAtEpochMs DESC")
    fun observeAllUsers(): Flow<List<UserProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(entity: UserProfileEntity)

    @Query("SELECT * FROM user_sessions WHERE userId = :userId ORDER BY updatedAtEpochMs DESC")
    fun observeSessions(userId: String): Flow<List<UserSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(entity: UserSessionEntity)

    @Query("DELETE FROM user_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    // --- Media Overrides, Seasons, Episodes, Streams, Subtitles & People ---
    @Query("SELECT * FROM media_overrides WHERE isPublished = 1")
    fun observePublishedMediaOverrides(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_overrides WHERE id = :mediaId LIMIT 1")
    suspend fun getMediaOverrideOnce(mediaId: String): MediaItemEntity?

    @Query("SELECT COUNT(*) FROM media_overrides")
    suspend fun countMediaOverrides(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMediaOverride(entity: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMediaOverrides(entities: List<MediaItemEntity>)

    @Query("DELETE FROM media_overrides WHERE id = :mediaId")
    suspend fun deleteMediaOverride(mediaId: String)

    @Query("SELECT * FROM seasons WHERE seriesId = :seriesId AND isPublished = 1 ORDER BY seasonNumber ASC")
    fun observeSeasons(seriesId: String): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM seasons WHERE id = :seasonId LIMIT 1")
    suspend fun getSeasonOnce(seasonId: String): SeasonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeason(entity: SeasonEntity)

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId AND isPublished = 1 ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun observeEpisodes(seriesId: String): Flow<List<EpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisode(entity: EpisodeEntity)

    @Query("SELECT * FROM video_sources WHERE isPublished = 1 ORDER BY updatedAtEpochMs DESC")
    fun observeVideoSources(): Flow<List<VideoSourceEntity>>

    @Query("SELECT * FROM video_sources WHERE id = :sourceId LIMIT 1")
    suspend fun getVideoSourceOnce(sourceId: String): VideoSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideoSource(entity: VideoSourceEntity)

    @Query("SELECT * FROM subtitle_sources WHERE isPublished = 1 ORDER BY updatedAtEpochMs DESC")
    fun observeSubtitleSources(): Flow<List<SubtitleSourceEntity>>

    @Query("SELECT * FROM subtitle_sources WHERE id = :subId LIMIT 1")
    suspend fun getSubtitleSourceOnce(subId: String): SubtitleSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtitleSource(entity: SubtitleSourceEntity)

    @Query("SELECT * FROM people WHERE isPublished = 1")
    fun observePeople(): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPerson(entity: PersonEntity)

    // --- Favorites, Watch History, Search History, Watchlists ---
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeFavorites(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE id = :favId LIMIT 1")
    suspend fun getFavoriteOnce(favId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :favId")
    suspend fun deleteFavorite(favId: String)

    @Query("SELECT * FROM watch_history WHERE userId = :userId ORDER BY updatedAtEpochMs DESC")
    fun observeWatchHistory(userId: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE id = :entryId LIMIT 1")
    suspend fun getWatchHistoryOnce(entryId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(entity: WatchHistoryEntity)

    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeSearchHistory(userId: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :itemId")
    suspend fun deleteSearchHistoryItem(itemId: String)

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clearSearchHistoryForUser(userId: String)

    @Query("SELECT * FROM watchlists WHERE userId = :userId ORDER BY updatedAtEpochMs DESC")
    fun observeMyWatchlists(userId: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlists WHERE isPublic = 1 ORDER BY updatedAtEpochMs DESC")
    fun observePublicWatchlists(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlists WHERE id = :listId LIMIT 1")
    suspend fun getWatchlistOnce(listId: String): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchlist(entity: WatchlistEntity)

    @Query("DELETE FROM watchlists WHERE id = :listId")
    suspend fun deleteWatchlist(listId: String)

    // --- Reviews, Follows, Reports, Support, Playback Errors ---
    @Query("SELECT * FROM reviews WHERE mediaId = :mediaId AND status = 'approved' ORDER BY updatedAtEpochMs DESC")
    fun observeApprovedReviewsForMedia(mediaId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE id = :reviewId LIMIT 1")
    suspend fun getReviewOnce(reviewId: String): ReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(entity: ReviewEntity)

    @Query("SELECT * FROM follows WHERE followerId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeFollows(userId: String): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows WHERE id = :followId LIMIT 1")
    suspend fun getFollowOnce(followId: String): FollowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollow(entity: FollowEntity)

    @Query("DELETE FROM follows WHERE id = :followId")
    suspend fun deleteFollow(followId: String)

    @Query("SELECT * FROM reports ORDER BY updatedAtEpochMs DESC")
    fun observeAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :reportId LIMIT 1")
    suspend fun getReportOnce(reportId: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReport(entity: ReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSupportTicket(entity: SupportTicketEntity)

    @Query("SELECT * FROM playback_errors ORDER BY createdAtEpochMs DESC")
    fun observePlaybackErrors(): Flow<List<PlaybackErrorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackError(entity: PlaybackErrorEntity)

    // --- Subscriptions, Payments & Notifications ---
    @Query("SELECT * FROM subscriptions WHERE userId = :userId LIMIT 1")
    fun observeSubscription(userId: String): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions WHERE userId = :userId LIMIT 1")
    suspend fun getSubscriptionOnce(userId: String): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions ORDER BY updatedAtEpochMs DESC")
    fun observeAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubscription(entity: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE userId = :userId")
    suspend fun deleteSubscription(userId: String)

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun observePayments(userId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(entity: PaymentEntity)

    @Query("SELECT * FROM notifications WHERE targetUid = 'ALL' OR targetUid = :userId ORDER BY createdAtEpochMs DESC")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun countNotifications(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotification(entity: NotificationEntity)

    // --- Admin Records & Audit Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdminRecord(entity: AdminRecordEntity)

    @Query("SELECT * FROM admin_audit_logs ORDER BY createdAtEpochMs DESC")
    fun observeAuditLogs(): Flow<List<AdminAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(entity: AdminAuditLogEntity)

    // --- Full Local Data Reset ---
    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun clearFavoritesForUser(userId: String)

    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun clearWatchHistoryForUser(userId: String)

    @Query("DELETE FROM watchlists WHERE userId = :userId")
    suspend fun clearWatchlistsForUser(userId: String)

    @Query("DELETE FROM reviews WHERE userId = :userId")
    suspend fun clearReviewsForUser(userId: String)

    @Query("DELETE FROM payments WHERE userId = :userId")
    suspend fun clearPaymentsForUser(userId: String)

    @Query("DELETE FROM media_overrides")
    suspend fun clearAllMediaOverrides()

    @Query("DELETE FROM episodes")
    suspend fun clearAllCustomEpisodes()

    @Query("DELETE FROM seasons")
    suspend fun clearAllCustomSeasons()

    @Query("DELETE FROM video_sources")
    suspend fun clearAllVideoSources()

    @Query("DELETE FROM subtitle_sources")
    suspend fun clearAllSubtitleSources()
}
