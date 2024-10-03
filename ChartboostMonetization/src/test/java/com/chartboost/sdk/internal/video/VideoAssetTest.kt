package com.chartboost.sdk.internal.video

import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class VideoAssetTest {
    private val url = "test.com"
    private val filename = "test.mp4"
    private val localFileMock = mockk<File>()
    private val directoryFileMock = mockk<File>()
    private var asset: VideoAsset? = null

    @Test
    fun assetCreatedTest() {
        asset = VideoAsset(url, filename, localFileMock, directoryFileMock)
        Assert.assertNotNull(asset!!.filename)
        Assert.assertNotNull(asset!!.url)
        Assert.assertNotNull(asset!!.localFile)
        Assert.assertNotNull(asset!!.directory)
        Assert.assertNotNull(asset!!.creationDate)
    }
}
