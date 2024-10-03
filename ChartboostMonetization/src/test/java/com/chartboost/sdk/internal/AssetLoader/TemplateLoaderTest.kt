package com.chartboost.sdk.internal.AssetLoader

import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEvent
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class TemplateLoaderTest {
    private lateinit var templateLoader: TemplateLoader
    private val eventTrackerMock =
        relaxedMockk<EventTrackerExtensions>().apply {
            every { any<TrackingEvent>().track() } answers { firstArg() }
        }
    private lateinit var tempDir: Path
    private lateinit var htmlFile: Path
    private val adTypeName = "testAdType"
    private val location = "testLocation"

    @BeforeEach
    fun setup() {
        templateLoader = TemplateLoader(eventTracker = eventTrackerMock)
    }

    @AfterEach
    fun cleanup() {
        Files.deleteIfExists(htmlFile)
        Files.deleteIfExists(tempDir)
    }

    @Test
    fun `formatTemplateHtml with valid input should return formatted HTML`() {
        createHtmlFile("<html>{{param1}}{%param2%}</html>")
        val allParams = mapOf("{{param1}}" to "value1", "{%param2%}" to "value2")
        val result =
            templateLoader.formatTemplateHtml(
                htmlFile.toFile(),
                allParams,
                adTypeName,
                location,
            )
        assert(result!!.contains("value1"))
        assert(result.contains("value2"))
        // No error tracking for valid input
        verify(exactly = 0) {
            with(eventTrackerMock) {
                any<TrackingEvent>().track()
            }
        }
    }

    @Test
    fun `formatTemplateHtml with valid input should return formatted HTML different param`() {
        createHtmlFile("<html>{{ param1 }}{% param2 %}</html>")
        val allParams = mapOf("{{ param1 }}" to "value1", "{% param2 %}" to "value2")
        val result =
            templateLoader.formatTemplateHtml(
                htmlFile.toFile(),
                allParams,
                adTypeName,
                location,
            )
        assert(result!!.contains("value1"))
        assert(result.contains("value2"))
        verify(exactly = 0) {
            with(eventTrackerMock) {
                any<TrackingEvent>().track()
            }
        }
    }

    @Test
    fun `formatTemplateHtml with missing parameter should throw exception and track error`() {
        createHtmlFile("<html>{{param1}}{%param2%}</html>")
        val allParams = mapOf("{{param1}}" to "value1", "{%param2%}" to "value2")
        val result =
            templateLoader.formatTemplateHtml(
                htmlFile.toFile(),
                allParams - "{{param1}}",
                adTypeName,
                location,
            )
        assertNull(result)
        verify(exactly = 1) {
            with(eventTrackerMock) {
                any<TrackingEvent>().track()
            }
        }
    }

    @Test
    fun `multiple replacements`() {
        createHtmlFile("This template\nhas {{ multiple }} replacements {% line %} for {{ real }}.\n")
        val allParams = mapOf("{{ multiple }}" to "three", "{% line %}" to "on one line", "{{ real }}" to "testing")
        val result =
            templateLoader.formatTemplateHtml(
                htmlFile.toFile(),
                allParams,
                adTypeName,
                location,
            )
        assert(result!!.contains("three"))
        assert(result.contains("on one line"))
        assert(result.contains("testing"))
        verify(exactly = 0) {
            with(eventTrackerMock) {
                any<TrackingEvent>().track()
            }
        }
    }

    @Test
    fun `multiple replacements brace brackets first`() {
        createHtmlFile("This template\n has {% multiple %} replacements {{ line }} for {% real %}.\n")
        val allParams = mapOf("{% multiple %}" to "three", "{{ line }}" to "on one line", "{% real %}" to "testing")
        val result =
            templateLoader.formatTemplateHtml(
                htmlFile.toFile(),
                allParams,
                adTypeName,
                location,
            )
        assert(result!!.contains("three"))
        assert(result.contains("on one line"))
        assert(result.contains("testing"))
        verify(exactly = 0) {
            with(eventTrackerMock) {
                any<TrackingEvent>().track()
            }
        }
    }

    @Test
    fun `replace only wrapped params`() {
        createHtmlFile("This word should not be replaced but {{ this one }} and {% that one %} should.\n")
        val allParams =
            mapOf(
                "word" to "don't replace this",
                "{{ this one }}" to "This One",
                "{% that one %}" to "That One",
            )
        val result =
            templateLoader.formatTemplateHtml(
                htmlFile.toFile(),
                allParams,
                adTypeName,
                location,
            )
        assert(!result!!.contains("don't replace this"))
        assert(result.contains("word"))
        assert(result.contains("This One"))
        assert(result.contains("That One"))
        verify(exactly = 0) {
            with(eventTrackerMock) {
                any<TrackingEvent>().track()
            }
        }
    }

    @Test
    fun `long html`() {
        val longText = "random text {{ word }}".repeat(10000)
        createHtmlFile(longText)

        val allParams =
            mapOf(
                "{{ word }}" to "replace word",
            )

        val result =
            templateLoader.formatTemplateHtml(
                htmlFile.toFile(),
                allParams,
                adTypeName,
                location,
            )

        assertTrue(result == "random text replace word".repeat(10000))
    }

    private fun createHtmlFile(htmlContent: String) {
        tempDir = Files.createTempDirectory("test")
        htmlFile = tempDir.resolve("temp_file.html")
        Files.write(
            htmlFile,
            htmlContent.toByteArray(),
            StandardOpenOption.CREATE,
        )
    }
}
