package com.chartboost.sdk.internal.measurement

import com.iab.omid.library.chartboost.adsession.*
import com.iab.omid.library.chartboost.adsession.media.MediaEvents
import com.iab.omid.library.chartboost.adsession.media.PlayerState
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull

@RunWith(MockitoJUnitRunner::class)
class OpenMeasurementTrackerTest {
    private val adSessionMock = mockk<AdSession>()
    private val adEventsMock = mockk<AdEvents>()
    private val mediaEventsMock = mockk<MediaEvents>()
    private val sessionHolder =
        OpenMeasurementSessionBuilder.OMSessionHolder(
            adSessionMock,
            adEventsMock,
            mediaEventsMock,
        )
    private val openMeasurementTracker = OpenMeasurementTracker(sessionHolder, true)

    @Before
    fun setup() {
        every { adSessionMock.finish() } just Runs
        every { adSessionMock.registerAdView(anyOrNull()) } just Runs
        every { adEventsMock.loaded() } just Runs
        every { mediaEventsMock.volumeChange(any()) } just Runs
        every { adEventsMock.impressionOccurred() } just Runs
    }

    @Test
    fun `tracker start session`() {
        openMeasurementTracker.startSession()
        verify(exactly = 1) { adSessionMock.start() }
    }

    @Test
    fun `tracker stop session`() {
        openMeasurementTracker.stopSession()
        verify(exactly = 1) { adSessionMock.finish() }
        verify(exactly = 1) { adSessionMock.registerAdView(null) }
        openMeasurementTracker.startSession()
        verify(exactly = 0) { adSessionMock.start() }
    }

    @Test
    fun `tracker signal impression event`() {
        openMeasurementTracker.signalImpressionEvent()
        verify(exactly = 1) { adEventsMock.impressionOccurred() }
    }

    @Test
    fun `tracker signal load event`() {
        openMeasurementTracker.signalLoadEvent()
        verify(exactly = 1) { adEventsMock.loaded() }
    }

    @Test
    fun `tracker signal buffer start`() {
        openMeasurementTracker.signalMediaBufferStart()
        verify(exactly = 1) { mediaEventsMock.bufferStart() }
    }

    @Test
    fun `tracker signal buffer finish`() {
        openMeasurementTracker.signalMediaBufferFinish()
        verify(exactly = 1) { mediaEventsMock.bufferFinish() }
    }

    @Test
    fun `tracker signal skipped`() {
        openMeasurementTracker.signalMediaSkipped()
        verify(exactly = 1) { mediaEventsMock.skipped() }
    }

    @Test
    fun `tracker signal pause`() {
        openMeasurementTracker.signalMediaPause()
        verify(exactly = 1) { mediaEventsMock.pause() }
    }

    @Test
    fun `tracker signal resume`() {
        openMeasurementTracker.signalMediaResume()
        verify(exactly = 1) { mediaEventsMock.resume() }
    }

    @Test
    fun `tracker signal volume change`() {
        openMeasurementTracker.signalMediaVolumeChange(0.5f)
        verify(exactly = 1) { mediaEventsMock.volumeChange(0.5f) }
    }

    @Test
    fun `tracker signal state change`() {
        openMeasurementTracker.signalMediaStateChange(PlayerState.FULLSCREEN)
        verify(exactly = 1) { mediaEventsMock.playerStateChange(PlayerState.FULLSCREEN) }
    }
}
