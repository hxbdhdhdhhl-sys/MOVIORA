package com.example.data.download

import android.content.Context
import android.net.Uri
import android.os.StatFs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class DownloadState {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class OfflineDownloadItem(
    val id: String,
    val mediaId: String,
    val episodeId: String,
    val titleAr: String,
    val subtitleInfoAr: String,
    val qualityLabel: String,
    val sourceUrl: String,
    val filePath: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val state: DownloadState,
    val errorMessage: String = ""
) {
    val progress: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class DeviceStorageSummary(
    val movioraUsedBytes: Long,
    val deviceFreeBytes: Long,
    val deviceTotalBytes: Long
)

class OfflineDownloadManager(context: Context) {

    private val appContext = context.applicationContext
    private val downloadsDir = File(appContext.filesDir, "moviora_downloads").apply { mkdirs() }
    private val indexFile = File(downloadsDir, "downloads_index.json")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _downloads = MutableStateFlow<List<OfflineDownloadItem>>(emptyList())
    val downloads: StateFlow<List<OfflineDownloadItem>> = _downloads.asStateFlow()

    init {
        loadIndexFromDisk()
    }

    fun getCompletedLocalUri(targetId: String): Uri? {
        val item = _downloads.value.firstOrNull {
            (it.id == targetId || it.mediaId == targetId || (it.episodeId.isNotBlank() && it.episodeId == targetId)) &&
                it.state == DownloadState.COMPLETED
        } ?: return null
        val f = File(item.filePath)
        return if (f.exists() && f.length() > 0L) Uri.fromFile(f) else null
    }

    fun getStorageSummary(): DeviceStorageSummary {
        val used = downloadsDir.listFiles()
            ?.filter { it.isFile && it.name != indexFile.name }
            ?.sumOf { it.length() } ?: 0L
        val stat = StatFs(appContext.filesDir.absolutePath)
        return DeviceStorageSummary(
            movioraUsedBytes = used,
            deviceFreeBytes = stat.availableBytes,
            deviceTotalBytes = stat.totalBytes
        )
    }

    fun startLicensedDownload(
        id: String,
        mediaId: String,
        episodeId: String,
        titleAr: String,
        subtitleInfoAr: String,
        qualityLabel: String,
        sourceUrl: String,
        isDownloadAllowed: Boolean
    ): Result<Unit> {
        if (!isDownloadAllowed) {
            return Result.failure(IllegalStateException("التنزيل دون اتصال غير متاح بموجب رخصة هذا العمل."))
        }
        if (!sourceUrl.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("يتطلب التنزيل رابط HTTPS مرخصاً وصالحاً."))
        }
        if (sourceUrl.substringBefore("?").endsWith(".m3u8", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("هذا المصدر يعمل ببث HLS المتكيف المباشر؛ للتنزيل المحلي اختر عملاً مرخصاً بصيغة ملف مباشر MP4 أو اربط ملف MP4 مرخص من لوحة الإدارة.")
            )
        }
        if (activeJobs.containsKey(id)) return Result.success(Unit)

        val safeName = id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_") + "_${qualityLabel}.mp4"
        val targetFile = File(downloadsDir, safeName)
        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L

        val initialItem = OfflineDownloadItem(
            id = id,
            mediaId = mediaId,
            episodeId = episodeId,
            titleAr = titleAr,
            subtitleInfoAr = subtitleInfoAr,
            qualityLabel = qualityLabel,
            sourceUrl = sourceUrl,
            filePath = targetFile.absolutePath,
            downloadedBytes = existingBytes,
            totalBytes = _downloads.value.firstOrNull { it.id == id }?.totalBytes ?: 0L,
            state = DownloadState.DOWNLOADING
        )
        upsertItem(initialItem)
        launchDownloadWorker(initialItem)
        return Result.success(Unit)
    }

    fun pauseDownload(id: String) {
        activeJobs.remove(id)?.cancel()
        val existing = _downloads.value.firstOrNull { it.id == id } ?: return
        if (existing.state == DownloadState.DOWNLOADING) {
            upsertItem(existing.copy(state = DownloadState.PAUSED))
        }
    }

    fun resumeDownload(id: String) {
        val existing = _downloads.value.firstOrNull { it.id == id } ?: return
        if (existing.state == DownloadState.COMPLETED || activeJobs.containsKey(id)) return
        val updated = existing.copy(state = DownloadState.DOWNLOADING, errorMessage = "")
        upsertItem(updated)
        launchDownloadWorker(updated)
    }

    fun deleteDownload(id: String) {
        activeJobs.remove(id)?.cancel()
        val existing = _downloads.value.firstOrNull { it.id == id }
        if (existing != null) {
            runCatching { File(existing.filePath).delete() }
        }
        _downloads.value = _downloads.value.filterNot { it.id == id }
        persistIndexToDisk()
    }

    fun clearAllDownloads() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        downloadsDir.listFiles()?.forEach { f ->
            if (f.name != indexFile.name) runCatching { f.delete() }
        }
        _downloads.value = emptyList()
        persistIndexToDisk()
    }

    private fun launchDownloadWorker(item: OfflineDownloadItem) {
        val job = scope.launch {
            val file = File(item.filePath)
            var currentOffset = if (file.exists()) file.length() else 0L
            try {
                val reqBuilder = Request.Builder().url(item.sourceUrl)
                if (currentOffset > 0L) {
                    reqBuilder.header("Range", "bytes=$currentOffset-")
                }
                httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                    if (!resp.isSuccessful && resp.code != 206) {
                        if (resp.code == 416 && currentOffset > 0L) {
                            upsertItem(
                                item.copy(
                                    downloadedBytes = currentOffset,
                                    totalBytes = currentOffset,
                                    state = DownloadState.COMPLETED
                                )
                            )
                            return@use
                        }
                        throw IllegalStateException("HTTP ${resp.code}")
                    }
                    val body = resp.body ?: throw IllegalStateException("استجابة فارغة من الخادم")
                    val isPartial = resp.code == 206
                    if (!isPartial) {
                        currentOffset = 0L
                    }
                    val contentLength = body.contentLength()
                    val total = if (contentLength > 0L) currentOffset + contentLength else item.totalBytes

                    RandomAccessFile(file, "rw").use { raf ->
                        raf.seek(currentOffset)
                        val buf = ByteArray(32 * 1024)
                        val input = body.byteStream()
                        var lastEmitMs = 0L
                        while (isActive) {
                            val read = input.read(buf)
                            if (read == -1) break
                            raf.write(buf, 0, read)
                            currentOffset += read
                            val now = System.currentTimeMillis()
                            if (now - lastEmitMs >= 350L) {
                                lastEmitMs = now
                                upsertItem(
                                    item.copy(
                                        downloadedBytes = currentOffset,
                                        totalBytes = total.coerceAtLeast(currentOffset),
                                        state = DownloadState.DOWNLOADING
                                    ),
                                    saveDisk = false
                                )
                            }
                        }
                    }

                    if (isActive) {
                        upsertItem(
                            item.copy(
                                downloadedBytes = currentOffset,
                                totalBytes = currentOffset,
                                state = DownloadState.COMPLETED,
                                errorMessage = ""
                            ),
                            saveDisk = true
                        )
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                upsertItem(
                    item.copy(
                        downloadedBytes = if (file.exists()) file.length() else currentOffset,
                        state = DownloadState.FAILED,
                        errorMessage = e.localizedMessage ?: "تعذر إكمال التنزيل بسبب انقطاع الشبكة"
                    ),
                    saveDisk = true
                )
            } finally {
                activeJobs.remove(item.id)
            }
        }
        activeJobs[item.id] = job
    }

    @Synchronized
    private fun upsertItem(item: OfflineDownloadItem, saveDisk: Boolean = true) {
        val current = _downloads.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) current[idx] = item else current.add(0, item)
        _downloads.value = current
        if (saveDisk) persistIndexToDisk()
    }

    @Synchronized
    private fun persistIndexToDisk() {
        runCatching {
            val arr = JSONArray()
            for (d in _downloads.value) {
                arr.put(
                    JSONObject().apply {
                        put("id", d.id)
                        put("mediaId", d.mediaId)
                        put("episodeId", d.episodeId)
                        put("titleAr", d.titleAr)
                        put("subtitleInfoAr", d.subtitleInfoAr)
                        put("qualityLabel", d.qualityLabel)
                        put("sourceUrl", d.sourceUrl)
                        put("filePath", d.filePath)
                        put("downloadedBytes", d.downloadedBytes)
                        put("totalBytes", d.totalBytes)
                        put("state", if (d.state == DownloadState.DOWNLOADING) DownloadState.PAUSED.name else d.state.name)
                        put("errorMessage", d.errorMessage)
                    }
                )
            }
            indexFile.writeText(arr.toString())
        }
    }

    private fun loadIndexFromDisk() {
        if (!indexFile.exists()) return
        runCatching {
            val arr = JSONArray(indexFile.readText())
            val list = mutableListOf<OfflineDownloadItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val path = o.optString("filePath")
                val file = File(path)
                val stateStr = o.optString("state", DownloadState.PAUSED.name)
                val state = runCatching { DownloadState.valueOf(stateStr) }.getOrDefault(DownloadState.PAUSED)
                if (state == DownloadState.COMPLETED && !file.exists()) continue
                list.add(
                    OfflineDownloadItem(
                        id = o.getString("id"),
                        mediaId = o.optString("mediaId"),
                        episodeId = o.optString("episodeId"),
                        titleAr = o.optString("titleAr"),
                        subtitleInfoAr = o.optString("subtitleInfoAr"),
                        qualityLabel = o.optString("qualityLabel", "720p"),
                        sourceUrl = o.optString("sourceUrl"),
                        filePath = path,
                        downloadedBytes = if (file.exists()) file.length() else o.optLong("downloadedBytes"),
                        totalBytes = o.optLong("totalBytes"),
                        state = state,
                        errorMessage = o.optString("errorMessage")
                    )
                )
            }
            _downloads.value = list
        }
    }
}
