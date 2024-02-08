/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import com.chartboost.heliumsdk.utils.LogController
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.slot
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KeywordsTest {
    private lateinit var keywords: Keywords

    @Before
    fun setUp() {
        keywords = Keywords()
    }

    @Test
    fun `add keywords with valid strings should be added`() {
        // Adding some keywords
        keywords.let {
            assertTrue(it.set("hi", "hello"))
            assertTrue(it.set("yo", "ok"))
            assertTrue(it.set("yo", "sweet"))
        }

        // Creating a similar expected map for comparison.
        val expectedMap = mutableMapOf<String, String>()
        expectedMap["hi"] = "hello"
        expectedMap["yo"] = "ok"
        expectedMap["yo"] = "sweet"

        keywords.get().let {
            assertEquals(expectedMap, it)
            assertEquals(expectedMap.size, it.size)
            assertEquals("sweet", it["yo"])
            assertEquals(expectedMap["yo"], it["yo"])
        }
    }

    @Test
    fun `set keywords with symbolic characters should be added`() {
        keywords.let {
            // Adding some non-alphanumeric characters.
            assertTrue(it.set("emojiKey", "`~😅!@#$%^&*()_+-={}|[]\\\\\\\""))
            assertTrue(it.set("`~😅!@#$%^&*()_+-={}|[]\\\\\\\"", "emojiValue"))
            assertTrue(it.set("🐰", "🥕"))
        }

        keywords.get().let { keywordsData ->
            // Check that they have been successfully added.
            assertEquals(3, keywordsData.size)
            assertEquals("emojiValue", keywordsData["`~😅!@#$%^&*()_+-={}|[]\\\\\\\""])
            assertEquals("`~😅!@#$%^&*()_+-={}|[]\\\\\\\"", keywordsData["emojiKey"])
            assertEquals("🥕", keywordsData["🐰"])
        }
    }

    @Test
    fun `set keywords with maximum characters limit should not add keyword`() {
        val captureMessage = slot<String>()
        val expectedLog =
            "The keyword or value exceeded the maximum allowable" +
                " characters. Maximum allowable characters for " +
                "Keyword is: 64 & for Value is: 256"

        mockkObject(LogController)
        every { LogController.w(capture(captureMessage)) } just Runs

        keywords.let {
            // Add a single item
            assertTrue(it.set("I", "1"))
            // Add a keyword that still is within the allowed limit.
            assertTrue(
                it.set(
                    ";SVwCc_BXC?R-3A%+/YFSK=Xj8b+v@jU4WSkA7bSjV@-&ckgQyN5=V8Z,P.+vhzj",
                    "yess 64 Characters",
                ),
            )
            // Oops, this one exceeds, it should not be added.
            assertFalse(
                it.set(
                    ";SVwCc_BXC?R-3A%+/YFSK=Xj8b+v@jU4WSkA7bSjV@-&ckgQyN5=V8Z,P.+vhzji",
                    "ohNo 65 Characters",
                ),
            )
            // Now, let's check that a value can be added within allowed limits.
            assertTrue(
                it.set(
                    "yes 256 Chars",
                    "vC8ddiNuCFwDUriZAHnGTPaCJE4ctyHjEVkNvRCfV8Y8i4eXS7GvVZxcAEn38SfqnbV3ei4n6xwt" +
                        "TRy6jwPGTEQc5HBKBY4aRLgLXrPDu56Ydf99YVf5faKbmHF9t6xZ2QaCQQKxxnEbfxBe" +
                        "6GCybJr98tuuuEYzCALaZYe4ymgmfSwSm7xtSuNag48Gvrz6P9DbyuHG6zqXj65bNKiz" +
                        "XLquDaEWXY5BAWJufgPETfHM6j6N67fqLen5kAXvxtXm",
                ),
            )
            // Whelp, if the value is long than what we allow, then we don't add.
            assertFalse(
                it.set(
                    "ohNo 257 Chars",
                    "v8C8ddiNuCFwDUriZAHnGTPaCJE4ctyHjEVkNvRCfV8Y8i4eXS7GvVZxcAEn38SfqnbV3ei4n6xw" +
                        "tTRy6jwPGTEQc5HBKBY4aRLgLXrPDu56Ydf99YVf5faKbmHF9t6xZ2QaCQQKxxnEbfxB" +
                        "e6GCybJr98tuuuEYzCALaZYe4ymgmfSwSm7xtSuNag48Gvrz6P9DbyuHG6zqXj65bNKi" +
                        "zXLquDaEWXY5BAWJufgPETfHM6j6N67fqLen5kAXvxtXm",
                ),
            )
        }

        // Only the allowed keywords should be added.
        assertEquals(3, keywords.get().size)

        // Assert that our captured log message is that of our expected.
        assertEquals(expectedLog, captureMessage.captured)
    }

    @Test
    fun `remove with existing key should return key's value`() {
        // Expected value.
        val chosenOne = "YES, I'm the chosen One"

        keywords.let {
            // Adding a small set of keys and values.
            assertTrue(it.set("You shall pass", "Yay"))
            assertTrue(it.set("You shall not", "Awe"))
            assertTrue(it.set("You are a wizard", chosenOne))
            assertEquals(3, it.get().size)

            // A remove should return the value of the given key.
            assertEquals(chosenOne, it.remove("You are a wizard"))
            assertEquals(2, it.get().size)
        }
    }
}
