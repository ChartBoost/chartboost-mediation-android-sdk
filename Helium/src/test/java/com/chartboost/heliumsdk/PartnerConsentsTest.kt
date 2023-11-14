/*
 * Copyright 2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PartnerConsentsTest {

    private var subject: PartnerConsents = PartnerConsents()
    private var mockPartnerConsentsObserver: PartnerConsents.PartnerConsentsObserver = mockk()

    @Before
    fun setUp() {
        subject = PartnerConsents()
        mockPartnerConsentsObserver = mockk()
        every { mockPartnerConsentsObserver.onPartnerConsentsUpdated() } just Runs
        subject.addPartnerConsentsObserver(mockPartnerConsentsObserver)
    }

    @Test
    fun `mergePartnerConsentsFromDisk should combine both maps`() {
        val diskConsents = mapOf("abc" to true, "def" to false)
        subject.addPartnerConsents(mapOf("ghi" to true, "jkl" to false))
        subject.mergePartnerConsentsFromDisk(diskConsents)

        val expectedMap = mapOf("abc" to true, "def" to false, "ghi" to true, "jkl" to false)
        assertEquals(expectedMap, subject.getPartnerIdToConsentGivenMapCopy())
    }

    @Test
    fun `mergePartnerConsentsFromDisk should have current consents override disk consents`() {
        val diskConsents = mapOf("abc" to true, "def" to false)
        subject.setPartnerConsent("abc", false)
        subject.mergePartnerConsentsFromDisk(diskConsents)

        val expectedMap = mapOf("abc" to false, "def" to false)
        assertEquals(expectedMap, subject.getPartnerIdToConsentGivenMapCopy())
    }

    @Test
    fun `addPartnerConsent should add consent and fire observers`() {
        val mockPartnerConsentsObserver: PartnerConsents.PartnerConsentsObserver = mockk()
        every { mockPartnerConsentsObserver.onPartnerConsentsUpdated() } just runs
        subject.addPartnerConsentsObserver(mockPartnerConsentsObserver)

        subject.setPartnerConsent("abc", true)

        assertEquals(mapOf("abc" to true), subject.getPartnerIdToConsentGivenMapCopy())
        verify { mockPartnerConsentsObserver.onPartnerConsentsUpdated() }
    }

    @Test
    fun `removePartnerConsent should remove consent and fire observers`() {
        subject.addPartnerConsents(mapOf("abc" to true, "def" to true))

        val actualReturnValue = subject.removePartnerConsent("abc")

        assertEquals(true, actualReturnValue)
        assertEquals(mapOf("def" to true), subject.getPartnerIdToConsentGivenMapCopy())
        verify { mockPartnerConsentsObserver.onPartnerConsentsUpdated() }
    }

    @Test
    fun `replacePartnerConsents should replace all contents and fire observers`() {
        subject.addPartnerConsents(mapOf("abc" to true, "def" to true))
        val expectedMap = mapOf("ghi" to true, "jkl" to false)

        subject.replacePartnerConsents(expectedMap)

        assertEquals(expectedMap, subject.getPartnerIdToConsentGivenMapCopy())
        verify { mockPartnerConsentsObserver.onPartnerConsentsUpdated() }
    }

    @Test
    fun `clear should remove all consents and fire observers`() {
        subject.addPartnerConsents(mapOf("abc" to true, "def" to true))

        subject.clear()

        val actualPartnerConsents = subject.getPartnerIdToConsentGivenMapCopy()
        assertTrue(actualPartnerConsents.isEmpty())
        verify { mockPartnerConsentsObserver.onPartnerConsentsUpdated() }
    }
}
