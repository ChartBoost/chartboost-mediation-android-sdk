/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

/**
 * List of errors supported by Chartboost Mediation and mediation adapters.
 */
enum class ChartboostMediationError(
    val code: String,
    val message: String,
    val cause: String,
    val resolution: String
) {
    // Start of 1XX error codes
    CM_INITIALIZATION_FAILURE_UNKNOWN(
        "CM_100",
        "Chartboost Mediation initialization has failed.",
        "There was an error that was not accounted for.",
        "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs."
    ),
    CM_INITIALIZATION_FAILURE_ABORTED(
        "CM_101",
        "Partner initialization has failed.",
        "The initialization process started but was aborted midway prior to completion.",
        "Contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs."
    ),
    CM_INITIALIZATION_FAILURE_AD_BLOCKER_DETECTED(
        "CM_102", "Partner initialization has failed.", "An ad blocker was detected.", "N/A."
    ),
    CM_INITIALIZATION_FAILURE_ADAPTER_NOT_FOUND(
        "CM_103",
        "Partner initialization has failed.",
        "The adapter instance responsible to initialize this partner is no longer in memory.",
        "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_INITIALIZATION_FAILURE_INVALID_APP_CONFIG(
        "CM_104",
        "Partner initialization has failed.",
        "Chartboost Mediation received an invalid app config payload from the ad server.",
        "If this problem persists, reach out to the Chartboost Mediation team for further assistance. If possible, always forward us a copy of Chartboost Mediation network traffic."
    ),
    CM_INITIALIZATION_FAILURE_INVALID_CREDENTIALS(
        "CM_105",
        "Partner initialization has failed.",
        "Invalid/empty credentials were supplied to initialize the partner.",
        "Ensure appropriate fields are correctly entered on the Chartboost Mediation dashboard."
    ),
    CM_INITIALIZATION_FAILURE_NO_CONNECTIVITY(
        "CM_106",
        "Partner initialization has failed.",
        "No Internet connectivity was available.",
        "Ensure there is Internet connectivity and try again."
    ),
    CM_INITIALIZATION_FAILURE_PARTNER_NOT_INTEGRATED(
        "CM_107",
        "Partner initialization has failed.",
        "The partner adapter and/or SDK might not have been properly integrated.",
        "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build."
    ),
    CM_INITIALIZATION_FAILURE_TIMEOUT(
        "CM_108",
        "Partner initialization has failed.",
        "The initialization operation has taken too long to complete.",
        "This should not be a critical error. Typically the partner can continue to finish initialization in the background. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_INITIALIZATION_SKIPPED(
        "CM_109",
        "Partner initialization was skipped.",
        "You explicitly skipped initializing the partner.",
        "N/A."
    ),
    CM_INITIALIZATION_FAILURE_EXCEPTION(
        "CM_110",
        "Partner initialization has failed.",
        "An exception was thrown during initialization.",
        "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_INITIALIZATION_FAILURE_ACTIVITY_NOT_FOUND(
        "CM_111",
        "Partner initialization has failed.",
        "There is no Activity with which to initialize the partner.",
        "Ensure that the Context passed to HeliumSdk.start() is an Activity."
    ),
    CM_INITIALIZATION_FAILURE_NETWORKING_ERROR(
        "CM_112",
        "Chartboost Mediation initialization has failed.",
        "Init request failed due to a networking error.",
        "Typically this error should resolve itself. If the error persists, contact Chartboost Mediation support and share a copy of your network traffic logs."
    ),
    CM_INITIALIZATION_FAILURE_OS_VERSION_NOT_SUPPORTED(
        "CM_113",
        "Partner initialization has failed.",
        "The partner does not support this OS version.",
        "This is an expected error and can be ignored. Devices running newer OS versions should work fine."
    ),
    CM_INITIALIZATION_FAILURE_SERVER_ERROR(
        "CM_114",
        "Partner initialization has failed.",
        "The initialization request failed due to a server error.",
        "If this problem persists, reach out to Chartboost Mediation Support and/or the mediation partner team for further assistance. If possible, always share a copy of your network traffic logs."
    ),
    CM_INITIALIZATION_FAILURE_INTERNAL_ERROR(
        "CM_115",
        "Chartboost Mediation initialization has failed.",
        "An error occurred within the Chartboost Mediation initialization sequence.",
        "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_INITIALIZATION_FAILURE_INITIALIZATION_IN_PROGRESS(
        "CM_116",
        "Chartboost Mediation initialization is already in progress.",
        "Multiple initialization requests were made.",
        "Chartboost Mediation is already initializing. Please wait."
    ),


    // Start of 2XX error codes
    CM_PREBID_FAILURE_UNKNOWN(
        "CM_200",
        "Partner token fetch has failed.",
        "There was an error that was not accounted for.",
        "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs."
    ),
    CM_PREBID_FAILURE_ADAPTER_NOT_FOUND(
        "CM_201",
        "Partner token fetch has failed.",
        "The adapter instance responsible for this token fetch is no longer in memory.",
        "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_PREBID_FAILURE_INVALID_ARGUMENT(
        "CM_202",
        "Partner token fetch has failed.",
        "Required data is missing.",
        "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_PREBID_FAILURE_NOT_INITIALIZED(
        "CM_203",
        "Partner token fetch has failed.",
        "The partner was not able to call its bidding APIs because it was not initialized, either because you have explicitly skipped its initialization or there were issues initializing it.",
        "If this network supports bidding and you have explicitly skipped its initialization, allow it to initialize. Otherwise, try to re-initialize it."
    ),
    CM_PREBID_FAILURE_PARTNER_NOT_INTEGRATED(
        "CM_204",
        "Partner token fetch has failed.",
        "The partner adapter and/or SDK might not have been properly integrated.",
        "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build."
    ),
    CM_PREBID_FAILURE_TIMEOUT(
        "CM_205",
        "Partner token fetch has failed.",
        "The token fetch operation has taken too long to complete.",
        "Try again. Typically, this issue should resolve itself. If the issue persists, contact the mediation partner and provide a copy of your console logs."
    ),
    CM_PREBID_FAILURE_EXCEPTION(
        "CM_206",
        "Partner token fetch has failed.",
        "An exception was thrown during token fetch.",
        "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_PREBID_FAILURE_OS_VERSION_NOT_SUPPORTED(
        "CM_207",
        "Partner token fetch has failed.",
        "The partner does not support this OS version.",
        "This is an expected error and can be ignored. Devices running newer OS versions should work fine."
    ),
    CM_PREBID_FAILURE_NETWORKING_ERROR(
        "CM_208",
        "Partner token fetch has failed.",
        "Prebid request failed due to a networking error.",
        "Typically this error should resolve by itself. If the error persists, contact Chartboost Mediation Support and/or the mediation partner team and share a copy of your network traffic logs."
    ),


    // Start of 3XX error codes
    CM_LOAD_FAILURE_UNKNOWN(
        "CM_300",
        "Partner ad load has failed.",
        "There was an error that was not accounted for.",
        "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs."
    ),
    CM_LOAD_FAILURE_ABORTED(
        "CM_301",
        "Partner ad load has failed.",
        "The ad load process started but was aborted midway prior to completion.",
        "Contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_AD_BLOCKER_DETECTED(
        "CM_302",
        "Partner ad load has failed.",
        "An ad blocker was detected.",
        "N/A."
    ),
    CM_LOAD_FAILURE_ADAPTER_NOT_FOUND(
        "CM_303",
        "Partner ad load has failed.",
        "The adapter instance responsible for this load operation is no longer in memory.",
        "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_AUCTION_NO_BID(
        "CM_304",
        "Partner ad load has failed.",
        "The auction for this ad request did not succeed.",
        "Try again. Typically, this issue should resolve itself. If the issue persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_AUCTION_TIMEOUT(
        "CM_305",
        "Partner ad load has failed.",
        "The auction for this ad request has taken too long to complete.",
        "Try again. Typically, this issue should resolve itself. If the issue persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_INVALID_AD_MARKUP(
        "CM_306",
        "Partner ad load has failed.",
        "The ad markup String is invalid.",
        "Contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your network traffic logs."
    ),
    CM_LOAD_FAILURE_INVALID_AD_REQUEST(
        "CM_307",
        "Partner ad load has failed.",
        "The ad request is malformed.",
        "Contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your network traffic logs."
    ),
    CM_LOAD_FAILURE_INVALID_BID_RESPONSE(
        "CM_308",
        "Partner ad load has failed.",
        "The auction for this ad request succeeded but the bid response is corrupt.",
        "Try again. Typically, this issue should resolve itself. If the issue persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_INVALID_CHARTBOOST_MEDIATION_PLACEMENT(
        "CM_309",
        "Partner ad load has failed.",
        "The Chartboost placement is invalid or empty.",
        "Ensure the Chartboost placement is properly defined on the Chartboost Mediation dashboard."
    ),
    CM_LOAD_FAILURE_INVALID_PARTNER_PLACEMENT(
        "CM_310",
        "Partner ad load has failed.",
        "The partner placement is invalid or empty.",
        "Ensure the partner placement is properly defined on the Chartboost Mediation dashboard."
    ),
    CM_LOAD_FAILURE_MISMATCHED_AD_FORMAT(
        "CM_311",
        "Partner ad load has failed.",
        "A placement for a different ad format was used in the ad request for the current ad format.",
        "Ensure you are using the correct placement for the correct ad format."
    ),
    CM_LOAD_FAILURE_NO_CONNECTIVITY(
        "CM_312",
        "Partner ad load has failed.",
        "No Internet connectivity was available.",
        "Ensure there is Internet connectivity and try again."
    ),
    CM_LOAD_FAILURE_NO_FILL(
        "CM_313",
        "Partner ad load has failed.",
        "There is no ad inventory at this time.",
        "Try again but be mindful of CM_LOAD_FAILURE_RATE_LIMITED."
    ),
    CM_LOAD_FAILURE_PARTNER_NOT_INITIALIZED(
        "CM_314",
        "Partner ad load has failed.",
        "The partner was not able to call its load APIs because it was not initialized, either because you have explicitly skipped its initialization or there were issues initializing it.",
        "If you would like to load and show ads from this partner, allow it to initialize or try to re-initialize it."
    ),
    CM_LOAD_FAILURE_OUT_OF_STORAGE(
        "CM_315",
        "Partner ad load has failed.",
        "The ad request might have succeeded but there was not enough storage to store the ad. Therefore this is treated as a failure.",
        "N/A."
    ),
    CM_LOAD_FAILURE_PARTNER_NOT_INTEGRATED(
        "CM_316",
        "Partner ad load has failed.",
        "The partner adapter and/or SDK might not have been properly integrated.",
        "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build."
    ),
    CM_LOAD_FAILURE_RATE_LIMITED(
        "CM_317",
        "Partner ad load has failed.",
        "Too many ad requests have been made over a short amount of time.",
        "Avoid continually making ad requests in a short amount of time. You may implement an exponential backoff strategy to help mitigate this issue."
    ),
    CM_LOAD_FAILURE_SHOW_IN_PROGRESS(
        "CM_318",
        "Partner ad load has failed.",
        "An ad is already showing.",
        "You can only load another ad once the current ad is done showing."
    ),
    CM_LOAD_FAILURE_TIMEOUT(
        "CM_319",
        "Partner ad load has failed.",
        "The ad request operation has taken too long to complete.",
        "If this issue persists, contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_UNSUPPORTED_AD_FORMAT(
        "CM_320",
        "Partner ad load has failed.",
        "The partner does not support that ad format.",
        "Try again with a different ad format. If the ad format you are requesting for is supported by the partner, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_PRIVACY_OPT_IN(
        "CM_321",
        "Partner ad load has failed.",
        "One or more privacy settings have been opted in.",
        "N/A."
    ),
    CM_LOAD_FAILURE_PRIVACY_OPT_OUT(
        "CM_322",
        "Partner ad load has failed.",
        "One or more privacy settings have been opted out.",
        "N/A."
    ),
    CM_LOAD_FAILURE_PARTNER_INSTANCE_NOT_FOUND(
        "CM_323",
        "Partner ad load has failed.",
        "The partner SDK instance is null.",
        "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build."
    ),
    CM_LOAD_FAILURE_MISMATCHED_AD_PARAMS(
        "CM_324",
        "Partner ad load has failed.",
        "The partner returned an ad with different ad parameters than the one requested.",
        "This is typically caused by a partner SDK bug. Contact the mediation partner and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_INVALID_BANNER_SIZE(
        "CM_325",
        "Partner ad load has failed.",
        "The supplied banner size is invalid.",
        "Ensure the requested banner size is valid."
    ),
    CM_LOAD_FAILURE_EXCEPTION(
        "CM_326",
        "Partner ad load has failed.",
        "An exception was thrown during ad load.",
        "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_LOAD_IN_PROGRESS(
        "CM_327",
        "Partner ad load has failed.",
        "An ad is already loading.",
        "Wait until the current ad load is done before loading another ad."
    ),
    CM_LOAD_FAILURE_ACTIVITY_NOT_FOUND(
        "CM_328",
        "Partner ad load has failed.",
        "There is no Activity to load the ad in.",
        "Ensure that the Context passed to HeliumSdk.start() is an Activity."
    ),
    CM_LOAD_FAILURE_NO_INLINE_VIEW(
        "CM_329",
        "Partner ad load has failed.",
        "The partner returns an ad with no inline view to show.",
        "This is typically caused by a partner adapter bug. Contact the mediation partner and provide a copy of your console logs."
    ),
    CM_LOAD_FAILURE_NETWORKING_ERROR(
        "CM_330",
        "Partner ad load has failed.",
        "Ad request failed due to a networking error.",
        "Typically this error should resolve itself. If the error persists, contact Chartboost Mediation support and share a copy of your network traffic logs."
    ),
    CM_LOAD_FAILURE_CHARTBOOST_MEDIATION_NOT_INITIALIZED(
        "CM_331",
        "Partner ad load has failed.",
        "The Chartboost Mediation SDK was not initialized.",
        "Ensure the Chartboost Mediation SDK is initialized before loading ads."
    ),
    CM_LOAD_FAILURE_OS_VERSION_NOT_SUPPORTED(
        "CM_332",
        "Partner ad load has failed.",
        "The partner does not support this OS version.",
        "This is an expected error and can be ignored. Devices running newer OS versions should work fine."
    ),
    CM_LOAD_FAILURE_SERVER_ERROR(
        "CM_333",
        "Partner ad load has failed.",
        "The load request failed due to a server error.",
        "If this problem persists, reach out to Chartboost Mediation Support and/or the mediation partner team for further assistance. If possible, always share a copy of your network traffic logs."
    ),
    CM_LOAD_FAILURE_INVALID_CREDENTIALS(
        "CM_334",
        "Partner ad load has failed.",
        "Invalid/empty credentials were supplied to load the ad.",
        "Ensure appropriate fields are correctly entered on the partner dashboard."
    ),
    CM_LOAD_FAILURE_WATERFALL_EXHAUSTED_NO_FILL(
        "CM_335",
        "All waterfall entries have been exhausted. No ad fill.",
        "All waterfall entries have resulted in an error or no fill.",
        "Try again. If the problem persists, verify Partner settings in the Chartboost Mediation dashboard."
    ),

    // Start of 4XX error codes
    CM_SHOW_FAILURE_UNKNOWN(
        "CM_400",
        "Partner ad show has failed.",
        "There was an error that was not accounted for.",
        "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs."
    ),
    CM_SHOW_FAILURE_ACTIVITY_NOT_FOUND(
        "CM_401",
        "Partner ad show has failed.",
        "There is no Activity to show the ad in.",
        "Ensure that the Context passed to HeliumSdk.start() is an Activity."
    ),
    CM_SHOW_FAILURE_AD_BLOCKER_DETECTED(
        "CM_402",
        "Partner ad show has failed.",
        "An ad blocker was detected.",
        "N/A."
    ),
    CM_SHOW_FAILURE_AD_NOT_FOUND(
        "CM_403",
        "Partner ad show has failed.",
        "An ad that might have been cached is no longer available to show.",
        "Try loading another ad but be mindful of CM_LOAD_FAILURE_RATE_LIMITED."
    ),
    CM_SHOW_FAILURE_AD_EXPIRED(
        "CM_404",
        "Partner ad show has failed.",
        "The ad was expired by the partner SDK after a set time window.",
        "Try loading another ad but be mindful of CM_LOAD_FAILURE_RATE_LIMITED."
    ),
    CM_SHOW_FAILURE_AD_NOT_READY(
        "CM_405",
        "Partner ad show has failed.",
        "There is no ad ready to show.",
        "Try loading another ad and ensure it is ready before it's shown."
    ),
    CM_SHOW_FAILURE_ADAPTER_NOT_FOUND(
        "CM_406",
        "Partner ad show has failed.",
        "The adapter instance responsible for this show operation is no longer in memory.",
        "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_SHOW_FAILURE_INVALID_CHARTBOOST_MEDIATION_PLACEMENT(
        "CM_407",
        "Partner ad show has failed.",
        "The Chartboost placement is invalid or empty.",
        "Ensure the Chartboost placement is properly defined on the Chartboost Mediation dashboard."
    ),
    CM_SHOW_FAILURE_INVALID_PARTNER_PLACEMENT(
        "CM_408",
        "Partner ad show has failed.",
        "The partner placement is invalid or empty.",
        "Ensure the partner placement is properly defined on the Chartboost Mediation dashboard."
    ),
    CM_SHOW_FAILURE_MEDIA_BROKEN(
        "CM_409",
        "Partner ad show has failed.",
        "The media associated with this ad is corrupt and cannot be rendered.",
        "Try loading another ad. If this problem persists, contact the mediation partner and provide a copy of your console and network traffic logs."
    ),
    CM_SHOW_FAILURE_NO_CONNECTIVITY(
        "CM_410",
        "Partner ad show has failed.",
        "No Internet connectivity was available.",
        "Ensure there is Internet connectivity and try again."
    ),
    CM_SHOW_FAILURE_NO_FILL(
        "CM_411",
        "Partner ad show has failed.",
        "There is no ad inventory at this time.",
        "Try loading another ad but be mindful of CM_LOAD_FAILURE_RATE_LIMITED."
    ),
    CM_SHOW_FAILURE_NOT_INITIALIZED(
        "CM_412",
        "Partner ad show has failed.",
        "The partner was not able to call its show APIs because it was not initialized, either because you have explicitly skipped its initialization or there were issues initializing it.",
        "If you would like to load and show ads from this partner, allow it to initialize or try to re-initialize it."
    ),
    CM_SHOW_FAILURE_PARTNER_NOT_INTEGRATED(
        "CM_413",
        "Partner ad show has failed.",
        "The partner adapter and/or SDK might not have been properly integrated.",
        "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build."
    ),
    CM_SHOW_FAILURE_SHOW_IN_PROGRESS(
        "CM_414",
        "Partner ad show has failed.",
        "An ad is already showing.",
        "You cannot show multiple fullscreen ads simultaneously. Wait until the current ad is done showing before showing another ad."
    ),
    CM_SHOW_FAILURE_TIMEOUT(
        "CM_415",
        "Partner ad show has failed.",
        "The show operation has taken too long to complete.",
        "If this issue persists, contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs."
    ),
    CM_SHOW_FAILURE_VIDEO_PLAYER_ERROR(
        "CM_416",
        "Partner ad show has failed.",
        "There was an error with the video player.",
        "Contact Chartboost Mediation Support or the mediation partner and provide details of your integration."
    ),
    CM_SHOW_FAILURE_PRIVACY_OPT_IN(
        "CM_417",
        "Partner ad show has failed.",
        "One or more privacy settings have been opted in.",
        "N/A."
    ),
    CM_SHOW_FAILURE_PRIVACY_OPT_OUT(
        "CM_418",
        "Partner ad show has failed.",
        "One or more privacy settings have been opted out.",
        "N/A."
    ),
    CM_SHOW_FAILURE_WRONG_RESOURCE_TYPE(
        "CM_419",
        "Partner ad show has failed.",
        "A resource was found but it doesn't match the ad type to be shown.",
        "This is an internal error. Typically, it should resolve itself. If this issue persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_SHOW_FAILURE_UNSUPPORTED_AD_FORMAT(
        "CM_420",
        "Partner ad show has failed.",
        "The ad format is not supported by the partner SDK.",
        "Try again with a different ad format. If the ad format you are requesting for is supported by the partner, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_SHOW_FAILURE_EXCEPTION(
        "CM_421",
        "Partner ad show has failed.",
        "An exception was thrown during ad show.",
        "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_SHOW_FAILURE_UNSUPPORTED_AD_SIZE(
        "CM_422",
        "Partner ad show has failed.",
        "The ad size is not supported by the partner SDK.",
        "If this issue persists, contact the mediation partner and provide a copy of your console log."
    ),
    CM_SHOW_FAILURE_INVALID_BANNER_SIZE(
        "CM_423",
        "Partner ad show has failed.",
        "The supplied banner size is invalid.",
        "Ensure the requested banner size is valid."
    ),

    // Start of 5XX error codes
    CM_INVALIDATE_FAILURE_UNKNOWN(
        "CM_500",
        "Partner ad invalidation has failed.",
        "There was an error that was not accounted for.",
        "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs."
    ),
    CM_INVALIDATE_FAILURE_AD_NOT_FOUND(
        "CM_501",
        "Partner ad invalidation has failed.",
        "There is no ad to invalidate.",
        "N/A."
    ),
    CM_INVALIDATE_FAILURE_ADAPTER_NOT_FOUND(
        "CM_502",
        "Partner ad invalidation has failed.",
        "The adapter instance responsible for this show operation is no longer in memory.",
        "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_INVALIDATE_FAILURE_NOT_INITIALIZED(
        "CM_503",
        "Partner ad invalidation has failed.",
        "The partner was not able to call its invalidate APIs because it was not initialized, either because you have explicitly skipped its initialization or there were issues initializing it.",
        "If this network supports ad invalidation and you have explicitly skipped its initialization, allow it to initialize. Otherwise, try to re-initialize it."
    ),
    CM_INVALIDATE_FAILURE_PARTNER_NOT_INTEGRATED(
        "CM_504",
        "Partner ad invalidation has failed.",
        "The partner adapter and/or SDK might not have been properly integrated.",
        "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build."
    ),
    CM_INVALIDATE_FAILURE_TIMEOUT(
        "CM_505",
        "Partner ad invalidation has failed.",
        "The invalidate operation has taken too long to complete.",
        "If this issue persists, contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs."
    ),
    CM_INVALIDATE_FAILURE_WRONG_RESOURCE_TYPE(
        "CM_506",
        "Partner ad invalidation has failed.",
        "A resource was found but it doesn't match the ad type to be invalidated.",
        "This is an internal error. Typically, it should resolve itself. If this issue persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_INVALIDATE_FAILURE_EXCEPTION(
        "CM_507",
        "Partner ad invalidation has failed.",
        "An exception was thrown during ad invalidation.",
        "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_INVALIDATE_UNSUPPORTED_AD_FORMAT(
        "CM_508",
        "Partner ad invalidation has failed.",
        "The ad format is not supported by the partner SDK.",
        "Try again with a different ad format. If the ad format you are requesting for is supported by the partner, contact Chartboost Mediation Support and provide a copy of your console logs."
    ),

    // Start of 6XX error codes
    CM_UNKNOWN_ERROR(
        "CM_600",
        "An unknown error has occurred. It is unclear if it originates from Chartboost Mediation or mediation partner(s).",
        "There is no known cause.",
        "No information is available about this error."
    ),
    CM_PARTNER_ERROR(
        "CM_601",
        "The partner has returned an error.",
        "Unknown.",
        "The Chartboost Mediation SDK does not have insights into this type of error. Contact the mediation partner and provide details of your integration."
    ),
    CM_INTERNAL_ERROR(
        "CM_602",
        "An internal error has occurred.",
        "Unknown.",
        "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_NO_CONNECTIVITY(
        "CM_603",
        "No Internet connectivity was available.",
        "Unknown.",
        "Ensure there is Internet connectivity and try again."
    ),
    CM_AD_SERVER_ERROR(
        "CM_604",
        "An ad server issue has occurred.",
        "Unknown.",
        "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs."
    ),
    CM_INVALID_ARGUMENTS(
        "CM_605",
        "Invalid/empty arguments were passed to the function call, which caused the function to terminate prematurely.",
        "Unknown.",
        "Depending on when this error occurs, it could be due to an issue in Chartboost Mediation or mediation partner(s) or your integration. Contact Chartboost Mediation Support and provide a copy of your console logs."
    );

    override fun toString(): String {
        return "$name ($code). Cause: $cause Resolution: $resolution"
    }
}
