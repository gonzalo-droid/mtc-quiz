package com.gondroid.core.domain.repository

import com.gondroid.core.domain.model.SubscriptionPlan
import kotlinx.coroutines.flow.Flow

interface PremiumRepository {

    /** True si el usuario tiene una suscripción premium activa (o la tenía en el último refresh). */
    val isPremiumFlow: Flow<Boolean>

    /** Planes disponibles con su precio real, tal como los devuelve Google Play. Vacío hasta llamar [loadAvailablePlans]. */
    val availablePlansFlow: Flow<List<SubscriptionPlan>>

    /** Consulta a Google Play los productos de suscripción y actualiza [availablePlansFlow]. */
    suspend fun loadAvailablePlans()

    /** Consulta a Google Play si hay compras activas y actualiza [isPremiumFlow]. Llamar en cada inicio de la app. */
    suspend fun refreshPurchaseState()

    /** Restaura compras (reinstalación / cambio de device). Internamente hace lo mismo que [refreshPurchaseState]. */
    suspend fun restorePurchases()
}
