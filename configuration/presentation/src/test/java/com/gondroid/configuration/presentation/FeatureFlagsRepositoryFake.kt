package com.gondroid.configuration.presentation

import com.gondroid.core.domain.repository.FeatureFlagsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Fake de flags remotos: el test cambia [premiumOfferEnabled] para simular un cambio en Remote Config. */
class FeatureFlagsRepositoryFake(
    isPremiumOfferEnabled: Boolean = true,
    studyBannerInterval: Int = 10
) : FeatureFlagsRepository {

    val premiumOfferEnabled = MutableStateFlow(isPremiumOfferEnabled)
    override val isPremiumOfferEnabled: StateFlow<Boolean> = premiumOfferEnabled

    val bannerInterval = MutableStateFlow(studyBannerInterval)
    override val studyBannerInterval: StateFlow<Int> = bannerInterval

    var refreshCount = 0
        private set

    override suspend fun refresh() {
        refreshCount++
    }
}
