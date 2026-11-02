package com.example.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

data class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val arabicName: String,
    val layoutDirection: LayoutDirection
)

data class MovioraDictionary(
    val langCode: String,
    val appName: String = "MOVIORA",
    val tagline: String,
    val navHome: String,
    val navLibrary: String,
    val navHistory: String,
    val navSaved: String,
    val navVip: String,
    val navProfile: String,
    val navAdmin: String,
    val signInWithGoogle: String,
    val signInSubtitle: String,
    val signingIn: String,
    val retry: String,
    val offlineBanner: String,
    val searchPlaceholder: String,
    val watchNow: String,
    val resumeWatching: String,
    val archiveMetadataOnly: String,
    val addToFavorites: String,
    val inFavorites: String,
    val addToWatchlist: String,
    val downloadOffline: String,
    val shareWork: String,
    val latestMovies: String,
    val latestSeries: String,
    val mostWatched: String,
    val topRated: String,
    val trendingNow: String,
    val movioraPicks: String,
    val newThisWeek: String,
    val vipExclusives: String,
    val continueWatching: String,
    val myFavorites: String,
    val recommendedForYou: String,
    val historicalWorks: String,
    val sovietArchive: String,
    val russianSeries: String,
    val ww1Series: String,
    val ww2Series: String,
    val militarySeries: String,
    val documentaries: String,
    val seasonsAndEpisodes: String,
    val castAndCrew: String,
    val reviewsAndRatings: String,
    val similarWorks: String,
    val qualityEngineTitle: String,
    val settingsTitle: String,
    val languageSetting: String,
    val themeSetting: String,
    val signOut: String
)

object MovioraLocalization {

    val supportedLanguages: List<SupportedLanguage> = listOf(
        SupportedLanguage("ar", "العربية", "العربية", LayoutDirection.Rtl),
        SupportedLanguage("en", "English", "الإنجليزية", LayoutDirection.Ltr),
        SupportedLanguage("ru", "Русский", "الروسية", LayoutDirection.Ltr),
        SupportedLanguage("tr", "Türkçe", "التركية", LayoutDirection.Ltr),
        SupportedLanguage("fr", "Français", "الفرنسية", LayoutDirection.Ltr),
        SupportedLanguage("de", "Deutsch", "الألمانية", LayoutDirection.Ltr),
        SupportedLanguage("es", "Español", "الإسبانية", LayoutDirection.Ltr),
        SupportedLanguage("ko", "한국어", "الكورية", LayoutDirection.Ltr)
    )

    fun detectDefaultLanguageCode(): String {
        val sys = Locale.getDefault().language.lowercase(Locale.ROOT)
        return if (supportedLanguages.any { it.code == sys }) sys else "ar"
    }

    fun layoutDirectionFor(code: String): LayoutDirection =
        supportedLanguages.firstOrNull { it.code == code }?.layoutDirection ?: LayoutDirection.Rtl

    fun dictionaryFor(code: String): MovioraDictionary = when (code) {
        "en" -> MovioraDictionary(
            langCode = "en",
            tagline = "Global Cinema, Historical Series & World War Archive",
            navHome = "Home",
            navLibrary = "Library",
            navHistory = "Archive",
            navSaved = "My Lists",
            navVip = "VIP",
            navProfile = "Account",
            navAdmin = "Admin",
            signInWithGoogle = "Continue with Google",
            signInSubtitle = "Sign in securely to sync watch progress, watchlists, downloads, and MOVIORA VIP across devices.",
            signingIn = "Signing in...",
            retry = "Retry",
            offlineBanner = "Offline mode — cached catalogue & downloaded works remain available",
            searchPlaceholder = "Search movies, series, actors, directors, country, or war era...",
            watchNow = "Watch Licensed Stream",
            resumeWatching = "Resume Playback",
            archiveMetadataOnly = "Archive Record — Stream Pending License",
            addToFavorites = "Favorite",
            inFavorites = "Favorited",
            addToWatchlist = "Watchlist",
            downloadOffline = "Download",
            shareWork = "Share",
            latestMovies = "Latest Movies",
            latestSeries = "Latest Series",
            mostWatched = "Most Watched",
            topRated = "Top Rated",
            trendingNow = "Trending Globally",
            movioraPicks = "MOVIORA Curated Picks",
            newThisWeek = "New Releases",
            vipExclusives = "Licensed Exclusives & VIP",
            continueWatching = "Continue Watching",
            myFavorites = "My Favorites",
            recommendedForYou = "Recommended For You",
            historicalWorks = "Historical Masterpieces",
            sovietArchive = "Soviet Series Archive (20)",
            russianSeries = "Russian War & Drama Series (18)",
            ww1Series = "World War I Archive",
            ww2Series = "World War II Archive",
            militarySeries = "Military & Espionage Operations",
            documentaries = "Historical Documentaries",
            seasonsAndEpisodes = "Seasons & Episodes",
            castAndCrew = "Director & Cast",
            reviewsAndRatings = "Community Reviews & Ratings",
            similarWorks = "Similar Works",
            qualityEngineTitle = "MOVIORA Quality Engine",
            settingsTitle = "Settings & Preferences",
            languageSetting = "App & Subtitle Language",
            themeSetting = "Appearance & VIP Theme",
            signOut = "Sign Out"
        )
        "ru" -> MovioraDictionary(
            langCode = "ru",
            tagline = "Мировое кино, исторические сериалы и советский архив",
            navHome = "Главная",
            navLibrary = "Каталог",
            navHistory = "Архив",
            navSaved = "Моё",
            navVip = "VIP",
            navProfile = "Профиль",
            navAdmin = "Админ",
            signInWithGoogle = "Войти через Google",
            signInSubtitle = "Безопасный вход для синхронизации истории просмотра, списков и подписки MOVIORA VIP.",
            signingIn = "Вход...",
            retry = "Повторить",
            offlineBanner = "Офлайн-режим — доступны загрузки и сохранённый каталог",
            searchPlaceholder = "Поиск фильмов, сериалов, актёров, режиссёров, стран и эпох...",
            watchNow = "Смотреть",
            resumeWatching = "Продолжить просмотр",
            archiveMetadataOnly = "Архивная запись — ожидание лицензии потока",
            addToFavorites = "В избранное",
            inFavorites = "В избранном",
            addToWatchlist = "В список",
            downloadOffline = "Скачать",
            shareWork = "Поделиться",
            latestMovies = "Новые фильмы",
            latestSeries = "Новые сериалы",
            mostWatched = "Самые просматриваемые",
            topRated = "Высокий рейтинг",
            trendingNow = "В тренде",
            movioraPicks = "Выбор MOVIORA",
            newThisWeek = "Новинки недели",
            vipExclusives = "Эксклюзивы MOVIORA VIP",
            continueWatching = "Продолжить просмотр",
            myFavorites = "Избранное",
            recommendedForYou = "Рекомендации для вас",
            historicalWorks = "Исторические произведения",
            sovietArchive = "Золотой советский архив (20)",
            russianSeries = "Российские военные сериалы (18)",
            ww1Series = "Первая мировая война",
            ww2Series = "Вторая мировая и ВОВ",
            militarySeries = "Военная разведка и операции",
            documentaries = "Историческая документалистика",
            seasonsAndEpisodes = "Сезоны и серии",
            castAndCrew = "Режиссёр и актёры",
            reviewsAndRatings = "Отзывы и оценки",
            similarWorks = "Похожие произведения",
            qualityEngineTitle = "Система качества MOVIORA",
            settingsTitle = "Настройки приложения",
            languageSetting = "Язык интерфейса и субтитров",
            themeSetting = "Тема оформления",
            signOut = "Выйти"
        )
        "tr", "fr", "de", "es", "ko" -> {
            val baseEn = dictionaryFor("en")
            when (code) {
                "tr" -> baseEn.copy(
                    langCode = "tr",
                    tagline = "Dünya Sineması, Tarihi Diziler ve Savaş Arşivi",
                    navHome = "Ana Sayfa",
                    navLibrary = "Kütüphane",
                    navHistory = "Tarih Arşivi",
                    navSaved = "Listelerim",
                    navProfile = "Hesabım",
                    watchNow = "Şimdi İzle",
                    searchPlaceholder = "Film, dizi, oyuncu, yönetmen veya dönem ara..."
                )
                "fr" -> baseEn.copy(
                    langCode = "fr",
                    tagline = "Cinéma Mondial, Séries Historiques et Archives",
                    navHome = "Accueil",
                    navLibrary = "Catalogue",
                    navHistory = "Archives",
                    navSaved = "Mes Listes",
                    navProfile = "Compte",
                    watchNow = "Regarder",
                    searchPlaceholder = "Rechercher films, séries, acteurs, réalisateurs..."
                )
                "de" -> baseEn.copy(
                    langCode = "de",
                    tagline = "Weltkino, Historische Serien & Kriegsarchiv",
                    navHome = "Start",
                    navLibrary = "Mediathek",
                    navHistory = "Archiv",
                    navSaved = "Listen",
                    navProfile = "Konto",
                    watchNow = "Jetzt ansehen",
                    searchPlaceholder = "Filme, Serien, Schauspieler oder Epochen suchen..."
                )
                "es" -> baseEn.copy(
                    langCode = "es",
                    tagline = "Cine Mundial, Series Históricas y Archivo Bélico",
                    navHome = "Inicio",
                    navLibrary = "Catálogo",
                    navHistory = "Archivo",
                    navSaved = "Mis Listas",
                    navProfile = "Cuenta",
                    watchNow = "Ver ahora",
                    searchPlaceholder = "Buscar películas, series, actores, directores..."
                )
                else -> baseEn.copy(
                    langCode = "ko",
                    tagline = "글로벌 시네마, 역사 시리즈 및 세계대전 아카이브",
                    navHome = "홈",
                    navLibrary = "라이브러리",
                    navHistory = "역사 아카이브",
                    navSaved = "보관함",
                    navProfile = "내 계정",
                    watchNow = "지금 시청",
                    searchPlaceholder = "영화, 시리즈, 배우, 감독, 국가 검색..."
                )
            }
        }
        else -> MovioraDictionary(
            langCode = "ar",
            tagline = "منصة السينما العالمية وأرشيف المسلسلات التاريخية والسوفيتية",
            navHome = "الرئيسية",
            navLibrary = "المكتبة",
            navHistory = "الأرشيف",
            navSaved = "مكتبتي",
            navVip = "VIP",
            navProfile = "حسابي",
            navAdmin = "الإدارة",
            signInWithGoogle = "الدخول الآمن عبر حساب Google",
            signInSubtitle = "سجّل دخولك لمزامنة سجل المشاهدة، القوائم الخاصة، التنزيلات المرخصة، واشتراك MOVIORA VIP عبر جميع أجهزتك.",
            signingIn = "جاري التحقق وتسجيل الدخول...",
            retry = "إعادة المحاولة",
            offlineBanner = "وضع عدم الاتصال — يمكنك تصفح الأرشيف ومشاهدة التنزيلات المحفوظة",
            searchPlaceholder = "ابحث بالاسم، الممثل، المخرج، الدولة، السنة، أو الحقبة التاريخية...",
            watchNow = "مشاهدة البث المرخص",
            resumeWatching = "متابعة المشاهدة",
            archiveMetadataOnly = "سجل أرشيفي موثق — بانتظار ربط مصدر بث مرخص",
            addToFavorites = "إضافة للمفضلة",
            inFavorites = "في المفضلة",
            addToWatchlist = "قوائمي",
            downloadOffline = "تنزيل أوفلاين",
            shareWork = "مشاركة",
            latestMovies = "أحدث الأفلام",
            latestSeries = "أحدث المسلسلات",
            mostWatched = "الأكثر مشاهدة",
            topRated = "الأعلى تقييماً",
            trendingNow = "الأعمال الرائجة عالمياً",
            movioraPicks = "اختيارات MOVIORA",
            newThisWeek = "أعمال جديدة هذا الأسبوع",
            vipExclusives = "المحتوى الحصري المرخص (VIP)",
            continueWatching = "أكمل المشاهدة",
            myFavorites = "قائمة المفضلة",
            recommendedForYou = "الأعمال المقترحة لك",
            historicalWorks = "الأعمال التاريخية الكبرى",
            sovietArchive = "الأرشيف السوفيتي الكامل (20 مسلسلاً)",
            russianSeries = "المسلسلات الروسية الحربية والتاريخية (18 مسلسلاً)",
            ww1Series = "مسلسلات الحرب العالمية الأولى",
            ww2Series = "مسلسلات الحرب العالمية الثانية",
            militarySeries = "المسلسلات العسكرية والاستخباراتية",
            documentaries = "الوثائقيات التاريخية المرخصة",
            seasonsAndEpisodes = "المواسم والحلقات",
            castAndCrew = "المخرج وطاقم التمثيل",
            reviewsAndRatings = "مراجعات وتقييمات المشاهدين",
            similarWorks = "أعمال مشابهة قد تعجبك",
            qualityEngineTitle = "نظام جودة MOVIORA الذكي",
            settingsTitle = "الإعدادات المتقدمة",
            languageSetting = "لغة الواجهة والترجمة",
            themeSetting = "المظهر وثيمات VIP",
            signOut = "تسجيل الخروج"
        )
    }
}

val LocalMovioraStrings = staticCompositionLocalOf {
    MovioraLocalization.dictionaryFor("ar")
}

val LocalAppLanguage = staticCompositionLocalOf { "ar" }
