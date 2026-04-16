package com.gondroid.core.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BillingManager {

    companion object {
        /**
         * Este es el Product ID que definirás en Google Play Console.
         * Debe coincidir EXACTAMENTE con el que crees allá.
         */
        const val PRODUCT_ID = "mtcquiz_premium_annual"
    }

    /**
     * Estado interno de si el usuario es premium.
     * MutableStateFlow para que la UI se actualice reactivamente.
     */
    private val _isPremium = MutableStateFlow(false)
    override val isPremiumFlow: Flow<Boolean> = _isPremium.asStateFlow()

    /**
     * Listener que Google Play llama cuando el usuario completa (o cancela) una compra.
     * Este es el "callback" principal del flujo de pago.
     */
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // El usuario pagó exitosamente
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("Billing: user canceled subscription flow")
            }
            else -> {
                Timber.e("Billing: purchase failed with code ${billingResult.responseCode}")
            }
        }
    }

    /**
     * BillingClient es el punto de entrada a Google Play Billing.
     * Se configura una sola vez y se reutiliza.
     * - enablePendingPurchases(): requerido por Google para soportar compras pendientes
     * - setListener: recibe el callback de compras
     */
    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    /**
     * Conecta el BillingClient a Google Play.
     * Es una operación asíncrona — usamos suspendCancellableCoroutine
     * para convertirla en una suspend function.
     */
    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true

        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Timber.d("Billing: connected successfully")
                        if (cont.isActive) cont.resume(true)
                    } else {
                        Timber.e("Billing: connection failed with code ${result.responseCode}")
                        if (cont.isActive) cont.resume(false)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.w("Billing: service disconnected")
                }
            })
        }
    }

    /**
     * Abre el sheet de pago de Google Play.
     *
     * Pasos internos:
     * 1. Conectar al BillingClient
     * 2. Consultar los detalles del producto (nombre, precio) a Google Play
     * 3. Construir los parámetros del flujo de compra
     * 4. Lanzar el flujo (Google muestra su UI de pago)
     */
    override suspend fun launchSubscription(activity: Activity): Boolean {
        if (!ensureConnected()) return false

        // Paso 1: Definir qué producto queremos consultar
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS) // SUBS = suscripción
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        // Paso 2: Consultar detalles del producto a Google Play
        val result: ProductDetailsResult = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.e("Billing: failed to query product details: ${result.billingResult.debugMessage}")
            return false
        }

        val productDetails = result.productDetailsList?.firstOrNull()
        if (productDetails == null) {
            Timber.e("Billing: product '$PRODUCT_ID' not found in Play Console")
            return false
        }

        // Paso 3: Obtener la oferta (plan base de la suscripción)
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Timber.e("Billing: no offer found for product '$PRODUCT_ID'")
            return false
        }

        // Paso 4: Construir y lanzar el flujo de compra
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        val flowResult = billingClient.launchBillingFlow(activity, flowParams)
        return flowResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Procesa una compra exitosa.
     *
     * IMPORTANTE: Google requiere que "acknowledges" (confirmes) la compra
     * dentro de 3 días. Si no lo hacés, Google la reembolsa automáticamente.
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            _isPremium.value = true

            // Acknowledge si no lo hicimos aún
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Timber.d("Billing: purchase acknowledged successfully")
                    } else {
                        Timber.e("Billing: failed to acknowledge purchase: ${result.debugMessage}")
                    }
                }
            }
        }
    }

    /**
     * Verifica con Google Play si hay suscripciones activas.
     * Se llama en cada inicio de la app para mantener el estado sincronizado.
     *
     * Ejemplo: si el usuario canceló la suscripción desde Google Play,
     * esta función detecta que ya no hay compras activas y pone isPremium = false.
     */
    override suspend fun refreshPurchaseState() {
        if (!ensureConnected()) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val hasActiveSub = result.purchasesList.any { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.contains(PRODUCT_ID)
            }
            _isPremium.value = hasActiveSub
            Timber.d("Billing: refreshed purchase state, isPremium=$hasActiveSub")
        } else {
            Timber.e("Billing: failed to query purchases: ${result.billingResult.debugMessage}")
        }
    }

    override suspend fun restorePurchases() = refreshPurchaseState()
}
