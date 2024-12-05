/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import com.chartboost.heliumsdk.domain.serializers.KeywordsSerializer
import com.chartboost.heliumsdk.utils.LogController
import kotlinx.serialization.Serializable
import java.util.*

/**
 * The Keywords class.
 */
@Serializable(with = KeywordsSerializer::class)
class Keywords {
    private val KEYWORD_CHAR_LIMIT = 64
    private val VALUE_CHAR_LIMIT = 256
    private val keywords: MutableMap<String, String> = mutableMapOf()

    /**
     * Sets the specified value for the keyword.
     * In the event that the keyword already exists,
     * it will overwrite the value if valid.
     * The value is limited to 256 characters.
     * @param keyword The keyword entry to use.
     * @param value The value associated with the keyword.
     * @return true if the keyword was set successfully. Otherwise, false if the keyword or
     * value exceed the maximum allowable characters.
     */
    operator fun set(
        keyword: String,
        value: String,
    ): Boolean {
        // If the keyword or value has more characters than their maximum allowed, return false.
        if (exceedsCharacterLimit(keyword, KEYWORD_CHAR_LIMIT) ||
            exceedsCharacterLimit(value, VALUE_CHAR_LIMIT)
        ) {
            LogController.w(
                "The keyword or value exceeded the maximum allowable" +
                    " characters. Maximum allowable characters for " +
                    "Keyword is: $KEYWORD_CHAR_LIMIT & for Value is: $VALUE_CHAR_LIMIT",
            )

            return false
        }

        keywords[keyword] = value
        return true
    }

    /**
     * Removes the specified keyword if it exists.
     * @param key: The keyword entry to remove
     * @return The value of the keyword that was removed if it exits, otherwise null.
     */
    fun remove(key: String): String? {
        return keywords.remove(key)
    }

    /**
     * Return an immutable copy of the keywords data.
     * @return an immutable copy of the keywords data.
     */
    fun get(): Map<String, String> {
        return Collections.unmodifiableMap(keywords)
    }

    /**
     * Check the character limit.
     * Returns whether or not the character exceeds the limit
     */
    private fun exceedsCharacterLimit(
        string: String,
        limit: Int,
    ): Boolean {
        return string.length > limit
    }
}
