package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Episode
import com.example.data.model.Review
import com.example.data.model.Season
import com.example.data.model.UiState
import com.example.ui.components.MovioraPosterCard
import com.example.ui.components.MovioraSectionHeader
import com.example.ui.components.parseAccentColor
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalMovioraStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovioraViewModel
import com.example.ui.viewmodel.PrimaryTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    mediaId: String,
    viewModel: MovioraViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val strings = LocalMovioraStrings.current
    val lang = LocalAppLanguage.current

    val catalogueState by viewModel.catalogueState.collectAsState()
    val people by viewModel.people.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val myWatchlists by viewModel.myWatchlists.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val videoSources by viewModel.videoSources.collectAsState()

    val seasons by remember(mediaId) { viewModel.repository.observeSeasons(mediaId) }
        .collectAsState(initial = emptyList())
    val episodes by remember(mediaId) { viewModel.repository.observeEpisodes(mediaId) }
        .collectAsState(initial = emptyList())
    val reviews by remember(mediaId) { viewModel.repository.observeApprovedReviewsForMedia(mediaId) }
        .collectAsState(initial = emptyList())

    val allItems = (catalogueState as? UiState.Success)?.data ?: emptyList()
    val item = allItems.firstOrNull { it.id == mediaId }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MovioraGold)
        }
        return
    }

    val isFavorite = favorites.any { it.mediaId == item.id }
    val streamInfo = remember(item, videoSources) { viewModel.resolveStreamFor(item) }
    val isVipLocked = item.isVipExclusive && subscription?.isCurrentlyActive != true && !isAdmin

    var selectedSeasonNumber by remember(seasons) {
        mutableIntStateOf(seasons.firstOrNull()?.seasonNumber ?: 1)
    }
    var showWatchlistSheet by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var reportingReview by remember { mutableStateOf<Review?>(null) }
    var reportReason by remember { mutableStateOf("") }

    val historyForMedia = watchHistory.filter { it.mediaId == item.id }
    val resumeEntry = historyForMedia.firstOrNull { !it.completed && it.positionMs > 3_000L }

    val similarItems = remember(allItems, item) {
        allItems.filter { other ->
            other.id != item.id && (
                other.archiveSection == item.archiveSection && item.archiveSection.isNotBlank() ||
                    other.genres.any { it in item.genres }
                )
        }.take(10)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("media_detail_screen"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Cover & Top Navigation
        item {
            val accent = parseAccentColor(item.accentColorHex)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent, MovioraPurpleDeep, MaterialTheme.colorScheme.background)
                        )
                    )
            ) {
                if (item.backdropUrl.startsWith("https://") || item.posterUrl.startsWith("https://")) {
                    AsyncImage(
                        model = item.backdropUrl.ifBlank { item.posterUrl },
                        contentDescription = item.localizedTitle(lang),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MovioraWarmIvory
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "شاهد أو استعرض «${item.titleAr} (${item.titleOriginal} - ${item.year})» عبر منصة MOVIORA السينمائية!"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, strings.shareWork))
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = strings.shareWork, tint = MovioraWarmIvory)
                            }
                        }
                    }

                    Spacer(Modifier.height(36.dp))

                    // Title & Core Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (streamInfo.isPlayable) MovioraGold else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (streamInfo.isPlayable) "بث مرخص (${streamInfo.protocol})" else strings.archiveMetadataOnly,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (streamInfo.isPlayable) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (item.isVipExclusive) {
                            Surface(color = MovioraRoyalPurple, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "حصري VIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = item.localizedTitle(lang),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${item.titleOriginal} • ${item.year} • ${item.country} • اللغة: ${item.originalLanguage}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MovioraGold
                    )

                    // Metadata Pills
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            AssistChip(
                                onClick = {},
                                label = { Text("★ ${String.format("%.1f", item.rating)}") }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        if (item.kind == "movie") "${item.durationMinutes} دقيقة"
                                        else "${item.seasonsCount} مواسم • ${item.episodesCount} حلقة"
                                    )
                                }
                            )
                        }
                        items(item.genres) { genre ->
                            AssistChip(onClick = {}, label = { Text(genre) })
                        }
                    }

                    // Primary Watch / VIP / Archive Action Button
                    Button(
                        onClick = {
                            when {
                                isVipLocked -> viewModel.selectTab(PrimaryTab.VIP)
                                streamInfo.isPlayable -> {
                                    viewModel.openPlayer(item.id, resumeEntry?.episodeId ?: "")
                                }
                                isAdmin -> {
                                    viewModel.selectTab(PrimaryTab.ADMIN)
                                }
                                else -> {
                                    viewModel.postToast("هذا العمل محفوظ كسجل أرشيفي موثق؛ يظهر زر التشغيل الفوري فور ربط مصدر بث مرخص من الإدارة.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (streamInfo.isPlayable && !isVipLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (streamInfo.isPlayable && !isVipLocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("detail_primary_watch_button")
                    ) {
                        Icon(
                            imageVector = when {
                                isVipLocked -> Icons.Default.WorkspacePremium
                                streamInfo.isPlayable -> Icons.Default.PlayArrow
                                else -> Icons.Default.Verified
                            },
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                isVipLocked -> "ترقية إلى MOVIORA VIP للمشاهدة"
                                streamInfo.isPlayable && resumeEntry != null -> strings.resumeWatching
                                streamInfo.isPlayable -> strings.watchNow
                                isAdmin -> "ربط رابط بث مرخص من لوحة الإدارة"
                                else -> strings.archiveMetadataOnly
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Secondary Action Buttons: Favorite, Watchlist, Offline Download
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleFavorite(item) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = MovioraGold
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isFavorite) strings.inFavorites else strings.addToFavorites)
                        }

                        OutlinedButton(
                            onClick = { showWatchlistSheet = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = MovioraGold)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.addToWatchlist)
                        }

                        if (streamInfo.allowOfflineDownload) {
                            OutlinedButton(
                                onClick = { showDownloadDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("detail_download_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = strings.downloadOffline, tint = MovioraGold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Synopsis, License & Historical Accuracy Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("قصة العمل", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                    Text(
                        text = item.localizedSynopsis(lang),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Text(
                        text = "الترجمات المتوفرة: ${item.availableSubtitles.joinToString(" • ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "الصوتيات المتاحة: ${item.availableAudio.joinToString(" • ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "حالة الترخيص: ${streamInfo.licenseLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MovioraGoldBright
                    )

                    if (item.historicalAccuracyNote.isNotBlank()) {
                        Surface(
                            color = MovioraPurpleDeep.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "توثيق MOVIORA التاريخي (${item.historicalEra.ifBlank { item.warConflict }})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MovioraGold
                                )
                                Text(
                                    text = item.historicalAccuracyNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MovioraWarmIvory
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Clickable Director & Cast Section -> Opens PersonDetailScreen
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MovioraSectionHeader(title = strings.castAndCrew)
                val crewNames = remember(item) {
                    (listOfNotNull(item.director.takeIf { it.isNotBlank() }?.let { "المخرج: $it" to it }) +
                        item.cast.map { "تمثيل: $it" to it })
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(crewNames) { (label, rawName) ->
                        val matchedPerson = people.firstOrNull {
                            it.nameAr.contains(rawName) || rawName.contains(it.nameAr) ||
                                it.nameEn.equals(rawName, ignoreCase = true)
                        }
                        Surface(
                            onClick = {
                                viewModel.openPersonDetail(
                                    personId = matchedPerson?.id ?: "person_${rawName.hashCode()}",
                                    fallbackName = rawName
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MovioraGold, modifier = Modifier.size(18.dp))
                                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // 4. Seasons & Episodes Section (for Series & Documentaries)
        if (item.kind != "movie" && seasons.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MovioraSectionHeader(
                        title = strings.seasonsAndEpisodes,
                        badgeText = "${episodes.size} حلقة"
                    )
                    if (item.upcomingSeasonNote.isNotBlank()) {
                        Text(
                            text = item.upcomingSeasonNote,
                            style = MaterialTheme.typography.labelMedium,
                            color = MovioraGold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(seasons, key = { it.id }) { s: Season ->
                            FilterChip(
                                selected = selectedSeasonNumber == s.seasonNumber,
                                onClick = { selectedSeasonNumber = s.seasonNumber },
                                label = { Text(s.titleAr.ifBlank { "الموسم ${s.seasonNumber}" }) }
                            )
                        }
                    }
                }
            }

            val seasonEpisodes = episodes.filter { it.seasonNumber == selectedSeasonNumber }
            items(seasonEpisodes, key = { it.id }) { ep: Episode ->
                val epStream = viewModel.resolveStreamFor(item, ep)
                val epHist = historyForMedia.firstOrNull { it.episodeId == ep.id }
                val epFrac = if (epHist != null && epHist.durationMs > 0) {
                    epHist.positionMs.toFloat() / epHist.durationMs.toFloat()
                } else 0f

                Card(
                    onClick = {
                        if (epStream.isPlayable && !isVipLocked) {
                            viewModel.openPlayer(item.id, ep.id)
                        } else if (isVipLocked) {
                            viewModel.selectTab(PrimaryTab.VIP)
                        } else {
                            viewModel.postToast("هذه الحلقة موثقة أرشيفياً وبانتظار إرفاق مصدر بث مرخص.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (epHist?.completed == true) Icons.Default.CheckCircle
                                        else if (epStream.isPlayable) Icons.Default.PlayArrow
                                        else Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (epHist?.completed == true) MovioraSuccess else MovioraGold
                                    )
                                }
                                Column {
                                    Text(
                                        text = "الحلقة ${ep.episodeNumber}: ${ep.titleAr}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${ep.durationSeconds / 60} دقيقة" +
                                            if (epHist?.completed == true) " • مكتملة" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (epStream.allowOfflineDownload) {
                                IconButton(onClick = { viewModel.startOfflineDownload(item, ep, "720p") }) {
                                    Icon(Icons.Default.Download, contentDescription = "تنزيل الحلقة", tint = MovioraGold)
                                }
                            }
                        }

                        if (ep.descriptionAr.isNotBlank()) {
                            Text(
                                text = ep.descriptionAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (epFrac > 0.02f) {
                            LinearProgressIndicator(
                                progress = { epFrac.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = MovioraGold
                            )
                        }
                    }
                }
            }
        }

        // 5. Community Reviews & Rating Submission
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.reviewsAndRatings, style = MaterialTheme.typography.titleMedium, color = MovioraGold)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (star in 1..5) {
                            IconButton(onClick = { reviewRating = star }) {
                                Icon(
                                    imageVector = if (star <= reviewRating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star نجوم",
                                    tint = MovioraGold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        placeholder = { Text("اكتب مراجعتك النقدية أو انطباعك عن العمل...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_comment_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = {
                            if (reviewComment.trim().length >= 2) {
                                viewModel.submitReview(item.id, reviewRating, reviewComment)
                                reviewComment = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("submit_review_button")
                    ) {
                        Text("نشر المراجعة")
                    }

                    reviews.forEach { rev ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${rev.authorName} • ${"★".repeat(rev.rating)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MovioraGoldBright
                            )
                            Row {
                                TextButton(onClick = { viewModel.toggleFollow(rev.userId, rev.authorName) }) {
                                    Text("متابعة", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { reportingReview = rev }) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = "إبلاغ عن مراجعة",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = rev.comment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 6. Similar Works
        if (similarItems.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MovioraSectionHeader(title = strings.similarWorks)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(similarItems, key = { "sim_${it.id}" }) { sim ->
                            MovioraPosterCard(
                                item = sim,
                                isPlayable = viewModel.resolveStreamFor(sim).isPlayable,
                                isFavorite = favorites.any { it.mediaId == sim.id },
                                onClick = { viewModel.openMediaDetail(sim.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add to Watchlist Modal
    if (showWatchlistSheet) {
        var newListName by remember { mutableStateOf("") }
        var newListPublic by remember { mutableStateOf(false) }
        ModalBottomSheet(onDismissRequest = { showWatchlistSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("حفظ في قوائم المشاهدة", style = MaterialTheme.typography.titleLarge)
                myWatchlists.forEach { wl ->
                    val inList = wl.mediaIds.contains(item.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleItemInWatchlist(wl.id, item.id) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${wl.name} (${if (wl.isPublic) "عامة" else "خاصة"})")
                        Checkbox(
                            checked = inList,
                            onCheckedChange = { viewModel.toggleItemInWatchlist(wl.id, item.id) }
                        )
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("إنشاء قائمة جديدة") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = newListPublic, onCheckedChange = { newListPublic = it })
                    Spacer(Modifier.width(8.dp))
                    Text("قائمة عامة قابلة للمشاركة في مجتمع MOVIORA")
                }
                Button(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            viewModel.createNewWatchlist(newListName, "", newListPublic)
                            newListName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إنشاء القائمة")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Offline Download Quality Selector Dialog
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("اختيار جودة التنزيل المرخص") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("العمل مرخص للتنزيل المحلي للمشاهدة بدون اتصال بالإنترنت:")
                    listOf(
                        "480p" to "توفير المساحة (~220 MB)",
                        "720p" to "جودة متوازنة HD (~480 MB)",
                        "1080p" to "جودة كاملة Full HD (~920 MB)"
                    ).forEach { (q, desc) ->
                        OutlinedButton(
                            onClick = {
                                showDownloadDialog = false
                                viewModel.startOfflineDownload(item, null, q)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$q — $desc")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadDialog = false }) { Text("إغلاق") }
            }
        )
    }

    // Report Review Dialog
    if (reportingReview != null) {
        AlertDialog(
            onDismissRequest = { reportingReview = null },
            title = { Text("الإبلاغ عن محتوى مخالف") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    placeholder = { Text("اذكر سبب البلاغ (حرق أحداث، إساءة، محتوى غير لائق)...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = reportingReview
                        if (target != null && reportReason.trim().length >= 3) {
                            viewModel.reportContent(target.id, "review", reportReason)
                            reportReason = ""
                            reportingReview = null
                        }
                    }
                ) {
                    Text("إرسال البلاغ")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportingReview = null }) { Text("إلغاء") }
            }
        )
    }
}
