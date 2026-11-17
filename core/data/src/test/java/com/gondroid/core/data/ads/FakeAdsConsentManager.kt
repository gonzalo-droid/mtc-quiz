package com.gondroid.core.data.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAdsConsentManager(
    canRequestAds: Boolean = true,
    isPrivacyOptionsRequired: Boolean = false
) : AdsConsentManager {
    override val canRequestAds = MutableStateFlow(canRequestAds)
    override val isPrivacyOptionsRequired = MutableStateFlow(isPrivacyOptionsRequired)
    var gatherConsentCalls = 0
    var privacyOptionsFormCalls = 0

    override fun gatherConsent(activity: Activity) {
        gatherConsentCalls++
    }

    override fun showPrivacyOptionsForm(activity: Activity) {
        privacyOptionsFormCalls++
    }
}
