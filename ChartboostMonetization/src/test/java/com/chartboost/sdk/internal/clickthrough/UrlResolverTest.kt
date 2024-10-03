package com.chartboost.sdk.internal.clickthrough

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.test.relaxedMockk
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UrlResolverTest {
    private val urlRedirectMock: UrlRedirect =
        mockk<UrlRedirect>().apply {
            every { redirect(any(), any()) } returns Result.success("foo")
        }

    private val clickTrackingMock = relaxedMockk<ClickTracking>()

    // Classes are needed here because plain lambdas won't work correctly with Result
    // since we need spying/mocking them to verify.
    internal class Fail {
        suspend fun action(urlArgs: UrlArgs): Result<UrlActionResult> =
            Result.failure(
                UrlOpenerFallbackReason.NotValidScheme,
            )
    }

    internal class Success {
        suspend fun action(urlArgs: UrlArgs): Result<UrlActionResult> = Result.success(UrlActionResult("success action"))
    }

    private val urlResolver =
        UrlResolver(
            urlRedirect = urlRedirectMock,
            actions = emptyList(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    @Test
    fun `when url is null, resolve() should return error`() {
        urlResolver.resolve(
            null,
            ClickPreference.CLICK_PREFERENCE_NATIVE,
            clickTrackingMock,
        ) shouldBe CBError.Click.URI_INVALID
    }

    @Test
    fun `when url is empty, resolve() should return error`() {
        urlResolver.resolve(
            "",
            ClickPreference.CLICK_PREFERENCE_NATIVE,
            clickTrackingMock,
        ) shouldBe CBError.Click.URI_INVALID
    }

    @Test
    fun `when redirection is successful, it should track`() {
        urlResolver.resolve(
            "foo",
            ClickPreference.CLICK_PREFERENCE_EMBEDDED,
            clickTrackingMock,
        )
        verify { clickTrackingMock.trackNavigationSuccess(any()) }
    }

    @Test
    fun `when redirection is failure, it should track`() {
        every { urlRedirectMock.redirect(any(), any()) } returns Result.failure(Exception())
        urlResolver.resolve(
            "foo",
            ClickPreference.CLICK_PREFERENCE_EMBEDDED,
            clickTrackingMock,
        )
        verify { clickTrackingMock.trackNavigationFailure(any()) }
    }

    @Test
    fun `when the first action fails, it should execute the next action`() {
        val fail = spyk(Fail())
        val success = spyk(Success())
        val urlResolver =
            UrlResolver(
                urlRedirect = urlRedirectMock,
                actions = listOf(fail::action, success::action),
                ioDispatcher = UnconfinedTestDispatcher(),
            )
        urlResolver.resolve(
            "foo",
            ClickPreference.CLICK_PREFERENCE_NATIVE,
            clickTrackingMock,
        )
        coVerify {
            fail.action(any())
            success.action(any())
        }
    }

    @Test
    fun `when an action fails with an uncontrolled error, it should track the error`() {
        val exception = Exception()
        val fail =
            mockk<Fail>().apply {
                coEvery { action(any()) } returns Result.failure(exception)
            }
        val urlResolver =
            UrlResolver(
                urlRedirect = urlRedirectMock,
                actions = listOf(fail::action),
                ioDispatcher = UnconfinedTestDispatcher(),
            )
        val url = "foo"
        urlResolver.resolve(
            url,
            ClickPreference.CLICK_PREFERENCE_NATIVE,
            clickTrackingMock,
        )
        verify { clickTrackingMock.trackNavigationFailure(any()) }
    }

    @Test
    fun `when the first action succeeds, it should not execute the next action`() {
        val fail = spyk(Fail())
        val success = spyk(Success())
        val urlResolver =
            UrlResolver(
                urlRedirect = urlRedirectMock,
                actions = listOf(success::action, fail::action),
                ioDispatcher = UnconfinedTestDispatcher(),
            )
        urlResolver.resolve(
            "foo",
            ClickPreference.CLICK_PREFERENCE_NATIVE,
            clickTrackingMock,
        )
        coVerify { success.action(any()) }
        coVerify(exactly = 0) { fail.action(any()) }
    }

    @Test
    fun `when all actions fail, it should track error`() {
        val fail = spyk(Fail())
        val urlResolver =
            UrlResolver(
                urlRedirect = urlRedirectMock,
                actions = listOf(fail::action),
                ioDispatcher = UnconfinedTestDispatcher(),
            )
        urlResolver.resolve(
            "foo",
            ClickPreference.CLICK_PREFERENCE_NATIVE,
            clickTrackingMock,
        )
        verify(exactly = 1) { clickTrackingMock.trackNavigationFailure(any()) }
    }

    @Test
    fun `when resolve() is executed without errors, it should return null`() {
        urlResolver.resolve(
            "foo",
            ClickPreference.CLICK_PREFERENCE_EMBEDDED,
            clickTrackingMock,
        ) shouldBe null
    }
}
