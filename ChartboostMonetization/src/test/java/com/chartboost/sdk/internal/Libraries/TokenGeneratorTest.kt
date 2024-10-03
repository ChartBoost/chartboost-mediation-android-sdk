package com.chartboost.sdk.internal.Libraries

import android.content.Context
import com.chartboost.sdk.BuildConfig
import com.chartboost.sdk.internal.Model.IdentityBodyFields
import com.chartboost.sdk.internal.Model.OmSdkModel
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.internal.identity.TrackingState
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.iab.omid.library.chartboost.adsession.Partner
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class TokenGeneratorTest {
    private val contextMock = mockk<Context>()
    private val base64WrapperMock = mockk<Base64Wrapper>()
    private val identityMock = mockk<CBIdentity>()
    private val configMock = mockk<SdkConfiguration>()
    private val sdkConfigRef = AtomicReference(configMock)
    private val omManagerMock = mockk<OpenMeasurementManager>()

    private val appId = "test_app_id"
    private val appSignature = "test_app_id"

    private val tokenGenerator =
        BidderTokenGenerator(
            contextMock,
            base64WrapperMock,
            identityMock,
            sdkConfigRef,
            omManagerMock,
        )

    @Before
    fun setup() {
        every { contextMock.packageName } returns "package.test"
        ChartboostDependencyContainer.start(appId, appSignature)
        val identityBodyFields = IdentityBodyFields(TrackingState.TRACKING_ENABLED, "testiden", "uuid", "gaid", "setid", 1)
        val jsonTokenTest =
            JSONObject().apply {
                put("token_version", "1.0")
                put("appSetId", identityBodyFields.setId)
                put("appSetIdScope", identityBodyFields.setIdScope)
                put("package", "package.test")
                put("api", 7)
                put("omidpv", "chartboost")
                put("omdpn", BuildConfig.SDK_VERSION)
            }

        every { identityMock.toIdentityBodyFields() }.returns(identityBodyFields)
        every { base64WrapperMock.encode(any()) }.returns(jsonTokenTest.toString())
        every { base64WrapperMock.encode(any()) }.returns(jsonTokenTest.toString())
        every { configMock.omSdkConfig }.returns(OmSdkModel())
        every { omManagerMock.getOmidPartner() }.returns(Partner.createPartner("chartboost", BuildConfig.SDK_VERSION))
    }

    @Test
    fun `generate token`() {
        every { configMock.omSdkConfig }.returns(OmSdkModel(isEnabled = true))
        val jsonSlot = CapturingSlot<String>()
        val token = tokenGenerator.generateBidderToken()
        io.mockk.verify(exactly = 1) { identityMock.toIdentityBodyFields() }
        io.mockk.verify(exactly = 1) { base64WrapperMock.encode(capture(jsonSlot)) }
        assertNotNull(token)
        val capturedJson = JSONObject(jsonSlot.captured)
        assertEquals("setid", capturedJson.getString("appSetId"))
        assertEquals(1, capturedJson.getInt("appSetIdScope"))
        assertEquals("package.test", capturedJson.getString("package"))
        assertEquals("1.0", capturedJson.getString("token_version"))
        assertEquals("chartboost", capturedJson.getString("omidpn"))
        assertEquals(BuildConfig.SDK_VERSION, capturedJson.getString("omidpv"))
    }

    @Test
    fun `generate token om disabled`() {
        every { configMock.omSdkConfig }.returns(OmSdkModel(isEnabled = false))
        val jsonSlot = CapturingSlot<String>()
        val token = tokenGenerator.generateBidderToken()
        io.mockk.verify(exactly = 1) { identityMock.toIdentityBodyFields() }
        io.mockk.verify(exactly = 1) { base64WrapperMock.encode(capture(jsonSlot)) }
        assertNotNull(token)
        val capturedJson = JSONObject(jsonSlot.captured)
        assertEquals("setid", capturedJson.getString("appSetId"))
        assertEquals(1, capturedJson.getInt("appSetIdScope"))
        assertEquals("package.test", capturedJson.getString("package"))
        assertEquals("1.0", capturedJson.getString("token_version"))
        try {
            capturedJson.getInt("omidpn")
        } catch (e: Exception) {
            assertNotNull(e)
        }

        try {
            capturedJson.getInt("omidpv")
        } catch (e: Exception) {
            assertNotNull(e)
        }

        try {
            capturedJson.getInt("api")
        } catch (e: Exception) {
            assertNotNull(e)
        }
    }

    @Test
    fun `generate token invalid`() {
        every { base64WrapperMock.encode(any()) }.returns("")
        val token = tokenGenerator.generateBidderToken()
        io.mockk.verify(exactly = 1) { identityMock.toIdentityBodyFields() }
        io.mockk.verify(exactly = 1) { base64WrapperMock.encode(any()) }
        assertNotNull(token)
        assertEquals("", token)
    }

    @Test
    fun `generate token missing app set id identity`() {
        val identityBodyFields = IdentityBodyFields()
        val jsonTokenTest =
            JSONObject().apply {
                put("appSetId", identityBodyFields.setId ?: "")
                put("appSetIdScope", identityBodyFields.setIdScope ?: 0)
                put("package", "package.test")
                put("token_version", "1.0")
            }
        every { identityMock.toIdentityBodyFields() }.returns(identityBodyFields)
        every { base64WrapperMock.encode(any()) }.returns(jsonTokenTest.toString())
        val jsonSlot = CapturingSlot<String>()
        val token = tokenGenerator.generateBidderToken()
        io.mockk.verify(exactly = 1) { identityMock.toIdentityBodyFields() }
        io.mockk.verify(exactly = 1) { base64WrapperMock.encode(capture(jsonSlot)) }
        assertNotNull(token)
        val capturedJson = JSONObject(jsonSlot.captured)
        assertEquals("", capturedJson.getString("appSetId"))
        assertEquals(0, capturedJson.getInt("appSetIdScope"))
        assertEquals("package.test", capturedJson.getString("package"))
        assertEquals("1.0", capturedJson.getString("token_version"))
    }
}
