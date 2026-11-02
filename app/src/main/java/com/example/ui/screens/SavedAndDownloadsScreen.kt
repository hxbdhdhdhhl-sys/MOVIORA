package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.download.DownloadState
import com.example.data.model.UiState
import com.example.ui.components.EmptyOrErrorCard
import com.example.ui.components.formatBytesHuman
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovioraViewModel

@Composable
fun SavedAndDownloadsScreen(viewModel: MovioraViewModel) {
    val catalogueState by viewModel.catalogueState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val myWatchlists by viewModel.myWatchlists.collectAsState()
    val publicWatchlists by viewModel.publicWatchlists.collectAsState()
    val follows by viewModel.follows.collectAsState()
    val downloads by viewModel.downloads.collectAsState()

    val allMedia = (catalogueState as? UiState.Success)?.data ?: emptyList()
    val mediaById = remember(allMedia) { allMedia.associateBy { it.id } }

    var activeSection by remember { mutableIntStateOf(0) } // 0=Favs, 1=History, 2=Watchlists, 3=Downloads
    var newListName by remember { mutableStateOf("") }
    var newListDesc by remember { mutableStateOf("") }
    var newListPublic by remember { mutableStateOf(false) }

    val storageSummary = remember(downloads) { viewModel.getStorageSummary() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("saved_and_downloads_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "مكتبتي والتنزيلات الأوفلاين",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(
                    0 to "المفضلة (${favorites.size})",
                    1 to "سجل المشاهدة (${watchHistory.size})",
                    2 to "القوائم والمجتمع (${myWatchlists.size})",
                    3 to "التنزيلات (${downloads.size})"
                )
                items(tabs) { (idx, label) ->
                    FilterChip(
                        selected = activeSection == idx,
                        onClick = { activeSection = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("saved_tab_$idx")
                    )
                }
            }
        }

        when (activeSection) {
            0 -> {
                if (favorites.isEmpty()) {
                    item {
                        EmptyOrErrorCard(
                            icon = Icons.Default.BookmarkBorder,
                            title = "قائمة المفضلة فارغة حالياً",
                            subtitle = "احفظ الأفلام والمسلسلات المفضلة لديك للوصول السريع إليها في أي وقت."
                        )
                    }
                } else {
                    items(favorites, key = { it.id }) { fav ->
                        val item = mediaById[fav.mediaId]
                        Card(
                            onClick = { viewModel.openMediaDetail(fav.mediaId) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item?.titleAr ?: fav.titleAr,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item != null) {
                                        Text(
                                            text = "${item.titleOriginal} • ${item.year} • ${item.country}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MovioraGold
                                        )
                                    }
                                }
                                if (item != null) {
                                    IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "إزالة", tint = MovioraDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                if (watchHistory.isEmpty()) {
                    item {
                        EmptyOrErrorCard(
                            icon = Icons.Default.History,
                            title = "لا يوجد سجل مشاهدة بعد",
                            subtitle = "عند بدء مشاهدة أي فيلم أو حلقة سيتم حفظ موضع التوقف تلقائياً."
                        )
                    }
                } else {
                    items(watchHistory, key = { it.id }) { entry ->
                        val item = mediaById[entry.mediaId]
                        val progress = if (entry.durationMs > 0) {
                            (entry.positionMs.toFloat() / entry.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Card(
                            onClick = {
                                if (item != null && viewModel.resolveStreamFor(item).isPlayable) {
                                    viewModel.openPlayer(entry.mediaId, entry.episodeId)
                                } else {
                                    viewModel.openMediaDetail(entry.mediaId)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item?.titleAr ?: entry.mediaId,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = if (entry.episodeId.isNotBlank()) {
                                                "الموسم ${entry.seasonNumber} • الحلقة ${entry.episodeNumber}"
                                            } else {
                                                "فيلم"
                                            } + if (entry.completed) " • مكتمل" else " • متابعة المشاهدة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MovioraGold
                                        )
                                    }
                                    Icon(Icons.Default.PlayCircleFilled, contentDescription = "تشغيل", tint = MovioraGold)
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = MovioraGold
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("إنشاء قائمة مشاهدة جديدة", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                            OutlinedTextField(
                                value = newListName,
                                onValueChange = { newListName = it },
                                label = { Text("اسم القائمة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newListDesc,
                                onValueChange = { newListDesc = it },
                                label = { Text("وصف مختصر (اختياري)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = newListPublic, onCheckedChange = { newListPublic = it })
                                Spacer(Modifier.width(8.dp))
                                Text("قائمة عامة يراها مجتمع MOVIORA")
                            }
                            Button(
                                onClick = {
                                    if (newListName.isNotBlank()) {
                                        viewModel.createNewWatchlist(newListName, newListDesc, newListPublic)
                                        newListName = ""
                                        newListDesc = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ القائمة")
                            }
                        }
                    }
                }

                items(myWatchlists, key = { "my_${it.id}" }) { wl ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${wl.name} (${wl.mediaIds.size} عمل)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { viewModel.deleteWatchlist(wl.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف القائمة", tint = MovioraDanger)
                                }
                            }
                            if (wl.description.isNotBlank()) {
                                Text(wl.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            wl.mediaIds.mapNotNull { mediaById[it] }.forEach { m ->
                                TextButton(onClick = { viewModel.openMediaDetail(m.id) }) {
                                    Text("• ${m.titleAr} (${m.year})", color = MovioraGold)
                                }
                            }
                        }
                    }
                }

                if (publicWatchlists.isNotEmpty()) {
                    item {
                        Text(
                            text = "قوائم مجتمع MOVIORA العامة",
                            style = MaterialTheme.typography.titleLarge,
                            color = MovioraGold
                        )
                    }
                    items(publicWatchlists, key = { "pub_${it.id}" }) { pub ->
                        val isFollowing = follows.any { it.targetUserId == pub.userId }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pub.name, style = MaterialTheme.typography.titleSmall)
                                    Text("بواسطة: ${pub.ownerName} • ${pub.mediaIds.size} أعمال", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(onClick = { viewModel.toggleFollow(pub.userId, pub.ownerName) }) {
                                    Text(if (isFollowing) "متابَع" else "متابعة")
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Storage Meter Card
                item {
                    Card(
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إدارة مساحة التنزيل الأوفلاين", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                                if (downloads.isNotEmpty()) {
                                    TextButton(onClick = { viewModel.downloadManager.clearAllDownloads() }) {
                                        Text("حذف الكل", color = MovioraDanger)
                                    }
                                }
                            }
                            Text(
                                text = "مستخدم بواسطة MOVIORA: ${formatBytesHuman(storageSummary.movioraUsedBytes)} • المساحة المتاحة في الجهاز: ${formatBytesHuman(storageSummary.deviceFreeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (downloads.isEmpty()) {
                    item {
                        EmptyOrErrorCard(
                            icon = Icons.Default.DownloadForOffline,
                            title = "لا توجد تنزيلات محفوظة",
                            subtitle = "يمكنك تنزيل الأفلام والوثائقيات المرخصة للتنزيل (MP4) ومشاهدتها لاحقاً بدون اتصال بالإنترنت."
                        )
                    }
                } else {
                    items(downloads, key = { it.id }) { dl ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dl.titleAr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${dl.subtitleInfoAr} • الجودة: ${dl.qualityLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MovioraGold
                                        )
                                        Text(
                                            text = "${formatBytesHuman(dl.downloadedBytes)} / ${formatBytesHuman(dl.totalBytes)} • الحالة: ${
                                                when (dl.state) {
                                                    DownloadState.DOWNLOADING -> "جاري التنزيل..."
                                                    DownloadState.PAUSED -> "متوقف مؤقتاً"
                                                    DownloadState.COMPLETED -> "جاهز للمشاهدة أوفلاين"
                                                    DownloadState.FAILED -> "فشل (${dl.errorMessage})"
                                                }
                                            }",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row {
                                        when (dl.state) {
                                            DownloadState.DOWNLOADING -> {
                                                IconButton(onClick = { viewModel.downloadManager.pauseDownload(dl.id) }) {
                                                    Icon(Icons.Default.PauseCircle, contentDescription = "إيقاف مؤقت", tint = MovioraGold)
                                                }
                                            }
                                            DownloadState.PAUSED, DownloadState.FAILED -> {
                                                IconButton(onClick = { viewModel.downloadManager.resumeDownload(dl.id) }) {
                                                    Icon(Icons.Default.PlayCircleOutline, contentDescription = "استئناف", tint = MovioraGold)
                                                }
                                            }
                                            DownloadState.COMPLETED -> {
                                                IconButton(onClick = { viewModel.openPlayer(dl.mediaId, dl.episodeId) }) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = "مشاهدة أوفلاين", tint = MovioraSuccess)
                                                }
                                            }
                                        }
                                        IconButton(onClick = { viewModel.downloadManager.deleteDownload(dl.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الملف", tint = MovioraDanger)
                                        }
                                    }
                                }

                                if (dl.state != DownloadState.COMPLETED) {
                                    LinearProgressIndicator(
                                        progress = { dl.progress },
                                        modifier = Modifier.fillMaxWidth().height(5.dp),
                                        color = MovioraGold
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
