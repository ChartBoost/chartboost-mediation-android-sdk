package com.chartboost.sdk.internal.WebView

import android.content.Context
import android.os.Build
import android.webkit.WebView
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.Libraries.CBJSON
import com.chartboost.sdk.test.JSONObjectMatcher
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.view.CBImpressionActivity
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class MRAID2RobolectricTest {
    private var activity: CBImpressionActivity? = null
    private var webview: WebView? = null
    private var context: Context? = null
    private lateinit var protocol: CBMraidWebViewProtocol

    private inner class Orientation(
        val allowOrientationChange: Boolean = true,
        val forceOrientation: String = "none",
    ) {
        val jsonObject: JSONObject
            get() {
                val json = JSONObject()
                CBJSON.put(json, "allowOrientationChange", allowOrientationChange)
                CBJSON.put(json, "forceOrientation", forceOrientation)
                return json
            }
    }

    private val eventTrackerMock = relaxedMockk<EventTrackerExtensions>()

    // Test setup
    @Before
    fun setupTestResources() {
        activity = Robolectric.buildActivity(CBImpressionActivity::class.java).create().get()
        webview = WebView(activity!!.applicationContext)
        val templateProxyMock = mockk<CBTemplateProxy>()
        every { templateProxyMock.callOnBackgroundJSFunction(any(), any(), any()) } answers {}
        every { templateProxyMock.callOnForegroundJSFunction(any(), any(), any()) } answers {}
        every {
            templateProxyMock.callOnPlaybackTimeJSFunction(
                any(),
                any(),
                any(),
                any(),
            )
        } answers {}
        every { templateProxyMock.callOnVideoFailedJSFunction(any(), any(), any()) } answers {}
        every {
            templateProxyMock.callOnVideoStartedJSFunction(
                any(),
                any(),
                any(),
                any(),
            )
        } answers {}
        every { templateProxyMock.callOnVideoEndedJSFunction(any(), any(), any()) } answers {}

        // Get the current activity context.
        context = RuntimeEnvironment.application
        assertNotNull(context)
        protocol =
            CBMraidWebViewProtocol(
                context!!,
                "default",
                MediaTypeOM.UNKNOWN,
                "interstitial",
                mockk(),
                mockk(),
                relaxedMockk(),
                templateProxyMock,
                null,
                "{ templateHtml }",
                mockk(),
                relaxedMockk(),
                mockk(),
                mockk(),
                relaxedMockk(),
                eventTracker = eventTrackerMock,
            )
        protocol.tryCreatingViewOnActivity(activity!!)
    }

    @Test
    @Throws(JSONException::class)
    fun spplyOrientationProperties_Check() {
        val input =
            arrayOf<Orientation>( // Different Orientation
                Orientation(true, "none"),
                Orientation(true, "portrait"),
                Orientation(true, "landscape"),
                Orientation(false, "none"), // Single value
                Orientation(true),
                Orientation(forceOrientation = "portrait"),
                Orientation(forceOrientation = "landscape"),
            )
        val expected =
            arrayOf<Orientation>(
                Orientation(true, "none"),
                Orientation(true, "portrait"),
                Orientation(true, "landscape"),
                Orientation(false, "none"),
                Orientation(true, "none"),
                Orientation(true, "portrait"),
                Orientation(true, "landscape"),
            )
        for (i in input.indices) {
            // Set the orientation
            val allowOrientationChange = input[i].allowOrientationChange
            val forceOrientation = input[i].forceOrientation

            protocol.setOrientationProperties(allowOrientationChange, forceOrientation)
            // Get the orientation
            val currOrientation = protocol.orientationProperties
            val actual = JSONObject(currOrientation)
            org.junit.Assert.assertThat(
                actual,
                JSONObjectMatcher.equalsJSONObject(
                    expected[i].jsonObject,
                ),
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun restoreOrientation_Check() {
        // original orientation
        val originalOrientation = protocol.orientationProperties
        // set the new orientation
        val newOrientation = Orientation(true, "portrait")
        protocol.setOrientationProperties(
            newOrientation.allowOrientationChange,
            newOrientation.forceOrientation,
        )
        // get the newly set orientation
        val newOrientationStr = protocol.orientationProperties
        // ensure that the new orientation is set correctly
        val actual = JSONObject(newOrientationStr)
        assertEquals(actual.toString(), newOrientation.jsonObject.toString())

        // restore the original orientation
        protocol.restoreOriginalOrientation()
        // get the restored orientations
        val restoreOrientation = protocol.orientationProperties
        // ensure that the restored orientation is same as the original orientation
        val initial = JSONObject(originalOrientation)
        val restored = JSONObject(restoreOrientation)
        assertEquals(initial.toString(), restored.toString())
    }

    @Test
    @Throws(JSONException::class)
    fun screenMaxSize_Check() {
        protocol.calcAndSetMaxScreenSize(activity!!)
        val screenSize = protocol.screenSize
        val displayMetrics = context!!.resources.displayMetrics
        val actual = JSONObject(screenSize)
        val expected =
            CBJSON.jsonObject(
                CBJSON.JKV("width", displayMetrics.widthPixels),
                CBJSON.JKV("height", displayMetrics.heightPixels),
            )
        org.junit.Assert.assertThat(actual, JSONObjectMatcher.equalsJSONObject(expected))
    }

    @Test
    @Throws(JSONException::class)
    fun containerMaxSize_Check() {
        protocol.calcAndSetDisplayableMaxSize(webview!!)
        val containerSize = protocol.maxSize
        val maxScreenSize = protocol.screenSize

        // Reference activity is CBImpressionActivity which has got no title bar and is fullscreen
        // thus the container size is same as the screen size
        val container = JSONObject(containerSize)
        val maxScreen = JSONObject(maxScreenSize)
        assertThat(maxScreen, JSONObjectMatcher.equalsJSONObject(container))
    }

    @Test
    @Throws(JSONException::class)
    fun containerMaxSize_withTop_Check() {
        protocol.calcAndSetDisplayableMaxSize(webview!!)
        val containerSize = protocol.maxSize
        val container = JSONObject(containerSize)
        val expected =
            CBJSON.jsonObject(
                CBJSON.JKV("width", protocol.screenWidth),
                CBJSON.JKV("height", protocol.screenHeight),
            )
        assertThat(container, JSONObjectMatcher.equalsJSONObject(expected))
    }

    @Test
    @Throws(JSONException::class)
    fun calculatePosition_Check() {
        // WebViewBase getView() is null here, default = current position
        protocol.calculatePosition()
        val defaultPosition = protocol.defaultPosition
        val currentPosition = protocol.currentPosition
        val defaultPos = JSONObject(defaultPosition)
        val currentPos = JSONObject(currentPosition)
        assertThat(currentPos, JSONObjectMatcher.equalsJSONObject(defaultPos))
    }
}
