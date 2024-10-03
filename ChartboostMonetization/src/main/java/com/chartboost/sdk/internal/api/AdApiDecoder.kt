package com.chartboost.sdk.internal.api

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.utils.Base64Wrapper

interface AdApiDecoder {
    fun decodeBidResponse(
        bidResponse: String?,
        base64Wrapper: Base64Wrapper,
        onLoadFailure: (String?, CBError.Type) -> Unit,
    ): Result<String?>

    sealed class AdApiDecoderError : Exception() {
        data object INTERNAL : AdApiDecoderError() {
            private fun readResolve(): Any = INTERNAL
        }

        data object BAD_BASE_64 : AdApiDecoderError() {
            private fun readResolve(): Any = BAD_BASE_64
        }

        data object INVALID_RESPONSE : AdApiDecoderError() {
            private fun readResolve(): Any = INVALID_RESPONSE
        }
    }
}
