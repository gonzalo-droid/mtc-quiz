package com.gondroid.core.domain.model

data class SubscriptionPlan(
    val productId: String,
    val billingPeriod: BillingPeriod,
    val formattedPrice: String,
)

enum class BillingPeriod { MONTHLY, ANNUAL }
