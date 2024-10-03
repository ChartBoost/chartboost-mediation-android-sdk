package com.chartboost.sdk.internal.WebView

import com.chartboost.sdk.test.relaxedMockk
import io.mockk.CapturingSlot
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CBTemplateProxyTest {
    private val webviewMock = mockk<CBWebView>()
    private val templateProxy =
        CBTemplateProxy(
            eventTracker = relaxedMockk(),
        )

    @Test
    fun callOnVideoFailedJSFunctionTest() {
        val expectedUrl = "javascript:Chartboost.EventHandler.handleNativeEvent(\"videoFailed\")"
        val urlCaptor = CapturingSlot<String>()
        templateProxy.callOnVideoFailedJSFunction(webviewMock, "location", "interstitial")
        verify { webviewMock.loadUrl(capture(urlCaptor)) }
        val url = urlCaptor.captured
        assertEquals(expectedUrl, url)
    }

    @Test
    fun callOnVideoEndedJSFunctionTest() {
        val expectedUrl = "javascript:Chartboost.EventHandler.handleNativeEvent(\"videoEnded\")"
        val urlCaptor = CapturingSlot<String>()
        templateProxy.callOnVideoEndedJSFunction(webviewMock, "location", "interstitial")
        verify { webviewMock.loadUrl(capture(urlCaptor)) }
        val url = urlCaptor.captured
        assertEquals(expectedUrl, url)
    }

    @Test
    fun callOnBackgroundJSFunctionTest() {
        val expectedUrl = "javascript:Chartboost.EventHandler.handleNativeEvent(\"onBackground\")"
        val urlCaptor = CapturingSlot<String>()
        templateProxy.callOnBackgroundJSFunction(webviewMock, "location", "interstitial")
        verify { webviewMock.loadUrl(capture(urlCaptor)) }
        val url = urlCaptor.captured
        assertEquals(expectedUrl, url)
    }

    @Test
    fun callOnForegroundJSFunctionTest() {
        val expectedUrl = "javascript:Chartboost.EventHandler.handleNativeEvent(\"onForeground\")"
        val urlCaptor = CapturingSlot<String>()
        templateProxy.callOnForegroundJSFunction(webviewMock, "location", "interstitial")
        verify { webviewMock.loadUrl(capture(urlCaptor)) }
        val url = urlCaptor.captured
        assertEquals(expectedUrl, url)
    }

    @Test
    fun callOnPlaybackTimeJSFunctionTest() {
        val time = 1.5f
        val expectedUrl =
            "javascript:Chartboost.EventHandler.handleNativeEvent(\"playbackTime\", {\"seconds\":$time})"
        val urlCaptor = CapturingSlot<String>()
        templateProxy.callOnPlaybackTimeJSFunction(webviewMock, time, "location", "interstitial")
        verify { webviewMock.loadUrl(capture(urlCaptor)) }
        val url = urlCaptor.captured
        assertEquals(expectedUrl, url)
    }

    @Test
    fun callOnVideoStartedJSFunctionTest() {
        val duration = 30.5f
        val expectedUrl =
            "javascript:Chartboost.EventHandler.handleNativeEvent(\"videoStarted\", {\"totalDuration\":$duration})"
        val urlCaptor = CapturingSlot<String>()
        templateProxy.callOnVideoStartedJSFunction(
            webviewMock,
            duration,
            "location",
            "interstitial",
        )
        verify { webviewMock.loadUrl(capture(urlCaptor)) }
        val url = urlCaptor.captured
        assertEquals(expectedUrl, url)
    }
}
