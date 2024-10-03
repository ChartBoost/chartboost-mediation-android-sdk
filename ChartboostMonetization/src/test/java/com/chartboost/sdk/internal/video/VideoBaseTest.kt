package com.chartboost.sdk.internal.video

import android.content.Context
import android.view.SurfaceView
import android.view.View.GONE
import android.widget.FrameLayout
import com.chartboost.sdk.internal.WebView.CBWebView
import com.chartboost.sdk.internal.WebView.CustomWebViewInterface
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTracker
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentCaptor

class VideoBaseTest {
    private val contextMock = mockk<Context>()
    private val customWebViewInterfaceMock = mockk<CustomWebViewInterface>()
    private val nativeBridgeCommandMock = relaxedMockk<NativeBridgeCommand>()
    private val surfaceMock = relaxedMockk<SurfaceView>()
    private val backgroundViewMock = mockk<FrameLayout>()
    private val eventTrackerMock = relaxedMockk<EventTracker>()
    private val cbWebViewMock =
        relaxedMockk<CBWebView>().apply {
            every { settings } returns relaxedMockk()
        }

    @Before
    fun setup() {
        mockkConstructor(FrameLayout::class)
        mockkConstructor(FrameLayout.LayoutParams::class)

        // Mock FrameLayout constructor
        every {
            constructedWith<FrameLayout>(
                any(),
            ) // Failed matching mocking signature for  left matchers: [any()] io.mockk.MockKException: Failed matching mocking signature for
            // anyConstructed<FrameLayout>() // Missing mocked calls inside every { ... } block: make sure the object inside the block is a mock
        } answers {
            mockk<FrameLayout>(relaxed = true)
        }

        // Mock FrameLayout.LayoutParams constructor
        every {
            constructedWith<FrameLayout.LayoutParams>(any(), any())
        } answers {
            mockk<FrameLayout.LayoutParams>().apply {
                every { width } returns FrameLayout.LayoutParams.MATCH_PARENT
                every { height } returns FrameLayout.LayoutParams.WRAP_CONTENT
            }
        }

        // Mock setting layoutParams
        every {
            anyConstructed<FrameLayout>().layoutParams = any()
        } just Runs
    }

    @Ignore("HB-8129")
    // NOTE: More robust tests are done in older tests CBWebViewBase etc... Due to construction
    // of this classed, currently is not possible to do more tests without refactoring WebViews and
    // Factory class which is capturing all the new classes for tests
    @Test
    fun initTest() {
        val htmlMock = "test html"
        val pathMock = "/path"
        val videoBase =
            VideoBase(
                contextMock,
                htmlMock,
                customWebViewInterfaceMock,
                nativeBridgeCommandMock,
                pathMock,
                surfaceMock,
                backgroundViewMock,
                eventTracker = eventTrackerMock,
                cbWebViewFactory = { cbWebViewMock },
            )
        assertNotNull(videoBase)
        verify(exactly = 1) { customWebViewInterfaceMock.onWebViewInit() }
        verify(exactly = 1) { customWebViewInterfaceMock.onRegisterWebViewTimeout() }
    }

    @Ignore("HB-8129")
    @Test
    fun removeSurfaceViewTest() {
        val visibilityCaptor = ArgumentCaptor.forClass(Int::class.java)
        val removeViewCaptor = ArgumentCaptor.forClass(SurfaceView::class.java)

        val htmlMock = "test html"
        val pathMock = "/path"
        val videoBase =
            spyk(
                VideoBase(
                    contextMock,
                    htmlMock,
                    customWebViewInterfaceMock,
                    nativeBridgeCommandMock,
                    pathMock,
                    surfaceMock,
                    backgroundViewMock,
                    eventTracker = eventTrackerMock,
                    cbWebViewFactory = { cbWebViewMock },
                ),
            )
        assertNotNull(videoBase)
        verify(exactly = 1) { customWebViewInterfaceMock.onWebViewInit() }
        verify(exactly = 1) { customWebViewInterfaceMock.onRegisterWebViewTimeout() }

        videoBase.removeSurfaceView()
        verify(exactly = 1) { surfaceMock.visibility = visibilityCaptor.capture() }
        val visibilityCaptured = visibilityCaptor.value
        assertEquals(GONE, visibilityCaptured)
        verify(exactly = 1) { backgroundViewMock.removeView(removeViewCaptor.capture()) }
        val removed = removeViewCaptor.value
        assertEquals(surfaceMock, removed)
    }
}
