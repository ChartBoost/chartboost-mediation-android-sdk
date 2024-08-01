/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.reflect.full.createInstance

class ChartboostMediationErrorTest {
    @Test
    fun `all Chartboost Mediation error codes are unique`() {
        val errorCodes = mutableSetOf<String>()
        ChartboostMediationError::class.sealedSubclasses.forEach { sealedClass ->
            sealedClass.sealedSubclasses.forEach { errorClass ->
                val error = errorClass.objectInstance ?: errorClass.createInstance()
                assertTrue("Duplicate error code found: ${error.code}", errorCodes.add(error.code))
            }
        }
    }

    @Test
    fun `all Chartboost Mediation errors have non-empty code, message, cause, and resolution`() {
        ChartboostMediationError::class.sealedSubclasses.forEach { sealedClass ->
            sealedClass.sealedSubclasses.forEach { errorClass ->
                val error = errorClass.objectInstance ?: errorClass.createInstance()
                assertTrue(
                    "Error code is empty for: ${error::class.simpleName}",
                    error.code.isNotEmpty(),
                )
                assertTrue(
                    "Error message is empty for: ${error::class.simpleName}",
                    error.message.isNotEmpty(),
                )
                assertTrue(
                    "Error cause is empty for: ${error::class.simpleName}",
                    error.cause.isNotEmpty(),
                )
                assertTrue(
                    "Error resolution is empty for: ${error::class.simpleName}",
                    error.resolution.isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun `there is at least 1 error code defined per range from 1XX to 6XX`() {
        val ranges =
            mapOf(
                "1XX" to 100..199,
                "2XX" to 200..299,
                "3XX" to 300..399,
                "4XX" to 400..499,
                "5XX" to 500..599,
                "6XX" to 600..699,
            )

        val errorCounts =
            mutableMapOf<String, Int>().apply {
                ranges.keys.forEach { put(it, 0) }
            }

        ChartboostMediationError::class.sealedSubclasses.forEach { sealedClass ->
            sealedClass.sealedSubclasses.forEach { errorClass ->
                val error = errorClass.objectInstance ?: errorClass.createInstance()
                ranges.forEach { (rangeName, range) ->
                    if (error.code.removePrefix("CM_").toInt() in range) {
                        errorCounts[rangeName] = errorCounts[rangeName]!! + 1
                    }
                }
            }
        }

        errorCounts.forEach { (range, count) ->
            assertTrue("No error codes found in range: $range", count > 0)
        }
    }

    @Test
    fun `Chartboost Mediation error codes increment by 1 in each range`() {
        val ranges =
            listOf(
                100..199,
                200..299,
                300..399,
                400..499,
                500..599,
                600..699,
            )

        ranges.forEach { range ->
            val codesInRange =
                ChartboostMediationError::class
                    .sealedSubclasses
                    .flatMap { sealedClass ->
                        sealedClass.sealedSubclasses.mapNotNull { errorClass ->
                            val error = errorClass.objectInstance ?: errorClass.createInstance()
                            error.code
                                .removePrefix("CM_")
                                .toInt()
                                .takeIf { it in range }
                        }
                    }.sorted()

            codesInRange.forEachIndexed { index, code ->
                if (index > 0) {
                    assertEquals(
                        "Error codes do not increment by 1 in range: $range",
                        codesInRange[index - 1] + 1,
                        code,
                    )
                }
            }
        }
    }

    @Test
    fun `Chartboost Mediation Error message strings should end with a period`() {
        ChartboostMediationError::class.sealedSubclasses.forEach { sealedClass ->
            sealedClass.sealedSubclasses.forEach { errorClass ->
                val error = errorClass.objectInstance ?: errorClass.createInstance()
                assertTrue(
                    "Error message does not end with a period for: ${error::class.simpleName}",
                    error.message.endsWith("."),
                )
            }
        }
    }

    @Test
    fun `Chartboost Mediation Error cause strings should end with a period`() {
        ChartboostMediationError::class.sealedSubclasses.forEach { sealedClass ->
            sealedClass.sealedSubclasses.forEach { errorClass ->
                val error = errorClass.objectInstance ?: errorClass.createInstance()
                assertTrue(
                    "Error cause does not end with a period for: ${error::class.simpleName}",
                    error.cause.endsWith("."),
                )
            }
        }
    }

    @Test
    fun `Chartboost Mediation Error resolution message strings should end with a period`() {
        ChartboostMediationError::class.sealedSubclasses.forEach { sealedClass ->
            sealedClass.sealedSubclasses.forEach { errorClass ->
                val error = errorClass.objectInstance ?: errorClass.createInstance()
                assertTrue(
                    "Error resolution does not end with a period for: ${error::class.simpleName}",
                    error.resolution.endsWith("."),
                )
            }
        }
    }

    @Test
    fun `Chartboost Mediation Error toString() should be in the correct format`() {
        ChartboostMediationError::class.sealedSubclasses.forEach { sealedClass ->
            sealedClass.sealedSubclasses.forEach { errorClass ->
                val error = errorClass.objectInstance ?: errorClass.createInstance()
                val expectedToString =
                    "${error.name} (${error.code}). Cause: ${error.cause} Resolution: ${error.resolution}"
                assertEquals(
                    "toString() does not match expected format for: ${error::class.simpleName}",
                    expectedToString,
                    error.toString(),
                )
            }
        }
    }
}
