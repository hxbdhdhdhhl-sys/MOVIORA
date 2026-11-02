package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.*
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.data.local.MovioraQualityPreset
import com.example.data.model.Episode
import com.example.data.model.UiState
import com.example.ui.components.parseAccentColor
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovioraViewModel
import kotlinx.coroutines.delay

data class DetectedVideoTrackQuality(
    val height: Int,
    val width: Int,
    val frameRate: Float,
    val bitrate: Int,
    val isHdr: Boolean,
    val label: String
)

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovioraPlayerScreen(
    mediaId: String,
    initialEpisodeId: String,
    viewModel: MovioraViewModel,
    onClosePlayer: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val catalogueState by viewModel.catalogueState.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val clientSettings by viewModel.clientSettings.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()

    val allMedia = (catalogueState as? UiState.Success)?.data ?: emptyList()
    val mediaItem = allMedia.firstOrNull { it.id == mediaId }
    val episodes by remember(mediaId) { viewModel.repository.observeEpisodes(mediaId) }
        .collectAsState(initial = emptyList())

    var currentEpisodeId by remember(initialEpisodeId) { mutableStateOf(initialEpisodeId) }
    val currentEpisode: Episode? = remember(episodes, currentEpisodeId) {
        if (currentEpisodeId.isNotBlank()) episodes.firstOrNull { it.id == currentEpisodeId }
        else episodes.firstOrNull()
    }

    val currentIdx = episodes.indexOfFirst { it.id == currentEpisode?.id }
    val prevEpisode = if (currentIdx > 0) episodes.getOrNull(currentIdx - 1) else null
    val nextEpisode = if (currentIdx >= 0 && currentIdx < episodes.lastIndex) episodes.getOrNull(currentIdx + 1) else null

    if (mediaItem == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MovioraGold)
        }
        return
    }

    val streamInfo = remember(mediaItem, currentEpisode) {
        viewModel.resolveStreamFor(mediaItem, currentEpisode)
    }

    var isLandscapeFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var playerVolume by remember { mutableFloatStateOf(1.0f) }
    var selectedManualHeight by remember { mutableStateOf<Int?>(null) } // null = Smart Auto
    var detectedQualities by remember { mutableStateOf<List<DetectedVideoTrackQuality>>(emptyList()) }
    var activeFormatSummary by remember { mutableStateOf("جاري فحص دقة المصدر...") }
    var showStatsHud by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showEpisodesSheet by remember { mutableStateOf(false) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var liveCueText by remember { mutableStateOf("") }
    var playerErrorText by remember { mutableStateOf<String?>(null) }
    var retryCounter by remember { mutableIntStateOf(0) }
    var autoNextCountdown by remember { mutableStateOf<Int?>(null) }

    val savedResumeMs = remember(watchHistory, mediaId, currentEpisode?.id) {
        val docKey = currentEpisode?.id?.ifBlank { mediaId } ?: mediaId
        watchHistory.firstOrNull { (it.episodeId == docKey || it.mediaId == mediaId) && !it.completed }?.positionMs ?: 0L
    }

    val exoPlayer = remember(context) {
        val trackSelector = DefaultTrackSelector(context)
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
    }

    // Apply MOVIORA Quality Engine constraints whenever preset, network, or manual selection changes
    LaunchedEffect(clientSettings.qualityPreset, networkStatus, selectedManualHeight) {
        val capHeight = selectedManualHeight ?: networkStatus.recommendedMaxVideoHeight(clientSettings)
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, capHeight)
            .build()
    }

    // Load MediaItem into ExoPlayer whenever stream URL or retry changes
    LaunchedEffect(streamInfo.url, retryCounter) {
        if (!streamInfo.isPlayable || streamInfo.url.isBlank()) {
            playerErrorText = "لا يتوفر مصدر بث مرخص نشط لهذا العمل حالياً."
            isBuffering = false
            return@LaunchedEffect
        }
        playerErrorText = null
        isBuffering = true
        autoNextCountdown = null

        val subtitleConfigs = streamInfo.externalSubtitles.map { sub ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                .setMimeType(if (sub.format == "SRT") MimeTypes.APPLICATION_SUBRIP else MimeTypes.TEXT_VTT)
                .setLanguage(sub.languageCode)
                .setLabel(sub.languageLabel)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }

        val mediaBuilder = MediaItem.Builder()
            .setUri(streamInfo.url)
            .setSubtitleConfigurations(subtitleConfigs)

        when (streamInfo.protocol.uppercase()) {
            "HLS" -> mediaBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            "DASH" -> mediaBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
        }

        exoPlayer.setMediaItem(mediaBuilder.build())
        exoPlayer.prepare()
        if (savedResumeMs > 2_000L) {
            exoPlayer.seekTo(savedResumeMs)
        }
        exoPlayer.playWhenReady = true
    }

    // Player Listener: inspect real tracks, cues, errors, and state
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(1L)
                }
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.savePlaybackProgress(
                        mediaId = mediaId,
                        episodeId = currentEpisode?.id ?: "",
                        seasonNumber = currentEpisode?.seasonNumber ?: 1,
                        episodeNumber = currentEpisode?.episodeNumber ?: 1,
                        positionMs = durationMs,
                        durationMs = durationMs
                    )
                    if (nextEpisode != null && clientSettings.autoPlayNextEpisode) {
                        autoNextCountdown = 5
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                val found = mutableListOf<DetectedVideoTrackQuality>()
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until group.length) {
                            val fmt = group.getTrackFormat(i)
                            if (fmt.height > 0) {
                                val hdr = fmt.colorInfo?.let { ColorInfo.isTransferHdr(it) } == true
                                val label = when {
                                    fmt.height >= 2160 -> "4K Ultra HD (${fmt.height}p)"
                                    fmt.height >= 1440 -> "2K QHD (${fmt.height}p)"
                                    fmt.height >= 1080 -> "1080p Full HD"
                                    fmt.height >= 720 -> "720p HD"
                                    fmt.height >= 480 -> "480p SD"
                                    fmt.height >= 360 -> "360p"
                                    fmt.height >= 240 -> "240p"
                                    else -> "${fmt.height}p"
                                } + (if (fmt.frameRate >= 55f) " • 60fps" else "") + (if (hdr) " • HDR" else "")
                                found.add(
                                    DetectedVideoTrackQuality(
                                        height = fmt.height,
                                        width = fmt.width,
                                        frameRate = fmt.frameRate,
                                        bitrate = fmt.bitrate,
                                        isHdr = hdr,
                                        label = label
                                    )
                                )
                            }
                        }
                    }
                }
                detectedQualities = found.distinctBy { it.height }.sortedByDescending { it.height }
                val vFmt = exoPlayer.videoFormat
                if (vFmt != null && vFmt.height > 0) {
                    val fpsTxt = if (vFmt.frameRate > 0) "${vFmt.frameRate.toInt()}fps" else "24fps"
                    val kbpsTxt = if (vFmt.bitrate > 0) "${vFmt.bitrate / 1000} kbps" else "متكيف"
                    activeFormatSummary = "${vFmt.width}×${vFmt.height} • $fpsTxt • $kbpsTxt"
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) {
                    activeFormatSummary = "${videoSize.width}×${videoSize.height} (${streamInfo.protocol})"
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                liveCueText = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n")
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playerErrorText = "تعذر تشغيل المصدر (${error.errorCodeName}): تحقق من اتصال الإنترنت أو اضغط إعادة المحاولة."
                viewModel.logPlayerError(mediaId, error.errorCodeName, error.message ?: "PlaybackException")
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration.coerceAtLeast(1L)
            if (pos > 2_000L) {
                viewModel.savePlaybackProgress(
                    mediaId = mediaId,
                    episodeId = currentEpisode?.id ?: "",
                    seasonNumber = currentEpisode?.seasonNumber ?: 1,
                    episodeNumber = currentEpisode?.episodeNumber ?: 1,
                    positionMs = pos,
                    durationMs = dur
                )
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Periodic progress updater + Firestore checkpoint every 10 seconds
    LaunchedEffect(exoPlayer, currentEpisodeId) {
        var tick = 0
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val d = exoPlayer.duration
            if (d > 0) durationMs = d
            tick++
            if (tick % 20 == 0 && isPlaying && currentPosMs > 3_000L) {
                viewModel.savePlaybackProgress(
                    mediaId = mediaId,
                    episodeId = currentEpisode?.id ?: "",
                    seasonNumber = currentEpisode?.seasonNumber ?: 1,
                    episodeNumber = currentEpisode?.episodeNumber ?: 1,
                    positionMs = currentPosMs,
                    durationMs = durationMs
                )
            }
            delay(500L)
        }
    }

    // Auto-play next episode countdown
    LaunchedEffect(autoNextCountdown) {
        val count = autoNextCountdown ?: return@LaunchedEffect
        if (count <= 0) {
            nextEpisode?.let { currentEpisodeId = it.id }
            autoNextCountdown = null
        } else {
            delay(1000L)
            autoNextCountdown = count - 1
        }
    }

    fun exitCleanup() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onClosePlayer()
    }

    BackHandler(onBack = ::exitCleanup)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
            .testTag("moviora_player_screen")
    ) {
        // Native Media3 Video Surface (with built-in subtitle view disabled so our custom styled subtitle overlay renders cleanly)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    subtitleView?.visibility = android.view.View.GONE
                    player = exoPlayer
                }
            },
            update = { view -> view.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Styled Subtitle Overlay (Size, Color, Offset)
        if (subtitlesEnabled && liveCueText.isNotBlank()) {
            val subColor = parseAccentColor(clientSettings.subtitleColorHex, Color.White)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = if (showControls) 96.dp else 28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = liveCueText,
                    color = subColor,
                    fontSize = clientSettings.subtitleFontSizeSp.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Technical Quality & Network HUD
        if (showStatsHud) {
            Surface(
                color = Color.Black.copy(alpha = 0.82f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 68.dp, start = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MOVIORA QUALITY HUD", fontFamily = CinzelDisplayFamily, fontSize = 11.sp, color = MovioraGold)
                    Text("الدقة النشطة: $activeFormatSummary", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Text("الوضع: ${clientSettings.qualityPreset.labelAr}", style = MaterialTheme.typography.labelSmall, color = MovioraGoldBright)
                    Text("الشبكة: ${networkStatus.transportLabelAr} (${networkStatus.downstreamKbps / 1000} Mbps)", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Text("مزامنة الترجمة: ${clientSettings.subtitleSyncOffsetMs} ms", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        // Buffering Indicator
        if (isBuffering && playerErrorText == null) {
            CircularProgressIndicator(
                color = MovioraGold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Error Recovery Card
        if (playerErrorText != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 420.dp)
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MovioraPanelDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MovioraGold, modifier = Modifier.size(36.dp))
                    Text(playerErrorText ?: "", color = MovioraWarmIvory, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = ::exitCleanup) { Text("رجوع") }
                        Button(
                            onClick = { retryCounter += 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = MovioraGold, contentColor = Color.Black)
                        ) {
                            Text("إعادة المحاولة")
                        }
                    }
                }
            }
        }

        // Auto-play Next Episode Banner
        if (autoNextCountdown != null && nextEpisode != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MovioraPanelElevated)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "الحلقة التالية (${nextEpisode.episodeNumber}) خلال $autoNextCountdown ث...",
                        color = MovioraWarmIvory,
                        style = MaterialTheme.typography.labelLarge
                    )
                    TextButton(onClick = { autoNextCountdown = null }) { Text("إلغاء") }
                    Button(
                        onClick = {
                            autoNextCountdown = null
                            currentEpisodeId = nextEpisode.id
                        }
                    ) { Text("تشغيل الآن") }
                }
            }
        }

        // Controls Overlay
        if (showControls && playerErrorText == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = ::exitCleanup, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                        Column {
                            Text(
                                text = mediaItem.titleAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = currentEpisode?.let { "م${it.seasonNumber} • ح${it.episodeNumber}: ${it.titleAr}" }
                                    ?: "${mediaItem.year} • ${streamInfo.protocol}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MovioraGoldBright
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showStatsHud = !showStatsHud }) {
                            Icon(Icons.Default.Analytics, contentDescription = "إحصائيات الجودة", tint = if (showStatsHud) MovioraGold else Color.White)
                        }
                        IconButton(onClick = { showSubtitleSheet = true }) {
                            Icon(Icons.Default.Subtitles, contentDescription = "الترجمة والصوت", tint = Color.White)
                        }
                        IconButton(
                            onClick = { showQualitySheet = true },
                            modifier = Modifier.testTag("player_quality_button")
                        ) {
                            Icon(Icons.Default.HighQuality, contentDescription = "جودة الفيديو", tint = MovioraGold)
                        }
                        if (episodes.size > 1) {
                            IconButton(onClick = { showEpisodesSheet = true }) {
                                Icon(Icons.Default.FormatListNumbered, contentDescription = "قائمة الحلقات", tint = Color.White)
                            }
                        }
                        IconButton(
                            onClick = {
                                isLandscapeFullscreen = !isLandscapeFullscreen
                                activity?.requestedOrientation = if (isLandscapeFullscreen) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isLandscapeFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "ملء الشاشة",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Transport Controls (Prev Ep, -10s, Play/Pause, +10s, Next Ep)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    if (prevEpisode != null) {
                        IconButton(onClick = { currentEpisodeId = prevEpisode.id }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "الحلقة السابقة", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "رجوع 10 ثوانٍ", tint = Color.White, modifier = Modifier.size(34.dp))
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MovioraGold)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = OnLuxuryGold,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs)) },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "تقديم 10 ثوانٍ", tint = Color.White, modifier = Modifier.size(34.dp))
                    }

                    if (nextEpisode != null) {
                        IconButton(onClick = { currentEpisodeId = nextEpisode.id }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.SkipNext, contentDescription = "الحلقة التالية", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                // Bottom Scrubber, Time, Speed & Volume Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Slider(
                        value = currentPosMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                        onValueChange = { newPos ->
                            currentPosMs = newPos.toLong()
                            exoPlayer.seekTo(newPos.toLong())
                        },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MovioraGold,
                            activeTrackColor = MovioraGold
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatDurationMs(currentPosMs)} / ${formatDurationMs(durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Speed Cycle Button
                            OutlinedButton(
                                onClick = {
                                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                    val next = speeds[(speeds.indexOf(playbackSpeed) + 1) % speeds.size]
                                    playbackSpeed = next
                                    exoPlayer.setPlaybackSpeed(next)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("${playbackSpeed}x", color = MovioraGoldBright, style = MaterialTheme.typography.labelSmall)
                            }

                            // Mute / Volume Toggle
                            IconButton(
                                onClick = {
                                    playerVolume = if (playerVolume > 0f) 0f else 1f
                                    exoPlayer.volume = playerVolume
                                }
                            ) {
                                Icon(
                                    imageVector = if (playerVolume > 0f) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "الصوت",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // MOVIORA QUALITY ENGINE Bottom Sheet (Strictly real tracks + Smart Presets)
    if (showQualitySheet) {
        ModalBottomSheet(onDismissRequest = { showQualitySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "نظام جودة MOVIORA الذكي",
                    style = MaterialTheme.typography.titleLarge,
                    color = MovioraGold
                )
                Text(
                    text = "يتم فحص مسارات الفيديو الحقيقية في المصدر لحظياً — لا تُعرض أي دقة غير متوفرة فعلياً في البث.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("الأوضاع التكيفية الذكية:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MovioraQualityPreset.entries) { preset ->
                        FilterChip(
                            selected = selectedManualHeight == null && clientSettings.qualityPreset == preset,
                            onClick = {
                                selectedManualHeight = null
                                viewModel.setQualityPreset(preset)
                            },
                            label = { Text(preset.labelAr) }
                        )
                    }
                }

                HorizontalDivider()
                Text("الدقات المتاحة فعلياً في هذا المصدر (${detectedQualities.size}):", style = MaterialTheme.typography.labelLarge)

                Surface(
                    onClick = {
                        selectedManualHeight = null
                        showQualitySheet = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedManualHeight == null) MovioraGold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "تلقائي متكيف حسب سرعة اتصالك (Auto)",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                detectedQualities.forEach { q ->
                    Surface(
                        onClick = {
                            selectedManualHeight = q.height
                            showQualitySheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedManualHeight == q.height) MovioraGold.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(q.label, fontWeight = FontWeight.SemiBold)
                            if (q.bitrate > 0) {
                                Text("${q.bitrate / 1000} kbps", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Subtitle & Audio Customization Sheet
    if (showSubtitleSheet) {
        ModalBottomSheet(onDismissRequest = { showSubtitleSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("إعدادات الترجمة والصوت والمزامنة", style = MaterialTheme.typography.titleLarge, color = MovioraGold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل عرض الترجمة")
                    Switch(checked = subtitlesEnabled, onCheckedChange = { subtitlesEnabled = it })
                }

                Text("حجم خط الترجمة: ${clientSettings.subtitleFontSizeSp} sp")
                Slider(
                    value = clientSettings.subtitleFontSizeSp.toFloat(),
                    onValueChange = { sz ->
                        viewModel.updateSubtitlesConfig(
                            lang = clientSettings.preferredSubtitleLang,
                            sizeSp = sz.toInt(),
                            colorHex = clientSettings.subtitleColorHex,
                            offsetMs = clientSettings.subtitleSyncOffsetMs
                        )
                    },
                    valueRange = 14f..28f
                )

                Text("لون نص الترجمة:")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            "#FFFFFF" to "أبيض سينمائي",
                            "#FFD54F" to "ذهبي كلاسيكي",
                            "#80DEEA" to "سماوي مريح",
                            "#A5D6A7" to "أخضر هادئ"
                        )
                    ) { (hex, label) ->
                        FilterChip(
                            selected = clientSettings.subtitleColorHex == hex,
                            onClick = {
                                viewModel.updateSubtitlesConfig(
                                    clientSettings.preferredSubtitleLang,
                                    clientSettings.subtitleFontSizeSp,
                                    hex,
                                    clientSettings.subtitleSyncOffsetMs
                                )
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مزامنة توقيت الترجمة: ${clientSettings.subtitleSyncOffsetMs} ms")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            viewModel.updateSubtitlesConfig(
                                clientSettings.preferredSubtitleLang,
                                clientSettings.subtitleFontSizeSp,
                                clientSettings.subtitleColorHex,
                                clientSettings.subtitleSyncOffsetMs - 250L
                            )
                        }) { Text("-250ms") }
                        OutlinedButton(onClick = {
                            viewModel.updateSubtitlesConfig(
                                clientSettings.preferredSubtitleLang,
                                clientSettings.subtitleFontSizeSp,
                                clientSettings.subtitleColorHex,
                                clientSettings.subtitleSyncOffsetMs + 250L
                            )
                        }) { Text("+250ms") }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Episode Drawer Sheet inside Player
    if (showEpisodesSheet) {
        ModalBottomSheet(onDismissRequest = { showEpisodesSheet = false }) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("اختيار الحلقة", style = MaterialTheme.typography.titleLarge, color = MovioraGold)
                }
                items(episodes, key = { it.id }) { ep ->
                    val active = ep.id == currentEpisode?.id
                    Card(
                        onClick = {
                            currentEpisodeId = ep.id
                            showEpisodesSheet = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (active) MovioraGold.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "م${ep.seasonNumber} • الحلقة ${ep.episodeNumber}: ${ep.titleAr}",
                            modifier = Modifier.padding(14.dp),
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
