package com.chartboost.sdk.internal.AdUnitManager.parsers

import android.os.Build
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class AdUnitParserCommonTest {
    @Test
    fun `retrieve video filename from the valid url`() {
        val filename = retrieveFilenameFromVideoUrl("https://test.com/video_1.mp4")
        assertEquals("video_1.mp4", filename)
    }

    @Test
    fun `retrieve video filename from the valid url no extension`() {
        val filename = retrieveFilenameFromVideoUrl("https://test.com/video_1")
        assertEquals("video_1", filename)
    }

    @Test
    fun `retrieve video filename from the invalid name missing name`() {
        val filename = retrieveFilenameFromVideoUrl("https://test.com/")
        assertEquals("", filename)
    }

    @Test
    fun `retrieve video filename from the invalid url missing domain still parsers valid name`() {
        val filename = retrieveFilenameFromVideoUrl("https://test/video_1.mp4")
        assertEquals("video_1.mp4", filename)
    }

    @Test
    fun `retrieve video filename from the invalid url missing full domain`() {
        val filename = retrieveFilenameFromVideoUrl("https://video_1.mp4")
        assertEquals("", filename)
    }

    @Test
    fun `retrieve video filename from the invalid url missing schema recreates valid url and name`() {
        val filename = retrieveFilenameFromVideoUrl("test.com/video_1.mp4")
        assertEquals("video_1.mp4", filename)
    }

    @Test
    fun `retrieve video filename from the invalid url with http schema returns valid url name`() {
        val filename = retrieveFilenameFromVideoUrl("http://test.com/video_1.mp4")
        assertEquals("video_1.mp4", filename)
    }
}
