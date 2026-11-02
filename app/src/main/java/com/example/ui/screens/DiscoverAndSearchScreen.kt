package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.ui.components.EmptyOrErrorCard
import com.example.ui.components.MovioraPosterCard
import com.example.ui.components.ShimmerLoadingGrid
import com.example.ui.i18n.LocalMovioraStrings
import com.example.ui.viewmodel.MovioraViewModel

@Composable
fun DiscoverAndSearchScreen(viewModel: MovioraViewModel) {
    val strings = LocalMovioraStrings.current
    val focusManager = LocalFocusManager.current

    val catalogueState by viewModel.catalogueState.collectAsState()
    val filter by viewModel.libraryFilter.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val videoSources by viewModel.videoSources.collectAsState()

    val favoriteIds = remember(favorites) { favorites.map { it.mediaId }.toSet() }

    val allGenres = listOf(
        "ALL" to "كل التصنيفات",
        "أفلام أكشن" to "أفلام أكشن",
        "أفلام تاريخية" to "أفلام تاريخية",
        "أفلام حربية" to "أفلام حربية",
        "أفلام دراما" to "أفلام دراما",
        "أفلام كوميديا" to "أفلام كوميديا",
        "أفلام رومانسية" to "أفلام رومانسية",
        "أفلام خيال علمي" to "أفلام خيال علمي",
        "أفلام مغامرات" to "أفلام مغامرات",
        "أفلام وثائقية" to "أفلام وثائقية",
        "أفلام رعب" to "أفلام رعب",
        "مسلسلات روسية" to "مسلسلات روسية",
        "مسلسلات سوفيتية" to "مسلسلات سوفيتية",
        "مسلسلات تركية" to "مسلسلات تركية",
        "مسلسلات كورية" to "مسلسلات كورية",
        "مسلسلات أوروبية" to "مسلسلات أوروبية",
        "مسلسلات أمريكية" to "مسلسلات أمريكية",
        "مسلسلات تاريخية" to "مسلسلات تاريخية",
        "مسلسلات عسكرية" to "مسلسلات عسكرية",
        "مسلسلات بوليسية" to "مسلسلات بوليسية",
        "مسلسلات سياسية" to "مسلسلات سياسية"
    )

    when (val state = catalogueState) {
        is UiState.Loading -> ShimmerLoadingGrid()
        is UiState.Error -> EmptyOrErrorCard(
            icon = Icons.Default.ErrorOutline,
            title = "تعذر تحميل المكتبة",
            subtitle = state.message,
            actionLabel = strings.retry,
            onAction = { viewModel.retryCatalogue() }
        )
        is UiState.Success -> {
            val allItems = state.data
            val playableLookup = remember(allItems, videoSources) {
                allItems.associate { it.id to viewModel.resolveStreamFor(it).isPlayable }
            }
            val filteredItems = remember(allItems, filter, videoSources) {
                viewModel.filterMediaList(allItems, filter)
            }
            val countries = remember(allItems) {
                listOf("ALL") + allItems.map { it.country }.filter { it.isNotBlank() }.distinct()
            }

            // Live autocomplete suggestions when typing >= 2 chars
            val liveSuggestions = remember(allItems, filter.searchQuery) {
                val q = filter.searchQuery.trim().lowercase()
                if (q.length < 2) emptyList()
                else {
                    (allItems.map { it.titleAr } + allItems.map { it.titleOriginal } + allItems.flatMap { it.cast } + allItems.map { it.director })
                        .filter { it.isNotBlank() && it.lowercase().contains(q) && it.lowercase() != q }
                        .distinct()
                        .take(6)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("discover_grid"),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Smart Search Box
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "مكتبة MOVIORA والبحث الذكي",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        OutlinedTextField(
                            value = filter.searchQuery,
                            onValueChange = { q -> viewModel.updateFilter { it.copy(searchQuery = q) } },
                            placeholder = { Text(strings.searchPlaceholder) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                if (filter.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.updateFilter { it.copy(searchQuery = "") } }
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    viewModel.submitSearchHistory(filter.searchQuery)
                                    focusManager.clearFocus()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("library_search_input")
                        )

                        // Live Suggestions
                        if (liveSuggestions.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(liveSuggestions) { suggestion ->
                                    SuggestionChip(
                                        onClick = {
                                            viewModel.updateFilter { it.copy(searchQuery = suggestion) }
                                            viewModel.submitSearchHistory(suggestion)
                                        },
                                        label = { Text(suggestion) },
                                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                    )
                                }
                            }
                        }

                        // Saved Search History
                        if (filter.searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سجل البحث الأخير",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { viewModel.deleteSearchHistory(null) }) {
                                    Text("مسح الكل", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(searchHistory.take(10), key = { it.id }) { hist ->
                                    InputChip(
                                        selected = false,
                                        onClick = { viewModel.updateFilter { it.copy(searchQuery = hist.query) } },
                                        label = { Text(hist.query) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { viewModel.deleteSearchHistory(hist.id) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Kind + Playable + Sort Row
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val kinds = listOf(
                                "ALL" to "الكل (${allItems.size})",
                                "movie" to "أفلام",
                                "series" to "مسلسلات",
                                "documentary" to "وثائقيات"
                            )
                            items(kinds) { (code, label) ->
                                FilterChip(
                                    selected = filter.selectedKind == code,
                                    onClick = { viewModel.updateFilter { it.copy(selectedKind = code) } },
                                    label = { Text(label) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filter.onlyPlayableNow,
                                    onClick = { viewModel.updateFilter { it.copy(onlyPlayableNow = !it.onlyPlayableNow) } },
                                    label = { Text("بث مرخص جاهز فقط") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }

                        // Genre Filter Row
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allGenres) { (code, label) ->
                                FilterChip(
                                    selected = filter.selectedGenre == code,
                                    onClick = { viewModel.updateFilter { it.copy(selectedGenre = code) } },
                                    label = { Text(label) }
                                )
                            }
                        }

                        // Country & Sort Row
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val sorts = listOf(
                                "RATING" to "الأعلى تقييماً",
                                "LATEST" to "الأحدث",
                                "MOST_WATCHED" to "الأكثر مشاهدة",
                                "OLDEST" to "الكلاسيكيات"
                            )
                            items(sorts) { (code, label) ->
                                AssistChip(
                                    onClick = { viewModel.updateFilter { it.copy(sortBy = code) } },
                                    label = { Text("ترتيب: $label") },
                                    leadingIcon = {
                                        if (filter.sortBy == code) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                            items(countries) { country ->
                                FilterChip(
                                    selected = filter.selectedCountry == country,
                                    onClick = { viewModel.updateFilter { it.copy(selectedCountry = country) } },
                                    label = { Text(if (country == "ALL") "كل الدول" else country) }
                                )
                            }
                        }

                        Text(
                            text = "النتائج المطابقة: ${filteredItems.size} عملاً",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (filteredItems.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyOrErrorCard(
                            icon = Icons.Default.SearchOff,
                            title = "لا توجد أعمال مطابقة للفلاتر الحالية",
                            subtitle = "جرّب تعديل كلمات البحث أو إعادة ضبط فلاتر التصنيف والدولة.",
                            actionLabel = "إعادة ضبط الفلاتر",
                            onAction = {
                                viewModel.updateFilter { com.example.ui.viewmodel.LibraryFilterState() }
                            }
                        )
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        MovioraPosterCard(
                            item = item,
                            isPlayable = playableLookup[item.id] == true,
                            isFavorite = favoriteIds.contains(item.id),
                            onClick = {
                                if (filter.searchQuery.length >= 2) {
                                    viewModel.submitSearchHistory(filter.searchQuery)
                                }
                                viewModel.openMediaDetail(item.id)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(item) }
                        )
                    }
                }
            }
        }
    }
}
