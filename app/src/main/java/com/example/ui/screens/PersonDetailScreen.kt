package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Person
import com.example.data.model.UiState
import com.example.ui.components.MovioraPosterCard
import com.example.ui.theme.MovioraGold
import com.example.ui.theme.MovioraPurpleDeep
import com.example.ui.viewmodel.MovioraViewModel

@Composable
fun PersonDetailScreen(
    personId: String,
    fallbackName: String,
    viewModel: MovioraViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val people by viewModel.people.collectAsState()
    val catalogueState by viewModel.catalogueState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val videoSources by viewModel.videoSources.collectAsState()

    val person = remember(people, personId, fallbackName) {
        people.firstOrNull { it.id == personId }
            ?: people.firstOrNull { fallbackName.isNotBlank() && (it.nameAr.contains(fallbackName) || fallbackName.contains(it.nameAr)) }
            ?: Person(
                id = personId,
                nameAr = fallbackName.ifBlank { "فنان سينمائي" },
                nameEn = fallbackName,
                role = "actor_director",
                country = "السينما العالمية",
                bioAr = "أحد صناع الأعمال الدرامية والتاريخية المسجلة في أرشيف ومكتبة MOVIORA."
            )
    }

    val allMedia = (catalogueState as? UiState.Success)?.data ?: emptyList()
    val filmography = remember(allMedia, person, fallbackName) {
        val searchKeys = listOf(person.nameAr, person.nameEn, fallbackName).filter { it.isNotBlank() }
        allMedia.filter { item ->
            searchKeys.any { key ->
                item.director.contains(key, ignoreCase = true) ||
                    item.cast.any { c -> c.contains(key, ignoreCase = true) } ||
                    person.notableWorks.any { nw -> item.titleAr.contains(nw) }
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("person_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(MovioraPurpleDeep)
                                .border(2.dp, MovioraGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = person.nameAr.take(2),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MovioraGold
                            )
                        }

                        Text(
                            text = person.nameAr,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (person.nameRu.isNotBlank() || person.nameEn.isNotBlank()) {
                            Text(
                                text = listOf(person.nameEn, person.nameRu).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MovioraGold
                            )
                        }
                        Text(
                            text = listOfNotNull(
                                when (person.role) {
                                    "director" -> "مخرج سينمائي"
                                    "actor" -> "ممثل"
                                    else -> "ممثل ومخرج"
                                },
                                person.country.takeIf { it.isNotBlank() },
                                person.birthYear.takeIf { it.isNotBlank() }?.let { "مواليد $it" }
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = person.bioAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (person.notableWorks.isNotEmpty()) {
                            Text(
                                text = "أشهر الأعمال: ${person.notableWorks.joinToString(" ، ")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MovioraGold
                            )
                        }
                    }
                }

                Text(
                    text = "قائمة الأعمال الكاملة في MOVIORA (${filmography.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        items(filmography, key = { it.id }) { work ->
            MovioraPosterCard(
                item = work,
                isPlayable = remember(work, videoSources) { viewModel.resolveStreamFor(work).isPlayable },
                isFavorite = favorites.any { it.mediaId == work.id },
                onClick = { viewModel.openMediaDetail(work.id) },
                onToggleFavorite = { viewModel.toggleFavorite(work) }
            )
        }
    }
}
