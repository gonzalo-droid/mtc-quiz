package com.gondroid.core.data.analytics

import com.google.common.truth.Truth.assertThat
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerImplTest {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var manager: AnalyticsManagerImpl

    @Before
    fun setUp() {
        firebaseAnalytics = mockk(relaxed = true)
        manager = AnalyticsManagerImpl(firebaseAnalytics)
    }

    @Test
    fun `logPaywallViewed logs paywall_viewed with no params`() {
        manager.logPaywallViewed()
        verify { firebaseAnalytics.logEvent("paywall_viewed", null) }
    }

    @Test
    fun `logSubscribeClicked logs product id param`() {
        val bundleSlot = slot<android.os.Bundle>()
        manager.logSubscribeClicked("mtcquiz_premium_monthly")
        verify { firebaseAnalytics.logEvent("premium_subscribe_clicked", capture(bundleSlot)) }
        assertThat(bundleSlot.captured.getString("product_id")).isEqualTo("mtcquiz_premium_monthly")
    }

    @Test
    fun `logPurchaseFailed logs product id and error code`() {
        val bundleSlot = slot<android.os.Bundle>()
        manager.logPurchaseFailed("mtcquiz_premium_annual", errorCode = 7)
        verify { firebaseAnalytics.logEvent("premium_purchase_failed", capture(bundleSlot)) }
        assertThat(bundleSlot.captured.getString("product_id")).isEqualTo("mtcquiz_premium_annual")
        assertThat(bundleSlot.captured.getInt("error_code")).isEqualTo(7)
    }

    @Test
    fun `logRestoreCompleted logs is_premium boolean param`() {
        val bundleSlot = slot<android.os.Bundle>()
        manager.logRestoreCompleted(isPremium = true)
        verify { firebaseAnalytics.logEvent("premium_restore_completed", capture(bundleSlot)) }
        assertThat(bundleSlot.captured.getBoolean("is_premium")).isTrue()
    }
}
