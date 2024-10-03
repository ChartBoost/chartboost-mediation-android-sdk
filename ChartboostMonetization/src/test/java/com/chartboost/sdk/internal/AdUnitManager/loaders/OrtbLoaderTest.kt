package com.chartboost.sdk.internal.AdUnitManager.loaders

import android.os.Build
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.parsers.OpenRTBAdUnitParser
import com.chartboost.sdk.internal.AssetLoader.AssetDownloadCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import org.json.JSONException
import org.json.JSONObject

class OrtbLoaderTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    val adTypeMock: AdType =
        mockk<AdType>().apply {
            every { name } returns "foo"
        }
    val downloaderMock: Downloader = mockk()
    val openRTBAdUnitParserMock: OpenRTBAdUnitParser = mockk()
    val jsonFactoryMock: (String) -> JSONObject = mockk()
    val androidVersionMock: () -> Int =
        mockk<() -> Int>().apply {
            every { this@apply.invoke() } returns Build.VERSION_CODES.LOLLIPOP
        }

    val appRequestMock: AppRequest =
        mockk<AppRequest>().apply {
            every { location } returns "foo"
            every { bidResponse } returns "bar"
        }
    val loadParamsMock: LoadParams =
        mockk<LoadParams>().apply {
            every { appRequest } returns appRequestMock
        }

    val eventTrackerMock: EventTrackerExtensions = relaxedMockk()

    Given("An OrtbLoader class instance") {
        val sut =
            OrtbLoader(
                adType = adTypeMock,
                downloader = downloaderMock,
                openRTBAdUnitParser = openRTBAdUnitParserMock,
                jsonFactory = jsonFactoryMock,
                androidVersion = androidVersionMock,
                eventTracker = eventTrackerMock,
            )

        val callback: LoadResult.() -> Unit = mockk()
        val loadResultCapturingSlot = slot<LoadResult>()
        justRun { callback.invoke(capture(loadResultCapturingSlot)) }

        And("Wrong Android version") {
            every { androidVersionMock.invoke() } returns Build.VERSION_CODES.LOLLIPOP - 1

            When("Calling loadAd()") {
                sut.loadAd(loadParamsMock, callback)

                Then("Result should be unsupported OS version") {
                    loadResultCapturingSlot.captured.run {
                        error?.type shouldBe CBError.Internal.UNSUPPORTED_OS_VERSION
                    }
                }
            }
        }

        And("Location is empty") {
            every { appRequestMock.location } returns ""

            When("Calling loadAd()") {
                sut.loadAd(loadParamsMock, callback)

                Then("Result should be unexpected response") {
                    loadResultCapturingSlot.captured.run {
                        error?.type shouldBe CBError.Internal.UNEXPECTED_RESPONSE
                    }
                }
            }
        }

        And("Bid response is invalid") {

            And("Bid response is null") {
                every { appRequestMock.bidResponse } returns null

                When("Calling loadAd()") {
                    sut.loadAd(loadParamsMock, callback)

                    Then("Result should be unexpected response") {
                        loadResultCapturingSlot.captured.run {
                            error?.type shouldBe CBError.Internal.UNEXPECTED_RESPONSE
                        }
                    }
                }
            }

            And("Bid response is empty") {
                every { appRequestMock.bidResponse } returns ""

                When("Calling loadAd()") {
                    sut.loadAd(loadParamsMock, callback)

                    Then("Result should be unexpected response") {
                        loadResultCapturingSlot.captured.run {
                            error?.type shouldBe CBError.Internal.UNEXPECTED_RESPONSE
                        }
                    }
                }
            }

            And("Bid response is not a valid JSON") {
                every { jsonFactoryMock.invoke(any()) } throws JSONException("foo")

                When("Calling loadAd()") {
                    sut.loadAd(loadParamsMock, callback)

                    Then("Result should be invalid response") {
                        loadResultCapturingSlot.captured.run {
                            error?.type shouldBe CBError.Internal.INVALID_RESPONSE
                        }
                    }
                }
            }

            And("OpenRTB Ad Unit Parser throws a JSONException") {
                every { jsonFactoryMock.invoke(any()) } returns mockk()
                every { openRTBAdUnitParserMock.parse(any(), any()) } throws JSONException("foo")

                When("Calling loadAd()") {
                    sut.loadAd(loadParamsMock, callback)

                    Then("Result should be invalid response") {
                        loadResultCapturingSlot.captured.run {
                            error?.type shouldBe CBError.Internal.INVALID_RESPONSE
                        }
                    }
                }
            }
        }

        And("Bid response is valid") {
            every { jsonFactoryMock.invoke(any()) } returns mockk()
            every { openRTBAdUnitParserMock.parse(any(), any()) } returns
                mockk<AdUnit>().apply {
                    every { assets } returns mockk()
                }
            justRun { downloaderMock.resume() }

            And("Downloader returns failure") {
                every {
                    downloaderMock.downloadAssets(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } answers {
                    (args[3] as AssetDownloadCallback).assetDownloadResult(false)
                }

                When("Calling loadAd()") {
                    sut.loadAd(loadParamsMock, callback)

                    Then("Result should be invalid response") {
                        loadResultCapturingSlot.captured.run {
                            error?.type shouldBe CBError.Internal.INVALID_RESPONSE
                        }
                    }
                }
            }

            And("Downloader returns success") {
                every {
                    downloaderMock.downloadAssets(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } answers {
                    (args[3] as AssetDownloadCallback).assetDownloadResult(true)
                }

                When("Calling loadAd()") {
                    sut.loadAd(loadParamsMock, callback)

                    Then("Result should be invalid response") {
                        loadResultCapturingSlot.captured.run {
                            error?.type.shouldBeNull()
                        }
                    }
                }
            }
        }
    }
})
