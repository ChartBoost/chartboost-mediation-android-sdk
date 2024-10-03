package com.chartboost.sdk.internal.WebView

import android.content.Context
import com.chartboost.sdk.internal.clickthrough.CBUrl
import com.chartboost.sdk.internal.clickthrough.UrlParser
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.legacy.PlayerState
import com.chartboost.sdk.legacy.VastVideoEvent
import com.chartboost.sdk.test.FakeUiPoster
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject

class NativeBridgeCommandTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val fakeUiPoster = FakeUiPoster()
    val impressionInterface: ImpressionInterface = mockk(relaxed = true)
    val hideViewCallback: HideCustomViewCallback = mockk(relaxed = true)
    val context: Context = mockk(relaxed = true)
    val urlParserMock: UrlParser = mockk(relaxed = true)

    val nativeBridgeCommand = NativeBridgeCommand(fakeUiPoster, urlParserMock)
    nativeBridgeCommand.setImpressionInterface(impressionInterface)
    nativeBridgeCommand.hideViewCallback = hideViewCallback

    Given("Native Bridge command") {
        When("nativeFunctionWithArgs is called with unknown functionName") {
            val result = nativeBridgeCommand.nativeFunctionWithArgs(null, "unknownRandom")
            Then("result should be a error message") {
                result shouldBe "Function name not recognized."
            }
        }
        When("nativeFunctionWithArgs is called with getParameters") {
            every { (impressionInterface.getAdUnitParameters()) } returns GET_PARAMETERS
            val result = nativeBridgeCommand.nativeFunctionWithArgs(null, "getParameters")
            Then("getAdUnitParameters is called") {
                verify { impressionInterface.getAdUnitParameters() }
            }
            Then("result should be getAdUnitParameters") {
                result shouldBe GET_PARAMETERS
            }
        }
        When("nativeFunctionWithArgs is called with getMaxSize") {
            every { (impressionInterface.getProtocolMaxSize()) } returns GET_MAX_SIZE
            val result = nativeBridgeCommand.nativeFunctionWithArgs(null, "getMaxSize")
            Then("getAdUnitParameters is called") {
                verify { impressionInterface.getProtocolMaxSize() }
            }
            Then("result should be getAdUnitParameters") {
                result shouldBe GET_MAX_SIZE
            }
        }
        When("nativeFunctionWithArgs is called with getScreenSize") {
            every { (impressionInterface.getProtocolScreenSize()) } returns GET_SCREEN_SIZE
            val result = nativeBridgeCommand.nativeFunctionWithArgs(null, "getScreenSize")
            Then("getAdUnitParameters is called") {
                verify { impressionInterface.getProtocolScreenSize() }
            }
            Then("result should be getAdUnitParameters") {
                result shouldBe GET_SCREEN_SIZE
            }
        }
        When("nativeFunctionWithArgs is called with getCurrentPosition") {
            every { (impressionInterface.getProtocolCurrentPosition()) } returns GET_CURRENT_POSITION
            val result = nativeBridgeCommand.nativeFunctionWithArgs(null, "getCurrentPosition")
            Then("getAdUnitParameters is called") {
                verify { impressionInterface.getProtocolCurrentPosition() }
            }
            Then("result should be getAdUnitParameters") {
                result shouldBe GET_CURRENT_POSITION
            }
        }
        When("nativeFunctionWithArgs is called with getDefaultPosition") {
            every { (impressionInterface.getProtocolDefaultPosition()) } returns GET_DEFAULT_POSITION
            val result = nativeBridgeCommand.nativeFunctionWithArgs(null, "getDefaultPosition")
            Then("getAdUnitParameters is called") {
                verify { impressionInterface.getProtocolDefaultPosition() }
            }
            Then("result should be getAdUnitParameters") {
                result shouldBe GET_DEFAULT_POSITION
            }
        }
        When("nativeFunctionWithArgs is called with get Orientation Properties") {
            every { (impressionInterface.getProtocolOrientationProperties()) } returns GET_ORIENTATION_PROPERTIES
            val result = nativeBridgeCommand.nativeFunctionWithArgs(null, "getOrientationProperties")
            Then("getAdUnitParameters is called") {
                verify { impressionInterface.getProtocolOrientationProperties() }
            }
            Then("result should be getAdUnitParameters") {
                result shouldBe GET_ORIENTATION_PROPERTIES
            }
        }
        When("nativeFunctionWithArgs is called with click") {
            And("args is null") {
                every { urlParserMock.parseOpenUrlArgsObject(any()) } returns CBUrl("", null)
                nativeBridgeCommand.nativeFunctionWithArgs(null, "click")
                Then("Something should happen") {
                    verify { impressionInterface.onTemplateClickEvent(CBUrl("", null)) }
                }
            }
            And("should dismiss is false") {
                val args = JSONObject("{\"shouldDismiss\":false}")
                every { urlParserMock.parseOpenUrlArgsObject(any()) } returns CBUrl("", false)
                nativeBridgeCommand.nativeFunctionWithArgs(args, "click")
                Then("Something should happen") {
                    verify { impressionInterface.onTemplateClickEvent(CBUrl("", false)) }
                }
            }
            And("should dismiss is true") {
                val args = JSONObject("{\"shouldDismiss\":true}")
                every { urlParserMock.parseOpenUrlArgsObject(any()) } returns CBUrl("", true)
                nativeBridgeCommand.nativeFunctionWithArgs(args, "click")
                Then("Something should happen") {
                    verify { impressionInterface.onTemplateClickEvent(CBUrl("", true)) }
                }
            }
        }
        When("nativeFunctionWithArgs is called with close command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "close")
            Then("Something should happen") {
                verify { impressionInterface.templateCloseEvent() }
            }
        }
        When("nativeFunctionWithArgs is called with skipped command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "skipped")
            Then("Something should happen") {
                verify { impressionInterface.sendWebViewVastOmEvent(VastVideoEvent.SKIP) }
            }
        }
        When("nativeFunctionWithArgs is called with videoCompleted command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "videoCompleted")
            Then("Something should happen") {
                verify { hideViewCallback.onHideCustomView() }
                verify { impressionInterface.updatePlayerState(PlayerState.IDLE) }
                verify { impressionInterface.templateVideoCompletedEvent() }
            }
        }
        When("nativeFunctionWithArgs is called with videoResumed command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "videoResumed")
            Then("Something should happen") {
                verify { impressionInterface.sendWebViewVastOmEvent(VastVideoEvent.RESUME) }
                verify { impressionInterface.updatePlayerState(PlayerState.PLAYING) }
            }
        }
        When("nativeFunctionWithArgs is called with videoPaused command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "videoPaused")
            Then("Something should happen") {
                verify { impressionInterface.sendWebViewVastOmEvent(VastVideoEvent.PAUSE) }
                verify { impressionInterface.updatePlayerState(PlayerState.PAUSED) }
            }
        }
        When("nativeFunctionWithArgs is called with currentVideoDuration command") {
            And("duration is lower than 0") {
                val args = JSONObject("{\"duration\":-1}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "currentVideoDuration")
                Then("Something should happen") {
                    verify(exactly = 0) { impressionInterface.setVideoPosition(any()) }
                    verify(exactly = 0) { impressionInterface.sendQuartileEvent(any(), any()) }
                }
            }
            And("duration is 0") {
                val args = JSONObject("{\"duration\":0}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "currentVideoDuration")
                Then("Something should happen") {
                    verify(exactly = 0) { impressionInterface.setVideoPosition(any()) }
                    verify(exactly = 0) { impressionInterface.sendQuartileEvent(any(), any()) }
                }
            }
            And("duration is higher than 0") {
                val args = JSONObject("{\"duration\":1}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "currentVideoDuration")
                Then("Something should happen") {
                    verify { impressionInterface.setVideoPosition(1000f) }
                    verify { impressionInterface.sendQuartileEvent(0f, 1000f) }
                }
            }
            And("duration is not there") {
                val args = JSONObject("{\"dereteon\":-1}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "currentVideoDuration")
                Then("Something should happen") {
                    verify {
                        impressionInterface.warning(
                            "Parsing exception unknown field for current player duration: org.json.JSONException: JSONObject[\"duration\"] not found.",
                        )
                    }
                }
            }
        }
        When("nativeFunctionWithArgs is called with totalVideoDuration command") {
            And("duration is there") {
                val args = JSONObject("{\"duration\":1}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "totalVideoDuration")
                Then("Something should happen") {
                    verify { impressionInterface.setVideoDuration(1000f) }
                }
            }
            And("duration is not there") {
                val args = JSONObject("{\"dereteon\":-1}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "totalVideoDuration")
                Then("Something should happen") {
                    verify { impressionInterface.setVideoDuration(0f) }
                }
            }
        }
        When("nativeFunctionWithArgs is called with show command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "show")
            Then("Something should happen") {
                verify { impressionInterface.onShowImpression() }
            }
        }
        When("nativeFunctionWithArgs is called with error command") {
            And("message is there") {
                val args = JSONObject("{\"message\":\"lol\"}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "error")
                Then("Something should happen") {
                    verify { impressionInterface.impressionNotifyDidCompleteRewardedOnError() }
                    verify { impressionInterface.onWebViewError("lol") }
                }
            }
            And("message is not there") {
                val args = JSONObject("{\"massega\":-1}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "error")
                Then("Something should happen") {
                    verify { impressionInterface.impressionNotifyDidCompleteRewardedOnError() }
                    verify { impressionInterface.onWebViewError("") }
                }
            }
        }
        When("nativeFunctionWithArgs is called with warning command") {
            And("message is there") {
                val args = JSONObject("{\"message\":\"lol\"}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "warning")
                Then("Something should happen") {
                    verify { impressionInterface.warning("lol") }
                }
            }
            And("message is not there") {
                val args = JSONObject("{\"massega\":-1}")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "warning")
                Then("Something should happen") {
                    verify { impressionInterface.warning("Warning message is empty") }
                }
            }
        }
        When("nativeFunctionWithArgs is called with debug command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "debug")
            Then("Nothing should happen") {
                verify { impressionInterface wasNot Called }
            }
        }
        When("nativeFunctionWithArgs is called with tracking command") {
            val args = JSONObject("{\"event\":\"lol\"}")
            nativeBridgeCommand.nativeFunctionWithArgs(args, "tracking")
            Then("Something should happen") {
                verify { impressionInterface.sendWebViewVASTTrackingEvents("lol") }
            }
        }
        When("nativeFunctionWithArgs is called with openUrl command") {
            val args = JSONObject("{\"url\":\"google.es\", \"shouldDismiss\":true }")
            every { urlParserMock.parseOpenUrlArgsObject(args) } returns CBUrl("https://google.es", true)
            And("url is google.es and shouldDismiss is true") {
                nativeBridgeCommand.nativeFunctionWithArgs(args, "openUrl")
                Then("Something should happen") {
                    verify { impressionInterface.onTemplateOpenURLEvent(CBUrl("https://google.es", true)) }
                }
            }
            And("url is http and shouldDismiss is false") {
                args.put("url", "http://google.es").put("shouldDismiss", false)
                every { urlParserMock.parseOpenUrlArgsObject(args) } returns CBUrl("http://google.es", false)
                nativeBridgeCommand.nativeFunctionWithArgs(args, "openUrl")
                Then("Something should happen") {
                    verify { impressionInterface.onTemplateOpenURLEvent(CBUrl("http://google.es", false)) }
                }
            }
        }
        When("nativeFunctionWithArgs is called with setOrientationProperties command") {
            val args = JSONObject("{\"allowOrientationChange\":true, \"forceOrientation\":\"portrait\" }")
            And("allowOrientationChange is true and forceOrientation is portrait") {
                nativeBridgeCommand.nativeFunctionWithArgs(args, "setOrientationProperties")
                Then("Something should happen") {
                    verify { impressionInterface.setOrientationProperties(true, "portrait") }
                }
            }
            And("allowOrientationChange is false and forceOrientation is landscape") {
                args.put("allowOrientationChange", false).put("forceOrientation", "landscape")
                nativeBridgeCommand.nativeFunctionWithArgs(args, "setOrientationProperties")
                Then("Something should happen") {
                    verify { impressionInterface.setOrientationProperties(false, "landscape") }
                }
            }
        }
        When("nativeFunctionWithArgs is called with reward command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "reward")
            Then("Something should happen") {
                verify { impressionInterface.templateRewardEvent() }
            }
        }
        When("nativeFunctionWithArgs is called with rewardedVideoCompleted command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "rewardedVideoCompleted")
            Then("Something should happen") {
                verify { impressionInterface.onRewardedVideoCompleted() }
            }
        }
        When("nativeFunctionWithArgs is called with playVideo command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "playVideo")
            Then("Something should happen") {
                verify { impressionInterface.playVideo() }
            }
        }
        When("nativeFunctionWithArgs is called with pauseVideo command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "pauseVideo")
            Then("Something should happen") {
                verify { impressionInterface.pauseVideo() }
            }
        }
        When("nativeFunctionWithArgs is called with closeVideo command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "closeVideo")
            Then("Something should happen") {
                verify { impressionInterface.closeVideo() }
            }
        }
        When("nativeFunctionWithArgs is called with mute command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "mute")
            Then("Something should happen") {
                verify { impressionInterface.mute() }
            }
        }
        When("nativeFunctionWithArgs is called with unmute command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "unmute")
            Then("Something should happen") {
                verify { impressionInterface.unmute() }
            }
        }
//        ------
        When("nativeFunctionWithArgs is called with OMMeasurementResources command") {
            nativeBridgeCommand.nativeFunctionWithArgs(JSONObject("{\"resources\":\"\"}"), "OMMeasurementResources")
            Then("Something should happen") {
                verify { impressionInterface.passVerificationResourcesFromTemplate(emptyList()) }
            }
        }
//        ***
        When("nativeFunctionWithArgs is called with start command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "start")
            Then("Something should happen") {
                verify { impressionInterface.sendWebViewVastOmEvent(VastVideoEvent.START) }
            }
        }
        When("nativeFunctionWithArgs is called with bufferStart command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "bufferStart")
            Then("Something should happen") {
                verify { impressionInterface.sendWebViewVastOmEvent(VastVideoEvent.BUFFER_START) }
            }
        }
        When("nativeFunctionWithArgs is called with bufferEnd command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "bufferEnd")
            Then("Something should happen") {
                verify { impressionInterface.sendWebViewVastOmEvent(VastVideoEvent.BUFFER_END) }
            }
        }
        When("nativeFunctionWithArgs is called with videoFinished command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "videoFinished")
            Then("Something should happen") {
                verify { impressionInterface.sendWebViewVastOmEvent(VastVideoEvent.COMPLETED) }
            }
        }
        When("nativeFunctionWithArgs is called with videoStarted command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "videoStarted")
            Then("Nothing should happen") {
                verify { impressionInterface wasNot Called }
            }
        }
        When("nativeFunctionWithArgs is called with videoEnded command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "videoEnded")
            Then("Nothing should happen") {
                verify { impressionInterface wasNot Called }
            }
        }
        When("nativeFunctionWithArgs is called with videoFailed command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "videoFailed")
            Then("Nothing should happen") {
                verify { impressionInterface wasNot Called }
            }
        }
        When("nativeFunctionWithArgs is called with playbackTime command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "playbackTime")
            Then("Nothing should happen") {
                verify { impressionInterface wasNot Called }
            }
        }
        When("nativeFunctionWithArgs is called with onBackground command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "onBackground")
            Then("Nothing should happen") {
                verify { impressionInterface wasNot Called }
            }
        }
        When("nativeFunctionWithArgs is called with onForeground command") {
            nativeBridgeCommand.nativeFunctionWithArgs(null, "onForeground")
            Then("Nothing should happen") {
                verify { impressionInterface wasNot Called }
            }
        }
    }
})

private const val GET_PARAMETERS = "return String getAdUnitParameters"
private const val GET_MAX_SIZE = "return String max size"
private const val GET_SCREEN_SIZE = "return String screen size"
private const val GET_CURRENT_POSITION = "return String current position"
private const val GET_DEFAULT_POSITION = "return String default position"
private const val GET_ORIENTATION_PROPERTIES = "return String get orientation properties"
