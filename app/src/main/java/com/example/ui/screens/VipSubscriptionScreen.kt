package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovioraViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VipSubscriptionScreen(viewModel: MovioraViewModel) {
    val subscription by viewModel.subscription.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val billingState by viewModel.billingState.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vip_subscription_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Crown VIP Header
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.5.dp, MovioraGold, RoundedCornerShape(24.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.movioraColors.vipHeaderStart, DeepPurple, CinematicBlack)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = MovioraGold,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "MOVIORA VIP",
                                fontFamily = CinzelDisplayFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = MovioraGold
                            )
                        }
                        Text(
                            text = "التجربة السينمائية الملكية الكاملة بأعلى دقة 4K / HDR مع تنزيل أوفلاين وثيمات ذهبية ومخملية حصرية — متاحة للتفعيل الفوري والتجربة المباشرة داخل التطبيق.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MovioraWarmIvory
                        )
                    }
                }
            }
        }

        // Active Subscription Card (if present)
        val sub = subscription
        if (sub != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (sub.isCurrentlyActive) "حالة اشتراكك: نشط ومفعّل للتجربة ★" else "حالة الاشتراك: ${sub.status}",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (sub.isCurrentlyActive) MovioraSuccess else MovioraWarning
                        )
                        Text("الباقة: ${sub.planId} • الوضع: تجربة داخلية مباشرة")
                        Text("تاريخ البدء: ${dateFormatter.format(Date(sub.startsAtEpochMs))}")
                        Text("صالح حتى: ${dateFormatter.format(Date(sub.expiresAtEpochMs))}")

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (sub.status == "active") {
                                OutlinedButton(
                                    onClick = { viewModel.cancelVipAutoRenew() },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("إيقاف التجديد التلقائي")
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.deactivateLocalVipPreview() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MovioraDanger),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("deactivate_vip_preview_btn")
                            ) {
                                Text("إنهاء وضع VIP التجريبي")
                            }
                        }
                    }
                }
            }
        }

        // Section Header
        item {
            Text(
                text = "اختر باقة VIP لتفعيلها وتجربتها فوراً داخل التطبيق",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Monthly / Quarterly / Annual Plan Cards
        items(billingState.plans, key = { it.planId }) { plan ->
            val isRecommended = plan.planId == "vip_yearly"
            val isCurrentPlan = sub?.isCurrentlyActive == true && sub.planId == plan.planId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isRecommended || isCurrentPlan) 1.5.dp else 0.5.dp,
                        color = if (isCurrentPlan) MovioraSuccess else if (isRecommended) MovioraGold else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(20.dp)
                    ),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.titleAr, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(plan.displayPrice, style = MaterialTheme.typography.headlineMedium, color = MovioraGold)
                        }
                        Surface(
                            color = MovioraGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = if (isCurrentPlan) "الباقة النشطة حالياً" else plan.savingsBadgeAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MovioraGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    plan.featuresAr.forEach { feat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MovioraGold, modifier = Modifier.size(16.dp))
                            Text(feat, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = { viewModel.activateLocalVipPreview(plan) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("subscribe_btn_${plan.planId}")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isCurrentPlan) "تمديد الباقة التجريبية (${plan.periodAr})"
                            else "تفعيل فوري للتجربة داخل التطبيق (${plan.periodAr})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Local Invoices History
        if (payments.isNotEmpty()) {
            item {
                Text(
                    text = "سجل التفعيلات والفواتير التجريبية",
                    style = MaterialTheme.typography.titleLarge,
                    color = MovioraGold
                )
            }
            items(payments, key = { it.id }) { pay ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MovioraGold)
                            Column {
                                Text("الباقة: ${pay.planId} • ${pay.amountDisplay}", style = MaterialTheme.typography.titleSmall)
                                Text("رقم العملية: ${pay.orderId}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text("مفعّلة", color = MovioraSuccess, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
