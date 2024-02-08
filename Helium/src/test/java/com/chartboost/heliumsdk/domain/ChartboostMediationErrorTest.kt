/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import org.junit.Test

class ChartboostMediationErrorTest {
    private val definedErrorCodes = ChartboostMediationError.values().map { it.code }
    private val definedChartboostMediationError = ChartboostMediationError.values()

    @Test
    fun `Chartboost Mediation error codes are unique`() {
        val uniqueErrorCodes = definedErrorCodes.distinct()

        if (definedErrorCodes.size != uniqueErrorCodes.size) {
            val duplicates = definedErrorCodes.groupingBy { it }.eachCount().filter { it.value > 1 }
            println("Duplicate error codes defined: $duplicates")
        }

        assert(definedErrorCodes.size == uniqueErrorCodes.size) { "Found duplicate Helium error codes." }
    }

    @Test
    fun `Chartboost Mediation error codes have the CM_ prefix and end with a three digit number`() {
        val invalidErrorCodes =
            definedErrorCodes.filter {
                !it.startsWith("CM_") || !it.matches(Regex("CM_\\d{3}"))
            }

        if (invalidErrorCodes.isNotEmpty()) {
            println("Error codes with incorrect syntax defined: $invalidErrorCodes")
        }

        assert(invalidErrorCodes.isEmpty()) { "Found Helium error codes with invalid syntax." }
    }

    @Test
    fun `Chartboost Mediation errors have non-empty code, message, cause, and resolution`() {
        val invalidErrorCodes =
            ChartboostMediationError.values()
                .filter { it.code.isBlank() || it.message.isBlank() || it.cause.isBlank() || it.resolution.isBlank() }

        if (invalidErrorCodes.isNotEmpty()) {
            println("Error codes with blank message/cause/resolution: $invalidErrorCodes")
        }

        assert(invalidErrorCodes.isEmpty()) { "Found Helium error codes with blank message/cause/resolution." }
    }

    @Test
    fun `There is at least 1 error code defined per range from 1XX to 6XX`() {
        val rangesWithoutErrorCodes =
            (100..600 step 100).filter { !definedErrorCodes.contains("CM_$it") }

        if (rangesWithoutErrorCodes.isNotEmpty()) {
            println("Missing error codes for range(s): $rangesWithoutErrorCodes")
        }

        assert(rangesWithoutErrorCodes.isEmpty()) { "Helium error codes are missing for 1 or more ranges." }
    }

    @Test
    fun `Chartboost Mediation error codes increment by 1 in each range`() {
        (100..600 step 100).forEach { rangeStart ->
            val errorCodesInRange =
                definedErrorCodes.filter {
                    it.substring("CM_".length).toInt() in rangeStart..rangeStart + 99
                }

            errorCodesInRange.forEachIndexed { index, errorCode ->
                val expectedErrorCode = "CM_${rangeStart + index}"
                if (errorCode != expectedErrorCode) {
                    println("Expected error code $expectedErrorCode next but found $errorCode")
                }
                assert(errorCode == expectedErrorCode) { "Helium error codes are not incrementing by 1 in range $rangeStart" }
            }
        }
    }

    @Test
    fun `Chartboost Mediation Error message strings should end with a period`() {
        definedChartboostMediationError.forEach {
            assert(it.message.contains(regex = Regex("[^.]\\.\\z"))) {
                "Missing period at end of message (${it.code}):\n" +
                    " \t\"${it.message}\"\n"
            }
        }
    }

    @Test
    fun `Chartboost Mediation Error cause strings should end with a period`() {
        definedChartboostMediationError.forEach {
            assert(it.cause.contains(regex = Regex("[^.]\\.\\z"))) {
                "Missing period at end of cause (${it.code}):\n" +
                    " \t\"${it.cause}\"\n"
            }
        }
    }

    @Test
    fun `Chartboost Mediation Error resolution message strings should end with a period`() {
        definedChartboostMediationError.forEach {
            assert(it.resolution.contains(regex = Regex("[^.]\\.\\z"))) {
                "Missing period at end of resolution (${it.code}):\n" +
                    "\"${it.resolution}\"\n"
            }
        }
    }

    @Test
    fun `Chartboost Mediation Error toString() should be in the correct format`() {
        definedChartboostMediationError.forEach {
            val expectedFormat = "${it.name} (${it.code}). Cause: ${it.cause} Resolution: ${it.resolution}"
            assert(it.toString() == String.format(expectedFormat))
        }
    }
}
