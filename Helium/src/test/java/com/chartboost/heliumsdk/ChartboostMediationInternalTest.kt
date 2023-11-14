/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import android.content.Context
import android.content.SharedPreferences
import com.chartboost.heliumsdk.controllers.PartnerController
import com.chartboost.heliumsdk.controllers.PrivacyController
import com.chartboost.heliumsdk.domain.GdprConsentStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class ChartboostMediationInternalTest {
    @MockK
    private lateinit var mockedContext: Context

    @MockK
    private lateinit var mockedSharedPreferences: SharedPreferences

    @MockK
    private lateinit var mockedPrivacyController: PrivacyController

    @MockK
    private lateinit var mockedPartnerController: PartnerController

    private var subject: ChartboostMediationInternal = ChartboostMediationInternal()

    @Before
    fun setUp() {
        mockedContext = mockk()
        mockedPrivacyController = mockk()
        mockedPartnerController = mockk()
        subject = ChartboostMediationInternal(mockedPartnerController)
        subject.appContext = mockedContext
        subject.privacyController = mockedPrivacyController

        every { mockedPrivacyController.gdpr } returns -1
        every { mockedPrivacyController.userConsent } returns false
        every { mockedPrivacyController.gdpr = any() } just Runs
        every { mockedPrivacyController.userConsent = any() } just Runs
        every { mockedPrivacyController.ccpaConsent } returns null
        every { mockedPrivacyController.ccpaConsent = any() } just Runs
        every { mockedPrivacyController.coppa } returns false
        every { mockedPrivacyController.coppa = any() } just Runs
        every { mockedPartnerController.setGdpr(any(), any(), any(), any()) } just Runs
        every { mockedPartnerController.setCcpaConsent(any(), any(), any(), any()) } just Runs
        every { mockedPartnerController.setUserSubjectToCoppa(any(), any()) } just Runs
    }

    @Test
    fun `setSubjectToGdpr to false should set subject to GDPR to false`() {
        subject.setSubjectToGdpr(false)

        verify { mockedPrivacyController.gdpr = 0 }
        verify { mockedPrivacyController.userConsent = false }
        verify {
            mockedPartnerController.setGdpr(
                mockedContext, false, GdprConsentStatus.GDPR_CONSENT_DENIED, subject.partnerConsents
            )
        }
    }

    @Test
    fun `setSubjectToGdpr to true should set subject to GDPR`() {
        subject.setSubjectToGdpr(true)

        verify { mockedPrivacyController.gdpr = 1 }
        verify { mockedPrivacyController.userConsent = false }
        verify {
            mockedPartnerController.setGdpr(
                mockedContext, true, GdprConsentStatus.GDPR_CONSENT_DENIED, subject.partnerConsents
            )
        }
    }

    @Test
    fun `setUserHasGivenConsent should set GDPR consent`() {
        subject.setSubjectToGdpr(true)
        subject.setUserHasGivenConsent(true)

        verify { mockedPrivacyController.gdpr = 1 }
        verify { mockedPrivacyController.userConsent = true }
        verify {
            mockedPartnerController.setGdpr(
                mockedContext, true, GdprConsentStatus.GDPR_CONSENT_GRANTED, subject.partnerConsents
            )
        }
    }

    @Test
    fun `setCcpaConsent to false should propagate value`() {
        subject.setCcpaConsent(false)

        verify { mockedPrivacyController.ccpaConsent = false }
        verify { mockedPartnerController.setCcpaConsent(mockedContext, false, "1YY-", subject.partnerConsents) }
    }

    @Test
    fun `setCcpaConsent to true should propagate value`() {
        subject.setCcpaConsent(true)

        verify { mockedPrivacyController.ccpaConsent = true }
        verify { mockedPartnerController.setCcpaConsent(mockedContext, true, "1YN-", subject.partnerConsents) }
    }

    @Test
    fun `setSubjectToCoppa to false should propagate value`() {
        subject.setSubjectToCoppa(false)

        verify { mockedPrivacyController.coppa = false }
        verify { mockedPartnerController.setUserSubjectToCoppa(mockedContext, false) }
    }

    @Test
    fun `setSubjectToCoppa to true should propagate value`() {
        subject.setSubjectToCoppa(true)

        verify { mockedPrivacyController.coppa = true }
        verify { mockedPartnerController.setUserSubjectToCoppa(mockedContext, true) }
    }
}
