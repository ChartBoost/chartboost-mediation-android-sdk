package com.chartboost.sdk.internal.api

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.api.AdApiDecoder.AdApiDecoderError.BAD_BASE_64
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.Base64Wrapper

object AdApiDecoderImpl : AdApiDecoder {
    override fun decodeBidResponse(
        bidResponse: String?,
        base64Wrapper: Base64Wrapper,
        onLoadFailure: (String?, CBError.Type) -> Unit,
    ): Result<String?> {
        var decodedBidResponse: String? = null

        // bidResponse null means we are in the normal flow AdGet
        bidResponse?.let { res ->
            base64Wrapper.decode(res).let {
                if (it.isEmpty()) {
                    Logger.e("Cannot decode provided bidResponse.")
                    onLoadFailure("", CBError.Impression.INVALID_RESPONSE)
                    return Result.failure(BAD_BASE_64)
                }
                decodedBidResponse = it
            }
        }

        return Result.success(decodedBidResponse)
    }
}
