package com.example.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VipPlanCatalogItem(
    val planId: String,
    val playProductId: String,
    val titleAr: String,
    val periodAr: String,
    val fallbackPriceDisplay: String,
    val savingsBadgeAr: String,
    val featuresAr: List<String>,
    val productDetails: ProductDetails? = null
) {
    val displayPrice: String
        get() = productDetails
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
            ?: fallbackPriceDisplay
}

data class BillingUiState(
    val isConnected: Boolean = false,
    val isProcessing: Boolean = false,
    val plans: List<VipPlanCatalogItem> = defaultPlans(),
    val statusMessageAr: String? = null,
    val isError: Boolean = false
) {
    companion object {
        fun defaultPlans(): List<VipPlanCatalogItem> = listOf(
            VipPlanCatalogItem(
                planId = "vip_monthly",
                playProductId = "moviora_vip_monthly",
                titleAr = "باقة MOVIORA VIP الشهرية",
                periodAr = "شهرياً",
                fallbackPriceDisplay = "$7.99 / شهر",
                savingsBadgeAr = "مرونة كاملة",
                featuresAr = listOf(
                    "مشاهدة كاملة بدون أي إعلانات",
                    "جودة Ultra 4K وHDR في المصادر الداعمة",
                    "تنزيل الأعمال المرخصة للمشاهدة بدون إنترنت",
                    "الوصول المبكر للأعمال الحصرية المرخصة",
                    "ثيمات MOVIORA VIP الذهبية والملكية + دعم أولوية"
                )
            ),
            VipPlanCatalogItem(
                planId = "vip_quarterly",
                playProductId = "moviora_vip_quarterly",
                titleAr = "باقة MOVIORA VIP ربع السنوية",
                periodAr = "كل 3 أشهر",
                fallbackPriceDisplay = "$19.99 / 3 أشهر",
                savingsBadgeAr = "وفّر 17%",
                featuresAr = listOf(
                    "جميع مزايا VIP الشهرية كاملة",
                    "تنزيلات أوفلاين غير محدودة للأعمال المرخصة",
                    "ملفات ترجمة احترافية ومزامنة متقدمة",
                    "أولوية في الدعم الفني المباشر"
                )
            ),
            VipPlanCatalogItem(
                planId = "vip_yearly",
                playProductId = "moviora_vip_yearly",
                titleAr = "باقة MOVIORA VIP السنوية الشاملة",
                periodAr = "سنوياً",
                fallbackPriceDisplay = "$64.99 / سنة",
                savingsBadgeAr = "الأكثر توفيراً — وفّر 32%",
                featuresAr = listOf(
                    "وصول سنوي كامل للأرشيف الحصري المرخص بجودة Ultra",
                    "جميع الثيمات الملكية والذهبية الحصرية",
                    "تنزيلات أوفلاين بأعلى دقة متاحة",
                    "أعلى أولوية في مركز مساعدة MOVIORA"
                )
            )
        )
    }
}

class MovioraBillingManager(
    context: Context,
    private val onVerifiedPurchase: suspend (
        planId: String,
        priceDisplay: String,
        currency: String,
        orderId: String,
        purchaseToken: String
    ) -> Result<Unit>
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        connectAndQueryProducts()
    }

    fun connectAndQueryProducts() {
        if (billingClient.isReady) {
            querySubscriptionProducts()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(isConnected = true)
                    querySubscriptionProducts()
                } else {
                    _state.value = _state.value.copy(
                        isConnected = false,
                        statusMessageAr = "خدمة فوترة Google Play غير متصلة حالياً على هذا الجهاز (رمز ${result.responseCode}).",
                        isError = false
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(isConnected = false)
            }
        })
    }

    private fun querySubscriptionProducts() {
        val productList = BillingUiState.defaultPlans().map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.playProductId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val byId = productDetailsList.associateBy { it.productId }
                val merged = BillingUiState.defaultPlans().map { item ->
                    item.copy(productDetails = byId[item.playProductId])
                }
                _state.value = _state.value.copy(plans = merged, isConnected = true)
            }
        }
    }

    /**
     * Launches the official Google Play Billing sheet.
     * STRICT RULE: Never activates VIP without a real PURCHASED callback and purchaseToken verification.
     */
    fun launchSubscriptionFlow(activity: Activity, plan: VipPlanCatalogItem) {
        if (!billingClient.isReady) {
            connectAndQueryProducts()
            _state.value = _state.value.copy(
                statusMessageAr = "تعذر الاتصال بخدمة Google Play Billing على هذا الجهاز حالياً. لن يتم تفعيل الاشتراك بدون عملية دفع مؤكدة.",
                isError = true
            )
            return
        }

        val details = plan.productDetails
        val offerToken = details?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (details == null || offerToken == null) {
            _state.value = _state.value.copy(
                statusMessageAr = "معرّف الباقة (${plan.playProductId}) بانتظار التفعيل في حساب التاجر على Google Play Console. لا يتم تفعيل VIP إلا بعد تأكيد دفع فعلي من Google Play أو من لوحة الإدارة.",
                isError = true
            )
            return
        }

        _state.value = _state.value.copy(isProcessing = true, statusMessageAr = null, isError = false)
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val launchResult = billingClient.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(
                isProcessing = false,
                statusMessageAr = "تعذر بدء عملية الدفع عبر Google Play: ${launchResult.debugMessage}",
                isError = true
            )
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            connectAndQueryProducts()
            _state.value = _state.value.copy(
                statusMessageAr = "يرجى التأكد من توفر متجر Google Play لاستعادة المشتريات.",
                isError = true
            )
            return
        }
        _state.value = _state.value.copy(isProcessing = true, statusMessageAr = null)
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchases = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (activePurchases.isEmpty()) {
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        statusMessageAr = "لم يتم العثور على اشتراكات نشطة سابقة مرتبطة بحساب Google Play الحالي.",
                        isError = false
                    )
                } else {
                    activePurchases.forEach { handleVerifiedPurchase(it) }
                }
            } else {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    statusMessageAr = "تعذر التحقق من سجل مشتريات Google Play حالياً.",
                    isError = true
                )
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _state.value = _state.value.copy(isProcessing = false)
                } else {
                    for (purchase in purchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            handleVerifiedPurchase(purchase)
                        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                            _state.value = _state.value.copy(
                                isProcessing = false,
                                statusMessageAr = "عملية الدفع قيد المعالجة لدى Google Play؛ سيتم تفعيل الاشتراك فور تأكيد التحصيل.",
                                isError = false
                            )
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    statusMessageAr = "تم إلغاء عملية الدفع من قِبل المستخدم. لم يتم خصم أي مبلغ ولم يتم تفعيل الاشتراك.",
                    isError = false
                )
            }
            else -> {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    statusMessageAr = "فشلت عملية التحقق من الدفع (${billingResult.responseCode}).",
                    isError = true
                )
            }
        }
    }

    private fun handleVerifiedPurchase(purchase: Purchase) {
        scope.launch {
            val productId = purchase.products.firstOrNull() ?: "moviora_vip_monthly"
            val matchedPlan = _state.value.plans.firstOrNull { it.playProductId == productId }
            val planId = matchedPlan?.planId ?: "vip_monthly"
            val priceDisplay = matchedPlan?.displayPrice ?: "$7.99"
            val currency = matchedPlan?.productDetails
                ?.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                ?.priceCurrencyCode ?: "USD"
            val orderId = purchase.orderId?.takeIf { it.isNotBlank() } ?: "gpa_${System.currentTimeMillis()}"

            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { /* acknowledged */ }
            }

            val res = onVerifiedPurchase(
                planId,
                priceDisplay,
                currency,
                orderId,
                purchase.purchaseToken
            )
            _state.value = if (res.isSuccess) {
                _state.value.copy(
                    isProcessing = false,
                    statusMessageAr = "تم التحقق من عملية الدفع عبر Google Play وتفعيل اشتراك MOVIORA VIP بنجاح!",
                    isError = false
                )
            } else {
                _state.value.copy(
                    isProcessing = false,
                    statusMessageAr = "تم الدفع، لكن حدث خطأ أثناء حفظ الفاتورة: ${res.exceptionOrNull()?.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(statusMessageAr = null, isError = false)
    }
}
