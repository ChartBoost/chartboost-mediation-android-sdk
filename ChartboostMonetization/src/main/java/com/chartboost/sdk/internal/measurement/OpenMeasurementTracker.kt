package com.chartboost.sdk.internal.measurement

import android.view.View
import com.chartboost.sdk.internal.logging.Logger
import com.iab.omid.library.chartboost.Omid
import com.iab.omid.library.chartboost.adsession.FriendlyObstructionPurpose
import com.iab.omid.library.chartboost.adsession.media.InteractionType
import com.iab.omid.library.chartboost.adsession.media.MediaEvents
import com.iab.omid.library.chartboost.adsession.media.PlayerState

internal class OpenMeasurementTracker(
    private val sessionHolder: OpenMeasurementSessionBuilder.OMSessionHolder,
    private val isOmSdkEnabled: Boolean,
) {
    private var isQuartile1Notified = false
    private var isMidpointNotified = false
    private var isQuartile3Notified = false
    private var isCompleteNotified = false
    private var isSkipped = false

    /**
     * startOmidSession - starts the Omid session for tracking purposes
     */
    fun startSession() {
        if (!isOmSdkEnabled) {
            Logger.e("OMSDK start session OM is disabled by the cb config!")
            return
        }

        catchException {
            sessionHolder.omSession?.let {
                it.start()
                Logger.d("Omid session started successfully! Version: ${Omid.getVersion()}")
            } ?: Logger.d("Omid start session is null!")
        }
    }

    /**
     * stopOmidSession - stops the Omid session
     */
    fun stopSession() {
        if (!isOmSdkEnabled) {
            Logger.e("OMSDK stop session OM is disabled by the cb config!")
            return
        }

        try {
            sessionHolder.omSession?.run {
                finish()
                registerAdView(null)
            }
            Omid.updateLastActivity()
            Logger.d("Omid session finished!")
        } catch (e: Exception) {
            Logger.e("OMSDK stop session exception", e)
        } finally {
            sessionHolder.omSession = null
            sessionHolder.omAdEvents = null
        }
    }

    /**
     * signalLoadEvent - signals the load event to the OMID API
     */
    fun signalLoadEvent() {
        if (!isOmSdkEnabled) {
            Logger.e("OMSDK signal load OM is disabled by the cb config!")
            return
        }

        catchException {
            sessionHolder.omAdEvents?.let {
                it.loaded()
                Logger.d("Signal om ad event loaded!")
            } ?: Logger.d("Omid load event is null!")
        }
    }

    /**
     * signalImpressionEvent - signals the impression event to the OMID API
     */
    fun signalImpressionEvent() {
        if (!isOmSdkEnabled) {
            Logger.e("OMSDK signal impression event OM is disabled by the cb config!")
            return
        }

        catchException {
            sessionHolder.omAdEvents?.let {
                it.impressionOccurred()
                Logger.d("Signal om ad event impression occurred!")
            } ?: Logger.d("Omid signal impression event is null!")
        }
    }

    fun registerFriendlyObstruction(obstructionView: View) {
        sessionHolder.omSession?.addFriendlyObstruction(
            obstructionView,
            FriendlyObstructionPurpose.OTHER,
            "Industry Icon",
        )
    }

    /**
     * Media events tracker:
     * start
     * first quartile [25%]
     * midpoint [50%]
     * third quartile [75%]
     * complete [only if ad reaches 100%]
     * pause [user initiated]
     * resume [user initated]
     * bufferStart [playback paused due to buffering]
     * bufferEnd [playback resumes after buffering]
     * player volume change
     * skipped [any early termination of playback]
     */

    /**
     * Track media event start with duration as float and volume 0 to 1 float
     */
    fun signalMediaStart(
        videoDuration: Float,
        videoVolume: Float,
    ) {
        isQuartile1Notified = false
        isMidpointNotified = false
        isQuartile3Notified = false
        catchException {
            getMediaEvents("${this::signalMediaStart.name} duration: $videoDuration and volume $videoVolume")
                ?.start(videoDuration, videoVolume)
        }
    }

    fun signalMediaFirstQuartile() {
        catchException {
            if (!isQuartile1Notified) {
                Logger.d("Signal media first quartile")
                getMediaEvents(this::signalMediaFirstQuartile.name)?.firstQuartile()
                isQuartile1Notified = true
            }
        }
    }

    fun signalMediaMidpoint() {
        catchException {
            if (!isMidpointNotified) {
                Logger.d("Signal media midpoint")
                getMediaEvents(this::signalMediaMidpoint.name)?.midpoint()
                isMidpointNotified = true
            }
        }
    }

    fun signalMediaThirdQuartile() {
        catchException {
            if (!isQuartile3Notified) {
                Logger.d("Signal media third quartile")
                getMediaEvents(this::signalMediaThirdQuartile.name)?.thirdQuartile()
                isQuartile3Notified = true
            }
        }
    }

    fun signalMediaComplete() {
        catchException {
            getMediaEvents(this::signalMediaComplete.name)?.complete()
            isCompleteNotified = true
        }
    }

    fun signalMediaPause() {
        catchException {
            getMediaEvents(this::signalMediaPause.name)?.pause()
        }
    }

    fun signalMediaResume() {
        catchException {
            getMediaEvents(this::signalMediaResume.name)?.resume()
        }
    }

    fun signalMediaBufferStart() {
        catchException {
            getMediaEvents(this::signalMediaBufferStart.name)?.bufferStart()
        }
    }

    fun signalMediaBufferFinish() {
        catchException {
            getMediaEvents(this::signalMediaBufferFinish.name)?.bufferFinish()
        }
    }

    /**
     * Track media event volume change from 0 to 1 flot
     */
    fun signalMediaVolumeChange(videoVolume: Float) {
        catchException {
            getMediaEvents(this::signalMediaVolumeChange.name + " volume: $videoVolume")?.volumeChange(videoVolume)
        }
    }

    fun signalMediaSkipped() {
        catchException {
            if (!isSkipped && !isCompleteNotified) {
                Logger.d("Signal media skipped")
                getMediaEvents(this::signalMediaSkipped.name)?.skipped()
                isSkipped = true
            }
        }
    }

    fun signalMediaStateChange(playerState: PlayerState) {
        catchException {
            getMediaEvents(this::signalMediaStateChange.name + " state: ${playerState.name}")?.playerStateChange(playerState)
        }
    }

    fun signalUserInteractionClick() {
        catchException {
            getMediaEvents(this::signalUserInteractionClick.name)?.adUserInteraction(InteractionType.CLICK)
        }
    }

    private fun getMediaEvents(functionName: String): MediaEvents? {
        if (sessionHolder.mediaEvents == null) {
            Logger.d("MediaEvents are null when executing $functionName")
        } else {
            Logger.d("MediaEvents valid when executing: $functionName")
        }
        return sessionHolder.mediaEvents
    }

    private inline fun catchException(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Logger.e("Error", e)
        }
    }
}
