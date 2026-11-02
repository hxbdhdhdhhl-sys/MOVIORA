package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalog.MovioraArchiveCatalog
import com.example.data.catalog.MovioraGlobalCatalog
import com.example.data.local.db.MovioraDatabase
import com.example.data.model.MediaItem
import com.example.data.repository.MovioraRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: MovioraDatabase
    private lateinit var repository: MovioraRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = MovioraDatabase.createInMemory(context)
        repository = MovioraRepository(
            dao = database.movioraDao(),
            activeLocalUserId = MovioraRepository.DEFAULT_LOCAL_USER_ID
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun verifyMovioraIdentityAndArchiveCompleteness() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MOVIORA", appName)
        assertEquals(20, MovioraArchiveCatalog.sovietArchiveItems.size)
        assertEquals(18, MovioraArchiveCatalog.russianWarArchiveItems.size)
        assertTrue(MovioraGlobalCatalog.allBaselineMedia.size >= 50)
    }

    @Test
    fun verifyStandaloneLocalRepositoryWithoutGoogleSignIn() = runBlocking {
        // 1. Local profile initializes immediately without Google Sign-In
        val profile = repository.ensureUserProfileAndSession().getOrThrow()
        assertEquals(MovioraRepository.DEFAULT_LOCAL_USER_ID, profile.userId)

        // 2. Catalogue emits full baseline + custom admin items locally
        val initialCatalogue = repository.observeCatalogue().first()
        assertTrue(initialCatalogue.size >= 50)

        val customId = repository.saveMediaItemAsAdmin(
            MediaItem(
                id = "custom_local_test_movie",
                titleAr = "فيلم تجريبي داخلي",
                titleOriginal = "Internal Test Film",
                kind = "movie",
                year = 2025,
                country = "روسيا"
            )
        ).getOrThrow()
        val updatedCatalogue = repository.observeCatalogue().first()
        assertTrue(updatedCatalogue.any { it.id == customId })

        // 3. Favorites & Watch History persist locally in Room
        val addedFav = repository.toggleFavorite(customId, "فيلم تجريبي داخلي").getOrThrow()
        assertTrue(addedFav)
        val favorites = repository.observeFavorites(MovioraRepository.DEFAULT_LOCAL_USER_ID).first()
        assertEquals(1, favorites.size)

        repository.saveWatchProgress(
            mediaId = customId,
            episodeId = "",
            seasonNumber = 1,
            episodeNumber = 1,
            positionMs = 45_000L,
            durationMs = 120_000L
        ).getOrThrow()
        val history = repository.observeWatchHistory(MovioraRepository.DEFAULT_LOCAL_USER_ID).first()
        assertEquals(1, history.size)
        assertEquals(45_000L, history.first().positionMs)

        // 4. Instant in-app VIP preview activates locally without external billing
        repository.activateLocalVipForTesting(
            planId = "vip_yearly",
            priceDisplay = "$39.99",
            durationDays = 365
        ).getOrThrow()
        val sub = repository.observeSubscription(MovioraRepository.DEFAULT_LOCAL_USER_ID).first()
        assertNotNull(sub)
        assertTrue(sub!!.isCurrentlyActive)
    }
}
