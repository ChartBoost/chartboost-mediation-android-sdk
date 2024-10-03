package com.chartboost.sdk.external.model

import com.chartboost.sdk.internal.Model.CBError.Impression
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CBErrorExternalTest : StringSpec({

    "CBImpressionError values should have correct ordinals" {
        // Cannot use withData() here, see https://github.com/kotest/kotest/issues/3351
        mapOf(
            Impression.INTERNAL to 0,
            Impression.INTERNET_UNAVAILABLE to 1,
            Impression.TOO_MANY_CONNECTIONS to 2,
            Impression.WRONG_ORIENTATION to 3,
            Impression.FIRST_SESSION_INTERSTITIALS_DISABLED to 4,
            Impression.NETWORK_FAILURE to 5,
            Impression.NO_AD_FOUND to 6,
            Impression.SESSION_NOT_STARTED to 7,
            Impression.IMPRESSION_ALREADY_VISIBLE to 8,
            Impression.NO_HOST_ACTIVITY to 9,
            Impression.USER_CANCELLATION to 10,
            Impression.INVALID_LOCATION to 11,
            Impression.VIDEO_UNAVAILABLE to 12,
            Impression.VIDEO_ID_MISSING to 13,
            Impression.ERROR_PLAYING_VIDEO to 14,
            Impression.INVALID_RESPONSE to 15,
            Impression.ASSETS_DOWNLOAD_FAILURE to 16,
            Impression.ERROR_CREATING_VIEW to 17,
            Impression.ERROR_DISPLAYING_VIEW to 18,
            Impression.INCOMPATIBLE_API_VERSION to 19,
            Impression.ERROR_LOADING_WEB_VIEW to 20,
            Impression.ASSET_PREFETCH_IN_PROGRESS to 21,
            Impression.ACTIVITY_MISSING_IN_MANIFEST to 22, // on IOS, this is CBLoadErrorWebViewScriptError
            Impression.EMPTY_LOCAL_VIDEO_LIST to 23,
            Impression.END_POINT_DISABLED to 24,
            Impression.HARDWARE_ACCELERATION_DISABLED to 25,
            Impression.PENDING_IMPRESSION_ERROR to 26,
            Impression.VIDEO_UNAVAILABLE_FOR_CURRENT_ORIENTATION to 27,
            Impression.ASSET_MISSING to 28,
        ).forEach { (value, expected) ->
            value.ordinal shouldBe expected
        }
    }
})
