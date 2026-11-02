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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalog.MovioraGlobalCatalog
import com.example.data.model.HistoricalTheaterMap
import com.example.data.model.MediaItem
import com.example.data.model.UiState
import com.example.ui.components.MovioraPosterCard
import com.example.ui.components.ShimmerLoadingGrid
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovioraViewModel

@Composable
fun HistoryArchiveScreen(viewModel: MovioraViewModel) {
    val catalogueState by viewModel.catalogueState.collectAsState()
    val people by viewModel.people.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val videoSources by viewModel.videoSources.collectAsState()
    val favoriteIds = remember(favorites) { favorites.map { it.mediaId }.toSet() }

    var activeView by remember { mutableIntStateOf(0) } // 0 = Eras, 1 = Maps, 2 = Timeline, 3 = Figures
    var selectedEra by remember { mutableStateOf("SOVIET") }

    val eraTabs = listOf(
        "SOVIET" to "الأرشيف السوفيتي (20)",
        "RUSSIAN_WAR" to "الحرب الروسية (18)",
        "WWII" to "الحرب العالمية الثانية",
        "WWI" to "الحرب العالمية الأولى",
        "COLD_WAR" to "الحرب الباردة",
        "EMPIRES" to "الإمبراطوريات",
        "ROME" to "روما القديمة",
        "EUROPEAN" to "التاريخ الأوروبي",
        "MILITARY" to "التاريخ العسكري",
        "DOCS" to "الوثائقيات التاريخية"
    )

    when (val state = catalogueState) {
        is UiState.Loading -> ShimmerLoadingGrid()
        is UiState.Error -> Text(state.message, modifier = Modifier.padding(24.dp))
        is UiState.Success -> {
            val allItems = state.data
            val byId = remember(allItems) { allItems.associateBy { it.id } }
            val playableLookup = remember(allItems, videoSources) {
                allItems.associate { it.id to viewModel.resolveStreamFor(it).isPlayable }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history_archive_list"),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Archive Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(MovioraPurpleDeep, MovioraObsidianBg)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "MOVIORA HISTORY ARCHIVE",
                                fontFamily = CinzelDisplayFamily,
                                fontSize = 13.sp,
                                letterSpacing = 2.sp,
                                color = MovioraGold
                            )
                            Text(
                                text = "موسوعة التاريخ العسكري وأرشيف المسلسلات السوفيتية والروسية",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MovioraWarmIvory
                            )
                            Text(
                                text = "توثيق شامل للحقب التاريخية، جبهات الحربين العالميتين، الخرائط الاستراتيجية، والمقارنة المنهجية بين الوقائع التاريخية والمعالجة الدرامية.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MovioraMutedSilver
                            )

                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val modes = listOf(
                                    0 to "الأقسام والحقب",
                                    1 to "الخرائط التفاعلية",
                                    2 to "الخط الزمني والحقيقة/الدراما",
                                    3 to "الشخصيات والمخرجون"
                                )
                                items(modes) { (idx, label) ->
                                    FilterChip(
                                        selected = activeView == idx,
                                        onClick = { activeView = idx },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("archive_mode_$idx")
                                    )
                                }
                            }
                        }
                    }
                }

                when (activeView) {
                    0 -> {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(eraTabs) { (code, title) ->
                                    FilterChip(
                                        selected = selectedEra == code,
                                        onClick = { selectedEra = code },
                                        label = { Text(title) }
                                    )
                                }
                            }
                        }

                        val eraItems = allItems.filter { item ->
                            when (selectedEra) {
                                "SOVIET" -> item.archiveSection == "SOVIET"
                                "RUSSIAN_WAR" -> item.archiveSection == "RUSSIAN_WAR"
                                "WWII" -> item.archiveSection == "WWII" || item.warConflict.contains("الثانية") || item.warConflict.contains("الوطنية العظمى")
                                "WWI" -> item.archiveSection == "WWI" || item.warConflict.contains("الأولى")
                                "COLD_WAR" -> item.archiveSection == "COLD_WAR" || item.warConflict.contains("الباردة")
                                "EMPIRES" -> item.archiveSection == "EMPIRES"
                                "ROME" -> item.archiveSection == "ROME"
                                "EUROPEAN" -> item.archiveSection == "EUROPEAN" || item.genres.contains("مسلسلات أوروبية")
                                "MILITARY" -> item.archiveSection == "MILITARY" || item.genres.any { it.contains("عسكرية") }
                                "DOCS" -> item.kind == "documentary"
                                else -> true
                            }
                        }

                        items(eraItems, key = { it.id }) { media ->
                            ArchiveDetailedRowCard(
                                item = media,
                                isPlayable = playableLookup[media.id] == true,
                                isFavorite = favoriteIds.contains(media.id),
                                onOpen = { viewModel.openMediaDetail(media.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(media) }
                            )
                        }
                    }

                    1 -> {
                        items(MovioraGlobalCatalog.historicalMaps, key = { it.id }) { theaterMap ->
                            InteractiveTheaterMapCard(
                                map = theaterMap,
                                mediaById = byId,
                                playableLookup = playableLookup,
                                favoriteIds = favoriteIds,
                                onOpenMedia = viewModel::openMediaDetail
                            )
                        }
                    }

                    2 -> {
                        items(MovioraGlobalCatalog.historicalTimeline, key = { it.id }) { event ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = MovioraGold.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = event.yearLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MovioraGold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = event.titleAr,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = event.summaryAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Fact vs Drama box
                                    Surface(
                                        color = MovioraPurpleDeep.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FactCheck,
                                                contentDescription = null,
                                                tint = MovioraGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "الحقيقة التاريخية مقابل المعالجة الدرامية",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MovioraGoldBright
                                                )
                                                Text(
                                                    text = event.factVsDramaNoteAr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MovioraWarmIvory
                                                )
                                            }
                                        }
                                    }

                                    val linked = event.linkedMediaIds.mapNotNull { byId[it] }
                                    if (linked.isNotEmpty()) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(linked, key = { "${event.id}_${it.id}" }) { m ->
                                                MovioraPosterCard(
                                                    item = m,
                                                    isPlayable = playableLookup[m.id] == true,
                                                    isFavorite = favoriteIds.contains(m.id),
                                                    width = 138.dp,
                                                    onClick = { viewModel.openMediaDetail(m.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        items(people, key = { it.id }) { person ->
                            Card(
                                onClick = { viewModel.openPersonDetail(person.id, person.nameAr) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MovioraPurpleDeep)
                                            .border(1.5.dp, MovioraGold, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = person.nameAr.take(2),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MovioraGold
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = person.nameAr,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${person.nameOriginalOrEn()} • ${person.country} (${person.birthYear})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MovioraGold
                                        )
                                        Text(
                                            text = person.bioAr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun com.example.data.model.Person.nameOriginalOrEn(): String =
    nameRu.ifBlank { nameEn }

@Composable
private fun ArchiveDetailedRowCard(
    item: MediaItem,
    isPlayable: Boolean,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.titleAr,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.titleOriginal} • ${item.year} • ${item.country}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MovioraGold
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "المفضلة",
                        tint = MovioraGold
                    )
                }
            }

            Text(
                text = item.synopsisAr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.historicalAccuracyNote.isNotBlank()) {
                Text(
                    text = "توثيق تاريخي: ${item.historicalAccuracyNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MovioraGoldBright
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isPlayable) MovioraSuccess.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPlayable) "بث مرخص متاح الآن" else "سجل أرشيفي موثق",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPlayable) MovioraSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "★ ${item.rating} • ${item.seasonsCount} مواسم • ${item.episodesCount} حلقة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InteractiveTheaterMapCard(
    map: HistoricalTheaterMap,
    mediaById: Map<String, MediaItem>,
    playableLookup: Map<String, Boolean>,
    favoriteIds: Set<String>,
    onOpenMedia: (String) -> Unit
) {
    var selectedFrontIndex by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = map.titleAr,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${map.eraAr} — ${map.regionAr}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tactical Canvas Map using DrawScope dimensions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.movioraColors.tacticalMapSurface)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepX = size.width / 8f
                    val stepY = size.height / 5f
                    for (i in 1..7) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(stepX * i, 0f),
                            end = Offset(stepX * i, size.height)
                        )
                    }
                    for (j in 1..4) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, stepY * j),
                            end = Offset(size.width, stepY * j)
                        )
                    }

                    // Strategic front line connecting nodes
                    val points = map.keyFronts.map { (_, coords) ->
                        Offset(coords.first * size.width, coords.second * size.height)
                    }
                    for (idx in 0 until (points.size - 1)) {
                        drawLine(
                            color = MovioraGold.copy(alpha = 0.65f),
                            start = points[idx],
                            end = points[idx + 1],
                            strokeWidth = 3.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                    }

                    points.forEachIndexed { idx, pt ->
                        val active = idx == selectedFrontIndex
                        drawCircle(
                            color = if (active) MovioraGoldBright.copy(alpha = 0.32f) else MovioraRoyalPurple.copy(alpha = 0.25f),
                            radius = if (active) 24f else 14f,
                            center = pt
                        )
                        drawCircle(
                            color = if (active) MovioraGold else MovioraWarmIvory,
                            radius = if (active) 9f else 6f,
                            center = pt
                        )
                    }
                }

                val currentFront = map.keyFronts.getOrNull(selectedFrontIndex)
                if (currentFront != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "المحور المختار: ${currentFront.first}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MovioraGoldBright,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(map.keyFronts.size) { idx ->
                    FilterChip(
                        selected = selectedFrontIndex == idx,
                        onClick = { selectedFrontIndex = idx },
                        label = { Text(map.keyFronts[idx].first) }
                    )
                }
            }

            Text(
                text = map.strategicSummaryAr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val linkedWorks = map.linkedMediaIds.mapNotNull { mediaById[it] }
            if (linkedWorks.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(linkedWorks, key = { "${map.id}_${it.id}" }) { m ->
                        MovioraPosterCard(
                            item = m,
                            isPlayable = playableLookup[m.id] == true,
                            isFavorite = favoriteIds.contains(m.id),
                            width = 136.dp,
                            onClick = { onOpenMedia(m.id) }
                        )
                    }
                }
            }
        }
    }
}
