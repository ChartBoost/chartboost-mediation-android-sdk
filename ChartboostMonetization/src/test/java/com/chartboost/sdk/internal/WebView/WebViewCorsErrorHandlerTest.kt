package com.chartboost.sdk.internal.WebView

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.json.JSONObject

internal class WebViewCorsErrorHandlerTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val mockCallback = mockk<WebViewCorsErrorHandler.CorsErrorCallback>()

    val expectedData =
        JSONObject().put(
            "message",
            "CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource",
        )

    Given("a WebViewCorsErrorHandler instance") {
        val webviewCorsErrorHandler = WebViewCorsErrorHandler()
        every { mockCallback.notifyCorsError(any()) } just Runs

        When("the webviewMessage contains a CORS error") {
            webviewCorsErrorHandler.handleCorsError(
                "'Access-Control-Allow-Origin' 'null'",
                mockCallback,
            )

            Then("the callback should be notified with the CORS error") {
                verify {
                    mockCallback.notifyCorsError(
                        withArg {
                            it.toString() shouldBe expectedData.toString()
                        },
                    )
                }
            }
        }

        When("the webviewMessage contains a CORS error with asset location") {
            webviewCorsErrorHandler.handleCorsError(
                "Chartboost Webview:Failed to load location/asset/disk:" +
                    " No 'Access-Control-Allow-Origin' header is present" +
                    " on the requested resource. Origin 'null' is therefore" +
                    " not allowed access. -- From line 0",
                mockCallback,
            )

            Then("cors url is ignored") {
                verify {
                    mockCallback.notifyCorsError(
                        withArg {
                            it.toString() shouldBe expectedData.toString()
                        },
                    )
                }
            }
        }

        When("the webviewMessage contains a CORS error with url") {
            webviewCorsErrorHandler.handleCorsError(
                "Chartboost Webview:Failed to load" +
                    " https://prod-template-logs-es.caffeine.io/v1:" +
                    " No 'Access-Control-Allow-Origin' header is present" +
                    " on the requested resource. Origin 'null' is therefore" +
                    " not allowed access. -- From line 0",
                mockCallback,
            )

            Then("handle the CORS error by closing the ad") {
                verify(exactly = 0) { mockCallback.notifyCorsError(any()) }
                /*                verify { mockCallback.notifyCorsError(withArg {
                                    it.toString() shouldBe expectedData.toString()
                                }) }*/
            }
        }

        When("the webviewMessage does not contain a CORS error") {
            val webviewMessage = "Regular error message without CORS error"
            every { mockCallback.notifyCorsError(any()) } just Runs

            webviewCorsErrorHandler.handleCorsError(webviewMessage, mockCallback)

            Then("the callback should not be notified") {
                verify(exactly = 0) { mockCallback.notifyCorsError(any()) }
            }
        }

        When("the webviewMessage does not contain a CORS error") {
            val webviewMessage =
                "Chartboost Webview:Failed to load https://prod-template-logs-es.caffeine.io/v1:" +
                    " No header is present on the requested resource. Origin is therefore" +
                    " not allowed access. -- From line 0"
            every { mockCallback.notifyCorsError(any()) } just Runs

            webviewCorsErrorHandler.handleCorsError(webviewMessage, mockCallback)

            Then("the callback should not be notified") {
                verify(exactly = 0) { mockCallback.notifyCorsError(any()) }
            }
        }

        When("the webviewMessage does not contain a CORS error") {
            val webviewMessage =
                "Chartboost Webview:Failed to load https://prod-template-logs-es.caffeine.io/v1:" +
                    " No 'Access-Control-Allow-Origin' header is present on the requested" +
                    " resource. Origin is therefore not allowed access. -- From line 0"
            every { mockCallback.notifyCorsError(any()) } just Runs

            webviewCorsErrorHandler.handleCorsError(webviewMessage, mockCallback)

            Then("the callback should not be notified") {
                verify(exactly = 0) { mockCallback.notifyCorsError(any()) }
            }
        }

        When("the webviewMessage does not contain a CORS error") {
            val webviewMessage =
                "Chartboost Webview:Failed to load https://prod-template-logs-es.caffeine.io/v1:" +
                    " No  header is present on the requested resource. Origin 'null' is" +
                    " therefore not allowed access. -- From line 0"
            every { mockCallback.notifyCorsError(any()) } just Runs

            webviewCorsErrorHandler.handleCorsError(webviewMessage, mockCallback)

            Then("the callback should not be notified") {
                verify(exactly = 0) { mockCallback.notifyCorsError(any()) }
            }
        }
    }
})
