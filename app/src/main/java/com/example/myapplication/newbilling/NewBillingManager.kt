package com.example.myapplication.newbilling

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class NewBillingManager(
    private val context: Context
) {
    private lateinit var billingClient: BillingClient
    var onProductDetailsReceived: ((List<ProductDetails>) -> Unit)? = null
    var onPurchaseCompleted: ((Purchase) -> Unit)? = null
    var onPurchaseFailed: ((BillingResult) -> Unit)? = null

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    purchases.forEach { purchase ->
                        handlePurchase(purchase)
                    }
                } else {
                    onPurchaseFailed?.invoke(billingResult)
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryAvailableProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle disconnection
            }
        })
    }

    private fun queryAvailableProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("android.test.purchased")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("android.test.canceled")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("android.test.refunded")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("android.test.item_unavailable")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                onProductDetailsReceived?.invoke(productDetailsList)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            onPurchaseCompleted?.invoke(purchase)
            // Acknowledge the purchase if necessary
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun endBillingConnection() {
        billingClient.endConnection()
    }
}