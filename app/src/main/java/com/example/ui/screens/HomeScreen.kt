package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppNotification
import com.example.data.model.MediaItem
import com.example.data.model.UiState
import com.example.ui.components.*
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalMovioraStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovioraViewModel
import com.example.ui.viewmodel.PrimaryTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MovioraViewModel) {
    val strings = LocalMovioraStrings.current
    val lang = LocalAppLanguage.current

    val catalogueState by viewModel.catalogueState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val videoSources by viewModel.videoSources.collectAsState()

    val favoriteIds = remember(favorites) { favorites.map { it.mediaId }.toSet() }
    val progressByMedia = remember(watchHistory) {
        watchHistory.associate { entry ->
            val frac = if (entry.durationMs > 0) entry.positionMs.toFloat() / entry.durationMs.toFloat() else 0f
            entry.mediaId to frac
        }
    }

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var selectedCountryQuick by remember { mutableStateOf<String?>(null) }

    when (val state = catalogueState) {
        is UiState.Loading -> ShimmerLoadingGrid()
        is UiState.Error -> EmptyOrErrorCard(
            icon = Icons.Default.ErrorOutline,
            title = "تعذر تحميل الصفحة الرئيسية",
            subtitle = state.message,
            actionLabel = strings.retry,
            onAction = { viewModel.retryCatalogue() }
        )
        is UiState.Success -> {
            val allItems = state.data
            val playableLookup = remember(allItems, videoSources) {
                allItems.associate { it.id to viewModel.resolveStreamFor(it).isPlayable }
            }

            val featuredList = remember(allItems) {
                val licensed = allItems.filter { it.streamStatus == "LICENSED_ACTIVE" }
                val archiveTop = allItems.filter { it.archiveSection == "SOVIET" || it.archiveSection == "WWII" }.take(3)
                (licensed.take(4) + archiveTop).distinctBy { it.id }
            }
            var featuredIndex by remember { mutableIntStateOf(0) }
            val heroItem = featuredList.getOrNull(featuredIndex % featuredList.size.coerceAtLeast(1))

            val continueItems = remember(allItems, watchHistory) {
                val byId = allItems.associateBy { it.id }
                watchHistory.filter { !it.completed && it.positionMs > 3_000L }
                    .mapNotNull { byId[it.mediaId] }
                    .distinctBy { it.id }
            }
            val favoriteMediaItems = remember(allItems, favoriteIds) {
                allItems.filter { favoriteIds.contains(it.id) }
            }
            val sovietItems = remember(allItems) { allItems.filter { it.archiveSection == "SOVIET" } }
            val russianWarItems = remember(allItems) { allItems.filter { it.archiveSection == "RUSSIAN_WAR" } }
            val ww1Items = remember(allItems) {
                allItems.filter { it.archiveSection == "WWI" || it.warConflict.contains("الأولى") }
            }
            val ww2Items = remember(allItems) {
                allItems.filter { it.archiveSection == "WWII" || it.warConflict.contains("الثانية") || it.warConflict.contains("الوطنية العظمى") }
            }
            val militaryItems = remember(allItems) {
                allItems.filter { it.archiveSection == "MILITARY" || it.genres.any { g -> g.contains("عسكرية") || g.contains("حربية") } }
            }
            val historicalItems = remember(allItems) {
                allItems.filter { it.genres.any { g -> g.contains("تاريخ") } || it.archiveSection.isNotBlank() }
            }
            val documentaryItems = remember(allItems) {
                allItems.filter { it.kind == "documentary" || it.genres.any { g -> g.contains("وثائق") } }
            }
            val latestMovies = remember(allItems) {
                allItems.filter { it.kind == "movie" }.sortedByDescending { it.year }
            }
            val latestSeries = remember(allItems) {
                allItems.filter { it.kind == "series" }.sortedByDescending { it.year }
            }
            val mostWatched = remember(allItems) { allItems.sortedByDescending { it.viewsCount }.take(15) }
            val topRated = remember(allItems) { allItems.sortedByDescending { it.rating }.take(15) }
            val trending = remember(allItems) {
                allItems.filter { playableLookup[it.id] == true } + allItems.sortedByDescending { it.viewsCount }.take(10)
            }.distinctBy { it.id }
            val movioraPicks = remember(allItems) {
                allItems.filter { it.rating >= 8.4 }.shuffled().take(12)
            }
            val newReleases = remember(allItems) {
                allItems.sortedByDescending { item ->
                    if (item.createdAtEpochMs > 0L) item.createdAtEpochMs else item.year.toLong() * 1_000_000L
                }.take(12)
            }
            val vipItems = remember(allItems) {
                val flagged = allItems.filter { it.isVipExclusive }
                if (flagged.isNotEmpty()) flagged else allItems.filter { playableLookup[it.id] == true }.take(8)
            }
            val personalized = remember(allItems, favoriteMediaItems, continueItems) {
                val seedGenres = (favoriteMediaItems + continueItems).flatMap { it.genres }.toSet()
                if (seedGenres.isEmpty()) {
                    allItems.sortedByDescending { it.rating }.take(12)
                } else {
                    allItems.filter { m -> m.genres.any { it in seedGenres } && m.id !in favoriteIds }.take(14)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_lazy_column"),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Top Cinema Header + Search Bar
                item {
                    HomeTopHeader(
                        isVipActive = subscription?.isCurrentlyActive == true,
                        unreadNotifications = notifications.size,
                        searchPlaceholder = strings.searchPlaceholder,
                        onOpenSearch = { viewModel.selectTab(PrimaryTab.LIBRARY) },
                        onOpenVip = { viewModel.selectTab(PrimaryTab.VIP) },
                        onOpenNotifications = { showNotificationsSheet = true }
                    )
                }

                // 2. Featured Hero Banner
                if (heroItem != null) {
                    item {
                        HeroFeaturedBanner(
                            item = heroItem,
                            lang = lang,
                            isPlayable = playableLookup[heroItem.id] == true,
                            isFavorite = favoriteIds.contains(heroItem.id),
                            featuredCount = featuredList.size,
                            selectedIndex = featuredIndex,
                            onSelectIndex = { featuredIndex = it },
                            onOpenDetail = { viewModel.openMediaDetail(heroItem.id) },
                            onWatchNow = {
                                if (playableLookup[heroItem.id] == true) {
                                    viewModel.openPlayer(heroItem.id)
                                } else {
                                    viewModel.openMediaDetail(heroItem.id)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(heroItem) }
                        )
                    }
                }

                // 3. Prominent Gateway: MOVIORA HISTORY & WORLD WAR ARCHIVE
                item {
                    ArchiveGatewayCard(
                        sovietCount = sovietItems.size,
                        russianCount = russianWarItems.size,
                        onExploreArchive = { viewModel.selectTab(PrimaryTab.ARCHIVE) }
                    )
                }

                // 4. Continue Watching
                if (continueItems.isNotEmpty()) {
                    item {
                        MediaHorizontalRow(
                            title = strings.continueWatching,
                            badge = "${continueItems.size}",
                            items = continueItems,
                            playableLookup = playableLookup,
                            favoriteIds = favoriteIds,
                            progressByMedia = progressByMedia,
                            onOpenMedia = viewModel::openMediaDetail,
                            onToggleFav = viewModel::toggleFavorite
                        )
                    }
                }

                // 5. Favorites Row
                if (favoriteMediaItems.isNotEmpty()) {
                    item {
                        MediaHorizontalRow(
                            title = strings.myFavorites,
                            badge = "${favoriteMediaItems.size}",
                            items = favoriteMediaItems,
                            playableLookup = playableLookup,
                            favoriteIds = favoriteIds,
                            progressByMedia = progressByMedia,
                            onOpenMedia = viewModel::openMediaDetail,
                            onToggleFav = viewModel::toggleFavorite,
                            onSeeAll = { viewModel.selectTab(PrimaryTab.SAVED) }
                        )
                    }
                }

                // 6. Licensed Playable Stream Row (Trending / Instant Watch)
                item {
                    MediaHorizontalRow(
                        title = strings.trendingNow,
                        subtitle = "بث مرخص فوري بجودة متكيفة HLS / MP4",
                        items = trending,
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite,
                        onSeeAll = {
                            viewModel.updateFilter { it.copy(onlyPlayableNow = true) }
                            viewModel.selectTab(PrimaryTab.LIBRARY)
                        }
                    )
                }

                // 7. Soviet Series Archive (20 Series)
                item {
                    MediaHorizontalRow(
                        title = strings.sovietArchive,
                        subtitle = "الكلاسيكيات السوفيتية التاريخية والحربية الكاملة",
                        badge = "${sovietItems.size}",
                        items = sovietItems,
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite,
                        onSeeAll = { viewModel.selectTab(PrimaryTab.ARCHIVE) }
                    )
                }

                // 8. Russian War Series (18 Series)
                item {
                    MediaHorizontalRow(
                        title = strings.russianSeries,
                        subtitle = "أبرز أعمال الدراما العسكرية والاستخباراتية الروسية",
                        badge = "${russianWarItems.size}",
                        items = russianWarItems,
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite,
                        onSeeAll = { viewModel.selectTab(PrimaryTab.ARCHIVE) }
                    )
                }

                // 9. Latest Movies
                if (latestMovies.isNotEmpty()) {
                    item {
                        MediaHorizontalRow(
                            title = strings.latestMovies,
                            items = latestMovies,
                            playableLookup = playableLookup,
                            favoriteIds = favoriteIds,
                            progressByMedia = progressByMedia,
                            onOpenMedia = viewModel::openMediaDetail,
                            onToggleFav = viewModel::toggleFavorite,
                            onSeeAll = {
                                viewModel.updateFilter { it.copy(selectedKind = "movie", onlyPlayableNow = false) }
                                viewModel.selectTab(PrimaryTab.LIBRARY)
                            }
                        )
                    }
                }

                // 10. Latest Series
                if (latestSeries.isNotEmpty()) {
                    item {
                        MediaHorizontalRow(
                            title = strings.latestSeries,
                            items = latestSeries.take(15),
                            playableLookup = playableLookup,
                            favoriteIds = favoriteIds,
                            progressByMedia = progressByMedia,
                            onOpenMedia = viewModel::openMediaDetail,
                            onToggleFav = viewModel::toggleFavorite
                        )
                    }
                }

                // 11. World War II Series
                if (ww2Items.isNotEmpty()) {
                    item {
                        MediaHorizontalRow(
                            title = strings.ww2Series,
                            items = ww2Items.take(16),
                            playableLookup = playableLookup,
                            favoriteIds = favoriteIds,
                            progressByMedia = progressByMedia,
                            onOpenMedia = viewModel::openMediaDetail,
                            onToggleFav = viewModel::toggleFavorite
                        )
                    }
                }

                // 12. World War I Series
                if (ww1Items.isNotEmpty()) {
                    item {
                        MediaHorizontalRow(
                            title = strings.ww1Series,
                            items = ww1Items,
                            playableLookup = playableLookup,
                            favoriteIds = favoriteIds,
                            progressByMedia = progressByMedia,
                            onOpenMedia = viewModel::openMediaDetail,
                            onToggleFav = viewModel::toggleFavorite
                        )
                    }
                }

                // 13. Military & Espionage Series
                if (militaryItems.isNotEmpty()) {
                    item {
                        MediaHorizontalRow(
                            title = strings.militarySeries,
                            items = militaryItems.take(15),
                            playableLookup = playableLookup,
                            favoriteIds = favoriteIds,
                            progressByMedia = progressByMedia,
                            onOpenMedia = viewModel::openMediaDetail,
                            onToggleFav = viewModel::toggleFavorite
                        )
                    }
                }

                // 14. Historical Works
                item {
                    MediaHorizontalRow(
                        title = strings.historicalWorks,
                        items = historicalItems.take(15),
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite
                    )
                }

                // 15. Historical Documentaries
                if (documentaryItems.isNotEmpty()) {
                    item {
                        MediaHorizontalRow(
                            title = strings.documentaries,
                            items = documentaryItems,
                            playableLookup = playableLookup,
                            favoriteIds = favoriteIds,
                            progressByMedia = progressByMedia,
                            onOpenMedia = viewModel::openMediaDetail,
                            onToggleFav = viewModel::toggleFavorite
                        )
                    }
                }

                // 16. Most Watched & Top Rated & MOVIORA Picks & VIP Exclusives & Personalized
                item {
                    MediaHorizontalRow(
                        title = strings.mostWatched,
                        items = mostWatched,
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite
                    )
                }
                item {
                    MediaHorizontalRow(
                        title = strings.topRated,
                        items = topRated,
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite
                    )
                }
                item {
                    MediaHorizontalRow(
                        title = strings.movioraPicks,
                        items = movioraPicks,
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite
                    )
                }
                item {
                    MediaHorizontalRow(
                        title = strings.newThisWeek,
                        items = newReleases,
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite
                    )
                }
                item {
                    MediaHorizontalRow(
                        title = strings.vipExclusives,
                        badge = "VIP",
                        items = vipItems,
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite,
                        onSeeAll = { viewModel.selectTab(PrimaryTab.VIP) }
                    )
                }
                item {
                    MediaHorizontalRow(
                        title = strings.recommendedForYou,
                        items = personalized,
                        playableLookup = playableLookup,
                        favoriteIds = favoriteIds,
                        progressByMedia = progressByMedia,
                        onOpenMedia = viewModel::openMediaDetail,
                        onToggleFav = viewModel::toggleFavorite
                    )
                }

                // 17. Browse by Country, Year & Language Quick Explorer
                item {
                    val countries = remember(allItems) {
                        allItems.map { it.country }.filter { it.isNotBlank() }.distinct()
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MovioraSectionHeader(
                            title = "تصفح حسب الدولة والسنة واللغة",
                            subtitle = "اختر دولة الإنتاج للانتقال الفوري إلى أعمالها في المكتبة"
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(countries) { country ->
                                FilterChip(
                                    selected = selectedCountryQuick == country,
                                    onClick = {
                                        selectedCountryQuick = country
                                        viewModel.updateFilter {
                                            it.copy(selectedCountry = country, selectedKind = "ALL", onlyPlayableNow = false)
                                        }
                                        viewModel.selectTab(PrimaryTab.LIBRARY)
                                    },
                                    label = { Text(country) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(onDismissRequest = { showNotificationsSheet = false }) {
            NotificationsSheetContent(
                notifications = notifications,
                onSelectMedia = { mediaId ->
                    showNotificationsSheet = false
                    if (mediaId.isNotBlank()) viewModel.openMediaDetail(mediaId)
                }
            )
        }
    }
}

@Composable
private fun HomeTopHeader(
    isVipActive: Boolean,
    unreadNotifications: Int,
    searchPlaceholder: String,
    onOpenSearch: () -> Unit,
    onOpenVip: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "MOVIORA",
                    fontFamily = CinzelDisplayFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = 2.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    onClick = onOpenVip,
                    color = if (isVipActive) MovioraGold else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP",
                            tint = if (isVipActive) OnLuxuryGold else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isVipActive) "VIP نشط" else "ترقية VIP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isVipActive) OnLuxuryGold else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("home_notifications_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge(containerColor = MovioraGold, contentColor = Color.Black) {
                                Text("${unreadNotifications.coerceAtMost(99)}")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "الإشعارات",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Quick Search Bar
        Surface(
            onClick = onOpenSearch,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .testTag("home_quick_search_bar")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = searchPlaceholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "فلاتر البحث",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroFeaturedBanner(
    item: MediaItem,
    lang: String,
    isPlayable: Boolean,
    isFavorite: Boolean,
    featuredCount: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onOpenDetail: () -> Unit,
    onWatchNow: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val strings = LocalMovioraStrings.current
    val accent = parseAccentColor(item.accentColorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("hero_banner_card"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 270.dp)
                .background(
                    Brush.linearGradient(
                        listOf(accent, MovioraPurpleDeep, MovioraObsidianBg)
                    )
                )
        ) {
            if (item.backdropUrl.startsWith("https://") || item.posterUrl.startsWith("https://")) {
                AsyncImage(
                    model = item.backdropUrl.ifBlank { item.posterUrl },
                    contentDescription = item.localizedTitle(lang),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.28f),
                                    MaterialTheme.movioraColors.heroScrimMid,
                                    MaterialTheme.movioraColors.heroScrimDeep
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MovioraGold,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isPlayable) "بث مرخص متاح" else "أرشيف MOVIORA الموثق",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnLuxuryGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "${item.year} • ${item.country} • ★ ${String.format("%.1f", item.rating)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MovioraGoldBright
                    )
                }

                Text(
                    text = item.localizedTitle(lang),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MovioraWarmIvory
                )
                Text(
                    text = item.titleOriginal,
                    style = MaterialTheme.typography.labelLarge,
                    color = MovioraGold.copy(alpha = 0.9f)
                )
                Text(
                    text = item.localizedSynopsis(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MovioraWarmIvory.copy(alpha = 0.88f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onWatchNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxuryGold,
                            contentColor = OnLuxuryGold
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("hero_watch_button")
                    ) {
                        Icon(
                            imageVector = if (isPlayable) Icons.Default.PlayArrow else Icons.Default.Info,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPlayable) strings.watchNow else "استعراض السجل الأرشيفي",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleFavorite,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = strings.addToFavorites,
                            tint = MovioraGold
                        )
                    }
                }

                if (featuredCount > 1) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(featuredCount) { index ->
                            val active = index == (selectedIndex % featuredCount)
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (active) 24.dp else 8.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (active) MovioraGold else Color.White.copy(alpha = 0.35f))
                                    .clickable { onSelectIndex(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveGatewayCard(
    sovietCount: Int,
    russianCount: Int,
    onExploreArchive: () -> Unit
) {
    Card(
        onClick = onExploreArchive,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, MovioraGold.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .testTag("home_archive_gateway_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MovioraPanelDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(MovioraPurpleDeep.copy(alpha = 0.85f), MovioraPanelDark)
                    )
                )
                .padding(18.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = MovioraGold.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.6f,
                    center = Offset(size.width * 0.9f, size.height * 0.5f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MovioraGold.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MovioraGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MOVIORA HISTORY & WORLD WAR ARCHIVE",
                        fontFamily = CinzelDisplayFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = MovioraGold
                    )
                    Text(
                        text = "موسوعة المسلسلات السوفيتية ($sovietCount) والروسية الحربية ($russianCount)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MovioraWarmIvory
                    )
                    Text(
                        text = "خرائط الجبهات التفاعلية • الخط الزمني التاريخي • المقارنة بين الوثيقة والدراما",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovioraMutedSilver
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaHorizontalRow(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    items: List<MediaItem>,
    playableLookup: Map<String, Boolean>,
    favoriteIds: Set<String>,
    progressByMedia: Map<String, Float>,
    onOpenMedia: (String) -> Unit,
    onToggleFav: (MediaItem) -> Unit,
    onSeeAll: (() -> Unit)? = null
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MovioraSectionHeader(
            title = title,
            subtitle = subtitle,
            badgeText = badge,
            onSeeAll = onSeeAll
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "${title}_${it.id}" }) { media ->
                MovioraPosterCard(
                    item = media,
                    isPlayable = playableLookup[media.id] == true,
                    isFavorite = favoriteIds.contains(media.id),
                    watchProgressFraction = progressByMedia[media.id] ?: 0f,
                    onClick = { onOpenMedia(media.id) },
                    onToggleFavorite = { onToggleFav(media) }
                )
            }
        }
    }
}

@Composable
private fun NotificationsSheetContent(
    notifications: List<AppNotification>,
    onSelectMedia: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "مركز إشعارات MOVIORA",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (notifications.isEmpty()) {
            Text(
                text = "لا توجد إشعارات جديدة حالياً. ستصلك تنبيهات الحلقات الجديدة والأعمال المضافة هنا.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            notifications.take(20).forEach { notif ->
                Card(
                    onClick = { onSelectMedia(notif.mediaId) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = notif.titleAr,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = notif.bodyAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
