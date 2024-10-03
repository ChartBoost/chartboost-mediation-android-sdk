package com.chartboost.sdk.internal.clickthrough

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.chartboost.sdk.test.relaxedMockk
import io.kotest.common.runBlocking
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import java.net.URISyntaxException

@OptIn(ExperimentalCoroutinesApi::class)
class UrlOpenerTest {
    private val contextMock = mockk<Context>()
    private val intentResolver = mockk<IntentResolver>()
    private val uriParserMock = mockk<(String) -> Uri>()
    private val uriMock = mockk<Uri>()
    private val intentFactoryMock = mockk<(Uri) -> Intent>()
    private val intentMock = mockk<Intent>(relaxed = true)

    private val args = UrlArgs("https://test.url/test", ClickPreference.CLICK_PREFERENCE_EMBEDDED)

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        every { contextMock.startActivity(any()) } just Runs
        every { uriMock.scheme } returns "https"
        every { uriParserMock(any()) } returns uriMock
        every { uriParserMock.invoke(any()) } returns uriMock
        every { intentFactoryMock(any()) } returns intentMock
    }

    @Test
    fun `open market intent can open deep link`() {
        every { intentResolver.canOpenDeeplink(any()) } returns true
        runBlocking {
            openDeepLink(
                args,
                contextMock,
                intentResolver,
                uriParserMock,
                intentFactoryMock,
                testDispatcher,
            ) shouldBeSuccess UrlActionResult("openDeepLink")
        }
        verify(exactly = 1) { contextMock.startActivity(any()) }
    }

    @Test
    fun `open market intent cannot open deep link`() {
        every { intentResolver.canOpenDeeplink(any()) } returns false
        runBlocking {
            openDeepLink(
                args,
                contextMock,
                intentResolver,
                uriParserMock,
                intentFactoryMock,
                testDispatcher,
            ) shouldBeFailure UrlOpenerFallbackReason.MissingAppToOpenSchema
        }
        verify(exactly = 0) { contextMock.startActivity(any()) }
    }

    @Test
    fun `open market uri exception`() {
        every { intentResolver.canOpenDeeplink(any()) } returns false
        runBlocking {
            openDeepLink(
                args,
                contextMock,
                intentResolver,
                uriParser = { throw URISyntaxException("foo", "bar") },
                intentFactoryMock,
                testDispatcher,
            ) shouldBeFailure UrlOpenerFallbackReason.MissingAppToOpenSchema
        }
        verify(exactly = 0) { contextMock.startActivity(any()) }
    }

    @Test
    fun `when open a url in native browser and click preference does not say so, return error`() {
        runBlocking {
            openInNativeBrowser(
                args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED),
                contextMock,
                uriParserMock,
                intentFactoryMock,
                testDispatcher,
            ) shouldBeFailure UrlOpenerFallbackReason.WrongPreference
        }
        verify(exactly = 0) { contextMock.startActivity(any()) }
    }

    @Test
    fun `when open a url in native browser and URL parsing throws exception, return error`() {
        val exception = URISyntaxException("foo", "bar")
        runBlocking {
            openInNativeBrowser(
                args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_NATIVE),
                contextMock,
                uriParser = { throw exception },
                intentFactoryMock,
                testDispatcher,
            ) shouldBeFailure exception
        }
        verify(exactly = 0) { contextMock.startActivity(any()) }
    }

    @Test
    fun `when open a url in native browser and no exception, start activity should be called`() {
        runBlocking {
            openInNativeBrowser(
                args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_NATIVE),
                contextMock,
                uriParserMock,
                intentFactoryMock,
                testDispatcher,
            )
        }
        verify { contextMock.startActivity(any()) }
    }

    @Test
    fun `when open a url in native browser and no exception, should return initial args`() {
        val testArgs = args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_NATIVE)
        runBlocking {
            openInNativeBrowser(
                testArgs,
                contextMock,
                uriParserMock,
                intentFactoryMock,
                testDispatcher,
            ) shouldBeSuccess UrlActionResult("openInNativeBrowser")
        }
    }

    @Test
    fun `when open a url in embedded browser and click preference does not say so, return error`() {
        runBlocking {
            openInEmbeddedBrowser(
                args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_NATIVE),
                contextMock,
            ) shouldBeFailure UrlOpenerFallbackReason.WrongPreference
        }
        verify(exactly = 0) { contextMock.startActivity(any()) }
    }

    @Test
    fun `when open a url in embedded browser and URL parsing throws exception, return error`() {
        val exception = URISyntaxException("foo", "bar")
        runBlocking {
            openInEmbeddedBrowser(
                args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED),
                contextMock,
                uriParser = { throw exception },
            ) shouldBeFailure exception
        }
        verify(exactly = 0) { contextMock.startActivity(any()) }
    }

    @Test
    fun `when open a url in embedded browser and no exception, start activity should be called`() {
        val intentMock: Intent =
            relaxedMockk<Intent>().apply {
                every { putExtra(any(), any<String>()) } returns this
            }
        runBlocking {
            openInEmbeddedBrowser(
                args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED),
                contextMock,
                uriParserMock,
                intentFactory = { intentMock },
                testDispatcher,
            )
        }
        verify { contextMock.startActivity(intentMock) }
    }

    @Test
    fun `when open a url in embedded browser and no exception, should return initial args`() {
        val testArgs = args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED)
        runBlocking {
            openInEmbeddedBrowser(
                testArgs,
                contextMock,
                uriParserMock,
                intentFactory = { relaxedMockk() },
                testDispatcher,
            ) shouldBeSuccess UrlActionResult("openInEmbeddedBrowser")
        }
    }

    @Test
    fun `when open a url in embedded browser with invalid scheme exception`() {
        every { uriMock.scheme } returns "http"
        val testArgs = args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED)
        runBlocking {
            openInEmbeddedBrowser(
                testArgs,
                contextMock,
                uriParserMock,
                intentFactory = { relaxedMockk() },
                testDispatcher,
            ) shouldBeSuccess UrlActionResult("openInEmbeddedBrowser")
        }
    }

    @Test
    fun `when open an unsecure link`() {
        every { uriMock.scheme } returns "http"
        val testArgs = args.copy(clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED)
        runBlocking {
            openUnsecureLink(
                testArgs,
                contextMock,
                uriParserMock,
                intentFactory = { relaxedMockk() },
                testDispatcher,
            ) shouldBeSuccess UrlActionResult("openUnsecureLink")
        }
    }
}
