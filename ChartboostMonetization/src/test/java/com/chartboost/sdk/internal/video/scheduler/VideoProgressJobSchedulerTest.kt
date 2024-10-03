package com.chartboost.sdk.internal.video.scheduler

import com.chartboost.sdk.internal.video.player.AdsVideoPlayerListener
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressScheduler
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressSchedulerCoroutines
import com.chartboost.sdk.test.advanceTimeBy
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forAll
import io.kotest.property.Exhaustive
import io.kotest.property.exhaustive.filter
import io.kotest.property.exhaustive.longs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

@ExperimentalCoroutinesApi
class VideoProgressJobSchedulerTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    val callbackMock: AdsVideoPlayerListener = mockk()
    val videoProgressMock: VideoProgressScheduler.VideoProgress = mockk()
    val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    Given("A VideoProgressJobScheduler instance") {
        val videoProgressJobScheduler =
            VideoProgressSchedulerCoroutines(
                callback = callbackMock,
                videoProgress = videoProgressMock,
                coroutineDispatcher = dispatcher,
            )

        val delay: Long = 100
        When("Calling startProgressUpdate() with $delay delay") {
            every { videoProgressMock.currentPosition() } returns 0
            every { callbackMock.onVideoDisplayProgress(any()) } just runs
            videoProgressJobScheduler.startProgressUpdate(delay)

            // Create time advances by 100ms from lowRange to highRange
            val lowRange = 100L
            val highRange = 1000L
            Exhaustive
                .longs(lowRange..highRange)
                .filter { it % 100 == 0L }
                .values
                .forAll { advance ->

                    // Give 10 ms more for the coroutines to have time to execute
                    And("Time advances by $advance + 10 ms") {
                        dispatcher.advanceTimeBy(advance + 10)

                        val times = (advance / delay).toInt()
                        Then("It should call the callback exactly $times times") {
                            verify(exactly = times) { callbackMock.onVideoDisplayProgress(any()) }
                        }

                        And("stopProgressUpdate() is called") {
                            videoProgressJobScheduler.stopProgressUpdate()

                            // Huge time advance to make sure the callback is never called again
                            val newAdvance = delay * 100
                            And("Time advances by another $newAdvance ms") {
                                dispatcher.advanceTimeBy(newAdvance)

                                Then("It should not call the callback again") {
                                    verify(exactly = times) { callbackMock.onVideoDisplayProgress(any()) }
                                }
                            }
                        }
                    }
                }
        }
    }
})
