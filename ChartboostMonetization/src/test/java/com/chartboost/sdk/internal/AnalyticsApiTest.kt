package com.chartboost.sdk.internal

import android.text.TextUtils
import android.util.Base64
import com.chartboost.sdk.Analytics
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.initialization.SdkInitializer
import com.chartboost.sdk.tracking.EventTracker
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify

class AnalyticsApiTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    val initializerMock = mockk<SdkInitializer>()
    val networkServiceMock = mockk<CBNetworkService>(relaxed = true)

    val requestBodyBuilderMock = mockk<RequestBodyBuilder>()
    every { requestBodyBuilderMock.build() } returns mockk()

    val eventTrackerMock =
        mockk<EventTracker>().apply {
            justRun { track(any()) }
        }

    // Mock static Android utils calls
    mockkStatic(TextUtils::class)
    every { TextUtils.isEmpty(any()) } answers { firstArg<String?>().isNullOrEmpty() }

    mockkStatic(Base64::class)
    every { Base64.encodeToString(any(), any()) } answers {
        java.util.Base64.getUrlEncoder().encodeToString(firstArg<ByteArray>())
    }

    suspend fun BehaviorSpecWhenContainerScope.ThenShouldNotSendAnyRequests() {
        Then("Should not send any requests") {
            verify(exactly = 0) { networkServiceMock.submit(any<CBRequest>()) }
        }
    }

    suspend fun BehaviorSpecWhenContainerScope.ThenShouldSendNotNullRequest(block: (CBRequest) -> Unit) {
        Then("Should send request with correct data") {
            val requestCaptor = slot<CBRequest>()
            verify(exactly = 1) { networkServiceMock.submit(capture(requestCaptor)) }
            val request = requestCaptor.captured
            request.shouldNotBeNull()
            block(request)
        }
    }

    fun CBRequest.assertIAPValues() {
        body.getJSONObject("iap").run {
            getString("localized-description") shouldBe "test-description"
            getString("localized-title") shouldBe "test-title"
            getString("productID") shouldBe "test-productID"
            getInt("price") shouldBe 1
            getString("currency") shouldBe "EUR"
        }
    }

    Given("AnalyticsApi instance") {
        val analyticsApi =
            AnalyticsApi(
                initializerMock,
                networkServiceMock,
                requestBodyBuilderMock,
                eventTracker = eventTrackerMock,
            )

        And("SDK is not initialized") {
            every { initializerMock.isSdkInitialized() } returns false

            And("IAP is Google Play") {
                val iapType = Analytics.IAPType.GOOGLE_PLAY

                When("Tracking in-app purchase events") {
                    analyticsApi.trackInAppPurchaseEvent(
                        productID = "test",
                        title = "test",
                        description = "test",
                        price = "1",
                        currency = "EUR",
                        purchaseData = "test",
                        purchaseSignature = "test",
                        userID = null,
                        purchaseToken = null,
                        iapType = iapType,
                    )
                    ThenShouldNotSendAnyRequests()
                }
            }

            And("IAP is Amazon") {
                val iapType = Analytics.IAPType.AMAZON

                When("Tracking in-app purchase events") {
                    analyticsApi.trackInAppPurchaseEvent(
                        productID = "test",
                        title = "test",
                        description = "test",
                        price = "1",
                        currency = "EUR",
                        purchaseData = null,
                        purchaseSignature = null,
                        userID = "test",
                        purchaseToken = "test",
                        iapType = iapType,
                    )
                    ThenShouldNotSendAnyRequests()
                }
            }

            When("Tracking level info") {
                analyticsApi.trackLevelInfo(
                    eventLabel = "test",
                    type = Analytics.LevelType.CHARACTER_LEVEL,
                    mainLevel = 1,
                    subLevel = 1,
                    description = "test",
                    timestamp = 1L,
                )
                ThenShouldNotSendAnyRequests()
            }
        }

        And("SDK is initialized") {
            every { initializerMock.isSdkInitialized() } returns true

            And("IAP is Google Play") {
                val iapType = Analytics.IAPType.GOOGLE_PLAY
                var purchaseData: String? = "test"
                var purchaseSignature: String? = "test"
                var userId: String? = null
                var purchaseToken: String? = null

                When("Tracking in-app purchase events") {
                    analyticsApi.trackInAppPurchaseEvent(
                        productID = "test-productID",
                        title = "test-title",
                        description = "test-description",
                        price = "1",
                        currency = "EUR",
                        purchaseData = purchaseData,
                        purchaseSignature = purchaseSignature,
                        userID = userId,
                        purchaseToken = purchaseToken,
                        iapType = iapType,
                    )
                    ThenShouldSendNotNullRequest { it.assertIAPValues() }
                }

                And("Invalid data") {
                    purchaseData = null
                    purchaseSignature = null

                    When("Tracking in-app purchase events") {
                        analyticsApi.trackInAppPurchaseEvent(
                            productID = "test",
                            title = "test",
                            description = "test",
                            price = "1",
                            currency = "EUR",
                            purchaseData = purchaseData,
                            purchaseSignature = purchaseSignature,
                            userID = userId,
                            purchaseToken = purchaseToken,
                            iapType = iapType,
                        )
                        ThenShouldNotSendAnyRequests()
                    }
                }

                And("Amazon data") {
                    userId = "test amazon"
                    purchaseToken = "test amazon"

                    When("Tracking in-app purchase events") {
                        analyticsApi.trackInAppPurchaseEvent(
                            productID = "test-productID",
                            title = "test-title",
                            description = "test-description",
                            price = "1",
                            currency = "EUR",
                            purchaseData = purchaseData,
                            purchaseSignature = purchaseSignature,
                            userID = userId,
                            purchaseToken = purchaseToken,
                            iapType = iapType,
                        )
                        ThenShouldSendNotNullRequest { it.assertIAPValues() }
                    }
                }
            }

            And("IAP is Amazon") {
                val iapType = Analytics.IAPType.AMAZON
                var purchaseData: String? = null
                var purchaseSignature: String? = null
                var userId: String? = "test amazon"
                var purchaseToken: String? = "test amazon"

                When("Tracking in-app purchase events") {
                    analyticsApi.trackInAppPurchaseEvent(
                        productID = "test-productID",
                        title = "test-title",
                        description = "test-description",
                        price = "1",
                        currency = "EUR",
                        purchaseData = purchaseData,
                        purchaseSignature = purchaseSignature,
                        userID = userId,
                        purchaseToken = purchaseToken,
                        iapType = iapType,
                    )
                    ThenShouldSendNotNullRequest { it.assertIAPValues() }
                }

                And("Invalid data") {
                    userId = null
                    purchaseToken = null

                    When("Tracking in-app purchase events") {
                        analyticsApi.trackInAppPurchaseEvent(
                            productID = "test",
                            title = "test",
                            description = "test",
                            price = "1",
                            currency = "EUR",
                            purchaseData = purchaseData,
                            purchaseSignature = purchaseSignature,
                            userID = userId,
                            purchaseToken = purchaseToken,
                            iapType = iapType,
                        )
                        ThenShouldNotSendAnyRequests()
                    }
                }

                And("Google Play data") {
                    purchaseData = "test"
                    purchaseSignature = "test"

                    When("Tracking in-app purchase events") {
                        analyticsApi.trackInAppPurchaseEvent(
                            productID = "test-productID",
                            title = "test-title",
                            description = "test-description",
                            price = "1",
                            currency = "EUR",
                            purchaseData = purchaseData,
                            purchaseSignature = purchaseSignature,
                            userID = userId,
                            purchaseToken = purchaseToken,
                            iapType = iapType,
                        )
                        ThenShouldSendNotNullRequest { it.assertIAPValues() }
                    }
                }
            }

            And("Tracking level info") {
                var eventLabel = "test-event-label"
                var mainLevel = 1
                var subLevel = 1
                var description = "test-description"

                And("Valid values") {

                    When("Tracking level info") {
                        analyticsApi.trackLevelInfo(
                            eventLabel = eventLabel,
                            type = Analytics.LevelType.CHARACTER_LEVEL,
                            mainLevel = mainLevel,
                            subLevel = subLevel,
                            description = description,
                            timestamp = 1L,
                        )
                        ThenShouldSendNotNullRequest { request ->
                            request.body.getJSONArray("track_info").getJSONObject(0).run {
                                getInt("main_level") shouldBe 1
                                getInt("event_field") shouldBe 3
                                getInt("sub_level") shouldBe 1
                                getInt("timestamp") shouldBe 1
                                getString("data_type") shouldBe "level_info"
                                getString("description") shouldBe "test-description"
                                getString("event_label") shouldBe "test-event-label"
                            }
                        }
                    }
                }

                And("Invalid sub level") {
                    subLevel = -1

                    When("Tracking level info") {
                        analyticsApi.trackLevelInfo(
                            eventLabel = eventLabel,
                            type = Analytics.LevelType.CHARACTER_LEVEL,
                            mainLevel = mainLevel,
                            subLevel = subLevel,
                            description = description,
                            timestamp = 1L,
                        )
                        ThenShouldNotSendAnyRequests()
                    }
                }

                And("Invalid main level") {
                    mainLevel = -1

                    When("Tracking level info") {
                        analyticsApi.trackLevelInfo(
                            eventLabel = eventLabel,
                            type = Analytics.LevelType.CHARACTER_LEVEL,
                            mainLevel = mainLevel,
                            subLevel = subLevel,
                            description = description,
                            timestamp = 1L,
                        )
                        ThenShouldNotSendAnyRequests()
                    }
                }

                And("Invalid event label") {
                    eventLabel = ""

                    When("Tracking level info") {
                        analyticsApi.trackLevelInfo(
                            eventLabel = eventLabel,
                            type = Analytics.LevelType.CHARACTER_LEVEL,
                            mainLevel = mainLevel,
                            subLevel = subLevel,
                            description = description,
                            timestamp = 1L,
                        )
                        ThenShouldNotSendAnyRequests()
                    }
                }

                And("Invalid description") {
                    description = ""

                    When("Tracking level info") {
                        analyticsApi.trackLevelInfo(
                            eventLabel = eventLabel,
                            type = Analytics.LevelType.CHARACTER_LEVEL,
                            mainLevel = mainLevel,
                            subLevel = subLevel,
                            description = description,
                            timestamp = 1L,
                        )
                        ThenShouldNotSendAnyRequests()
                    }
                }
            }
        }
    }
})
