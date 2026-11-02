package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.billing.BillingUiState
import com.example.data.billing.MovioraBillingManager
import com.example.data.billing.VipPlanCatalogItem
import com.example.data.catalog.MovioraGlobalCatalog
import com.example.data.download.DeviceStorageSummary
import com.example.data.download.OfflineDownloadItem
import com.example.data.download.OfflineDownloadManager
import com.example.data.local.MovioraPreferences
import com.example.data.local.MovioraQualityPreset
import com.example.data.local.UserClientSettings
import com.example.data.model.*
import com.example.data.repository.MovioraRepository
import com.example.util.LiveNetworkStatus
import com.example.util.NetworkAndNotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class PrimaryTab {
    HOME,
    LIBRARY,
    ARCHIVE,
    SAVED,
    VIP,
    PROFILE,
    ADMIN
}

sealed interface OverlayScreen {
    data class MediaDetail(val mediaId: String) : OverlayScreen
    data class PersonDetail(val personId: String, val fallbackName: String = "") : OverlayScreen
    data class Player(val mediaId: String, val episodeId: String = "") : OverlayScreen
}

data class ResolvedStreamInfo(
    val isPlayable: Boolean,
    val url: String,
    val protocol: String,
    val licenseLabel: String,
    val allowOfflineDownload: Boolean,
    val externalSubtitles: List<SubtitleSource>
)

data class LibraryFilterState(
    val searchQuery: String = "",
    val selectedKind: String = "ALL", // ALL | movie | series | documentary
    val selectedGenre: String = "ALL",
    val selectedCountry: String = "ALL",
    val selectedEraOrWar: String = "ALL",
    val onlyPlayableNow: Boolean = false,
    val sortBy: String = "RATING" // RATING | LATEST | MOST_WATCHED | OLDEST
)

@OptIn(ExperimentalCoroutinesApi::class)
class MovioraViewModel(
    private val userId: String,
    val repository: MovioraRepository,
    val preferences: MovioraPreferences,
    val downloadManager: OfflineDownloadManager,
    val billingManager: MovioraBillingManager,
    networkFlow: Flow<LiveNetworkStatus>
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(PrimaryTab.HOME)
    val selectedTab: StateFlow<PrimaryTab> = _selectedTab.asStateFlow()

    private val _overlayStack = MutableStateFlow<List<OverlayScreen>>(emptyList())
    val overlayStack: StateFlow<List<OverlayScreen>> = _overlayStack.asStateFlow()

    private val _libraryFilter = MutableStateFlow(LibraryFilterState())
    val libraryFilter: StateFlow<LibraryFilterState> = _libraryFilter.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isAdmin = MutableStateFlow(true)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    val clientSettings: StateFlow<UserClientSettings> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserClientSettings())

    val networkStatus: StateFlow<LiveNetworkStatus> = networkFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveNetworkStatus())

    val billingState: StateFlow<BillingUiState> = billingManager.state
    val downloads: StateFlow<List<OfflineDownloadItem>> = downloadManager.downloads

    private val retryTrigger = MutableStateFlow(0)

    val catalogueState: StateFlow<UiState<List<MediaItem>>> = retryTrigger
        .flatMapLatest {
            repository.observeCatalogue()
                .map<List<MediaItem>, UiState<List<MediaItem>>> { UiState.Success(it) }
                .onStart { emit(UiState.Loading) }
                .catch { e ->
                    if (e is CancellationException) throw e
                    // Fallback gracefully to bundled archive metadata if offline on first run
                    val fallback = MovioraGlobalCatalog.allBaselineMedia
                    if (fallback.isNotEmpty()) {
                        emit(UiState.Success(fallback))
                    } else {
                        emit(UiState.Error(e.localizedMessage ?: "تعذر تحميل المكتبة"))
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val people: StateFlow<List<Person>> = repository.observePeople()
        .catch { if (it is CancellationException) throw it; emit(MovioraGlobalCatalog.baselinePeople) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovioraGlobalCatalog.baselinePeople)

    val videoSources: StateFlow<List<VideoSource>> = repository.observeVideoSources()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subtitleSources: StateFlow<List<SubtitleSource>> = repository.observeSubtitleSources()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(userId)
        .catch { if (it is CancellationException) throw it; emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sessions: StateFlow<List<UserSession>> = repository.observeSessions(userId)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<FavoriteItem>> = repository.observeFavorites(userId)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntry>> = repository.observeWatchHistory(userId)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntry>> = repository.observeSearchHistory(userId)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val myWatchlists: StateFlow<List<Watchlist>> = repository.observeMyWatchlists(userId)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val publicWatchlists: StateFlow<List<Watchlist>> = repository.observePublicWatchlists()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val follows: StateFlow<List<Follow>> = repository.observeFollows(userId)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subscription: StateFlow<Subscription?> = repository.observeSubscription(userId)
        .catch { if (it is CancellationException) throw it; emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val payments: StateFlow<List<Payment>> = repository.observePayments(userId)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(userId)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Admin-only streams gated behind _isAdmin
    val adminReports: StateFlow<List<Report>> = _isAdmin
        .flatMapLatest { admin -> if (admin) repository.observeOpenReportsAsAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val adminPlaybackErrors: StateFlow<List<PlaybackErrorLog>> = _isAdmin
        .flatMapLatest { admin -> if (admin) repository.observePlaybackErrorsAsAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val adminAuditLogs: StateFlow<List<AdminAuditLog>> = _isAdmin
        .flatMapLatest { admin -> if (admin) repository.observeAuditLogsAsAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val adminUsers: StateFlow<List<UserProfile>> = _isAdmin
        .flatMapLatest { admin -> if (admin) repository.observeAllUsersAsAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val adminSubscriptions: StateFlow<List<Subscription>> = _isAdmin
        .flatMapLatest { admin -> if (admin) repository.observeAllSubscriptionsAsAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureUserProfileAndSession()
            val admin = repository.checkIsAdmin()
            _isAdmin.value = admin
            if (admin) {
                repository.seedCatalogueIfAdminAndEmpty()
            }
            refreshCacheSize()
        }
    }

    // --- Navigation Helpers ---

    fun selectTab(tab: PrimaryTab) {
        _overlayStack.value = emptyList()
        _selectedTab.value = tab
    }

    fun openMediaDetail(mediaId: String) {
        _overlayStack.value = _overlayStack.value + OverlayScreen.MediaDetail(mediaId)
    }

    fun openPersonDetail(personId: String, fallbackName: String = "") {
        _overlayStack.value = _overlayStack.value + OverlayScreen.PersonDetail(personId, fallbackName)
    }

    fun openPlayer(mediaId: String, episodeId: String = "") {
        _overlayStack.value = _overlayStack.value + OverlayScreen.Player(mediaId, episodeId)
    }

    fun popOverlay(): Boolean {
        val current = _overlayStack.value
        if (current.isNotEmpty()) {
            _overlayStack.value = current.dropLast(1)
            return true
        }
        if (_selectedTab.value != PrimaryTab.HOME) {
            _selectedTab.value = PrimaryTab.HOME
            return true
        }
        return false
    }

    fun retryCatalogue() {
        retryTrigger.value += 1
    }

    fun postToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // --- Stream Resolution ---

    fun resolveStreamFor(media: MediaItem, episode: Episode? = null): ResolvedStreamInfo {
        val sources = videoSources.value
        val subs = subtitleSources.value
        val epTargetSource = episode?.let { ep -> sources.firstOrNull { it.targetId == ep.id } }
        val mediaTargetSource = sources.firstOrNull { it.targetId == media.id }

        val localUri = downloadManager.getCompletedLocalUri(episode?.id?.ifBlank { media.id } ?: media.id)
        if (localUri != null) {
            return ResolvedStreamInfo(
                isPlayable = true,
                url = localUri.toString(),
                protocol = "MP4",
                licenseLabel = "نسخة مرخصة محفوظة على الجهاز (أوفلاين)",
                allowOfflineDownload = true,
                externalSubtitles = subs.filter { it.targetId == media.id || it.targetId == episode?.id }
            )
        }

        val chosenSource = epTargetSource ?: mediaTargetSource
        if (chosenSource != null && chosenSource.url.startsWith("https://")) {
            return ResolvedStreamInfo(
                isPlayable = true,
                url = chosenSource.url,
                protocol = chosenSource.protocol,
                licenseLabel = chosenSource.licenseName,
                allowOfflineDownload = chosenSource.allowOffline,
                externalSubtitles = subs.filter { it.targetId == media.id || it.targetId == episode?.id }
            )
        }

        val epUrl = episode?.streamUrl?.takeIf { it.startsWith("https://") }
        if (epUrl != null) {
            return ResolvedStreamInfo(
                isPlayable = true,
                url = epUrl,
                protocol = episode.streamProtocol.ifBlank { "MP4" },
                licenseLabel = media.licenseInfo.ifBlank { "مصدر بث مرخص" },
                allowOfflineDownload = media.isDownloadAllowed,
                externalSubtitles = subs.filter { it.targetId == media.id || it.targetId == episode.id }
            )
        }

        if (media.streamStatus == "LICENSED_ACTIVE" && media.streamUrl.startsWith("https://")) {
            return ResolvedStreamInfo(
                isPlayable = true,
                url = media.streamUrl,
                protocol = media.streamProtocol.ifBlank { "MP4" },
                licenseLabel = media.licenseInfo.ifBlank { "ملك عام / مرخص رسمياً" },
                allowOfflineDownload = media.isDownloadAllowed,
                externalSubtitles = subs.filter { it.targetId == media.id }
            )
        }

        return ResolvedStreamInfo(
            isPlayable = false,
            url = "",
            protocol = "NONE",
            licenseLabel = media.licenseInfo.ifBlank { "سجل أرشيفي موثق — بانتظار ربط مصدر بث مرخص من الإدارة" },
            allowOfflineDownload = false,
            externalSubtitles = emptyList()
        )
    }

    // --- Library Filter & Search ---

    fun updateFilter(transform: (LibraryFilterState) -> LibraryFilterState) {
        _libraryFilter.value = transform(_libraryFilter.value)
    }

    fun submitSearchHistory(query: String) {
        viewModelScope.launch {
            repository.recordSearchQuery(query)
        }
    }

    fun deleteSearchHistory(itemId: String? = null) {
        viewModelScope.launch {
            repository.clearSearchHistory(itemId)
        }
    }

    fun filterMediaList(items: List<MediaItem>, filter: LibraryFilterState): List<MediaItem> {
        val q = filter.searchQuery.trim().lowercase()
        val filtered = items.filter { item ->
            val matchesQuery = if (q.isEmpty()) true else {
                item.titleAr.lowercase().contains(q) ||
                    item.titleOriginal.lowercase().contains(q) ||
                    item.titleEn.lowercase().contains(q) ||
                    item.titleRu.lowercase().contains(q) ||
                    item.director.lowercase().contains(q) ||
                    item.country.lowercase().contains(q) ||
                    item.year.toString().contains(q) ||
                    item.warConflict.lowercase().contains(q) ||
                    item.historicalEra.lowercase().contains(q) ||
                    item.genres.any { it.lowercase().contains(q) } ||
                    item.cast.any { it.lowercase().contains(q) }
            }
            val matchesKind = filter.selectedKind == "ALL" || item.kind == filter.selectedKind
            val matchesGenre = filter.selectedGenre == "ALL" || item.genres.contains(filter.selectedGenre)
            val matchesCountry = filter.selectedCountry == "ALL" || item.country == filter.selectedCountry
            val matchesEra = filter.selectedEraOrWar == "ALL" ||
                item.archiveSection == filter.selectedEraOrWar ||
                item.warConflict == filter.selectedEraOrWar
            val matchesPlayable = !filter.onlyPlayableNow || resolveStreamFor(item).isPlayable

            matchesQuery && matchesKind && matchesGenre && matchesCountry && matchesEra && matchesPlayable
        }

        return when (filter.sortBy) {
            "LATEST" -> filtered.sortedByDescending { it.year }
            "OLDEST" -> filtered.sortedBy { it.year }
            "MOST_WATCHED" -> filtered.sortedByDescending { it.viewsCount }
            else -> filtered.sortedByDescending { it.rating }
        }
    }

    // --- User Actions ---

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, item.titleAr)
                .onSuccess { added ->
                    postToast(if (added) "أُضيف «${item.titleAr}» إلى المفضلة" else "أُزيل من المفضلة")
                }
                .onFailure { postToast("تعذر تحديث المفضلة: ${it.localizedMessage}") }
        }
    }

    fun createNewWatchlist(name: String, description: String, isPublic: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val ownerName = userProfile.value?.displayName ?: "مشترك MOVIORA"
            repository.createWatchlist(name, description, isPublic, ownerName)
                .onSuccess { postToast("تم إنشاء القائمة «$name» بنجاح") }
                .onFailure { postToast("تعذر إنشاء القائمة: ${it.localizedMessage}") }
        }
    }

    fun toggleItemInWatchlist(listId: String, mediaId: String) {
        viewModelScope.launch {
            repository.toggleMediaInWatchlist(listId, mediaId)
                .onSuccess { postToast("تم تحديث القائمة بنجاح") }
        }
    }

    fun deleteWatchlist(listId: String) {
        viewModelScope.launch {
            repository.deleteWatchlist(listId)
                .onSuccess { postToast("تم حذف القائمة") }
        }
    }

    fun submitReview(mediaId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val name = userProfile.value?.displayName ?: "مشاهد MOVIORA"
            repository.submitOrUpdateReview(mediaId, name, rating, comment)
                .onSuccess { postToast("تم حفظ مراجعتك وتقييمك بنجاح") }
                .onFailure { postToast("تعذر إرسال المراجعة: ${it.localizedMessage}") }
        }
    }

    fun reportContent(targetId: String, targetType: String, reason: String) {
        viewModelScope.launch {
            repository.submitReport(targetId, targetType, reason)
                .onSuccess { postToast("تم استلام بلاغك وسيتم فحصه من فريق الإدارة") }
                .onFailure { postToast("تعذر إرسال البلاغ: ${it.localizedMessage}") }
        }
    }

    fun toggleFollow(targetUserId: String, targetName: String) {
        viewModelScope.launch {
            repository.toggleFollowUser(targetUserId, targetName)
                .onSuccess { followed ->
                    postToast(if (followed) "تتابع الآن $targetName" else "تم إلغاء المتابعة")
                }
                .onFailure { postToast(it.localizedMessage ?: "تعذر تنفيذ العملية") }
        }
    }

    fun savePlaybackProgress(
        mediaId: String,
        episodeId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        positionMs: Long,
        durationMs: Long
    ) {
        viewModelScope.launch {
            repository.saveWatchProgress(mediaId, episodeId, seasonNumber, episodeNumber, positionMs, durationMs)
        }
    }

    fun logPlayerError(mediaId: String, code: String, message: String) {
        viewModelScope.launch {
            repository.logPlaybackError(mediaId, code, message)
        }
    }

    fun startOfflineDownload(media: MediaItem, episode: Episode? = null, qualityLabel: String = "720p") {
        val stream = resolveStreamFor(media, episode)
        val dlId = episode?.id?.takeIf { it.isNotBlank() } ?: media.id
        val subTitle = if (episode != null) {
            "الموسم ${episode.seasonNumber} • الحلقة ${episode.episodeNumber}: ${episode.titleAr}"
        } else {
            "${media.year} • ${media.country}"
        }
        downloadManager.startLicensedDownload(
            id = dlId,
            mediaId = media.id,
            episodeId = episode?.id ?: "",
            titleAr = media.titleAr,
            subtitleInfoAr = subTitle,
            qualityLabel = qualityLabel,
            sourceUrl = stream.url,
            isDownloadAllowed = stream.allowOfflineDownload
        ).onSuccess {
            postToast("بدأ تنزيل «${media.titleAr}» بجودة $qualityLabel للمشاهدة بدون إنترنت")
        }.onFailure {
            postToast(it.localizedMessage ?: "التنزيل غير متاح لهذا العمل")
        }
    }

    fun getStorageSummary(): DeviceStorageSummary = downloadManager.getStorageSummary()

    fun subscribeWithGooglePlay(activity: Activity, plan: VipPlanCatalogItem) {
        billingManager.launchSubscriptionFlow(activity, plan)
    }

    fun activateLocalVipPreview(plan: VipPlanCatalogItem) {
        viewModelScope.launch {
            val days = when (plan.planId) {
                "vip_yearly" -> 365
                "vip_quarterly" -> 90
                else -> 30
            }
            repository.activateLocalVipForTesting(
                planId = plan.planId,
                priceDisplay = plan.displayPrice,
                durationDays = days
            ).onSuccess {
                postToast("تم تفعيل «${plan.titleAr}» فورياً للتجربة داخل التطبيق!")
            }.onFailure {
                postToast("تعذر تفعيل الباقة: ${it.localizedMessage}")
            }
        }
    }

    fun deactivateLocalVipPreview() {
        viewModelScope.launch {
            repository.deactivateLocalVip()
            if (clientSettings.value.themeMode.startsWith("vip_")) {
                preferences.setThemeMode("dark")
            }
            postToast("تم إنهاء وضع VIP التجريبي والعودة للوضع المجاني")
        }
    }

    fun toggleAdminPreviewMode(enabled: Boolean) {
        _isAdmin.value = enabled
        if (!enabled && _selectedTab.value == PrimaryTab.ADMIN) {
            _selectedTab.value = PrimaryTab.HOME
        }
        postToast(if (enabled) "تم إظهار شريط ولوحة الإدارة للتجربة" else "تم إخفاء لوحة الإدارة لمعاينة واجهة المشاهد العادي")
    }

    fun resetLocalDemoData() {
        viewModelScope.launch {
            repository.resetLocalAppData()
                .onSuccess {
                    if (clientSettings.value.themeMode.startsWith("vip_")) {
                        preferences.setThemeMode("dark")
                    }
                    postToast("تمت إعادة ضبط البيانات المحلية بنجاح")
                }
        }
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    fun cancelVipAutoRenew() {
        viewModelScope.launch {
            repository.cancelSubscriptionAutoRenew()
                .onSuccess { postToast("تم إيقاف التجديد التلقائي؛ سيستمر اشتراكك حتى نهاية الفترة المدفوعة") }
        }
    }

    // --- Settings & Profile Updates ---

    fun setLanguage(code: String) {
        viewModelScope.launch {
            preferences.setLanguage(code)
            userProfile.value?.let { p ->
                repository.updateUserProfile(
                    displayName = p.displayName,
                    photoUrl = p.photoUrl,
                    preferredLanguage = code,
                    preferredSubtitleLang = p.preferredSubtitleLang,
                    themeMode = p.themeMode,
                    notificationsEnabled = p.notificationsEnabled
                )
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
            userProfile.value?.let { p ->
                repository.updateUserProfile(
                    displayName = p.displayName,
                    photoUrl = p.photoUrl,
                    preferredLanguage = p.preferredLanguage,
                    preferredSubtitleLang = p.preferredSubtitleLang,
                    themeMode = mode,
                    notificationsEnabled = p.notificationsEnabled
                )
            }
        }
    }

    fun setQualityPreset(preset: MovioraQualityPreset) {
        viewModelScope.launch { preferences.setQualityPreset(preset) }
    }

    fun setWifiAndMobileQuality(wifiHeight: Int, mobileHeight: Int) {
        viewModelScope.launch {
            preferences.setWifiMaxHeight(wifiHeight)
            preferences.setMobileMaxHeight(mobileHeight)
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoPlayNext(enabled) }
    }

    fun updateSubtitlesConfig(lang: String, sizeSp: Int, colorHex: String, offsetMs: Long) {
        viewModelScope.launch {
            preferences.setSubtitlePreferences(lang, sizeSp, colorHex, offsetMs)
            postToast("تم حفظ إعدادات الترجمة والمزامنة")
        }
    }

    fun updateNotificationsConfig(ep: Boolean, rel: Boolean, rec: Boolean, sub: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationPreferences(ep, rel, rec, sub)
        }
    }

    fun updateProfileInfo(displayName: String, photoUrl: String) {
        viewModelScope.launch {
            val s = clientSettings.value
            repository.updateUserProfile(
                displayName = displayName,
                photoUrl = photoUrl,
                preferredLanguage = s.languageCode,
                preferredSubtitleLang = s.preferredSubtitleLang,
                themeMode = s.themeMode,
                notificationsEnabled = s.notifNewReleases
            ).onSuccess {
                postToast("تم تحديث بيانات الحساب بنجاح")
            }.onFailure {
                postToast("تعذر تحديث الملف الشخصي: ${it.localizedMessage}")
            }
        }
    }

    fun revokeDeviceSession(sessionId: String) {
        viewModelScope.launch {
            repository.revokeSession(sessionId)
                .onSuccess { postToast("تم إنهاء الجلسة بنجاح") }
        }
    }

    fun submitSupportTicket(subject: String, message: String) {
        viewModelScope.launch {
            val isVip = subscription.value?.isCurrentlyActive == true
            repository.submitSupportTicket(subject, message, isVip)
                .onSuccess { postToast("تم إرسال طلب الدعم الفني بنجاح") }
                .onFailure { postToast("تعذر إرسال التذكرة: ${it.localizedMessage}") }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSizeBytes.value = preferences.calculateCacheSizeBytes()
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            preferences.clearAppCache()
            _cacheSizeBytes.value = preferences.calculateCacheSizeBytes()
            postToast("تم مسح الذاكرة المؤقتة بنجاح")
        }
    }

    companion object {
        fun factory(context: Context, userId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appCtx = context.applicationContext
                val repo = MovioraRepository(appCtx)
                val prefs = MovioraPreferences(appCtx)
                val dlMgr = OfflineDownloadManager(appCtx)
                val billingMgr = MovioraBillingManager(appCtx) { planId, price, curr, orderId, token ->
                    repo.recordVerifiedGooglePlayPurchase(planId, price, curr, orderId, token)
                }
                val netFlow = NetworkAndNotificationHelper.observeNetworkStatus(appCtx)
                MovioraViewModel(
                    userId = userId,
                    repository = repo,
                    preferences = prefs,
                    downloadManager = dlMgr,
                    billingManager = billingMgr,
                    networkFlow = netFlow
                )
            }
        }
    }
}
