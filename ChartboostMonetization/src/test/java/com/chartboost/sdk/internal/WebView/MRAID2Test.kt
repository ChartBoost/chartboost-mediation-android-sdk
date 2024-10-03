package com.chartboost.sdk.internal.WebView

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.util.DisplayMetrics
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.External.Android
import com.chartboost.sdk.internal.Libraries.CBJSON
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.legacy.Factory
import com.chartboost.sdk.test.JSONObjectMatcher
import com.chartboost.sdk.test.TestFactory
import com.chartboost.sdk.test.TestUtils
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MRAID2Test {
    private lateinit var context: Context
    private lateinit var testFactory: TestFactory
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var protocol: CBViewProtocol

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

    private inner class Size(var width: Int, var height: Int) {
        val jsonObject: JSONObject
            get() =
                CBJSON.jsonObject(
                    CBJSON.JKV("width", width),
                    CBJSON.JKV("height", height),
                )
    }

    private inner class Position(var left: Int, var top: Int, var width: Int, var height: Int) {
        val jsonObject: JSONObject
            get() =
                CBJSON.jsonObject(
                    CBJSON.JKV("x", left),
                    CBJSON.JKV("y", top),
                    CBJSON.JKV("width", width),
                    CBJSON.JKV("height", height),
                )
    }

    @Before
    fun setupTestEnv() {
        // the jsonObject() return null as new objects are not created during unit test. We are using
        // TestFactory intercepts to create new jsonObject() during tests.
        testFactory = TestFactory()
        Factory.install(testFactory)
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
        // Setup the context, WindowManager and Display which are used in the CBUtils.getOrientation()
        // function called from the CBWebViewProtocol() constructor
        context = mockk<Context>()
        sharedPreferences = mockk<SharedPreferences>()
        protocol =
            spyk(
                CBMraidWebViewProtocol(
                    context,
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
                ),
            )
    }

    @Test
    @Throws(JSONException::class)
    fun getDefaultPosition_Check() {
        // Test Default Positions
        val testPosition =
            arrayOf<Position>(
                Position(0, 0, 0, 0),
                Position(-1, 3, 80, 94),
            )
        Assert.assertNotNull(protocol)
        for (pos in testPosition) {
            protocol.defaultXPos = pos.left
            protocol.defaultYPos = pos.top
            protocol.defaultWidth = pos.width
            protocol.defaultHeight = pos.height
            val defaultPos = protocol.defaultPosition
            val defaultPosition = JSONObject(defaultPos)
            assertThat(
                defaultPosition,
                JSONObjectMatcher.equalsJSONObject(pos.jsonObject),
            )
            verify(atLeast = 1) { protocol!!.calculatePosition() }
        }
    }

    @Test
    @Throws(JSONException::class)
    fun getCurrentPosition_Check() {
        // Test current positions
        val testPosition =
            arrayOf<Position>(
                Position(0, 0, 0, 0),
                Position(-1, 3, 80, 94),
            )
        Assert.assertNotNull(protocol)
        for (pos in testPosition) {
            val currentPos = protocol!!.currentPosition
            val currentPosition = JSONObject(currentPos)
            Assert.assertThat(
                currentPosition,
                JSONObjectMatcher.equalsJSONObject(Position(0, 0, 0, 0).jsonObject),
            )
            verify(atLeast = 1) { protocol!!.calculatePosition() }
        }
    }

    @Ignore("HB-8129")
    @Test
    @Throws(JSONException::class)
    fun defaultOrientationProperties_Check() {
//        val webView = testFactory.interceptedMock(CBWebView::class.java)
//        every {webView.settings} returns mockk())

        // sdkVersionAtLeast() requires this.
        Android.initialize(
            mockk<Android>(),
        )

        // Cover the constructor execution in unit test. Create WebViewBase object
        protocol.createViewObject(context)
        // Get the orientation
        val currOrientation = protocol.orientationProperties
        val actual = JSONObject(currOrientation)
        val expected =
            CBJSON.jsonObject(
                CBJSON.JKV("allowOrientationChange", true),
                CBJSON.JKV("forceOrientation", "none"),
            )
        assertThat(actual, JSONObjectMatcher.equalsJSONObject(expected))
    }

    @Test
    @Throws(JSONException::class)
    fun setAndGetOrientation_Portrait_Landscape_None_Check() {
        val input =
            arrayOf<Orientation>(
                // Different Orientation
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
        Assert.assertNotNull(protocol)
        for (i in input.indices) {
            protocol.setOrientationProperties(
                input[i].allowOrientationChange,
                input[i].forceOrientation,
            )
            val currentOri = protocol.orientationProperties
            val current = JSONObject(currentOri)
            assertThat(
                current,
                JSONObjectMatcher.equalsJSONObject(
                    expected[i].jsonObject,
                ),
            )
            verify(
                atLeast = 1,
            ) {
                protocol!!.setOrientationProperties(
                    input[i].allowOrientationChange,
                    input[i].forceOrientation,
                )
            }
        }
    }

    @Test
    @Throws(JSONException::class)
    fun getOrientation_Portrait_Landscape_None_Check() {
        val input =
            arrayOf<Orientation>(
                // Different Orientation
                Orientation(true, "none"),
                Orientation(true, "portrait"),
                Orientation(true, "landscape"),
                Orientation(false, "none"),
            )
        val expected =
            arrayOf<Orientation>(
                Orientation(true, "none"),
                Orientation(true, "portrait"),
                Orientation(true, "landscape"),
                Orientation(false, "none"),
            )
        Assert.assertNotNull(protocol)
        for (i in input.indices) {
            // Set the given orientation
            TestUtils.setFieldWithReflection(
                protocol,
                CBViewProtocol::class.java,
                "allowOrientationChange",
                input[i].allowOrientationChange,
            )
            TestUtils.setFieldWithReflection(
                protocol,
                CBViewProtocol::class.java,
                "forceOrientation",
                protocol.forceOrientationFromString(input[i].forceOrientation),
            )
            val currentOri = protocol.orientationProperties
            val current = JSONObject(currentOri)
            assertThat(
                current,
                JSONObjectMatcher.equalsJSONObject(
                    expected[i].jsonObject,
                ),
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun getMaxSize_Check() {
        val testSize =
            arrayOf(
                Size(1024, 1024),
                Size(768, 1024),
                Size(300, 300),
                Size(800, 480),
                Size(98, 98),
            )
        Assert.assertNotNull(protocol)
        for (testMaxSize in testSize) {
            // Set the given orientation
            TestUtils.setFieldWithReflection(
                protocol,
                CBViewProtocol::class.java,
                "maxContainerWidth",
                testMaxSize.width,
            )
            TestUtils.setFieldWithReflection(
                protocol,
                CBViewProtocol::class.java,
                "maxContainerHeight",
                testMaxSize.height,
            )
            val currentMaxSize = protocol.maxSize
            val current = JSONObject(currentMaxSize)
            assertThat(
                current,
                JSONObjectMatcher.equalsJSONObject(testMaxSize.jsonObject),
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun getScreenSize_Check() {
        // Different screen size
        val testSize =
            arrayOf<Size>(
                Size(1024, 1024),
                Size(768, 1024),
                Size(300, 300),
                Size(800, 480),
                Size(98, 98),
            )
        Assert.assertNotNull(protocol)
        for (testScrSize in testSize) {
            // Set the given orientation
            TestUtils.setFieldWithReflection(
                protocol,
                CBViewProtocol::class.java,
                "screenWidth",
                testScrSize.width,
            )
            TestUtils.setFieldWithReflection(
                protocol,
                CBViewProtocol::class.java,
                "screenHeight",
                testScrSize.height,
            )
            val currentSize = protocol.screenSize
            val current = JSONObject(currentSize)
            assertThat(
                current,
                JSONObjectMatcher.equalsJSONObject(testScrSize.jsonObject),
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun getScreenSize_With_DisplayMatrix_Test() {
        val testSize =
            arrayOf<Size>(
                Size(1024, 1024),
                Size(768, 1024),
                Size(300, 300),
                Size(800, 480),
                Size(98, 98),
            )
        Assert.assertNotNull(protocol)
        val context = mockk<Context>()
        val displayMetrics = mockk<DisplayMetrics>()
        val resource =
            mockk<Resources>()
        for (i in testSize.indices) {
            val testWidth = testSize[i].width
            val testHeight = testSize[i].height
            displayMetrics.widthPixels = testWidth
            displayMetrics.heightPixels = testHeight
            every { context.resources } returns resource
            every { context.resources.displayMetrics } returns displayMetrics
            protocol.calcAndSetMaxScreenSize(context)
            val screenSize = protocol.screenSize
            val scrSize = JSONObject(screenSize)
            assertThat(
                scrSize,
                JSONObjectMatcher.equalsJSONObject(
                    testSize[i].jsonObject,
                ),
            )
        }
    }

    @Test
    fun forceOrientationString_Test() {
        // MAP => Input Orientation as Integer, Expected Orientation as String
        val orientationMatrix = HashMap<Int, String>()
        orientationMatrix[ActivityInfo.SCREEN_ORIENTATION_PORTRAIT] = "portrait"
        orientationMatrix[ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE] = "landscape"
        orientationMatrix[ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED] = "none"
        orientationMatrix[-3] = "error"
        Assert.assertNotNull(protocol)
        for ((key, value) in orientationMatrix) {
            val testOrientation = protocol.forceOrientationString(key)
            Assert.assertEquals(value, testOrientation)
        }
    }

    @Test
    fun forceOrientationFromString_Test() {
        // MAP => Input Orientation as String, Expected Orientation as Integer
        val orientationMatrix = HashMap<String, Int>()
        orientationMatrix["portrait"] = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        orientationMatrix["landscape"] = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        orientationMatrix["none"] = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        orientationMatrix[""] = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        Assert.assertNotNull(protocol)
        for ((key, value) in orientationMatrix) {
            val testOrientation =
                protocol.forceOrientationFromString(
                    key,
                )
            Assert.assertEquals(value, testOrientation)
        }
    }
}
