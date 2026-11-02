package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserProfileEntity::class,
        UserSessionEntity::class,
        MediaItemEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        VideoSourceEntity::class,
        SubtitleSourceEntity::class,
        PersonEntity::class,
        FavoriteEntity::class,
        WatchHistoryEntity::class,
        SearchHistoryEntity::class,
        WatchlistEntity::class,
        ReviewEntity::class,
        FollowEntity::class,
        SubscriptionEntity::class,
        PaymentEntity::class,
        NotificationEntity::class,
        ReportEntity::class,
        SupportTicketEntity::class,
        PlaybackErrorEntity::class,
        AdminRecordEntity::class,
        AdminAuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(MovioraRoomConverters::class)
abstract class MovioraDatabase : RoomDatabase() {

    abstract fun movioraDao(): MovioraDao

    companion object {
        @Volatile
        private var INSTANCE: MovioraDatabase? = null

        fun getInstance(context: Context): MovioraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MovioraDatabase::class.java,
                    "moviora_internal_local.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): MovioraDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                MovioraDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
