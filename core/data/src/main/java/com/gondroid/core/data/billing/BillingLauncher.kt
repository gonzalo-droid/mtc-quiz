package com.gondroid.core.data.billing

import android.app.Activity

/**
 * Aparte de [com.gondroid.core.domain.repository.PremiumRepository] porque lanzar el flujo de
 * compra de Play Billing requiere una [Activity], y `core:domain` es un módulo Kotlin puro sin
 * dependencias de Android.
 */
interface BillingLauncher {
    suspend fun launchSubscription(activity: Activity, productId: String): Boolean
}
