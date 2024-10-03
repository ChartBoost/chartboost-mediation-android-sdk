package com.chartboost.sdk.internal.WebView

import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CBHtmlWebChromeClientTest {
    private val cBHtmlWebChromeClient = CBHtmlWebChromeClient()

    private val jsPromptResult = mockk<JsPromptResult>()

    private val jsResult = mockk<JsResult>()

    @Before
    fun setup() {
        every { jsResult.confirm() } just Runs
        every { jsResult.cancel() } just Runs

        every { jsPromptResult.confirm() } just Runs
    }

    @Test
    fun `onConsoleMessage returns true`() {
        val message = mockk<ConsoleMessage>()

        every { message.message() } returns "This is a test message"
        every { message.lineNumber() } returns 123
        every { message.sourceId() } returns "This is a test source id"

        val result = cBHtmlWebChromeClient.onConsoleMessage(message)

        Assert.assertTrue(result)
    }

    @Test
    fun `onJsAlert confirms result and returns true`() {
        val message = "Test Alert Message"
        val view = mockk<WebView>()

        val result = cBHtmlWebChromeClient.onJsAlert(view, "url", message, jsResult)

        verify { jsResult.confirm() }
        Assert.assertTrue(result)
    }

    @Test
    fun `onJsConfirm cancels result and returns true`() {
        val message = "Test Confirm Message"
        val view = mockk<WebView>()

        val result = cBHtmlWebChromeClient.onJsConfirm(view, "url", message, jsResult)

        verify { jsResult.cancel() }
        Assert.assertTrue(result)
    }

    @Test
    fun `onJsPrompt confirms result and returns true`() {
        val message = "Test Prompt Message"
        val defaultValue = "default"
        val view = mockk<WebView>()

        val result =
            cBHtmlWebChromeClient.onJsPrompt(view, "url", message, defaultValue, jsPromptResult)

        verify { jsPromptResult.confirm() }
        Assert.assertTrue(result)
    }
}
