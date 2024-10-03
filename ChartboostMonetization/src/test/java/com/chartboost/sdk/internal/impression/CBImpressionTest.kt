package com.chartboost.sdk.internal.impression

import android.content.Context
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.Window
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.ImpressionMediaType
import com.chartboost.sdk.internal.Model.ImpressionState
import com.chartboost.sdk.internal.Networking.requests.ClickRequest
import com.chartboost.sdk.internal.Networking.requests.CompleteRequest
import com.chartboost.sdk.internal.Networking.requests.models.ClickParams
import com.chartboost.sdk.internal.Networking.requests.models.CompleteParamsModel
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.WebView.CBMraidWebViewProtocol
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.CBUrl
import com.chartboost.sdk.internal.clickthrough.ClickPreference
import com.chartboost.sdk.internal.clickthrough.ClickTracking
import com.chartboost.sdk.internal.clickthrough.ImpressionClickCallback
import com.chartboost.sdk.internal.clickthrough.IntentResolver
import com.chartboost.sdk.internal.clickthrough.UrlResolver
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.internal.video.VideoProtocol
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.legacy.VastVideoEvent
import com.chartboost.sdk.test.impressionFactory
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.view.CBImpressionActivity
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource
import com.iab.omid.library.chartboost.adsession.media.PlayerState
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.net.URL

@ExperimentalCoroutinesApi
class CBImpressionTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    val contextMock = mockk<Context>(relaxed = true)
    val urlResolverMock = mockk<UrlResolver>()
    val clickRequestMock = mockk<ClickRequest>()
    val completeRequestMock = mockk<CompleteRequest>()
    val mediaTypeMock = ImpressionMediaType.INTERSTITIAL
    val openMeasurementImpressionCallbackMock = mockk<OpenMeasurementImpressionCallback>()
    val appRequestMock = mockk<AppRequest>()
    val downloaderMock = mockk<Downloader>()
    val videoProtocolMock = mockk<VideoProtocol>()
    val webviewProtocolMock = mockk<CBMraidWebViewProtocol>()
    val adUnitMock = mockk<AdUnit>()
    val adTypeMock = mockk<AdType>()
    val location = "default"
    val impressionIntermediateCallbackMock = mockk<ImpressionIntermediateCallback>()
    val impressionClickCallback = mockk<ImpressionClickCallback>()
    val adUnitRendererImpressionCallbackMock = mockk<AdUnitRendererImpressionCallback>()
    val viewBaseMock = mockk<ViewBase>()
    val activityMock = mockk<CBImpressionActivity>()
    val intentResolverMock = mockk<IntentResolver>()
    val clickTrackingMock = mockk<ClickTracking>()
    val eventTrackerMock: EventTrackerExtensions = relaxedMockk()
    val impressionCounter: ImpressionCounter = mockk()

    fun createDependency(
        adType: AdType,
        viewProtocol: CBViewProtocol,
    ): ImpressionDependency {
        return ImpressionDependency(
            urlResolver = urlResolverMock,
            intentResolverMock,
            clickRequestMock,
            clickTrackingMock,
            completeRequestMock,
            mediaTypeMock,
            openMeasurementImpressionCallbackMock,
            appRequestMock,
            downloaderMock,
            viewProtocol,
            impressionCounter = impressionCounter,
            adUnitMock,
            adType,
            location,
            impressionIntermediateCallbackMock,
            impressionClickCallback,
            adUnitRendererImpressionCallbackMock,
            eventTracker = eventTrackerMock,
        )
    }

    val dependency = createDependency(adTypeMock, videoProtocolMock)

    val adid = "test ad id"
    val templateTest = "test template"
    val impressionId = "test impression id"
    val cgn = "test cgn"
    val to = "test to"
    val creative = "test creative"
    val link = "test link"
    val deepLink = "test deep link"
    val reward = 0
    val currency = "test currency"
    val clkp = ClickPreference.CLICK_PREFERENCE_EMBEDDED
    val videoPosition = 1f
    val videoDuration = 1f
    val events = mutableMapOf<String, List<String>>()

    val externalView: ViewGroup? = null
    val windowMock = mockk<Window>()
    val decorView = mockk<View>()

    Given("Impression") {
        events["test_event_key"] = listOf("event_url")
        every { adUnitMock.adId } returns adid
        every { adUnitMock.impressionId } returns impressionId
        every { adUnitMock.template } returns templateTest
        every { adUnitMock.cgn } returns cgn
        every { adUnitMock.to } returns to
        every { adUnitMock.to } returns to
        every { adUnitMock.creative } returns creative
        every { adUnitMock.link } returns link
        every { adUnitMock.deepLink } returns deepLink
        every { adUnitMock.rewardAmount } returns reward
        every { adUnitMock.rewardCurrency } returns currency
        every { adUnitMock.events } returns events
        every { adUnitMock.clkp } returns clkp
        every { adUnitMock.getParametersAsString() } returns "params"
        every { viewBaseMock.visibility = any() } just Runs
        every { viewBaseMock.visibility } returns GONE
        every { adUnitRendererImpressionCallbackMock.onImpressionReadyToBeDisplayed() } just Runs
        every { adUnitRendererImpressionCallbackMock.onImpressionClicked(any()) } just Runs
        every { adUnitRendererImpressionCallbackMock.onImpressionDismissed(any()) } just Runs
        every { adUnitRendererImpressionCallbackMock.onImpressionError(any(), any()) } just Runs
        every { adUnitRendererImpressionCallbackMock.onImpressionRewarded(any(), any()) } just Runs
        every {
            adUnitRendererImpressionCallbackMock.onImpressionClickedFailed(
                any(),
                any(),
                any(),
            )
        } just Runs
        every { adUnitRendererImpressionCallbackMock.onImpressionCloseTriggered(any()) } just Runs
        every { adUnitRendererImpressionCallbackMock.onImpressionShownFully(any()) } just Runs
        every { adUnitRendererImpressionCallbackMock.onImpressionViewCreated(any()) } just Runs
        every { adUnitRendererImpressionCallbackMock.closeActivity() } just Runs
        every { completeRequestMock.execute(any(), any()) } just Runs
        every { clickRequestMock.execute(any(), any()) } just Runs
        every { impressionIntermediateCallbackMock.callImpressionDismissCallback() } just Runs
        every { impressionClickCallback.setImpressionClosed(any()) } just Runs
        every { impressionClickCallback.callDismissAfterClick() } just Runs
        every { impressionClickCallback.callImpressionClickFailureCallback(any(), any()) } just Runs
        every { impressionClickCallback.callImpressionClickSuccessCallback() } just Runs
        every { impressionIntermediateCallbackMock.setImpressionState(any()) } just Runs
        every { impressionIntermediateCallbackMock.destroyImpression() } just Runs
        every { impressionIntermediateCallbackMock.callImpressionDismissCallback() } just Runs
        every { impressionIntermediateCallbackMock.callOnClose() } just Runs
        every { downloaderMock.resume() } just Runs
        every { downloaderMock.pause() } just Runs
        every { adTypeMock.name } returns "Interstitial"
        every { adTypeMock.canBeClosed } returns true
        every { openMeasurementImpressionCallbackMock.onImpressionDestroyWebview() } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyClick() } just Runs
        every { openMeasurementImpressionCallbackMock.onImpressionNotifyStateChanged(any()) } just Runs
        every { viewBaseMock.visibility = any() } just Runs
        every { impressionClickCallback.setImpressionClick(any()) } just Runs
        every { videoProtocolMock.videoPosition } returns videoPosition
        every { videoProtocolMock.videoDuration } returns videoDuration
        every { videoProtocolMock.videoPosition = any() } just Runs
        every { videoProtocolMock.videoDuration = any() } just Runs
        every { videoProtocolMock.view } returns viewBaseMock
        every { videoProtocolMock.destroy() } just Runs
        every { videoProtocolMock.onResume() } just Runs
        every { videoProtocolMock.onPause() } just Runs
        every { videoProtocolMock.sendRequest(any()) } just Runs
        every { videoProtocolMock.setOrientationProperties(any(), any()) } just Runs
        every { videoProtocolMock.restoreOriginalOrientation() } just Runs
        every { videoProtocolMock.tryCreatingViewOnActivity(activityMock) } returns null
        every { videoProtocolMock.tryCreatingViewOnHostView(any()) } returns null
        every { videoProtocolMock.onConfigurationChange() } just Runs
        every { videoProtocolMock.destroy() } just Runs
        every { videoProtocolMock.sendWebViewVastOmEvent(any()) } just Runs
        every { videoProtocolMock.passVerificationResourcesFromTemplate(any()) } just Runs
        every { videoProtocolMock.sendQuartileEvent(any(), any()) } just Runs
        every { videoProtocolMock.updatePlayerState(any()) } just Runs
        every { videoProtocolMock.getAssetDownloadStateNow() } returns -1
        every { videoProtocolMock.orientationProperties } returns "orientation"
        every { videoProtocolMock.defaultPosition } returns "1"
        every { videoProtocolMock.currentPosition } returns "1"
        every { videoProtocolMock.screenSize } returns "1"
        every { videoProtocolMock.maxSize } returns "1"
        every { videoProtocolMock.onWebViewError(any()) } returns
            CBError.Impression.WEB_VIEW_CLIENT_RECEIVED_ERROR
        every { webviewProtocolMock.onConfigurationChange() } just Runs
        every { webviewProtocolMock.destroy() } just Runs
        every { webviewProtocolMock.mute() } just Runs
        every { webviewProtocolMock.unmute() } just Runs
        every { webviewProtocolMock.sendWebViewVastOmEvent(any()) } just Runs
        every { activityMock.addContentView(any(), any()) } just Runs
        every { activityMock.window } returns windowMock
        every { windowMock.decorView } returns decorView
        every { decorView.background } returns mockk()

        every { activityMock.applicationContext } returns contextMock
        every { adTypeMock.shouldDisplayOnHostView } returns false
        every { urlResolverMock.resolve(any(), any(), any()) } returns null
        every { clickTrackingMock.trackNavigationSuccess(any()) } just Runs
        every { clickTrackingMock.trackNavigationFailure(any()) } just Runs

        val impression: CBImpression = impressionFactory(dependency, externalView)

        When("prepare and display view returns true") {
            every { videoProtocolMock.prepare() } returns null
            impression.prepareAndDisplay()

            verify(exactly = 1) { videoProtocolMock.prepare() }
            assertEquals(impression.getImpressionState(), ImpressionState.LOADING)

            Then("then send impression ready to be displayed callback") {
                verify(exactly = 1) { adUnitRendererImpressionCallbackMock.onImpressionReadyToBeDisplayed() }
            }
        }

        When("prepare and display view returns error") {
            every { videoProtocolMock.prepare() } returns CBError.Impression.ERROR_CREATING_VIEW
            impression.prepareAndDisplay()

            verify(exactly = 1) { videoProtocolMock.prepare() }
            assertEquals(ImpressionState.LOADING, impression.getImpressionState())
            Then("then don't do anything") {
                verify(exactly = 0) { adUnitRendererImpressionCallbackMock.onImpressionReadyToBeDisplayed() }
            }
        }

        When("send impression complete request") {
            impression.sendImpressionCompleteRequest()
            Then("execute complete request") {
                val completeParamModelSlot = CapturingSlot<CompleteParamsModel>()
                verify(exactly = 1) {
                    completeRequestMock.execute(
                        any(),
                        capture(completeParamModelSlot),
                    )
                }
                assertEquals(adid, completeParamModelSlot.captured.adId)
                assertEquals(cgn, completeParamModelSlot.captured.cgn)
                assertEquals(reward, completeParamModelSlot.captured.rewardAmount)
                assertEquals(location, completeParamModelSlot.captured.location)
                assertEquals(currency, completeParamModelSlot.captured.rewardCurrency)
                assertEquals(videoDuration, completeParamModelSlot.captured.videoDuration)
                assertEquals(videoPosition, completeParamModelSlot.captured.videoPostion)
            }
        }

        When("impression click success callback with dismiss false") {
            impression.onOpenURL(CBUrl("url", false))
            impression.onImpressionClickSuccessCallback()
            Then("impression click callback is send with impression id") {
                verify(exactly = 1) {
                    adUnitRendererImpressionCallbackMock.onImpressionClicked(
                        impressionId,
                    )
                }

                verify(exactly = 0) {
                    impressionClickCallback.callDismissAfterClick()
                }
            }

            Then("execute click request") {
                val clickParamModelSlot = CapturingSlot<ClickParams>()
                verify(exactly = 1) {
                    clickRequestMock.execute(
                        any(),
                        capture(clickParamModelSlot),
                    )
                }
                assertEquals(adid, clickParamModelSlot.captured.adId)
                assertEquals(cgn, clickParamModelSlot.captured.cgn)
                assertEquals(to, clickParamModelSlot.captured.to)
                assertEquals(creative, clickParamModelSlot.captured.creative)
                assertEquals(location, clickParamModelSlot.captured.location)
                assertEquals(null, clickParamModelSlot.captured.retargetReinstall)
                assertEquals(videoDuration, clickParamModelSlot.captured.videoDuration)
                assertEquals(videoPosition, clickParamModelSlot.captured.videoPosition)
                assertEquals(
                    ImpressionMediaType.INTERSTITIAL,
                    clickParamModelSlot.captured.impressionMediaType,
                )
            }
        }

        When("impression click success callback with dismiss true") {
            impression.onOpenURL(CBUrl("url", true))
            impression.onImpressionClickSuccessCallback()
            Then("impression click callback is send with impression id") {
                verify(exactly = 1) {
                    adUnitRendererImpressionCallbackMock.onImpressionClicked(
                        impressionId,
                    )
                }

                verify(exactly = 1) {
                    impressionClickCallback.callDismissAfterClick()
                }
            }

            Then("execute click request") {
                val clickParamModelSlot = CapturingSlot<ClickParams>()
                verify(exactly = 1) {
                    clickRequestMock.execute(
                        any(),
                        capture(clickParamModelSlot),
                    )
                }
                assertEquals(adid, clickParamModelSlot.captured.adId)
                assertEquals(cgn, clickParamModelSlot.captured.cgn)
                assertEquals(to, clickParamModelSlot.captured.to)
                assertEquals(creative, clickParamModelSlot.captured.creative)
                assertEquals(location, clickParamModelSlot.captured.location)
                assertEquals(null, clickParamModelSlot.captured.retargetReinstall)
                assertEquals(videoDuration, clickParamModelSlot.captured.videoDuration)
                assertEquals(videoPosition, clickParamModelSlot.captured.videoPosition)
                assertEquals(
                    ImpressionMediaType.INTERSTITIAL,
                    clickParamModelSlot.captured.impressionMediaType,
                )
            }
        }

        When("set impression state") {
            impression.setImpressionState(ImpressionState.DISPLAYED)
            Then("get impression state") {
                assertEquals(ImpressionState.DISPLAYED, impression.getImpressionState())
            }
        }

        When("on click state other than displayed") {
            val clickResult = impression.onClick(false, ImpressionState.LOADED)
            Then("click returns false") {
                assertFalse(clickResult)
            }
        }

        When("on click displayed") {
            every { intentResolverMock.canOpenDeeplink(any()) } returns true
            impression.onClick(false, ImpressionState.DISPLAYED)

            When("deep link is not empty") {
                And(" url opener can open link") {
                    verify(exactly = 1) { intentResolverMock.canOpenDeeplink(any()) }
                    Then("set retarget install true") {
                        // cannot verify
                    }
                }

                And("url opener can't open link") {
                    every { intentResolverMock.canOpenDeeplink(any()) } returns false

                    verify(exactly = 1) { intentResolverMock.canOpenDeeplink(any()) }
                    Then("set retarget install false") {
                        // cannot verify
                    }
                }
            }
        }

        When("on click state displayed") {
            And("clicked is false") {
                impression.click = false
                And("initiateClickAttempt return null and should dismiss true") {
                    every { intentResolverMock.canOpenDeeplink(any()) } returns true

                    impression.onClick(true, ImpressionState.DISPLAYED)

                    Then("onClick returns true") {
                        assertTrue(impression.click)
                        verify(exactly = 1) {
                            impressionClickCallback.setImpressionClosed(
                                false,
                            )
                        }
                        verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyClick() }
                        verify(exactly = 1) { urlResolverMock.resolve("test deep link", any(), any()) }
                    }
                }
            }
        }

        When("call on close") {
            impression.setImpressionState(ImpressionState.DISPLAYED)
            impression.callOnClose()
            Then("trigger close impression") {
                assertTrue(impression.canBeClosed())
                verify(exactly = 1) {
                    openMeasurementImpressionCallbackMock.onImpressionNotifyStateChanged(
                        PlayerState.NORMAL,
                    )
                }
                verify(exactly = 1) {
                    adUnitRendererImpressionCallbackMock.onImpressionCloseTriggered(
                        appRequestMock,
                    )
                }
            }
        }

        When("can be closed") {
            val canBeClosed = impression.canBeClosed()
            Then("can be closed is true") {
                assertTrue(canBeClosed)
            }
        }

        When("get location") {
            val loc = impression.getLocation()
            Then("can be closed is true") {
                assertEquals(loc, location)
            }
        }

        When("is impression rewarded") {
            When("impression is rewarded") {
                val dependencyRewarded = createDependency(AdType.Rewarded, videoProtocolMock)
                val impressionRewarded = impressionFactory(dependencyRewarded, null)
                val isRewarded = impressionRewarded.isImpressionRewarded()
                Then("assert true") {
                    assertTrue(isRewarded)
                }
            }
            When("impression is interstitial") {
                val dependencyInterstitial =
                    createDependency(AdType.Interstitial, videoProtocolMock)
                val impressionInterstitial = impressionFactory(dependencyInterstitial, null)
                val isRewarded = impressionInterstitial.isImpressionRewarded()
                Then("assert false") {
                    assertFalse(isRewarded)
                }
            }
            When("impression is banner") {
                val dependencyBanner = createDependency(AdType.Banner, videoProtocolMock)
                val impressionBanner = impressionFactory(dependencyBanner, null)
                val isRewarded = impressionBanner.isImpressionRewarded()
                Then("assert false") {
                    assertFalse(isRewarded)
                }
            }
        }

        When("protocol destroy") {
            impression.viewProtocolDestroy()
            Then("destroy protocol is called") {
                verify(exactly = 1) { videoProtocolMock.destroy() }
            }
        }

        When("protocol return download state for non video protocol") {
            val downloadState = impression.getViewProtocolAssetDownloadStateNow()
            Then("download state is IMPRESSION_WITHOUT_VIDEO_STATE") {
                assertEquals(downloadState, -1)
            }
        }

        When("impression view on start") {
            impression.onStart()
            Then("set impression click to false") {
                verify(exactly = 1) { impressionClickCallback.setImpressionClick(false) }
            }
        }

        When("impression view pause and resume") {
            impression.onPause()
            Then("impression on pause") {
                verify(exactly = 1) { videoProtocolMock.onPause() }
            }
            impression.onResume()
            Then("set impression click to false") {
                verify(exactly = 1) { impressionClickCallback.setImpressionClick(false) }
                verify(exactly = 1) { videoProtocolMock.onResume() }
            }
        }

        When("display on activity fullscreen") {
            impression.displayOnActivity(ImpressionState.LOADED, activityMock)
            Then("show content") {
                verify(exactly = 1) {
                    impressionIntermediateCallbackMock.setImpressionState(
                        ImpressionState.DISPLAYED,
                    )
                }
                verify(exactly = 1) { videoProtocolMock.tryCreatingViewOnActivity(activityMock) }
            }
        }

        When("display on host - banner") {
            val bannerView = mockk<ViewGroup>()
            every { bannerView.addView(any()) } just Runs
            every { viewBaseMock.context } returns contextMock

            impression.displayOnHostView(bannerView)
            Then("show content in the view") {
                verify(exactly = 1) { videoProtocolMock.tryCreatingViewOnHostView(bannerView) }
                verify(exactly = 1) {
                    impressionIntermediateCallbackMock.setImpressionState(
                        ImpressionState.DISPLAYED,
                    )
                }
                verify(exactly = 1) {
                    adUnitRendererImpressionCallbackMock.onImpressionViewCreated(
                        any(),
                    )
                }
                verify(exactly = 1) { bannerView.addView(viewBaseMock) }
                verify(exactly = 1) { downloaderMock.pause() }
            }
        }

        When("display on host - banner null") {
            impression.displayOnHostView(null)
            Then("send failure") {
                verify(exactly = 1) {
                    adUnitRendererImpressionCallbackMock.onImpressionError(
                        appRequestMock,
                        CBError.Impression.ERROR_DISPLAYING_VIEW,
                    )
                }
                verify(exactly = 0) { videoProtocolMock.tryCreatingViewOnHostView(null) }
                verify(exactly = 0) {
                    impressionIntermediateCallbackMock.setImpressionState(
                        ImpressionState.DISPLAYED,
                    )
                }
                verify(exactly = 0) {
                    adUnitRendererImpressionCallbackMock.onImpressionViewCreated(
                        any(),
                    )
                }
                verify(exactly = 0) { downloaderMock.pause() }
            }
        }

        When("get adUnit impressionId") {
            val impId = impression.getAdUnitImpressionId()
            Then("equals test impression id") {
                assertEquals(impressionId, impId)
            }
        }

        When("get adUnit template") {
            val template = impression.getAdUnitTemplate()
            Then("equals test template") {
                assertEquals(templateTest, template)
            }
        }

        When("destroy view protocol") {
            impression.viewProtocolDestroy()
            Then("view protocol call destroy") {
                verify(exactly = 1) { videoProtocolMock.destroy() }
            }
        }

        When("is view protocol not created") {
            every { viewBaseMock.rootView } returns null
            val isViewNotCreated = impression.isViewProtocolViewNotCreated()
            Then("view is not created") {
                assertTrue(isViewNotCreated)
            }
        }

        When("is view protocol is created") {
            every { viewBaseMock.rootView } returns mockk()
            val isViewNotCreated = impression.isViewProtocolViewNotCreated()
            Then("view is created") {
                assertFalse(isViewNotCreated)
            }
        }

        When("get main view protocol view") {
            val view = impression.getViewProtocolView()
            Then("view is not null") {
                assertNotNull(view)
            }
        }

        When("get view protocol asset download state") {
            val state = impression.getViewProtocolAssetDownloadStateNow()
            Then("state is unknown") {
                assertEquals(-1, state)
            }
        }

        When("impression configuration change") {
            val dependencyWebview = createDependency(AdType.Interstitial, webviewProtocolMock)
            val impressionWebview = impressionFactory(dependencyWebview, null)
            impressionWebview.viewProtocolConfigurationChange()
            Then("protocol configuration change") {
                verify(exactly = 1) { webviewProtocolMock.onConfigurationChange() }
            }
        }

        When("impression failure") {
            And("ad was shown") {
                impression.setIsVideoShowSent(true)
                impression.onImpressionFailure(CBError.Impression.ERROR_CREATING_VIEW)
                Then("close impression without reporting failure") {
                    verify(exactly = 1) { adUnitRendererImpressionCallbackMock.closeActivity() }
                    verify(exactly = 0) {
                        adUnitRendererImpressionCallbackMock.onImpressionError(
                            appRequestMock,
                            CBError.Impression.ERROR_CREATING_VIEW,
                        )
                    }
                }
            }

            And("ad was not shown") {
                impression.setIsVideoShowSent(false)
                impression.onImpressionFailure(CBError.Impression.ERROR_CREATING_VIEW)
                Then("report failure") {
                    verify(exactly = 0) { impressionIntermediateCallbackMock.callImpressionDismissCallback() }
                    verify(exactly = 0) { videoProtocolMock.sendWebViewVastOmEvent(VastVideoEvent.SKIP) }
                    verify(exactly = 0) { impressionIntermediateCallbackMock.callOnClose() }
                    verify(exactly = 0) { videoProtocolMock.restoreOriginalOrientation() }
                    verify(exactly = 1) {
                        adUnitRendererImpressionCallbackMock.onImpressionError(
                            appRequestMock,
                            CBError.Impression.ERROR_CREATING_VIEW,
                        )
                    }
                }
            }
        }

        When("notify rewarded complete on error") {
            every { impressionCounter.impressionNotifyDidCompleteAdPlayCount } returns 1
            every { impressionCounter.impressionNotifyDidCompleteAdPlayCount = any() } just Runs
            val dependencyRewarded = createDependency(AdType.Rewarded, videoProtocolMock)
            val impressionRewarded = impressionFactory(dependencyRewarded, null)
            And("set show to sent") {
                impressionRewarded.setIsVideoShowSent(true)
                impressionRewarded.impressionNotifyDidCompleteRewardedOnError()
                Then("call impression rewarded") {
                    verify(exactly = 1) {
                        adUnitRendererImpressionCallbackMock.onImpressionRewarded(
                            impressionId,
                            reward,
                        )
                    }
                }
            }

            And("set show not sent") {
                impressionRewarded.setIsVideoShowSent(false)
                impressionRewarded.impressionNotifyDidCompleteRewardedOnError()
                Then("doesn't call anything") {
                    verify(exactly = 0) {
                        adUnitRendererImpressionCallbackMock.onImpressionRewarded(
                            impressionId,
                            reward,
                        )
                    }
                }
            }
        }

        When("send webview vast tracking events") {
            impression.sendWebViewVASTTrackingEvents("test_event_key")
            Then("protocol sends request") {
                verify(exactly = 1) { videoProtocolMock.sendRequest("event_url") }
            }
        }

        When("set orientation properties") {
            impression.setOrientationProperties(true, "horizontal")
            Then("protocol sets orientation properties") {
                verify(exactly = 1) {
                    videoProtocolMock.setOrientationProperties(
                        true,
                        "horizontal",
                    )
                }
            }
        }

        When("reward video completed") {
            And("counts are 1") {
                every { impressionCounter.onRewardedVideoCompletedPlayCount } returns 1
                every { impressionCounter.onRewardedVideoCompletedPlayCount = any() } just Runs
                every { impressionCounter.impressionNotifyDidCompleteAdPlayCount } returns 1
                every { impressionCounter.impressionNotifyDidCompleteAdPlayCount = any() } just Runs
                every { impressionCounter.impressionSendVideoCompleteRequestPlayCount } returns 1
                every {
                    impressionCounter.impressionSendVideoCompleteRequestPlayCount = any()
                } just Runs
                impression.onRewardedVideoCompleted()
                Then("execute complete request") {
                    verify(exactly = 1) { completeRequestMock.execute(any(), any()) }
                }
            }

            And("counts are 0") {
                every { impressionCounter.onRewardedVideoCompletedPlayCount } returns 0
                every { impressionCounter.onRewardedVideoCompletedPlayCount = any() } just Runs
                every { impressionCounter.impressionNotifyDidCompleteAdPlayCount } returns 0
                every { impressionCounter.impressionNotifyDidCompleteAdPlayCount = any() } just Runs
                every { impressionCounter.impressionSendVideoCompleteRequestPlayCount } returns 0
                every {
                    impressionCounter.impressionSendVideoCompleteRequestPlayCount = any()
                } just Runs
                impression.onRewardedVideoCompleted()
                Then("execute complete request") {
                    verify(exactly = 1) { completeRequestMock.execute(any(), any()) }
                }
            }

            And("counts are 2") {
                every { impressionCounter.onRewardedVideoCompletedPlayCount } returns 2
                every { impressionCounter.onRewardedVideoCompletedPlayCount = any() } just Runs
                every { impressionCounter.impressionNotifyDidCompleteAdPlayCount } returns 1
                every { impressionCounter.impressionNotifyDidCompleteAdPlayCount = any() } just Runs
                every { impressionCounter.impressionSendVideoCompleteRequestPlayCount } returns 1
                every {
                    impressionCounter.impressionSendVideoCompleteRequestPlayCount = any()
                } just Runs
                impression.onRewardedVideoCompleted()
                Then("execute complete request") {
                    verify(exactly = 0) { completeRequestMock.execute(any(), any()) }
                }
            }
        }

        When("play video") {
            impression.playVideo()
            Then("video protocol plays video") {
                verify(exactly = 1) { videoProtocolMock.playVideo() }
            }
        }

        When("pause video") {
            impression.pauseVideo()
            Then("video protocol pause video") {
                verify(exactly = 1) { videoProtocolMock.pauseVideo() }
            }
        }

        When("close video") {
            impression.closeVideo()
            Then("video protocol close video") {
                verify(exactly = 1) { videoProtocolMock.closeVideo() }
            }
        }

        When("mute") {
            And("protocol is video") {
                impression.muteVideo()
                Then("protocol mute video") {
                    verify(exactly = 1) { videoProtocolMock.muteVideo() }
                }
            }

            And("protocol is webview") {
                val dependencyWebview = createDependency(AdType.Interstitial, webviewProtocolMock)
                val impressionWebview = impressionFactory(dependencyWebview, null)
                impressionWebview.muteVideo()
                Then("protocol mute and send om event") {
                    verify(exactly = 1) { webviewProtocolMock.mute() }
                    verify(exactly = 1) { webviewProtocolMock.sendWebViewVastOmEvent(VastVideoEvent.VOLUME_CHANGE) }
                }
            }
        }

        When("unmute") {
            And("protocol is video") {
                impression.unmuteVideo()
                Then("protocol mute video") {
                    verify(exactly = 1) { videoProtocolMock.unmuteVideo() }
                }
            }

            And("protocol is webview") {
                val dependencyWebview = createDependency(AdType.Interstitial, webviewProtocolMock)
                val impressionWebview = impressionFactory(dependencyWebview, null)
                impressionWebview.unmuteVideo()
                Then("protocol mute and send om event") {
                    verify(exactly = 1) { webviewProtocolMock.unmute() }
                    verify(exactly = 1) { webviewProtocolMock.sendWebViewVastOmEvent(VastVideoEvent.VOLUME_CHANGE) }
                }
            }
        }

        When("pass verification resources from template") {
            val resource =
                VerificationScriptResource.createVerificationScriptResourceWithoutParameters(
                    URL("https://verification.com"),
                )
            val scripts = listOf<VerificationScriptResource>(resource)
            impression.passVerificationResourcesFromTemplate(scripts)
            Then("pass list to protocol") {
                verify(exactly = 1) {
                    videoProtocolMock.passVerificationResourcesFromTemplate(
                        scripts,
                    )
                }
            }
        }

        When("send quartile event") {
            impression.sendQuartileEvent(1f, 1f)
            Then("pass values to protocol") {
                verify(exactly = 1) { videoProtocolMock.sendQuartileEvent(1f, 1f) }
            }
        }

        When("set video duration") {
            impression.setVideoDuration(1f)
            Then("pass video duration to protocol") {
                verify(exactly = 1) { videoProtocolMock.videoDuration = 1f }
            }
        }

        When("set video position") {
            impression.setVideoPosition(1f)
            Then("pass video position to protocol") {
                verify(exactly = 1) { videoProtocolMock.videoPosition = 1f }
            }
        }

        When("template sends video completed event") {
            every { impressionCounter.onVideoCompletedPlayCount } returns 1
            every { impressionCounter.onVideoCompletedPlayCount = any() } just Runs
            every { impressionCounter.impressionNotifyDidCompleteAdPlayCount } returns 1
            every { impressionCounter.impressionNotifyDidCompleteAdPlayCount = any() } just Runs
            every { impressionCounter.impressionSendVideoCompleteRequestPlayCount } returns 1
            justRun { impressionCounter.impressionSendVideoCompleteRequestPlayCount = any() }

            And("impression is interstitial") {
                val dependencyInterstitial =
                    createDependency(AdType.Interstitial, videoProtocolMock)
                val impressionInterstitial = impressionFactory(dependencyInterstitial, null)
                impressionInterstitial.templateVideoCompletedEvent()
                Then("execute complete request") {
                    verify(exactly = 0) {
                        adUnitRendererImpressionCallbackMock.onImpressionRewarded(
                            impressionId,
                            reward,
                        )
                    }
                    verify(exactly = 1) { completeRequestMock.execute(any(), any()) }
                }
            }

            And("impression is rewarded") {
                val dependencyRewarded = createDependency(AdType.Rewarded, videoProtocolMock)
                val impressionRewarded = impressionFactory(dependencyRewarded, null)
                impressionRewarded.templateVideoCompletedEvent()
                Then("execute complete request") {
                    verify(exactly = 1) {
                        adUnitRendererImpressionCallbackMock.onImpressionRewarded(
                            impressionId,
                            reward,
                        )
                    }
                    verify(exactly = 1) { completeRequestMock.execute(any(), any()) }
                }
            }

            And("impression is banner") {
                val dependencyBanner = createDependency(AdType.Banner, videoProtocolMock)
                val impressionBanner = impressionFactory(dependencyBanner, externalView)
                impressionBanner.templateVideoCompletedEvent()
                Then("execute complete request") {
                    verify(exactly = 0) {
                        adUnitRendererImpressionCallbackMock.onImpressionRewarded(
                            impressionId,
                            reward,
                        )
                    }
                    verify(exactly = 1) { completeRequestMock.execute(any(), any()) }
                }
            }
        }

        When("update player state") {
            impression.updatePlayerState(com.chartboost.sdk.legacy.PlayerState.IDLE)
            Then("update player state protocol") {
                verify(exactly = 1) { videoProtocolMock.updatePlayerState(com.chartboost.sdk.legacy.PlayerState.IDLE) }
            }
        }

        When("send webview vast om event") {
            impression.sendWebViewVastOmEvent(VastVideoEvent.COMPLETED)
            Then("send webview vast om event protocol") {
                verify(exactly = 1) { videoProtocolMock.sendWebViewVastOmEvent(VastVideoEvent.COMPLETED) }
            }
        }

        When("template click event") {
            And("impression is displayed") {
                every { intentResolverMock.canOpenDeeplink(any()) } returns false
                impression.setImpressionState(ImpressionState.DISPLAYED)
                impression.templateClickEvent(null)
                Then("attempt to click url") {
                    verify(exactly = 1) { openMeasurementImpressionCallbackMock.onImpressionNotifyClick() }
                    verify(exactly = 1) { urlResolverMock.resolve("test link", any(), any()) }
                }
            }
        }

        When("error web view") {
            impression.setIsVideoShowSent(false)
            impression.errorWebView("error")
            Then("error web view protocol and send impression callback") {
                verify(exactly = 1) { videoProtocolMock.onWebViewError("error") }
                verify(exactly = 1) {
                    adUnitRendererImpressionCallbackMock.onImpressionError(
                        appRequestMock,
                        CBError.Impression.WEB_VIEW_CLIENT_RECEIVED_ERROR,
                    )
                }
            }
        }

        When("get protocol orientation properties") {
            val orientation = impression.getProtocolOrientationProperties()
            Then("orientation is returned") {
                assertEquals("orientation", orientation)
            }
        }

        When("get protocol default position") {
            val defaultPosition = impression.getProtocolDefaultPosition()
            Then("default position is returned") {
                assertEquals("1", defaultPosition)
            }
        }

        When("get protocol current position") {
            val currentPosition = impression.getProtocolCurrentPosition()
            Then("current position is returned") {
                assertEquals("1", currentPosition)
            }
        }

        When("get protocol screen size") {
            val screenSize = impression.getProtocolScreenSize()
            Then("screen size is returned") {
                assertEquals("1", screenSize)
            }
        }

        When("get protocol screen max") {
            val maxSize = impression.getProtocolMaxSize()
            Then("screen max is returned") {
                assertEquals("1", maxSize)
            }
        }

        When("get adunit parameters") {
            val parameters = impression.getAdUnitParameters()
            Then("parameter is returned") {
                assertEquals("params", parameters)
            }
        }
    }
})
