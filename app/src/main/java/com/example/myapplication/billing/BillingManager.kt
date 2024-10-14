package com.example.myapplication.billing

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.android.billingclient.api.*

/*class BillingManager(private val context: Context, private val listener: BillingListener) {

    private lateinit var billingClient: BillingClient

    interface BillingListener {
        fun onPurchaseCompleted(purchase: Purchase)
        fun onPurchaseFailed()
    }

    fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases() // Required for Billing Library 3.0 and higher
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    listener.onPurchaseFailed()
                }
            }
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Billing service is ready, query available products
                    queryAvailableProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection if needed
            }
        })
    }

    // Query products available for purchase from Play Console
    private fun queryAvailableProducts() {
        val skuList = listOf("your_product_id") // Define product IDs from Play Console
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                // Handle SKU details (show products to user)
            }
        }
    }

    // Launch the billing flow for a product
    fun launchBillingFlow(activity: Activity, skuDetails: SkuDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    // Handle a successful purchase
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            listener.onPurchaseCompleted(purchase)

            // Acknowledge the purchase if necessary (non-consumable products)
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    // Handle acknowledgment result
                }
            }
        }
    }

    // Query past purchases using queryPurchasesAsync()
    fun queryPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchasesList != null) {
                for (purchase in purchasesList) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    // End the connection when not needed anymore
    fun endConnection() {
        billingClient.endConnection()
    }
}*/


class BillingManager private constructor(private val context: Context) {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases() // Required to support pending purchases
        .setListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                onPurchaseFailed?.invoke()
            } else {
                // Handle other error cases if necessary
                Toast.makeText(context, "Billing Error: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
            }
        }
        .build()

    // Listeners for purchase events
    private var onPurchaseCompleted: ((Purchase) -> Unit)? = null
    private var onPurchaseFailed: (() -> Unit)? = null
    private var onProductDetailsReceived: ((List<ProductDetails>) -> Unit)? = null

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Billing service is ready, now query available products
                    queryAvailableProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Implement retry logic if needed
            }
        })
    }

    companion object {
        @Volatile
        private var instance: BillingManager? = null

        // Retrieve a singleton instance of BillingManager
        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context).also { instance = it }
            }
        }

        // Store the ad removal state in SharedPreferences
        fun storeAdRemovalState(context: Context, hasRemovedAds: Boolean) {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("ads_removed", hasRemovedAds).apply()
        }

        // Retrieve the ad removal state from SharedPreferences
        fun hasRemovedAds(context: Context): Boolean {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            return sharedPrefs.getBoolean("ads_removed", false)
        }
    }

    // Setters for listeners
    fun setOnPurchaseCompletedListener(listener: (Purchase) -> Unit) {
        onPurchaseCompleted = listener
    }

    fun setOnPurchaseFailedListener(listener: () -> Unit) {
        onPurchaseFailed = listener
    }

    fun setOnProductDetailsReceivedListener(listener: (List<ProductDetails>) -> Unit) {
        onProductDetailsReceived = listener
    }

    // Query available products from Google Play Console
    private fun queryAvailableProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("example_product_id_1") // Replace with actual product IDs
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("example_product_id_2")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onProductDetailsReceived?.invoke(productDetailsList)
            } else {
                // Handle error in querying product details
                Toast.makeText(context, "Error querying product details: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launch the billing flow for a selected product
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    // Handle completed purchases, including acknowledgment for non-consumable products
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Check each product and handle accordingly
            for (product in purchase.products) {
                when (product) {
                    "remove_ads" -> storeAdRemovalState(context, true)
                    "premium_access" -> {
                        // Handle premium access logic
                    }
                    else -> {
                        // Handle other products if necessary
                    }
                }
            }

            // Acknowledge the purchase if not acknowledged yet
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        onPurchaseCompleted?.invoke(purchase)
                    } else {
                        // Handle acknowledgment failure
                        Toast.makeText(context, "Failed to acknowledge purchase: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                onPurchaseCompleted?.invoke(purchase)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            // Handle pending state and notify the user
            Toast.makeText(context, "Purchase is pending. You will be notified when it's complete.", Toast.LENGTH_LONG).show()
        }
    }

    // Query the user's active purchases
    fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchasesList) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    // End the billing connection when it's no longer needed
    fun endConnection() {
        billingClient.endConnection()
    }
}


