package com.chartboost.sdk.internal.AdUnitManager.render

import android.content.Intent
import com.chartboost.sdk.internal.Libraries.DisplayMeasurement
import com.chartboost.sdk.internal.Libraries.DisplaySize
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.utils.ImpressionActivityIntentWrapper
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEvent
import com.chartboost.sdk.tracking.TrackingEventName
import com.chartboost.sdk.view.CBImpressionActivity
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.concurrent.atomic.AtomicReference

class RendererActivityBridgeImplTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerTest

    val impressionActivityIntentWrapperMock = mockk<ImpressionActivityIntentWrapper>(relaxed = true)
    val sdkConfigurationMock = mockk<SdkConfiguration>()
    val configRef = AtomicReference(sdkConfigurationMock)
    val activityRendererInterfaceMock = mockk<ActivityRendererInterface>()
    val activityMock = mockk<CBImpressionActivity>()
    val intentMock = mockk<Intent>()
    val viewBaseMock = mockk<ViewBase>()
    val adUnitRendererActivityInterfaceMock = mockk<AdUnitRendererActivityInterface>()
    val displayMeasurementMock = mockk<DisplayMeasurement>()
    val eventTrackerMock = relaxedMockk<EventTrackerExtensions>()

    every { impressionActivityIntentWrapperMock.getImpressionActivityIntent() } returns intentMock
    every { impressionActivityIntentWrapperMock.startActivity(any()) } just Runs
    every { adUnitRendererActivityInterfaceMock.onActivityIsReadyToDisplay(any()) } just Runs
    every { adUnitRendererActivityInterfaceMock.failure(any()) } just Runs
    every { adUnitRendererActivityInterfaceMock.impressionOnStart() } just Runs
    every { adUnitRendererActivityInterfaceMock.impressionOnResume() } just Runs
    every { adUnitRendererActivityInterfaceMock.impressionOnPause() } just Runs
    every { adUnitRendererActivityInterfaceMock.impressionOnDestroy() } just Runs
    every { adUnitRendererActivityInterfaceMock.onConfigurationChange() } just Runs
    every { adUnitRendererActivityInterfaceMock.onBackPressed() } returns true
    every { activityRendererInterfaceMock.displayViewOnActivity(any()) } just Runs
    every { activityRendererInterfaceMock.finishActivity() } just Runs
    every { activityRendererInterfaceMock.finishActivity() } just Runs
    every { displayMeasurementMock.getDeviceSize() } returns DisplaySize(100, 10)

    Given("RendererActivityBridgeImpl instance") {
        val rendererActivityBridge =
            RendererActivityBridgeImpl(
                impressionActivityIntentWrapperMock,
                eventTrackerMock,
            )

        When("start activity to set ad unit interface") {
            rendererActivityBridge.startActivity(adUnitRendererActivityInterfaceMock)
            Then("start activity") {
                verify(exactly = 1) { impressionActivityIntentWrapperMock.getImpressionActivityIntent() }
                verify(exactly = 1) { impressionActivityIntentWrapperMock.startActivity(intentMock) }
            }
            And(" set interface to the activity") {
                rendererActivityBridge.setActivityRendererInterface(
                    activityRendererInterfaceMock,
                    activityMock,
                )

                Then("on activity is ready to display") {
                    verify(exactly = 1) {
                        adUnitRendererActivityInterfaceMock.onActivityIsReadyToDisplay(
                            activityMock,
                        )
                    }
                }

                And("finish activity") {
                    rendererActivityBridge.finishActivity()
                    Then("calls activity finish") {
                        verify(exactly = 1) { activityRendererInterfaceMock.finishActivity() }
                    }
                }

                And("display on activity") {
                    rendererActivityBridge.displayViewOnActivity(viewBaseMock)
                    Then("display view in the activity via activity interface") {
                        verify(exactly = 1) {
                            activityRendererInterfaceMock.displayViewOnActivity(
                                viewBaseMock,
                            )
                        }
                    }
                }

                And("activity on start") {
                    rendererActivityBridge.onStart()
                    Then("calls impression on start") {
                        verify(exactly = 1) { adUnitRendererActivityInterfaceMock.impressionOnStart() }
                    }
                }

                And("activity on resume") {
                    rendererActivityBridge.onResume()
                    Then("calls impression on resume") {
                        verify(exactly = 1) { adUnitRendererActivityInterfaceMock.impressionOnResume() }
                    }
                }

                And("activity on pause") {
                    rendererActivityBridge.onPause()
                    Then("calls impression on pause") {
                        verify(exactly = 1) { adUnitRendererActivityInterfaceMock.impressionOnPause() }
                    }
                }

                And("activity on configuration changed") {
                    rendererActivityBridge.onConfigurationChange()
                    Then("calls impression onConfigurationChange") {
                        verify(exactly = 1) { adUnitRendererActivityInterfaceMock.onConfigurationChange() }
                    }
                }

                And("activity on failure") {
                    rendererActivityBridge.failure(CBError.Impression.NO_HOST_ACTIVITY)
                    Then("calls impression failure") {
                        verify(exactly = 1) {
                            adUnitRendererActivityInterfaceMock.failure(
                                CBError.Impression.NO_HOST_ACTIVITY,
                            )
                        }
                    }
                }

                And("activity on back press") {
                    rendererActivityBridge.onBackPressed()
                    Then("calls impression back press") {
                        verify(exactly = 1) { adUnitRendererActivityInterfaceMock.onBackPressed() }
                    }
                }
            }

            When("activity destroy") {
                val slot = CapturingSlot<TrackingEvent>()
                rendererActivityBridge.onDestroy()
                Then("calls impression back press") {
                    verify(exactly = 1) { with(eventTrackerMock) { capture(slot).track() } }
                    assertEquals(slot.captured.name, TrackingEventName.Show.DISMISS_MISSING)
                    verify(exactly = 1) { adUnitRendererActivityInterfaceMock.impressionOnDestroy() }
                }
            }
        }
    }
})
