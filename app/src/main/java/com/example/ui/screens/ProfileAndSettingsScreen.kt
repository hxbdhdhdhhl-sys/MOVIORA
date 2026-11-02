package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.MovioraQualityPreset
import com.example.ui.components.formatBytesHuman
import com.example.ui.i18n.LocalMovioraStrings
import com.example.ui.i18n.MovioraLocalization
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovioraViewModel
import com.example.util.NetworkAndNotificationHelper

@Composable
fun ProfileAndSettingsScreen(
    viewModel: MovioraViewModel
) {
    val context = LocalContext.current
    val strings = LocalMovioraStrings.current

    val profile by viewModel.userProfile.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val settings by viewModel.clientSettings.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val cacheBytes by viewModel.cacheSizeBytes.collectAsState()
    val isVip = subscription?.isCurrentlyActive == true

    var editName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
    var editPhoto by remember(profile?.photoUrl) { mutableStateOf(profile?.photoUrl ?: "") }
    var supportSubject by remember { mutableStateOf("") }
    var supportMessage by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }
    var legalDialogType by remember { mutableStateOf<String?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NetworkAndNotificationHelper.postLocalNotificationIfPermitted(
                context = context,
                notificationId = 1001,
                title = "MOVIORA",
                body = "تم تفعيل إشعارات MOVIORA الفورية بنجاح!"
            )
            viewModel.postToast("تم تفعيل صلاحية الإشعارات بنجاح")
        } else {
            viewModel.postToast("يمكنك تفعيل الإشعارات لاحقاً من إعدادات النظام")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_and_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Standalone Internal App Profile Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MovioraGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MovioraGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "وضع التطبيق الداخلي المستقل • يعمل مباشرة بدون حساب خارجي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MovioraGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MovioraPurpleDeep)
                                .border(2.dp, MovioraGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (profile?.displayName ?: "M").take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MovioraGold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.displayName ?: "مشترك MOVIORA",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "قاعدة بيانات محلية مدمجة (Room SQLite)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isVip) "عضوية VIP مفعّلة محلياً ★" else "الوضع المجاني (يمكنك تفعيل VIP للتجربة من تبويب VIP)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MovioraGold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("الاسم المعروض داخل التطبيق") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhoto,
                        onValueChange = { editPhoto = it },
                        label = { Text("رابط الصورة الرمزية (HTTPS اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.updateProfileInfo(editName, editPhoto) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("حفظ الاسم والإعدادات المحلية")
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إظهار لوحة الإدارة في الشريط العلوي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "يتيح لك التبديل الفوري بين تجربة المدير وتجربة المشاهد العادي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAdmin,
                            onCheckedChange = { viewModel.toggleAdminPreviewMode(it) },
                            modifier = Modifier.testTag("toggle_admin_preview_switch")
                        )
                    }
                }
            }
        }

        // 2. Language & Appearance Card (8 Languages + 4 Themes)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.languageSetting, style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MovioraLocalization.supportedLanguages, key = { it.code }) { lang ->
                            FilterChip(
                                selected = settings.languageCode == lang.code,
                                onClick = { viewModel.setLanguage(lang.code) },
                                label = { Text("${lang.nativeName} (${lang.arabicName})") }
                            )
                        }
                    }

                    HorizontalDivider()
                    Text(strings.themeSetting, style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val themes = listOf(
                            "dark" to "الداكن الفاخر (الافتراضي)",
                            "light" to "الفاتح العاجي",
                            "vip_gold" to "الذهبي الإمبراطوري (VIP)",
                            "vip_velvet" to "المخملي الملكي (VIP)"
                        )
                        items(themes) { (code, label) ->
                            val locked = code.startsWith("vip_") && !isVip
                            FilterChip(
                                selected = settings.themeMode == code,
                                onClick = {
                                    if (locked) {
                                        viewModel.postToast("فعّل باقة VIP التجريبية من تبويب VIP لفتح هذا الثيم فوراً")
                                    } else {
                                        viewModel.setThemeMode(code)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // 3. MOVIORA Quality Engine & Playback Preferences
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.qualityEngineTitle, style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MovioraQualityPreset.entries) { preset ->
                            FilterChip(
                                selected = settings.qualityPreset == preset,
                                onClick = { viewModel.setQualityPreset(preset) },
                                label = { Text(preset.labelAr) }
                            )
                        }
                    }

                    Text("سقف الجودة على شبكة Wi-Fi:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(720 to "720p HD", 1080 to "1080p Full HD", 2160 to "4K Ultra HD")) { (h, lbl) ->
                            FilterChip(
                                selected = settings.wifiDefaultMaxHeight == h,
                                onClick = { viewModel.setWifiAndMobileQuality(h, settings.mobileDefaultMaxHeight) },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Text("سقف الجودة على بيانات الهاتف (Mobile Data):", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(360 to "360p توفير", 480 to "480p", 720 to "720p HD", 1080 to "1080p")) { (h, lbl) ->
                            FilterChip(
                                selected = settings.mobileDefaultMaxHeight == h,
                                onClick = { viewModel.setWifiAndMobileQuality(settings.wifiDefaultMaxHeight, h) },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التشغيل التلقائي للحلقة التالية")
                        Switch(
                            checked = settings.autoPlayNextEpisode,
                            onCheckedChange = { viewModel.setAutoPlayNext(it) }
                        )
                    }
                }
            }
        }

        // 4. Notifications & Cache Management
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إعدادات الإشعارات", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(
                                onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            ) {
                                Text("تفعيل إشعارات النظام")
                            }
                        }
                    }

                    SettingToggleRow("إشعارات الحلقات الجديدة", settings.notifNewEpisodes) {
                        viewModel.updateNotificationsConfig(it, settings.notifNewReleases, settings.notifRecommendations, settings.notifSubscriptionAlerts)
                    }
                    SettingToggleRow("إشعارات الإصدارات الجديدة", settings.notifNewReleases) {
                        viewModel.updateNotificationsConfig(settings.notifNewEpisodes, it, settings.notifRecommendations, settings.notifSubscriptionAlerts)
                    }
                    SettingToggleRow("توصيات مخصصة", settings.notifRecommendations) {
                        viewModel.updateNotificationsConfig(settings.notifNewEpisodes, settings.notifNewReleases, it, settings.notifSubscriptionAlerts)
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الذاكرة المؤقتة (Cache)", style = MaterialTheme.typography.titleSmall)
                            Text("الحجم الحالي: ${formatBytesHuman(cacheBytes)}", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { viewModel.clearAppCache() }) {
                            Text("مسح الذاكرة المؤقتة")
                        }
                    }
                }
            }
        }

        // 5. Active Local Session Info
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("الجلسة المحلية النشطة (${sessions.size})", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                    sessions.forEach { sess ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(sess.deviceModel, style = MaterialTheme.typography.bodyMedium)
                                Text(sess.platform, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MovioraSuccess
                            )
                        }
                    }
                }
            }
        }

        // 6. Help Center, Legal & Local Data Reset
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ملاحظات ومقترحات التجربة", style = MaterialTheme.typography.titleMedium, color = MovioraGold)
                    OutlinedTextField(
                        value = supportSubject,
                        onValueChange = { supportSubject = it },
                        label = { Text("عنوان الملاحظة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supportMessage,
                        onValueChange = { supportMessage = it },
                        label = { Text("تفاصيل الملاحظة أو الاختبار") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (supportSubject.length >= 3 && supportMessage.length >= 5) {
                                viewModel.submitSupportTicket(supportSubject, supportMessage)
                                supportSubject = ""
                                supportMessage = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(if (isVip) "حفظ الملاحظة بأولوية VIP" else "حفظ الملاحظة محلياً")
                    }

                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { legalDialogType = "PRIVACY" }) { Text("سياسة الخصوصية") }
                        TextButton(onClick = { legalDialogType = "TERMS" }) { Text("شروط الاستخدام") }
                    }
                    Text(
                        text = "إصدار التطبيق: MOVIORA v1.0.0 (Standalone Internal Build)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MovioraDanger),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("reset_local_data_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("إعادة ضبط البيانات التجريبية المحلية")
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("إعادة ضبط البيانات المحلية") },
            text = { Text("سيتم مسح المفضلة، سجل المشاهدة، القوائم المخصصة، والتعديلات الإدارية المحفوظة محلياً وإعادة التطبيق إلى حالته الأولية النظيفة. هل تريد المتابعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        viewModel.resetLocalDemoData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MovioraDanger)
                ) {
                    Text("نعم، إعادة الضبط")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("تراجع") }
            }
        )
    }

    if (legalDialogType != null) {
        AlertDialog(
            onDismissRequest = { legalDialogType = null },
            title = {
                Text(if (legalDialogType == "PRIVACY") "سياسة خصوصية MOVIORA" else "شروط الاستخدام وحقوق الملكية")
            },
            text = {
                Text(
                    if (legalDialogType == "PRIVACY") {
                        "يعمل تطبيق MOVIORA في الوضع الداخلي المستقل مع حفظ جميع تفضيلاتك، قوائمك، وسجل مشاهداتك محلياً على الجهاز دون الحاجة لتسجيل الدخول بأي حساب خارجي."
                    } else {
                        "تقتصر المشاهدة والتنزيل داخل MOVIORA على المصادر المرخصة قانونياً أو الواقعة في الملك العام (Public Domain / CC). الأعمال الأرشيفية التي لا تملك رخصة بث نشطة تُعرض كسجل توثيقي معلوماتي حتى يتم ربط مصدر مرخص من لوحة الإدارة."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { legalDialogType = null }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
