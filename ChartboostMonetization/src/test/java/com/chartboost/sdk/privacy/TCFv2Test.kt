package com.chartboost.sdk.privacy

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Before
import org.junit.Test

private const val TCFSTRING_KEY = "IABTCF_TCString"

class TCFv2Test {
    private val sharedPreferencesMock = mockk<SharedPreferences>()
    private val tcfstringMock = "base64url-encoded TC string with segments"
    private lateinit var tcfv2: TCFv2

    @Before
    fun setup() {
        every { sharedPreferencesMock.getString(TCFSTRING_KEY, null) } returns tcfstringMock
        tcfv2 = TCFv2(sharedPreferencesMock)
    }

    @Test
    fun `tcfv2 tcf string`() {
        val tcfString = tcfv2.getTCFString()
        assertEquals(tcfstringMock, tcfString)
    }

    @Test
    fun `tcfv2 tcf string is null`() {
        every { sharedPreferencesMock.getString(TCFSTRING_KEY, null) } returns null
        val tcfString = tcfv2.getTCFString()
        assertNull(tcfString)
    }
}
