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
import com.example.data.model.Episode
import com.example.data.model.MediaItem
import com.example.data.model.UiState
import com.example.ui.components.EmptyOrErrorCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovioraViewModel
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(viewModel: MovioraViewModel) {
    val scope = rememberCoroutineScope()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val catalogueState by viewModel.catalogueState.collectAsState()
    val videoSources by viewModel.videoSources.collectAsState()
    val reports by viewModel.adminReports.collectAsState()
    val errors by viewModel.adminPlaybackErrors.collectAsState()
    val auditLogs by viewModel.adminAuditLogs.collectAsState()
    val users by viewModel.adminUsers.collectAsState()
    val subs by viewModel.adminSubscriptions.collectAsState()

    if (!isAdmin) {
        EmptyOrErrorCard(
            icon = Icons.Default.AdminPanelSettings,
            title = "لوحة الإدارة محمية بصلاحيات الخادم",
            subtitle = "تُفتح لوحة إدارة MOVIORA تلقائياً لحساب المالك الموثق أو المديرين المعتمدين في قاعدة بيانات Firestore."
        )
        return
    }

    val allMedia = (catalogueState as? UiState.Success)?.data ?: emptyList()
    var adminTab by remember { mutableIntStateOf(0) } // 0=Stats, 1=Media/Ep, 2=Streams/Subs, 3=Users/VIP, 4=Moderation/Logs

    // Form states
    var titleAr by remember { mutableStateOf("") }
    var titleOrig by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("series") }
    var yearStr by remember { mutableStateOf("2025") }
    var country by remember { mutableStateOf("روسيا") }
    var synopsisAr by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var castCsv by remember { mutableStateOf("") }
    var genreLabel by remember { mutableStateOf("مسلسلات تاريخية") }
    var archiveSec by remember { mutableStateOf("SOVIET") }
    var streamUrlNew by remember { mutableStateOf("") }

    // Episode Form
    var epSeriesId by remember { mutableStateOf(allMedia.firstOrNull()?.id ?: "") }
    var epSeasonStr by remember { mutableStateOf("1") }
    var epNumStr by remember { mutableStateOf("1") }
    var epTitleAr by remember { mutableStateOf("") }
    var epStreamUrl by remember { mutableStateOf("") }

    // Stream Attachment Form
    var targetId by remember { mutableStateOf(allMedia.firstOrNull()?.id ?: "") }
    var sourceUrl by remember { mutableStateOf("") }
    var sourceProtocol by remember { mutableStateOf("HLS") }
    var sourceLicense by remember { mutableStateOf("ترخيص رسمي معتمد") }
    var sourceOffline by remember { mutableStateOf(false) }

    var subLangCode by remember { mutableStateOf("ar") }
    var subLangLabel by remember { mutableStateOf("العربية") }
    var subUrl by remember { mutableStateOf("") }

    // User VIP & Broadcast Form
    var vipTargetUid by remember { mutableStateOf(com.example.data.repository.MovioraRepository.DEFAULT_LOCAL_USER_ID) }
    var adminEmailGrant by remember { mutableStateOf("") }
    var notifTitle by remember { mutableStateOf("") }
    var notifBody by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "لوحة إدارة MOVIORA المركزية",
                style = MaterialTheme.typography.headlineMedium,
                color = MovioraGold
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(
                    0 to "الإحصائيات",
                    1 to "إدارة الأعمال والحلقات",
                    2 to "ربط البث والترجمة",
                    3 to "المستخدمون وVIP",
                    4 to "البلاغات والإشعارات والسجل"
                )
                items(tabs) { (idx, label) ->
                    FilterChip(
                        selected = adminTab == idx,
                        onClick = { adminTab = idx },
                        label = { Text(label) }
                    )
                }
            }
        }

        when (adminTab) {
            0 -> {
                item {
                    val playableCount = allMedia.count { viewModel.resolveStreamFor(it).isPlayable }
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("مؤشرات المنصة الحية", style = MaterialTheme.typography.titleLarge, color = MovioraGold)
                            Text("• إجمالي الأعمال في المكتبة والأرشيف: ${allMedia.size}")
                            Text("• الأعمال ذات البث المرخص الجاهز للتشغيل: $playableCount")
                            Text("• مصادر الفيديو الإضافية المربوطة: ${videoSources.size}")
                            Text("• إجمالي المستخدمين المسجلين: ${users.size}")
                            Text("• اشتراكات VIP النشطة: ${subs.count { it.isCurrentlyActive }}")
                            Text("• البلاغات المفتوحة للمراجعة: ${reports.count { it.status == "open" }}")
                            Text("• تقارير أخطاء التشغيل: ${errors.size}")

                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.repository.seedCatalogueIfAdminAndEmpty()
                                            .onSuccess { c ->
                                                viewModel.postToast(if (c > 0) "تمت تهيئة $c عملاً في السحابة" else "المكتبة السحابية مهيأة ومحدثة")
                                            }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("فحص ومزامنة الفهرس السحابي الأساسي")
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("إضافة أو تحديث فيلم / مسلسل", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                            OutlinedTextField(value = titleAr, onValueChange = { titleAr = it }, label = { Text("الاسم بالعربية") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = titleOrig, onValueChange = { titleOrig = it }, label = { Text("الاسم الأصلي / الدولي") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = kind, onValueChange = { kind = it }, label = { Text("النوع (movie/series)") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = yearStr, onValueChange = { yearStr = it }, label = { Text("سنة الإنتاج") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("الدولة") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = genreLabel, onValueChange = { genreLabel = it }, label = { Text("التصنيف") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = archiveSec, onValueChange = { archiveSec = it }, label = { Text("قسم الأرشيف (SOVIET / RUSSIAN_WAR / WWII...)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = director, onValueChange = { director = it }, label = { Text("المخرج") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = castCsv, onValueChange = { castCsv = it }, label = { Text("الممثلون (مفصولون بفاصلة)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = synopsisAr, onValueChange = { synopsisAr = it }, label = { Text("قصة العمل") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = streamUrlNew, onValueChange = { streamUrlNew = it }, label = { Text("رابط البث المرخص HTTPS (اختياري)") }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (titleAr.isNotBlank() && titleOrig.isNotBlank()) {
                                        val cleanUrl = streamUrlNew.trim()
                                        val hasStream = cleanUrl.startsWith("https://")
                                        val item = MediaItem(
                                            id = "custom_${System.currentTimeMillis()}",
                                            titleAr = titleAr.trim(),
                                            titleOriginal = titleOrig.trim(),
                                            titleEn = titleOrig.trim(),
                                            kind = kind.trim(),
                                            year = yearStr.toIntOrNull() ?: 2025,
                                            country = country.trim(),
                                            synopsisAr = synopsisAr.trim(),
                                            genres = listOf(genreLabel.trim()),
                                            director = director.trim(),
                                            cast = castCsv.split(",", "،").map { it.trim() }.filter { it.isNotBlank() },
                                            archiveSection = archiveSec.trim(),
                                            streamStatus = if (hasStream) "LICENSED_ACTIVE" else "ARCHIVE_METADATA_ONLY",
                                            streamUrl = if (hasStream) cleanUrl else "",
                                            streamProtocol = when {
                                                !hasStream -> "NONE"
                                                cleanUrl.contains(".m3u8") -> "HLS"
                                                cleanUrl.contains(".mpd") -> "DASH"
                                                else -> "MP4"
                                            },
                                            licenseInfo = if (hasStream) "مضاف ومعتمد من إدارة MOVIORA" else "سجل أرشيفي"
                                        )
                                        scope.launch {
                                            viewModel.repository.saveMediaItemAsAdmin(item)
                                                .onSuccess {
                                                    viewModel.postToast("تم حفظ العمل في قاعدة البيانات بنجاح")
                                                    titleAr = ""
                                                    titleOrig = ""
                                                    synopsisAr = ""
                                                    streamUrlNew = ""
                                                }
                                                .onFailure { viewModel.postToast("خطأ: ${it.localizedMessage}") }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ العمل في السحابة")
                            }
                        }
                    }
                }

                // Add Episode Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("إضافة حلقة جديدة لمسلسل", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                            OutlinedTextField(value = epSeriesId, onValueChange = { epSeriesId = it }, label = { Text("معرف المسلسل (seriesId)") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = epSeasonStr, onValueChange = { epSeasonStr = it }, label = { Text("رقم الموسم") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = epNumStr, onValueChange = { epNumStr = it }, label = { Text("رقم الحلقة") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = epTitleAr, onValueChange = { epTitleAr = it }, label = { Text("عنوان الحلقة") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = epStreamUrl, onValueChange = { epStreamUrl = it }, label = { Text("رابط بث الحلقة HTTPS") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    if (epSeriesId.isNotBlank() && epTitleAr.isNotBlank()) {
                                        val hasUrl = epStreamUrl.trim().startsWith("https://")
                                        val ep = Episode(
                                            seriesId = epSeriesId.trim(),
                                            seasonNumber = epSeasonStr.toIntOrNull() ?: 1,
                                            episodeNumber = epNumStr.toIntOrNull() ?: 1,
                                            titleAr = epTitleAr.trim(),
                                            streamUrl = if (hasUrl) epStreamUrl.trim() else "",
                                            streamProtocol = if (!hasUrl) "NONE" else if (epStreamUrl.contains(".m3u8")) "HLS" else "MP4"
                                        )
                                        scope.launch {
                                            viewModel.repository.addEpisodeAsAdmin(ep)
                                                .onSuccess { viewModel.postToast("تمت إضافة الحلقة بنجاح") }
                                                .onFailure { viewModel.postToast("خطأ: ${it.localizedMessage}") }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ الحلقة")
                            }
                        }
                    }
                }

                // Quick Delete / Manage Existing Items
                items(allMedia.take(25), key = { "adm_${it.id}" }) { m ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.titleAr, fontWeight = FontWeight.Bold)
                                Text("معرف: ${m.id}", style = MaterialTheme.typography.labelSmall)
                            }
                            Row {
                                TextButton(onClick = {
                                    targetId = m.id
                                    epSeriesId = m.id
                                    adminTab = 2
                                }) { Text("ربط بث") }
                                IconButton(onClick = {
                                    scope.launch {
                                        viewModel.repository.deleteMediaItemAsAdmin(m.id, m)
                                            .onSuccess { viewModel.postToast("تم إخفاء/حذف العمل") }
                                    }
                                }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MovioraDanger)
                                }
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
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("ربط مصدر فيديو مرخص (HLS / DASH / MP4)", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                            OutlinedTextField(value = targetId, onValueChange = { targetId = it }, label = { Text("معرف العمل أو الحلقة (targetId)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = sourceUrl, onValueChange = { sourceUrl = it }, label = { Text("رابط البث المشفر https://...") }, modifier = Modifier.fillMaxWidth())
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("HLS", "DASH", "MP4")) { p ->
                                    FilterChip(selected = sourceProtocol == p, onClick = { sourceProtocol = p }, label = { Text(p) })
                                }
                            }
                            OutlinedTextField(value = sourceLicense, onValueChange = { sourceLicense = it }, label = { Text("بيان الترخيص القانوني") }, modifier = Modifier.fillMaxWidth())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = sourceOffline, onCheckedChange = { sourceOffline = it })
                                Spacer(Modifier.width(8.dp))
                                Text("السماح بالتنزيل المحلي بدون إنترنت (MP4)")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.repository.attachLicensedVideoSourceAsAdmin(
                                            targetId = targetId.trim(),
                                            targetType = "movie",
                                            url = sourceUrl.trim(),
                                            protocol = sourceProtocol,
                                            maxQualityLabel = "1080p",
                                            licenseName = sourceLicense,
                                            allowOffline = sourceOffline
                                        ).onSuccess {
                                            viewModel.postToast("تم ربط مصدر البث المرخص وتفعيل زر المشاهدة فوراً!")
                                            sourceUrl = ""
                                        }.onFailure {
                                            viewModel.postToast(it.localizedMessage ?: "خطأ في الرابط")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تفعيل مصدر البث المرخص")
                            }

                            HorizontalDivider()
                            Text("إرفاق ملف ترجمة خارجي (VTT / SRT)", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = subLangCode, onValueChange = { subLangCode = it }, label = { Text("رمز اللغة (ar/en/ru)") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = subLangLabel, onValueChange = { subLangLabel = it }, label = { Text("اسم اللغة") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = subUrl, onValueChange = { subUrl = it }, label = { Text("رابط ملف الترجمة https://...") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.repository.attachSubtitleSourceAsAdmin(
                                            targetId = targetId.trim(),
                                            languageCode = subLangCode.trim(),
                                            languageLabel = subLangLabel.trim(),
                                            url = subUrl.trim()
                                        ).onSuccess {
                                            viewModel.postToast("تم ربط ملف الترجمة بنجاح")
                                            subUrl = ""
                                        }.onFailure { viewModel.postToast(it.localizedMessage ?: "خطأ") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ ملف الترجمة")
                            }
                        }
                    }
                }
            }

            3 -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("تفعيل اشتراك VIP أو منح صلاحية إدارية", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                            OutlinedTextField(value = vipTargetUid, onValueChange = { vipTargetUid = it }, label = { Text("معرف المستخدم (UID)") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (vipTargetUid.isNotBlank()) {
                                            scope.launch {
                                                viewModel.repository.grantVipAsAdmin(vipTargetUid.trim(), "vip_monthly", 30)
                                                    .onSuccess { viewModel.postToast("تم تفعيل VIP لمدة 30 يوماً") }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("منح شهر VIP") }
                                Button(
                                    onClick = {
                                        if (vipTargetUid.isNotBlank()) {
                                            scope.launch {
                                                viewModel.repository.grantVipAsAdmin(vipTargetUid.trim(), "vip_yearly", 365)
                                                    .onSuccess { viewModel.postToast("تم تفعيل VIP لمدة سنة") }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("منح سنة VIP") }
                            }

                            OutlinedTextField(value = adminEmailGrant, onValueChange = { adminEmailGrant = it }, label = { Text("البريد الإلكتروني للمدير الجديد") }, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(
                                onClick = {
                                    if (vipTargetUid.isNotBlank() && adminEmailGrant.isNotBlank()) {
                                        scope.launch {
                                            viewModel.repository.addAdminRole(vipTargetUid.trim(), adminEmailGrant.trim(), "content_manager")
                                                .onSuccess { viewModel.postToast("تم منح صلاحية مدير المحتوى") }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("إضافة كمدير محتوى") }
                        }
                    }
                }

                items(users, key = { it.userId }) { u ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.displayName, fontWeight = FontWeight.Bold)
                                Text("${u.email} • UID: ${u.userId}", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { vipTargetUid = u.userId }) { Text("اختيار") }
                        }
                    }
                }
            }

            else -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("إرسال إشعار عام لجميع المشتركين", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                            OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = { Text("عنوان الإشعار") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = notifBody, onValueChange = { notifBody = it }, label = { Text("نص الإشعار") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    if (notifTitle.isNotBlank() && notifBody.isNotBlank()) {
                                        scope.launch {
                                            viewModel.repository.broadcastNotificationAsAdmin("ALL", "new_release", notifTitle, notifBody)
                                                .onSuccess {
                                                    viewModel.postToast("تم إرسال الإشعار بنجاح")
                                                    notifTitle = ""
                                                    notifBody = ""
                                                }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("بث الإشعار الفوري") }
                        }
                    }
                }

                items(reports, key = { "rep_${it.id}" }) { rep ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("بلاغ (${rep.status}) على ${rep.targetType}: ${rep.targetId}", fontWeight = FontWeight.Bold)
                            Text("السبب: ${rep.reason}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    scope.launch {
                                        viewModel.repository.resolveReportAsAdmin(
                                            rep,
                                            "resolved",
                                            hideReviewId = if (rep.targetType == "review") rep.targetId else null
                                        ).onSuccess { viewModel.postToast("تم إخفاء المراجعة وإغلاق البلاغ") }
                                    }
                                }) { Text("إخفاء المحتوى وحل البلاغ") }
                                OutlinedButton(onClick = {
                                    scope.launch { viewModel.repository.resolveReportAsAdmin(rep, "dismissed") }
                                }) { Text("حفظ بدون إجراء") }
                            }
                        }
                    }
                }

                items(auditLogs.take(15), key = { "log_${it.id}" }) { log ->
                    Text(
                        text = "• [${log.action}] ${log.details}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
