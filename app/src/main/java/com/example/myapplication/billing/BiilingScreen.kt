package com.example.myapplication.billing

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails

@Composable
fun PurchaseApp() {
    var isPurchased by remember { mutableStateOf(false) }
    var productDetailsList by remember { mutableStateOf<List<ProductDetails>?>(null) }
    var billingManager by remember { mutableStateOf<BillingManager?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        billingManager = BillingManager.getInstance(context).apply {
            setOnPurchaseCompletedListener { purchase ->
                isPurchased = true
                // Handle purchase completion
            }

            setOnPurchaseFailedListener {
                // Handle purchase failure
            }

            setOnProductDetailsReceivedListener { productDetails ->
                // Update the product details list
                productDetailsList = productDetails
            }

            // Query available products
            queryPurchases()
            isPurchased = BillingManager.hasRemovedAds(context)
        }
    }


    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = if (isPurchased) "Product Purchased!" else "No Purchase Yet")

        LazyColumn {
            productDetailsList?.let { products ->
                items(products) { productDetails ->
                    ProductItem(productDetails, onPurchaseClick = { selectedProduct ->
                        activity?.let { act ->
                            billingManager?.launchBillingFlow(act, selectedProduct)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun ProductItem(productDetails: ProductDetails, onPurchaseClick: (ProductDetails) -> Unit) {
    Card(modifier = Modifier.padding(8.dp)) {
        ListItem(modifier = Modifier, headlineContent = { Text(text = productDetails.title) }, supportingContent = { Text(text = productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: "--") }, trailingContent = {
            Button(onClick = { onPurchaseClick(productDetails) }) {
                Text("Buy")
            }
        })
    }
}