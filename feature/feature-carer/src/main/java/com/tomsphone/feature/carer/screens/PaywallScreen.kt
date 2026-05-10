package com.tomsphone.feature.carer.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.billingclient.api.ProductDetails
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.billing.PaywallViewModel
import com.tomsphone.feature.carer.components.CarerBreadcrumb

private fun ProductDetails.formattedOneTimePrice(): String? {
    return oneTimePurchaseOfferDetails?.formattedPrice
}

/**
 * Shown after the one-month trial when the user has not purchased.
 * Play Console formatted price is shown when loaded (product IDs from app BuildConfig).
 */
@Composable
fun PaywallScreen(
    onBack: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val entitlement by viewModel.entitlement.collectAsState()
    val products by viewModel.productDetails.collectAsState()
    val billingReady by viewModel.billingReady.collectAsState()

    val standardPd = products[viewModel.standardProductId]
    val earlyPd = products[viewModel.earlyAdopterProductId]

    LaunchedEffect(Unit) {
        viewModel.refreshPurchases()
    }

    var showReviewCode by remember { mutableStateOf(false) }
    var reviewInput by remember { mutableStateOf("") }
    var reviewMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(Modifier.fillMaxSize()) {
            CarerBreadcrumb(
                title = "Purchase",
                parentTitle = "Settings",
                onBack = onBack
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
            ) {
                Text(
                    text = "Continue with Wanda’s Phone",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.wandasColors.onBackground
                )
                Text(
                    text = when {
                        entitlement.isTrialPeriod && (entitlement.trialDaysRemainingInclusive ?: 0) > 0 ->
                            "You still have time on your free month. You can buy now or return before the trial ends."
                        else ->
                            "Your free month has ended. Complete a one-time purchase to keep assistant access to settings."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.88f)
                )

                Text(
                    text = "The purchase button uses the price from Google Play for your lifetime product.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.75f)
                )

                if (!billingReady) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Connecting to Google Play…",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = {
                        if (activity != null) viewModel.launchStandardPurchase(activity)
                    },
                    enabled = activity != null && standardPd != null && billingReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium)
                ) {
                    val price = standardPd?.formattedOneTimePrice()
                    Text(
                        text = if (price != null) "Lifetime unlock — $price" else "Lifetime unlock",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (earlyPd != null) {
                    OutlinedButton(
                        onClick = {
                            if (activity != null) viewModel.launchEarlyAdopterPurchase(activity)
                        },
                        enabled = activity != null && billingReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium)
                    ) {
                        val price = earlyPd.formattedOneTimePrice()
                        Text(
                            text = if (price != null) "Early adopter — $price" else "Early adopter",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "Early adopter pricing is a separate Play product — remove or unpublish it when the window closes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.6f)
                    )
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium)
                ) {
                    Text("Back to home")
                }

                TextButton(onClick = { showReviewCode = !showReviewCode }) {
                    Text("Have a review or license code?")
                }

                AnimatedVisibility(visible = showReviewCode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = reviewInput,
                            onValueChange = { reviewInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Code") },
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                viewModel.submitReviewCode(reviewInput) { ok ->
                                    reviewMessage = if (ok) {
                                        "Unlocked. Thank you."
                                    } else {
                                        "That code did not match."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit code")
                        }
                        reviewMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}
