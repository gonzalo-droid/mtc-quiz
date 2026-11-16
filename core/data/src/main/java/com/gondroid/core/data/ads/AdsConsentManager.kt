package com.gondroid.core.data.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Google's UMP consent flow, which has to resolve before the Mobile Ads SDK starts or any ad is
 * requested (AdMob terms for users in the EEA, the UK and Switzerland).
 */
interface AdsConsentManager {
    /** True once UMP allows ad requests; every ad load must check it first. */
    val canRequestAds: StateFlow<Boolean>

    /** True when the user is in a region where a "change my choice" entry point is mandatory. */
    val isPrivacyOptionsRequired: StateFlow<Boolean>

    /**
     * Refreshes the consent status, shows the form when UMP says it is needed and starts the
     * Mobile Ads SDK once ads may be requested. Call on every launch, from the first activity.
     */
    fun gatherConsent(activity: Activity)

    /** Re-opens the consent form so the user can change their choice. */
    fun showPrivacyOptionsForm(activity: Activity)
}
