package com.tomsphone.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val skuConfig: BillingSkuConfig,
    private val entitlementRepository: EntitlementRepository
) {
    private companion object {
        const val TAG = "BillingCoordinator"
    }

    private val _connectionReady = MutableStateFlow(false)
    val connectionReady: StateFlow<Boolean> = _connectionReady.asStateFlow()

    private val _productDetailsById = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsById: StateFlow<Map<String, ProductDetails>> = _productDetailsById.asStateFlow()

    private var billingClient: BillingClient? = null

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.w(TAG, "Purchase update: ${billingResult.responseCode} ${billingResult.debugMessage}")
        }
    }

    fun connectIfNeeded() {
        if (billingClient?.isReady == true) {
            _connectionReady.value = true
            queryProductDetails()
            syncOwnedPurchases()
            return
        }
        // Required for in-app (one-time) products; BillingClient throws IllegalArgumentException if omitted.
        @Suppress("DEPRECATION")
        val client = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        billingClient = client
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionReady.value = true
                    queryProductDetails()
                    syncOwnedPurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                    _connectionReady.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionReady.value = false
            }
        })
    }

    fun launchPurchaseFlow(activity: Activity, productId: String): Boolean {
        val client = billingClient ?: return false
        if (!client.isReady) return false
        val details = _productDetailsById.value[productId] ?: run {
            Log.w(TAG, "No ProductDetails for $productId")
            return false
        }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(params))
            .build()
        val result = client.launchBillingFlow(activity, flowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun queryProductDetails() {
        val client = billingClient ?: return
        val productList = buildList {
            if (skuConfig.standardLifetimeProductId.isNotBlank()) {
                add(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(skuConfig.standardLifetimeProductId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                )
            }
            if (skuConfig.earlyAdopterLifetimeProductId.isNotBlank() &&
                skuConfig.earlyAdopterLifetimeProductId != skuConfig.standardLifetimeProductId
            ) {
                add(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(skuConfig.earlyAdopterLifetimeProductId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                )
            }
        }
        if (productList.isEmpty()) {
            Log.w(TAG, "queryProductDetails: no product IDs configured")
            return
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        client.queryProductDetailsAsync(params) { billingResult, list ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryProductDetails failed: ${billingResult.responseCode}")
                return@queryProductDetailsAsync
            }
            val map = list.associateBy { it.productId }
            _productDetailsById.value = map
        }
    }

    fun syncOwnedPurchases() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryPurchases failed: ${billingResult.responseCode}")
                return@queryPurchasesAsync
            }
            purchases.forEach { handlePurchase(it) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val productId = purchase.products.firstOrNull() ?: return
        val matchesStandard = productId == skuConfig.standardLifetimeProductId
        val matchesEarly =
            skuConfig.earlyAdopterLifetimeProductId.isNotBlank() &&
                productId == skuConfig.earlyAdopterLifetimeProductId
        if (!matchesStandard && !matchesEarly) {
            return
        }
        val client = billingClient ?: return
        if (!purchase.isAcknowledged) {
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(acknowledgeParams) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    ioScope.launch { entitlementRepository.setPurchasedProductId(productId) }
                } else {
                    Log.w(TAG, "Acknowledge failed: ${result.responseCode}")
                }
            }
        } else {
            ioScope.launch { entitlementRepository.setPurchasedProductId(productId) }
        }
    }
}
