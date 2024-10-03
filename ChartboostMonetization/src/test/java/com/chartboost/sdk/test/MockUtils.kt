package com.chartboost.sdk.test

import android.net.Uri
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlin.reflect.KClass

inline fun <reified T : Any> relaxedMockk(
    name: String? = null,
    vararg moreInterfaces: KClass<*>,
    block: T.() -> Unit = {},
) = mockk(
    name = name,
    relaxed = true,
    moreInterfaces = moreInterfaces,
    relaxUnitFun = true,
    block = block,
)

inline fun <reified T : Any> justRunMockk(
    name: String? = null,
    vararg moreInterfaces: KClass<*>,
    block: T.() -> Unit = {},
) = mockk(
    name = name,
    relaxed = false,
    moreInterfaces = moreInterfaces,
    relaxUnitFun = true,
    block = block,
)

fun mockAndroidLog() {
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0
    every { Log.i(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
}

fun mockAndroidUri(): Uri {
    mockkStatic(android.net.Uri::class)
    val uri =
        mockk<Uri>().apply {
            every { scheme } returns "http"
            every { lastPathSegment } returns "test"
            every { path } returns "test"
        }
    every { Uri.parse(any()) } returns uri
    return uri
}
