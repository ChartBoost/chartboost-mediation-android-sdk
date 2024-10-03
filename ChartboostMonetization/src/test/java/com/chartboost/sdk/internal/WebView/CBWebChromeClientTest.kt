package com.chartboost.sdk.internal.WebView

import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.WebChromeClient.CustomViewCallback
import android.widget.FrameLayout
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject

class CBWebChromeClientTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val activityNonVideoView: View = mockk(relaxed = true)
    val nativeBridgeCommand: NativeBridgeCommand = mockk(relaxed = true)
    val corsHandler: WebViewCorsErrorHandler = mockk(relaxed = true)

    Given("a chromeClient") {
        val chromeClient =
            CBRichWebChromeClient(
                activityNonVideoView,
                nativeBridgeCommand,
                corsHandler,
            )
        Then("vide view callback is set on NativeBridgeCommand") {
            verify { nativeBridgeCommand.hideViewCallback = chromeClient }
        }
        When("check if video is full screen") {
            val isFullScreen = chromeClient.isVideoFullscreen
            Then("it should return false") {
                isFullScreen shouldBe false
            }
        }
        And("show full screen is called") {
            val frameLayout: FrameLayout = mockk(relaxed = true)
            chromeClient.onShowCustomView(frameLayout, null)

            When("check if video is full screen") {
                val isFullScreen = chromeClient.isVideoFullscreen
                Then("it should return true") {
                    isFullScreen shouldBe true
                }
            }
        }
        When("onConsoleMessage is called with a CORS error") {
            val consoleMessage: ConsoleMessage = mockk(relaxed = true)
            val returnValue = chromeClient.onConsoleMessage(consoleMessage)

            Then("return value should be true") {
                returnValue shouldBe true
            }
            Then("it should handle the CORS error") {
                chromeClient.onConsoleMessage(consoleMessage) shouldBe true
                verify { corsHandler.handleCorsError(consoleMessage.message(), chromeClient) }
            }
        }
        When("notifyCorsError is called with data") {
            val data =
                JSONObject().put(
                    "message",
                    "CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource",
                )
            chromeClient.notifyCorsError(data)
            Then("it should call nativeFunctionWithArgs") {
                verify { nativeBridgeCommand.nativeFunctionWithArgs(data, "error") }
            }
        }
        When("onShowCustomView is called with a view and custom view callback") {
            val view = mockk<FrameLayout>()
            val customViewCallback = mockk<CustomViewCallback>(relaxed = true)

            chromeClient.onShowCustomView(view, customViewCallback)
            Then("video should be full screen") {
                chromeClient.isVideoFullscreen shouldBe true
            }
            Then("it should hide the non video view") {
                verify { activityNonVideoView.setVisibility(View.INVISIBLE) }
            }
            And("onHideCustomView is called") {
                chromeClient.onHideCustomView()
                Then("it should handle hiding the video view") {
                    chromeClient.isVideoFullscreen shouldBe false
                }
                Then("it should call callback on custom view hidden") {
                    customViewCallback.onCustomViewHidden()
                }
            }
            And("onBackPressed is called") {
                val backpressedReturn = chromeClient.onBackPressed()
                Then("it should handle back key press") {
                    backpressedReturn shouldBe true
                }
                Then("onHideCustomView is called") {
                    chromeClient.isVideoFullscreen shouldBe false
                    customViewCallback.onCustomViewHidden()
                }
            }
        }
        When("onBackPressed is called") {
            Then("it should return false") {
                chromeClient.onBackPressed() shouldBe false
            }
        }
        And("Native Bridge Comand returns a known result") {
            every { nativeBridgeCommand.nativeFunctionWithArgs(any(), any()) } returns "nativeResult"

            When("onJsPrompt is called") {
                val args = "{\"arg1\":\"value1\"}"
                val message = "{\"eventType\":\"eventName\",\"eventArgs\":$args}"
                val jsPromptResult: JsPromptResult = mockk(relaxed = true)

                val promptResult = chromeClient.onJsPrompt(null, null, message, null, jsPromptResult)
                Then("it should return true") {
                    promptResult shouldBe true
                }
                Then("it should call nativeFunctionWithArgs and confirm the result") {
                    verify {
                        nativeBridgeCommand.nativeFunctionWithArgs(
                            withArg { it.toString() shouldBe args },
                            "eventName",
                        )
                    }
                    verify { jsPromptResult.confirm("nativeResult") }
                }
            }
        }
    }
})
