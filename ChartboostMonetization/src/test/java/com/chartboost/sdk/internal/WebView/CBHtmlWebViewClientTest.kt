package com.chartboost.sdk.internal.WebView

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.chartboost.sdk.internal.Libraries.CBConstants.API_ENDPOINT
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.tracking.EventTracker
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CBHtmlWebViewClientTest {
    private lateinit var client: CBHtmlWebViewClient
    private val impressionInterfaceMock = mockk<ImpressionInterface>()
    private val callbackMock = mockk<CustomWebViewInterface>()
    private val eventTrackerMock = mockk<EventTracker>()
    private val webResourceRequestMock = mockk<WebResourceRequest>()
    private val uriMock = mockk<Uri>()

    private val singleClickGestureDetector = SingleClickGestureDetector(mockk<Context>())

    @Before
    fun setup() {
        every { impressionInterfaceMock.onTemplateOpenURLEvent(any()) } just Runs
        every { impressionInterfaceMock.onClickBeforeLoadFinished(any()) } just Runs

        every { uriMock.toString() } returns API_ENDPOINT

        every { callbackMock.onPageFinished() } just Runs

        every { webResourceRequestMock.url } returns uriMock

        client =
            CBHtmlWebViewClient(
                impressionInterfaceMock,
                singleClickGestureDetector,
                callbackMock,
                eventTrackerMock,
            )
    }

    @Test
    fun `shouldOverrideUrlLoading should not call onTemplateOpenURLEvent when hasClick false and hasLoadFinished false should return true`() {
        singleClickGestureDetector.hasClick = false
        client.hasLoadFinished = false

        val result = client.shouldOverrideUrlLoading(mockk<WebView>(), webResourceRequestMock)

        verify(exactly = 0) { impressionInterfaceMock.onTemplateOpenURLEvent(any()) }

        Assert.assertTrue(result)
    }

    @Test
    fun `shouldOverrideUrlLoading should not call onTemplateOpenURLEvent when hasClick true and hasLoadFinished false should return true`() {
        singleClickGestureDetector.hasClick = true
        client.hasLoadFinished = false

        val result = client.shouldOverrideUrlLoading(mockk<WebView>(), webResourceRequestMock)

        verify(exactly = 0) { impressionInterfaceMock.onTemplateOpenURLEvent(any()) }

        Assert.assertTrue(result)
    }

    @Test
    fun `shouldOverrideUrlLoading should not call onTemplateOpenURLEvent when hasClick false and hasLoadFinished true should return false`() {
        singleClickGestureDetector.hasClick = false
        client.hasLoadFinished = true

        val result = client.shouldOverrideUrlLoading(mockk<WebView>(), webResourceRequestMock)

        verify(exactly = 0) { impressionInterfaceMock.onTemplateOpenURLEvent(any()) }
        // TODO Based on the latest changes in the code it seems this should be false
        Assert.assertFalse(result)
    }

    @Test
    fun `shouldOverrideUrlLoading should call onTemplateOpenURLEvent when hasClick true and hasLoadFinished true should return true`() {
        singleClickGestureDetector.hasClick = true
        client.hasLoadFinished = true

        val result = client.shouldOverrideUrlLoading(mockk<WebView>(), webResourceRequestMock)

        verify(exactly = 1) { impressionInterfaceMock.onTemplateOpenURLEvent(any()) }

        Assert.assertTrue(result)
    }
}
