package com.chartboost.sdk.internal.video.repository.exoplayer

import android.content.Context
import com.chartboost.sdk.internal.video.VideoAsset
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.test.getPrivateField
import com.chartboost.sdk.test.justRunMockk
import com.chartboost.sdk.test.mockAndroidLog
import com.chartboost.sdk.test.mockAndroidUri
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify

class ExoPlayerDownloadManagerImplTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    mockAndroidLog()
    mockAndroidUri()

    mockkStatic("com.chartboost.sdk.internal.video.repository.exoplayer.UtilsKt")

    mockkStatic(DownloadService::class)
    justRun { DownloadService.sendRemoveDownload(any(), any(), any(), any()) }
    justRun { DownloadService.sendAddDownload(any(), any(), any(), any(), any()) }
    justRun { DownloadService.sendSetStopReason(any(), any(), any(), any(), any()) }

    val contextMock: Context = mockk()
    every { contextMock.applicationContext } returns contextMock

    val videoCachePolicyMock: VideoCachePolicy = mockk()
    val exoPlayerFileCachingMock: ExoPlayerFileCaching = mockk()
    val cacheMock: Cache = mockk()
    val cacheDataSourceFactoryMock: CacheDataSource.Factory = mockk()
    val httpDataSourceFactoryMock: DefaultHttpDataSource.Factory = mockk()
    val downloadManagerMock: DownloadManager = mockk()
    val databaseProviderMock: DatabaseProvider = mockk()
    val fakePrecacheFilesManagerMock: FakePrecacheFilesManager = justRunMockk()
    val cookieHandlerMock: () -> Unit =
        mockk<() -> Unit>().apply {
            justRun { this@apply() }
        }

    fun ExoPlayerDownloadManagerImpl.downloadManager(): DownloadManager = getPrivateField("downloadManager")!!

    fun ExoPlayerDownloadManagerImpl.fileCaching(): ExoPlayerFileCaching = getPrivateField("fileCaching")!!

    fun ExoPlayerDownloadManagerImpl.cacheDataSourceFactory(): CacheDataSource.Factory = getPrivateField("cacheDataSourceFactory")!!

    fun ExoPlayerDownloadManagerImpl.fakePrecacheFilesManager(): FakePrecacheFilesManager = getPrivateField("fakePrecacheFilesManager")!!

    Given("A ExoPlayerDownloadManagerImpl") {
        val exoPlayerDownloadManager =
            spyk(
                ExoPlayerDownloadManagerImpl(
                    ExoPlayerDownloadManagerDependencies(
                        context = contextMock,
                        videoCachePolicy = videoCachePolicyMock,
                        fileCachingFactory = { exoPlayerFileCachingMock },
                        cacheFactory = { _, _, _, _ -> cacheMock },
                        cacheDataSourceFactoryFactory = { _, _ -> cacheDataSourceFactoryMock },
                        httpDataSourceFactory = httpDataSourceFactoryMock,
                        downloadManagerFactory = { _, _, _, _, _ -> downloadManagerMock },
                        databaseProviderFactory = { databaseProviderMock },
                        setCookieHandler = cookieHandlerMock,
                        fakePrecacheFilesManagerFactory = { fakePrecacheFilesManagerMock },
                    ),
                ),
            )

        When("initialize() is called") {
            exoPlayerDownloadManager.initialize()

            Then("it should initialize the cookie handler") {
                verify(exactly = 1) { cookieHandlerMock() }
            }

            Then("it should initialize the file caching") {
                shouldNotThrow<UninitializedPropertyAccessException> {
                    exoPlayerDownloadManager.fileCaching() shouldBe exoPlayerFileCachingMock
                }
            }

            Then("it should initialize the cache data source factory") {
                shouldNotThrow<UninitializedPropertyAccessException> {
                    exoPlayerDownloadManager.cacheDataSourceFactory() shouldBe cacheDataSourceFactoryMock
                }
            }

            Then("it should initialize the fake precache files manager") {
                shouldNotThrow<UninitializedPropertyAccessException> {
                    exoPlayerDownloadManager.fakePrecacheFilesManager() shouldBe fakePrecacheFilesManagerMock
                }
            }

            Then("it should initialize the download manager") {
                shouldNotThrow<UninitializedPropertyAccessException> {
                    exoPlayerDownloadManager.downloadManager() shouldBe downloadManagerMock
                }
            }
        }

        And("is initialized") {
            exoPlayerDownloadManager.initialize()

            val downloadMockId = "bar"
            val downloadMock: DownloadWrapper =
                mockk<DownloadWrapper>().apply {
                    every { id } returns downloadMockId
                }

            And("download by this key exists") {
                every { any<DownloadManager>().download(downloadMockId) } returns downloadMock

                When("removeDownload() is called") {
                    exoPlayerDownloadManager.removeDownload(downloadMockId)

                    Then("it should remove the download") {
                        verify(exactly = 1) {
                            DownloadService.sendRemoveDownload(
                                contextMock,
                                VideoRepositoryDownloadService::class.java,
                                downloadMockId,
                                false,
                            )
                            fakePrecacheFilesManagerMock.downloadRemoved(downloadMock)
                            downloadManagerMock.download(downloadMockId)
                        }
                    }
                }

                And("state is downloading") {
                    every { downloadMock.state } returns Download.STATE_DOWNLOADING

                    When("isDownloadingOrDownloaded() is called") {
                        Then("it should return true") {
                            exoPlayerDownloadManager.isDownloadingOrDownloaded(downloadMockId) shouldBe true
                        }
                    }
                }

                And("state is completed") {
                    every { downloadMock.state } returns Download.STATE_COMPLETED

                    When("isDownloadingOrDownloaded() is called") {
                        Then("it should return true") {
                            exoPlayerDownloadManager.isDownloadingOrDownloaded(downloadMockId) shouldBe true
                        }
                    }
                }

                And("state is not downloading or completed") {
                    every { downloadMock.state } returns Download.STATE_QUEUED

                    When("isDownloadingOrDownloaded() is called") {
                        Then("it should return false") {
                            exoPlayerDownloadManager.isDownloadingOrDownloaded(downloadMockId) shouldBe false
                        }
                    }
                }

                When("downloadPercentage() is called") {
                    every { downloadMock.percentDownloaded } returns 50.0f

                    Then("it should return the download percentage") {
                        exoPlayerDownloadManager.downloadPercentage(downloadMockId) shouldBe 0.5f
                    }
                }

                When("download() is called") {
                    Then("it should return the download") {
                        exoPlayerDownloadManager.download(downloadMockId) shouldBe downloadMock
                    }
                }
            }

            And("download by this key does not exist") {
                every { any<DownloadManager>().download(any()) } returns null

                When("removeDownload() is called") {
                    exoPlayerDownloadManager.removeDownload("foo")

                    Then("it should not remove the download or notify the listener") {
                        verify(exactly = 0) {
                            DownloadService.sendRemoveDownload(any(), any(), any(), any())
                            fakePrecacheFilesManagerMock.downloadRemoved(downloadMock)
                        }
                    }
                }

                When("isDownloadingOrDownloaded() is called") {
                    Then("it should return false") {
                        exoPlayerDownloadManager.isDownloadingOrDownloaded("foo") shouldBe false
                    }
                }

                When("downloadPercentage() is called") {
                    Then("it should return the download percentage") {
                        exoPlayerDownloadManager.downloadPercentage(downloadMockId) shouldBe 0f
                    }
                }

                When("download() is called") {
                    Then("it should return null") {
                        exoPlayerDownloadManager.download(downloadMockId) shouldBe null
                    }
                }
            }

            And("video asset url is blank") {
                val videoAssetMock: VideoAsset =
                    mockk<VideoAsset>().apply {
                        every { url } returns ""
                    }

                When("addDownload() is called") {
                    exoPlayerDownloadManager.addDownload(videoAssetMock)

                    Then("it should not start downloading") {
                        verify(exactly = 0) {
                            DownloadService.sendAddDownload(any(), any(), any(), any(), any())
                        }
                    }
                }

                When("addDownload() is called with a stopReason") {
                    exoPlayerDownloadManager.addDownload(videoAssetMock, DownloadStopReason.FORCED_OUT)

                    Then("it should not start downloading") {
                        verify(exactly = 0) {
                            DownloadService.sendAddDownload(any(), any(), any(), any(), any())
                        }
                    }
                }
            }

            And("video asset url is not blank") {
                val videoAssetMock: VideoAsset =
                    mockk<VideoAsset>().apply {
                        every { url } returns "foo"
                        every { filename } returns "bar"
                    }

                When("addDownload() is called") {
                    exoPlayerDownloadManager.addDownload(videoAssetMock)

                    Then("it should start downloading") {
                        verify(exactly = 1) {
                            DownloadService.sendAddDownload(
                                contextMock,
                                VideoRepositoryDownloadService::class.java,
                                any(),
                                DownloadStopReason.NONE.value,
                                false,
                            )
                        }
                    }
                }

                When("addDownload() is called with a stopReason") {
                    exoPlayerDownloadManager.addDownload(videoAssetMock, DownloadStopReason.FORCED_OUT)

                    Then("it should start downloading") {
                        verify(exactly = 1) {
                            DownloadService.sendAddDownload(
                                contextMock,
                                VideoRepositoryDownloadService::class.java,
                                any(),
                                DownloadStopReason.FORCED_OUT.value,
                                false,
                            )
                        }
                    }
                }
            }

            And("there are no downloads") {
                mockkStatic("com.chartboost.sdk.internal.video.repository.exoplayer.UtilsKt")
                every { any<DownloadManager>().downloads() } returns emptyList()

                When("cleanDownloads() is called") {
                    exoPlayerDownloadManager.cleanDownloads()

                    Then("it should not remove any downloads") {
                        verify(exactly = 0) {
                            DownloadService.sendRemoveDownload(any(), any(), any(), any())
                            fakePrecacheFilesManagerMock.downloadRemoved(downloadMock)
                        }
                    }
                }

                When("startDownload() is called") {
                    val videoAssetMock: VideoAsset =
                        mockk<VideoAsset>().apply {
                            every { filename } returns "foo"
                            every { url } returns "bar"
                        }
                    exoPlayerDownloadManager.startDownload(videoAssetMock)

                    Then("it should start downloading") {
                        verify(exactly = 1) {
                            DownloadService.sendAddDownload(
                                contextMock,
                                VideoRepositoryDownloadService::class.java,
                                any(),
                                DownloadStopReason.NONE.value,
                                false,
                            )
                        }
                    }
                }

                When("onEvictingUrl is called") {
                    Then("it should not remove any downloads") {
                        exoPlayerDownloadManager.onEvictingUrl("foo")
                        verify(exactly = 0) {
                            DownloadService.sendRemoveDownload(any(), any(), any(), any())
                            fakePrecacheFilesManagerMock.downloadRemoved(downloadMock)
                        }
                    }
                }
            }

            And("there are downloads") {
                val download1: DownloadWrapper =
                    mockk<DownloadWrapper>().apply {
                        every { id } returns "foo"
                        every { updateTime } returns 100L
                    }
                val download2: DownloadWrapper =
                    mockk<DownloadWrapper>().apply {
                        every { id } returns "bar"
                        every { updateTime } returns 200L
                    }
                every { any<DownloadManager>().downloads() } returns listOf(download1, download2)

                And("TTL is expired for some of them") {
                    every { videoCachePolicyMock.isTimeToLiveExpired(100L) } returns true
                    every { videoCachePolicyMock.isTimeToLiveExpired(200L) } returns false

                    When("cleanDownloads() is called") {
                        exoPlayerDownloadManager.cleanDownloads()

                        Then("it should remove TTL expired downloads") {
                            verify {
                                DownloadService.sendRemoveDownload(
                                    contextMock,
                                    VideoRepositoryDownloadService::class.java,
                                    "foo",
                                    false,
                                )
                                fakePrecacheFilesManagerMock.downloadRemoved(download1)
                            }
                            verify(exactly = 0) {
                                DownloadService.sendRemoveDownload(
                                    contextMock,
                                    VideoRepositoryDownloadService::class.java,
                                    "bar",
                                    false,
                                )
                                fakePrecacheFilesManagerMock.downloadRemoved(download2)
                            }
                        }
                    }
                }

                When("startDownload() is called") {
                    val videoAssetMock: VideoAsset =
                        mockk<VideoAsset>().apply {
                            every { filename } returns "foobar"
                            every { url } returns "barfoo"
                        }
                    exoPlayerDownloadManager.startDownload(videoAssetMock)

                    Then("it should stop all downloads except this new one") {
                        verify(exactly = 2) {
                            DownloadService.sendSetStopReason(
                                contextMock,
                                VideoRepositoryDownloadService::class.java,
                                any(),
                                DownloadStopReason.FORCED_OUT.value,
                                false,
                            )
                        }
                    }

                    Then("it should add new download") {
                        verify(exactly = 1) {
                            DownloadService.sendAddDownload(
                                contextMock,
                                VideoRepositoryDownloadService::class.java,
                                any(),
                                DownloadStopReason.NONE.value,
                                false,
                            )
                        }
                    }

                    Then("it should start downloading") {
                        verify(exactly = 1) {
                            DownloadService.sendAddDownload(
                                contextMock,
                                VideoRepositoryDownloadService::class.java,
                                any(),
                                DownloadStopReason.NONE.value,
                                false,
                            )
                        }
                    }
                }

                When("onEvictingUrl is called") {
                    every { download1.uri } returns "foobar"
                    every { download2.uri } returns "barfoo"
                    exoPlayerDownloadManager.onEvictingUrl("barfoo")

                    Then("it should remove all downloads with this url") {
                        verify(exactly = 1) {
                            DownloadService.sendRemoveDownload(
                                contextMock,
                                VideoRepositoryDownloadService::class.java,
                                "bar",
                                false,
                            )
                            fakePrecacheFilesManagerMock.downloadRemoved(download2)
                        }
                    }
                }
            }
        }
    }
})
