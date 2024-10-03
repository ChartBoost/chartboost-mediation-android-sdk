package com.chartboost.sdk.internal.utils

import android.util.Base64
import android.util.Base64.NO_WRAP
import com.chartboost.sdk.internal.logging.Logger

class Base64Wrapper {
    fun encode(originalString: String): String =
        runCatching {
            Base64.encodeToString(originalString.toByteArray(), NO_WRAP).clean()
        }.onFailure {
            Logger.e("Cannot encode to base64 string: ${it.localizedMessage}")
        }.getOrDefault("")

    fun decode(encodedString: String): String =
        runCatching {
            String(Base64.decode(encodedString.clean(), NO_WRAP))
        }.onFailure {
            Logger.e("Cannot decode base64 string: ${it.localizedMessage}")
        }.getOrDefault("")

    private fun String.clean(): String = replace("\n", "").trim { it <= ' ' }
}
