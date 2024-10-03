package com.chartboost.sdk.internal.video

import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.utils.now
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.test.setPrivateField
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.io.File

class VideoCachePolicyTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    val fileMock: File = mockk()
    val reachabilityMock = mockk<CBReachability>()

    // Default is Wifi
    every { reachabilityMock.isConnectionCellular() } returns false

    mockkStatic(::now)
    every { now() } returns 0L

    Given("VideoCachePolicy with 2 hours TTL") {
        val policy =
            VideoCachePolicy(
                ttl = 7200, // 2 hours
                reachability = reachabilityMock,
            )

        And("File is 0 seconds old") {
            every { fileMock.lastModified() } returns System.currentTimeMillis()

            When("Checking if it's time to leave for the file") {
                val isFileTimeToLeave = policy.isFileTimeToLeave(fileMock)

                Then("Should be false") {
                    isFileTimeToLeave.shouldBeFalse()
                }
            }
        }

        And("File is 2 hours old") {
            val twoHours = 2 * 60 * 60 * 1000
            val twoHoursAgo = now() - twoHours
            every { fileMock.lastModified() } returns twoHoursAgo

            When("Checking if it's time to leave for the file") {
                val isFileTimeToLeave = policy.isFileTimeToLeave(fileMock)

                Then("Should be false") {
                    isFileTimeToLeave.shouldBeFalse()
                }
            }
        }

        And("File is 2 hours and one second old") {
            val twoHoursAndOneSecond = 2 * 60 * 60 * 1000 + 1000
            val twoHoursAndOneSecondAgo = now() - twoHoursAndOneSecond
            every { fileMock.lastModified() } returns twoHoursAndOneSecondAgo

            When("Checking if it's time to leave for the file") {
                val isFileTimeToLeave = policy.isFileTimeToLeave(fileMock)

                Then("Should be true") {
                    isFileTimeToLeave.shouldBeTrue()
                }
            }
        }

        And("File is 3 hours old") {
            val threeHours = 3 * 60 * 60 * 1000
            val threeHoursAgo = now() - threeHours
            every { fileMock.lastModified() } returns threeHoursAgo

            When("Checking if it's time to leave for the file") {
                val isFileTimeToLeave = policy.isFileTimeToLeave(fileMock)

                Then("Should be true") {
                    isFileTimeToLeave.shouldBeTrue()
                }
            }
        }
    }

    Given("VideoCachePolicy with 1 MB maximum storage") {
        val policy =
            VideoCachePolicy(
                maxBytes = 1048576, // 1 MB
                reachability = reachabilityMock,
            )

        When("Checking if cache is full with exact cache size") {
            val isCacheFull = policy.isVideoCacheMaxSizeReached(1048576)

            Then("Should be true") {
                isCacheFull.shouldBeTrue()
            }
        }

        When("Checking if cache is full with cache size less than maximum") {
            val isCacheFull = policy.isVideoCacheMaxSizeReached(1048575)

            Then("Should be false") {
                isCacheFull.shouldBeFalse()
            }
        }

        When("Checking if cache is full with cache size greater than maximum") {
            val isCacheFull = policy.isVideoCacheMaxSizeReached(1048577)

            Then("Should be true") {
                isCacheFull.shouldBeTrue()
            }
        }
    }

    Given("VideoCachePolicy with 1 hour time window and 1 unit per time window for wifi") {
        val policy =
            VideoCachePolicy(
                timeWindow = 3600, // 1 hour,
                maxUnitsPerTimeWindow = 1,
                reachability = reachabilityMock,
            )

        And("Time window has not elapsed") {
            policy.setPrivateField("timeWindowStartTimeStamp", now())

            When("Checking if max count for time window is reached") {
                val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                Then("Should be false") {
                    isMaxCountReached.shouldBeFalse()
                }
            }

            And("Cached video count is equal to max units per time window") {
                policy.setPrivateField("timeWindowCachedVideosCount", 1)

                When("Checking if max count for time window is reached for wifi") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be true") {
                        isMaxCountReached.shouldBeTrue()
                    }
                }
            }

            And("Cached video count is less than max units per time window") {
                policy.setPrivateField("timeWindowCachedVideosCount", 0)

                When("Checking if max count for time window is reached") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be false") {
                        isMaxCountReached.shouldBeFalse()
                    }
                }
            }

            And("Cached video count is greater than max units per time window") {
                policy.setPrivateField("timeWindowCachedVideosCount", 2)

                When("Checking if max count for time window is reached") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be true") {
                        isMaxCountReached.shouldBeTrue()
                    }
                }
            }

            And("Add a download to time window") {
                policy.addDownloadToTimeWindow()

                When("Checking if max count for time window is reached") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be true") {
                        isMaxCountReached.shouldBeTrue()
                    }
                }
            }
        }

        And("Time window has elapsed") {
            policy.setPrivateField("timeWindowStartTimeStamp", now() - (3600 * 1000 + 1000))

            When("Checking if max count for time window is reached") {
                val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                Then("Should be false") {
                    isMaxCountReached.shouldBeFalse()
                }
            }

            And("Cached video count is equal to max units per time window") {
                policy.setPrivateField("timeWindowCachedVideosCount", 1)

                When("Checking if max count for time window is reached") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be false") {
                        isMaxCountReached.shouldBeFalse()
                    }
                }
            }

            And("Cached video count is less than max units per time window") {
                policy.setPrivateField("timeWindowCachedVideosCount", 0)

                When("Checking if max count for time window is reached") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be false") {
                        isMaxCountReached.shouldBeFalse()
                    }
                }
            }

            And("Cached video count is greater than max units per time window") {
                policy.setPrivateField("timeWindowCachedVideosCount", 2)

                When("Checking if max count for time window is reached") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be false") {
                        isMaxCountReached.shouldBeFalse()
                    }
                }
            }

            And("Add a download to time window") {
                policy.addDownloadToTimeWindow()

                When("Checking if max count for time window is reached") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be false") {
                        isMaxCountReached.shouldBeFalse()
                    }
                }
            }
        }
    }

    Given("VideoCachePolicy with 1 hour time window and 1 unit per time window for cellular connection") {
        val policy =
            VideoCachePolicy(
                timeWindowCellular = 3600, // 1 hour,
                maxUnitsPerTimeWindowCellular = 1,
                reachability = reachabilityMock,
            )

        And("Connection is cellular") {
            every { reachabilityMock.isConnectionCellular() } returns true

            And("Time window has not elapsed") {
                policy.setPrivateField("timeWindowStartTimeStamp", now())

                When("Checking if max count for time window is reached") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be false") {
                        isMaxCountReached.shouldBeFalse()
                    }
                }

                And("Cached video count is equal to max units per time window") {
                    policy.setPrivateField("timeWindowCachedVideosCount", 1)

                    When("Checking if max count for time window is reached") {
                        val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                        Then("Should be true") {
                            isMaxCountReached.shouldBeTrue()
                        }
                    }
                }

                And("Cached video count is less than max units per time window") {
                    policy.setPrivateField("timeWindowCachedVideosCount", 0)

                    When("Checking if max count for time window is reached") {
                        val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                        Then("Should be false") {
                            isMaxCountReached.shouldBeFalse()
                        }
                    }
                }

                And("Cached video count is greater than max units per time window") {
                    policy.setPrivateField("timeWindowCachedVideosCount", 2)

                    When("Checking if max count for time window is reached") {
                        val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                        Then("Should be true") {
                            isMaxCountReached.shouldBeTrue()
                        }
                    }
                }
            }

            And("Time window has elapsed") {
                policy.setPrivateField(
                    "timeWindowStartTimeStamp",
                    now() - (3600 * 1000 + 1000),
                )

                When("Checking if max count for time window is reached") {
                    val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                    Then("Should be false") {
                        isMaxCountReached.shouldBeFalse()
                    }
                }

                And("Cached video count is equal to max units per time window") {
                    policy.setPrivateField("timeWindowCachedVideosCount", 1)

                    When("Checking if max count for time window is reached") {
                        val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                        Then("Should be false") {
                            isMaxCountReached.shouldBeFalse()
                        }
                    }
                }

                And("Cached video count is less than max units per time window") {
                    policy.setPrivateField("timeWindowCachedVideosCount", 0)

                    When("Checking if max count for time window is reached") {
                        val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                        Then("Should be false") {
                            isMaxCountReached.shouldBeFalse()
                        }
                    }
                }

                And("Cached video count is greater than max units per time window") {
                    policy.setPrivateField("timeWindowCachedVideosCount", 2)

                    When("Checking if max count for time window is reached") {
                        val isMaxCountReached = policy.isMaxCountForTimeWindowReached()

                        Then("Should be false") {
                            isMaxCountReached.shouldBeFalse()
                        }
                    }
                }
            }
        }
    }
})
