package com.chartboost.sdk.internal.identity

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.chartboost.sdk.internal.External.Android
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
class CBIdentityTest {
    private val contextMock = mockk<Context>()
    private val androidMock = mockk<Android>()
    private val ifaMock = mockk<IFA>()
    private val base64WrapperMock = mockk<Base64Wrapper>()
    private val resultAppSetIdInfo = mockk<AppSetIdInfo>()
    private val advertisingIDHolderMock =
        AdvertisingIDHolder(
            TrackingState.TRACKING_ENABLED,
            "aaaa-bbbb-cccc-dddd",
        )
    private lateinit var identity: CBIdentity

    @Before
    fun setup() {
        every { resultAppSetIdInfo.id } returns "test"
        every { resultAppSetIdInfo.scope } returns 1
        val sharedPrefs = mockk<SharedPreferences>()
        every { sharedPrefs.getString(any(), any()) } returns "0"
        every { contextMock.contentResolver } returns mockk()
        every { androidMock.getAppSetIdTask(any()) } returns mockTask
        every { contextMock.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { base64WrapperMock.encode(any()) } returns "base64encoded"
        every { ifaMock.getAdvertisingIdHolder() } returns advertisingIDHolderMock
        every { ifaMock.getLocalAdvertisingId(any(), any()) } returns "uuid"

        identity =
            CBIdentity(
                contextMock,
                androidMock,
                ifaMock,
                base64WrapperMock,
                ioDispatcher = UnconfinedTestDispatcher(),
            )
    }

    @Test
    fun `onAppSetIdNetworkCallSuccess returns correct identity data`() {
        val identityBodyFields = identity.toIdentityBodyFields()
        with(identityBodyFields) {
            assertEquals(gaid, "aaaa-bbbb-cccc-dddd")
            assertEquals(trackingState, TrackingState.TRACKING_ENABLED)
            assertEquals(uuid, "000000000")
            assertEquals(identifiers, "base64encoded")
            assertEquals(setId, "test")
            assertEquals(setIdScope, 1)
        }
    }

    private val mockTask =
        object : Task<AppSetIdInfo?>() {
            override fun addOnFailureListener(p0: OnFailureListener): Task<AppSetIdInfo?> {
                return this
            }

            override fun addOnFailureListener(
                p0: Activity,
                p1: OnFailureListener,
            ): Task<AppSetIdInfo?> {
                return this
            }

            override fun addOnFailureListener(
                p0: Executor,
                p1: OnFailureListener,
            ): Task<AppSetIdInfo?> {
                return this
            }

            override fun getException(): Exception? {
                return java.lang.Exception("Test")
            }

            override fun getResult(): AppSetIdInfo? {
                return resultAppSetIdInfo
            }

            override fun <X : Throwable?> getResult(p0: Class<X>): AppSetIdInfo? {
                return resultAppSetIdInfo
            }

            override fun isCanceled(): Boolean {
                return false
            }

            override fun isComplete(): Boolean {
                return true
            }

            override fun isSuccessful(): Boolean {
                return true
            }

            override fun addOnSuccessListener(
                p0: Executor,
                onCompleteListener: OnSuccessListener<in AppSetIdInfo?>,
            ): Task<AppSetIdInfo?> {
                onCompleteListener.onSuccess(resultAppSetIdInfo)
                return this
            }

            override fun addOnSuccessListener(
                p0: Activity,
                onCompleteListener: OnSuccessListener<in AppSetIdInfo?>,
            ): Task<AppSetIdInfo?> {
                onCompleteListener.onSuccess(resultAppSetIdInfo)
                return this
            }

            override fun addOnSuccessListener(onCompleteListener: OnSuccessListener<in AppSetIdInfo?>): Task<AppSetIdInfo?> {
                onCompleteListener.onSuccess(resultAppSetIdInfo)
                return this
            }
        }
}
