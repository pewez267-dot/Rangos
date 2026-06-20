package com.switchtune.app.ui.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.switchtune.app.BuildConfig
import com.switchtune.app.R
import com.switchtune.app.data.billing.BillingManager

@Composable
fun PaywallScreen(
    state: BillingManager.BillingState,
    onBuy: (Activity) -> Unit,
    onRestore: () -> Unit,
    onClearError: () -> Unit,
    onDebugUnlock: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = when (state.error) {
        null -> null
        else -> stringResource(R.string.paywall_error)
    }
    LaunchedEffect(state.error) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(88.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.paywall_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (state.entitlement == BillingManager.Entitlement.PENDING) {
                Text(
                    text = stringResource(R.string.paywall_pending),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Spacer(Modifier.height(32.dp))

            val priceLabel = state.formattedPrice ?: "$1.95"
            Button(
                onClick = { activity?.let(onBuy) },
                enabled = activity != null && state.entitlement != BillingManager.Entitlement.PENDING,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(stringResource(R.string.paywall_buy, priceLabel))
            }
            TextButton(
                onClick = onRestore,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.paywall_restore))
            }

            // DEBUG-only shortcut so the app can be tested locally before the
            // Play Console product exists. Stripped from release builds.
            if (BuildConfig.DEBUG) {
                TextButton(
                    onClick = onDebugUnlock,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("Skip (debug only)")
                }
            }
        }
    }
}
