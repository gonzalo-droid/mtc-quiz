package com.gondroid.core.data.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/**
 * Gestiona la conexión con Google Play Billing para suscripciones.
 *
 * Flujo típico:
 * 1. La app observa [isPremiumFlow] para saber si mostrar ads o no
 * 2. El usuario toca "Suscribirme" → se llama [launchSubscription]
 * 3. Google Play muestra el sheet de pago
 * 4. Si el pago es exitoso, [isPremiumFlow] emite true automáticamente
 * 5. En cada inicio de app, [refreshPurchaseState] verifica con Google si sigue activa
 */
interface BillingManager {

    /**
     * Flow que emite true si el usuario tiene una suscripción premium activa.
     * Todos los puntos de ads observan este flow para decidir si mostrar o no.
     */
    val isPremiumFlow: Flow<Boolean>

    /**
     * Abre el sheet de pago de Google Play para comprar la suscripción.
     * Requiere una Activity para mostrar la UI de Google.
     * Retorna true si el flujo de compra se inició correctamente.
     */
    suspend fun launchSubscription(activity: Activity): Boolean

    /**
     * Consulta a Google Play si hay compras activas y actualiza [isPremiumFlow].
     * Llamar en cada inicio de la app (en caso de que el usuario haya cancelado
     * o renovado desde otra parte).
     */
    suspend fun refreshPurchaseState()

    /**
     * Restaura compras (útil si el usuario reinstala la app o cambia de device).
     * Internamente hace lo mismo que [refreshPurchaseState].
     */
    suspend fun restorePurchases()
}
