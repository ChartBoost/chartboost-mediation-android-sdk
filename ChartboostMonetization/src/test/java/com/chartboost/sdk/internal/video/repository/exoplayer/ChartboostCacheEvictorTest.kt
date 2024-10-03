package com.chartboost.sdk.internal.video.repository.exoplayer

import com.chartboost.sdk.test.getPrivateField
import com.chartboost.sdk.test.justRunMockk
import com.chartboost.sdk.test.mockAndroidLog
import com.chartboost.sdk.test.setPrivateField
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.util.TreeSet

class ChartboostCacheEvictorTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    mockAndroidLog()

    val evictUrlCallbackMock: ChartboostCacheEvictor.EvictUrlCallback = justRunMockk()
    val treeSetMock: TreeSet<CacheSpan> =
        justRunMockk<TreeSet<CacheSpan>>().apply {
            every { add(any()) } returns true
            every { remove(any()) } returns true
        }

    fun ChartboostCacheEvictor.currentSize(): Long = getPrivateField("currentSize")!!

    Given("A ChartboostCacheEvictor with 100 bytes max size") {
        val maxBytes = 100L
        val chartboostCacheEvictor =
            spyk(
                ChartboostCacheEvictor(
                    maxBytes = maxBytes,
                    evictUrlCallback = evictUrlCallbackMock,
                    treeSetFactory = { treeSetMock },
                ),
                recordPrivateCalls = true,
            )

        And("mocking private function evictCache()") {
            justRun { chartboostCacheEvictor["evictCache"](any<Cache>(), any<Long>()) }

            When("onStartFile() is called") {
                val cache: Cache = mockk()
                chartboostCacheEvictor.onStartFile(cache, "", 0, 100)

                Then("should call evictCache()") {
                    verify { chartboostCacheEvictor["evictCache"](cache, 100L) }
                }
            }

            When("onSpanAdded() is called") {
                val cache: Cache = mockk()
                chartboostCacheEvictor.onSpanAdded(cache, mockk())

                Then("should call evictCache()") {
                    verify { chartboostCacheEvictor["evictCache"](cache, 0L) }
                }
            }

            When("onSpanTouched() is called") {
                val cache: Cache = mockk()
                chartboostCacheEvictor.onSpanTouched(cache, mockk(), mockk())

                Then("should call evictCache()") {
                    verify { chartboostCacheEvictor["evictCache"](cache, 0L) }
                }
            }
        }

        And("cache span tree is empty") {
            every { treeSetMock.isEmpty() } returns true

            And("file length that is less than maximum bytes") {
                val fileLength = maxBytes - 10

                When("onStartFile is called") {
                    val cache: Cache = mockk()
                    chartboostCacheEvictor.onStartFile(cache, "", 0, fileLength)

                    Then("it should not evict any cache span") {
                        verify(exactly = 0) {
                            cache.removeSpan(any())
                            evictUrlCallbackMock.onEvictingUrl(any())
                        }
                    }
                }
            }

            And("file length is equal to the maximum bytes") {
                When("onStartFile is called") {
                    val cache: Cache = justRunMockk()
                    chartboostCacheEvictor.onStartFile(cache, "", 0, maxBytes)

                    Then("it should not evict any cache span") {
                        verify(exactly = 0) {
                            cache.removeSpan(any())
                            evictUrlCallbackMock.onEvictingUrl(any())
                        }
                    }
                }
            }

            And("file length is greater than maximum bytes") {
                val fileLength = maxBytes + 10

                When("onStartFile is called") {
                    val cache: Cache = justRunMockk()
                    chartboostCacheEvictor.onStartFile(cache, "", 0, fileLength)

                    Then("it should not evict any cache span") {
                        verify(exactly = 0) {
                            cache.removeSpan(any())
                            evictUrlCallbackMock.onEvictingUrl(any())
                        }
                    }
                }
            }

            When("onSpanRemoved is called") {
                val currentSize = chartboostCacheEvictor.currentSize()
                val cacheSpan: CacheSpan = CacheSpan("", 0, 20)
                chartboostCacheEvictor.onSpanRemoved(mockk(), cacheSpan)

                Then("it should update size accordingly") {
                    val newSize = chartboostCacheEvictor.currentSize()
                    newSize shouldBe currentSize - 20
                }
            }
        }

        And("cache span tree is not empty") {
            every { treeSetMock.isEmpty() } returns false

            And("file length is less than maximum bytes") {
                val fileLength = maxBytes - 10

                When("onStartFile is called") {
                    val cache: Cache = mockk()
                    chartboostCacheEvictor.onStartFile(cache, "", 0, fileLength)

                    Then("it should not evict any cache span") {
                        verify(exactly = 0) {
                            cache.removeSpan(any())
                            evictUrlCallbackMock.onEvictingUrl(any())
                        }
                    }
                }
            }

            And("file length is equal to the maximum bytes") {
                When("onStartFile is called") {
                    val cache: Cache = mockk()
                    chartboostCacheEvictor.onStartFile(cache, "", 0, maxBytes)

                    Then("it should not evict any cache span") {
                        verify(exactly = 0) {
                            cache.removeSpan(any())
                            evictUrlCallbackMock.onEvictingUrl(any())
                        }
                    }
                }
            }

            And("file length is greater than maximum bytes") {
                val fileLength = maxBytes + 10

                val cache: Cache =
                    mockk<Cache>().apply {
                        justRun { removeSpan(any()) }
                    }
                val cacheSpan = CacheSpan("key", 0, 20)
                every { treeSetMock.first() } returns cacheSpan

                // Make sure the callback is being called, otherwise we get stuck in an infinite loop
                every { cache.removeSpan(any()) } answers {
                    chartboostCacheEvictor.onSpanRemoved(cache, cacheSpan)
                }

                When("onStartFile is called") {
                    chartboostCacheEvictor.onStartFile(cache, "", 0, fileLength)

                    Then("it should evict cache spans") {
                        verify(exactly = 1) {
                            cache.removeSpan(cacheSpan)
                            treeSetMock.remove(cacheSpan)
                            evictUrlCallbackMock.onEvictingUrl(cacheSpan.key)
                        }
                    }
                }
            }

            And("current size is 0") {
                And("Cache span length does not exceed max bytes") {
                    val cacheSpanLength = maxBytes - 10
                    val cache: Cache = mockk()
                    val cacheSpan = CacheSpan("key", 0, cacheSpanLength)

                    When("onSpanAdded is called") {
                        chartboostCacheEvictor.onSpanAdded(cache, cacheSpan)

                        Then("it should add the cache span to the tree set") {
                            verify(exactly = 1) {
                                treeSetMock.add(cacheSpan)
                            }
                        }

                        Then("it should not evict any cache span") {
                            verify(exactly = 0) {
                                cache.removeSpan(any())
                                evictUrlCallbackMock.onEvictingUrl(any())
                            }
                        }
                    }
                }

                And("Cache span length exceeds max bytes") {
                    val cacheSpanLength = maxBytes + 10
                    val cache: Cache = mockk()
                    val cacheSpan = CacheSpan("key", 0, cacheSpanLength)
                    every { treeSetMock.first() } returns cacheSpan

                    // Make sure the callback is being called, otherwise we get stuck in an infinite loop
                    every { cache.removeSpan(any()) } answers {
                        chartboostCacheEvictor.onSpanRemoved(cache, cacheSpan)
                    }

                    When("onSpanAdded is called") {
                        chartboostCacheEvictor.onSpanAdded(cache, cacheSpan)

                        Then("it should add the cache span to the tree set") {
                            verify(exactly = 1) {
                                treeSetMock.add(cacheSpan)
                            }
                        }

                        Then("it should evict cache spans") {
                            verify(exactly = 1) {
                                cache.removeSpan(any())
                                evictUrlCallbackMock.onEvictingUrl(any())
                            }
                        }
                    }
                }
            }

            And("current size is close to max bytes") {
                chartboostCacheEvictor.setPrivateField("currentSize", maxBytes - 10)

                And("Cache span length does not exceed max bytes") {
                    val cacheSpanLength = 5L
                    val cache: Cache = mockk()
                    val cacheSpan = CacheSpan("key", 0, cacheSpanLength)

                    When("onSpanAdded is called") {
                        chartboostCacheEvictor.onSpanAdded(cache, cacheSpan)

                        Then("it should add the cache span to the tree set") {
                            verify(exactly = 1) {
                                treeSetMock.add(cacheSpan)
                            }
                        }

                        Then("it should not evict any cache span") {
                            verify(exactly = 0) {
                                cache.removeSpan(any())
                                evictUrlCallbackMock.onEvictingUrl(any())
                            }
                        }
                    }
                }

                And("Cache span length exceeds max bytes") {
                    val cacheSpanLength = 20L
                    val cache: Cache = mockk()
                    val cacheSpan = CacheSpan("key", 0, cacheSpanLength)
                    every { treeSetMock.first() } returns cacheSpan

                    // Make sure the callback is being called, otherwise we get stuck in an infinite loop
                    every { cache.removeSpan(any()) } answers {
                        chartboostCacheEvictor.onSpanRemoved(cache, cacheSpan)
                    }

                    When("onSpanAdded is called") {
                        chartboostCacheEvictor.onSpanAdded(cache, cacheSpan)

                        Then("it should add the cache span to the tree set") {
                            verify(exactly = 1) {
                                treeSetMock.add(cacheSpan)
                            }
                        }

                        Then("it should evict cache spans") {
                            verify(exactly = 1) {
                                cache.removeSpan(any())
                                evictUrlCallbackMock.onEvictingUrl(any())
                            }
                        }
                    }
                }
            }
        }

        When("onSpanTouched is called") {
            val oldSpan: CacheSpan = mockk()
            val newSpan: CacheSpan = mockk()
            chartboostCacheEvictor.onSpanTouched(mockk(), oldSpan, newSpan)

            Then("it should call removed and added") {
                verify(exactly = 1) {
                    chartboostCacheEvictor.onSpanRemoved(any(), oldSpan)
                    chartboostCacheEvictor.onSpanAdded(any(), newSpan)
                }
            }
        }
    }
})
