package com.chartboost.sdk.internal.measurement

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.impression.CBImpression
import com.iab.omid.library.chartboost.adsession.AdEvents
import com.iab.omid.library.chartboost.adsession.AdSession
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource
import com.iab.omid.library.chartboost.adsession.media.InteractionType
import com.iab.omid.library.chartboost.adsession.media.MediaEvents
import com.iab.omid.library.chartboost.adsession.media.PlayerState
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.*

class OpenMeasurementControllerTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerTest

    val openMeasurementManagerMock = mockk<OpenMeasurementManager>()
    val openMeasurementSessionBuilderMock = mockk<OpenMeasurementSessionBuilder>()

    every { openMeasurementManagerMock.initialize() } just Runs
    every { openMeasurementManagerMock.getOmidPartner() } returns mockk()
    every { openMeasurementManagerMock.isOmSdkEnabled() } returns true

    val adEventsMock = mockk<AdEvents>()
    val adSessionMock = mockk<AdSession>()
    val adMediaEventsMock = mockk<MediaEvents>()
    val sessionHolderMock =
        OpenMeasurementSessionBuilder.OMSessionHolder(
            adSessionMock,
            adEventsMock,
            adMediaEventsMock,
        )

    every { adMediaEventsMock.volumeChange(any()) } just Runs
    every { adMediaEventsMock.start(any(), any()) } just Runs
    every { adSessionMock.finish() } just Runs
    every { adSessionMock.registerAdView(any()) } just Runs
    every { adEventsMock.impressionOccurred() } just Runs
    every {
        openMeasurementSessionBuilderMock.createOmSession(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    } returns sessionHolderMock

    val impression = mockk<CBImpression>()
    val verificationList = emptyList<VerificationScriptResource>()

    val view =
        mockk<ViewBase>().apply {
            every { webView = any() } just Runs
            every { webView } returns mockk()
        }

    every { impression.getViewProtocolView() } returns view

    val adUnitMock = mockk<AdUnit>()
    every { adUnitMock.mtype } returns MediaTypeOM.VIDEO
    every { impression.getAdUnitImpressionId() } returns "impression id"
    every { openMeasurementManagerMock.isOmSdkActive() } returns true
    every { openMeasurementManagerMock.getOmSdkJsLib() } returns "omsdk"
    every { openMeasurementManagerMock.isVerificationEnabled() } returns true
    every { openMeasurementManagerMock.getVerificationListFromConfig() } returns mockk()

    Given("OpenMeasurementController instance") {
        val openMeasurementController =
            OpenMeasurementController(
                openMeasurementManagerMock,
                openMeasurementSessionBuilderMock,
            )

        When("Create impression on webview page started") {
            openMeasurementController.onImpressionOnWebviewPageStarted(MediaTypeOM.NATIVE, view.webView!!, verificationList)
            Then("OM manager creates tracking") {
                verify(exactly = 1) { openMeasurementManagerMock.getOmidPartner() }
                verify(exactly = 1) { openMeasurementManagerMock.getOmSdkJsLib() }
                verify(exactly = 1) {
                    openMeasurementSessionBuilderMock.createOmSession(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                }
            }

            When("OM controller signal impression") {
                openMeasurementController.signalImpressionEvent()
                Then("OM ad event signal impression occurred") {
                    verify(exactly = 1) { adEventsMock.impressionOccurred() }
                }
            }

            When("OM controller on impression destroy webview") {
                openMeasurementController.onImpressionDestroyWebview()
                Then("OM controller ad session finished and unregister ad view ") {
                    verify(exactly = 1) { adSessionMock.finish() }
                    verify(exactly = 1) { adSessionMock.registerAdView(null) }
                }
            }

            When("OM controller on impression notify video started") {
                openMeasurementController.onImpressionNotifyVideoStarted(1f, 1f)
                Then("Media events register start event") {
                    verify(exactly = 1) { adMediaEventsMock.start(1f, 1f) }
                }
            }

            When("OM controller on impression notify video buffer start true") {
                openMeasurementController.onImpressionNotifyVideoBuffer(true)
                Then("Media events register start event") {
                    verify(exactly = 1) { adMediaEventsMock.bufferStart() }
                }
            }

            When("OM controller on impression notify video buffer start false") {
                openMeasurementController.onImpressionNotifyVideoBuffer(false)
                Then("Media events register start event") {
                    verify(exactly = 1) { adMediaEventsMock.bufferFinish() }
                }
            }

            When("OM controller on impression notify video first quartile") {
                openMeasurementController.onImpressionNotifyVideoProgress(Quartile.FIRST)
                Then("Media events register first quartile event") {
                    verify(exactly = 1) { adMediaEventsMock.firstQuartile() }
                }
            }

            When("OM controller on impression notify video second quartile") {
                openMeasurementController.onImpressionNotifyVideoProgress(Quartile.MIDDLE)
                Then("Media events register midpoint event") {
                    verify(exactly = 1) { adMediaEventsMock.midpoint() }
                }
            }

            When("OM controller on impression notify video first quartile") {
                openMeasurementController.onImpressionNotifyVideoProgress(Quartile.THIRD)
                Then("Media events register third quartile event") {
                    verify(exactly = 1) { adMediaEventsMock.thirdQuartile() }
                }
            }

            When("OM controller on impression notify video complete") {
                openMeasurementController.onImpressionNotifyVideoComplete()
                Then("Media events register complete event") {
                    verify(exactly = 1) { adMediaEventsMock.complete() }
                }
            }

            When("OM controller on impression notify video skipped") {
                openMeasurementController.onImpressionNotifyVideoSkipped()
                Then("Media events register skipped event") {
                    verify(exactly = 1) { adMediaEventsMock.skipped() }
                }
            }

            When("OM controller on impression notify video pause") {
                openMeasurementController.onImpressionNotifyVideoPaused()
                Then("Media events register pause event") {
                    verify(exactly = 1) { adMediaEventsMock.pause() }
                }
            }

            When("OM controller on impression notify video resume") {
                openMeasurementController.onImpressionNotifyVideoResumed()
                Then("Media events register resume event") {
                    verify(exactly = 1) { adMediaEventsMock.resume() }
                }
            }

            When("OM controller on impression notify volume changed") {
                openMeasurementController.onImpressionNotifyVolumeChanged(1f)
                Then("Media events register resume event") {
                    verify(exactly = 1) { adMediaEventsMock.volumeChange(1f) }
                }
            }

            When("OM controller on impression notify state changed") {
                openMeasurementController.onImpressionNotifyStateChanged(PlayerState.FULLSCREEN)
                Then("Media events register state event") {
                    verify(exactly = 1) { adMediaEventsMock.playerStateChange(PlayerState.FULLSCREEN) }
                }
            }

            When("OM controller on impression notify click") {
                openMeasurementController.onImpressionNotifyClick()
                Then("Media events register state event") {
                    verify(exactly = 1) { adMediaEventsMock.adUserInteraction(InteractionType.CLICK) }
                }
            }
        }
    }
})
