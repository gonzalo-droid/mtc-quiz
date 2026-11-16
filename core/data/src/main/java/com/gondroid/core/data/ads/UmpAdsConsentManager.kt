package com.gondroid.core.data.ads

import android.app.Activity
import android.content.Context
import com.gondroid.core.data.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

@Singleton
class UmpAdsConsentManager @Inject constructor(@ApplicationContext private val context: Context) :
    AdsConsentManager {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val isMobileAdsStarted = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    override val isPrivacyOptionsRequired: StateFlow<Boolean> =
        _isPrivacyOptionsRequired.asStateFlow()

    override fun gatherConsent(activity: Activity) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            buildRequestParameters(activity),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { Timber.w("UMP: consent form error %s", it.message) }
                    onConsentResolved()
                }
            },
            { requestError ->
                // Offline or misconfigured: fall back to the consent stored by an earlier launch.
                Timber.w("UMP: consent update failed %s", requestError.message)
                onConsentResolved()
            }
        )
        // Consent given on an earlier launch is still valid: don't make ads wait for the network.
        onConsentResolved()
    }

    override fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { Timber.w("UMP: privacy options error %s", it.message) }
            onConsentResolved()
        }
    }

    private fun onConsentResolved() {
        _isPrivacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        val canRequest = consentInformation.canRequestAds()
        if (canRequest && isMobileAdsStarted.compareAndSet(false, true)) {
            MobileAds.initialize(context)
        }
        _canRequestAds.value = canRequest
    }

    private fun buildRequestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        debugGeography()?.let { geography ->
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(geography)
                    .setForceTesting(true)
                    .build()
            )
        }
        return builder.build()
    }

    private fun debugGeography(): Int? {
        if (!BuildConfig.DEBUG) return null
        return when (BuildConfig.UMP_DEBUG_GEOGRAPHY.uppercase()) {
            "EEA" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
            "NOT_EEA" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA
            else -> null
        }
    }
}
