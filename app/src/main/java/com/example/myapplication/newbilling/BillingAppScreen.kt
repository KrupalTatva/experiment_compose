package com.example.myapplication.newbilling

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BillingTestScreen(this)
        }
    }
}

@Composable
fun BillingTestScreen(activity: Activity) {
    var purchaseResult by remember { mutableStateOf("No purchases yet") }
    var productDetailsList by remember { mutableStateOf<List<ProductDetails>?>(null) }
    var purchasesList by remember { mutableStateOf<List<Purchase>?>(null) }

    // Declare billingClient
    val billingClient = remember {
        BillingClient.newBuilder(activity)
            .enablePendingPurchases()
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    // Handle purchases
                    for (purchase in purchases) {
                        purchaseResult = "Purchase completed: ${purchase.products.joinToString(", ")}"
                        purchasesList = purchases
                    }
                } else {
                    purchaseResult = "Purchase failed or canceled: ${billingResult.responseCode}"
                }
            }
            .build()
    }

    LaunchedEffect(purchasesList) {
        purchasesList?.let { purchases ->
            for (purchase in purchases) {
                purchaseResult = "Purchase completed: ${purchase.products.joinToString(", ")}"
                handlePurchase(purchase, billingClient)
            }
        }
    }

    // Start BillingClient connection and query products
    LaunchedEffect(Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Ready to query available products
                    queryAvailableProducts(billingClient) { products ->
                        productDetailsList = products
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle disconnection
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Billing Example")

        productDetailsList?.let { products ->
            products.forEach { product ->
                Button(onClick = {
                    launchBillingFlow(activity, billingClient, product)
                }) {
                    Text(text = "Buy ${product.title}")
                }
            }
        } ?: run {
            Text(text = "Loading products...")
        }

        Text(text = purchaseResult)
    }
}

// Function to handle purchases
fun handlePurchase(purchase: Purchase, billingClient: BillingClient) {
    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
        if (!purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Purchase acknowledged
                }
            }
        }
    }
}

// Query available products
fun queryAvailableProducts(billingClient: BillingClient, onProductsQueried: (List<ProductDetails>) -> Unit) {
    val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
        .setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("dummy_product_id_1") // Use your actual product ID
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        )
        .build()

    billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // Send product details to the callback
            onProductsQueried(productDetailsList)
        }
    }
}

// Launch the billing flow
fun launchBillingFlow(activity: Activity, billingClient: BillingClient, productDetails: ProductDetails) {
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
