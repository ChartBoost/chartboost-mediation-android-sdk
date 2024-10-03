package com.chartboost.sdk

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.Display
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.impression.CBImpression
import com.chartboost.sdk.test.TestContainer
import com.chartboost.sdk.test.TestUtils
import com.chartboost.sdk.view.CBImpressionActivity
import io.mockk.every
import io.mockk.mockk

internal class DisplayImpressionSetup
    @JvmOverloads
    internal constructor(
        tc: TestContainer,
        val adType: AdType = tc.interstitialTraits,
    ) {
        val location = TestUtils.randomString("the location")

        @JvmField
        internal val impression: CBImpression

        val impressionCallback = mockk<AdUnitRendererImpressionCallback>()

        @JvmField
        val displayActivity = mockk<CBImpressionActivity>()

        val displayActivityWindow = mockk<Window>()

        @JvmField
        val displayActivityWindowDecorView = mockk<View>()

        val displayActivityWindowDecorViewBackground = mockk<Drawable>()

        val displayActivityWindowAttributes = mockk<WindowManager.LayoutParams>()

        init {
            impression =
                tc.impressionBuilder(adType)
                    .withCallback(impressionCallback)
                    .withLocation(location)
                    .buildMock()
            val manager = mockk<WindowManager>()
            val display = mockk<Display>()
            val resources = mockk<Resources>()
            val configuration = mockk<Configuration>()
            every { displayActivity.getSystemService(Context.WINDOW_SERVICE) }.returns(manager)
            every { displayActivity.resources }.returns(resources)
            every { displayActivity.window }.returns(displayActivityWindow)
            every { displayActivity.getActivity() }.returns(displayActivity)
            every { displayActivity.setFullscreen() }.answers { }
            every { displayActivity.isActivityHardwareAccelerated() }.returns(true)
            every { displayActivity.finish() }.answers { }
            every { displayActivity.requestedOrientation = any() }.answers { }
            every { displayActivity.addContentView(any(), any()) }.answers { }

            every { manager.defaultDisplay } returns display
            every { resources.configuration } returns configuration
            every { displayActivityWindow.decorView } returns displayActivityWindowDecorView
            every { displayActivityWindow.attributes } returns displayActivityWindowAttributes
            every { displayActivityWindowDecorView.background } returns displayActivityWindowDecorViewBackground
        }
    }
