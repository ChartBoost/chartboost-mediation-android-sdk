/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * List of errors supported by Chartboost Mediation and mediation adapters.
 */
@Serializable
sealed class ChartboostMediationError(
    override val code: String,
    override val message: String,
    override val cause: String,
    override val resolution: String,
    override val serverErrorName: String,
) : ChartboostMediationErrorContract {
    /**
     * The name of the error, e.g. `ShowError.AdNotReady`
     */
    val name: String =
        run {
            val className = this::class.simpleName ?: "Unknown"
            val enclosingClassName =
                this::class
                    .supertypes
                    .firstOrNull()
                    ?.toString()
                    ?.split('.')
                    ?.lastOrNull()
                    ?: "ChartboostMediationError"
            "$enclosingClassName.$className"
        }

    /**
     * The toString representation of the error.
     */
    override fun toString(): String = "$name ($code). Cause: $cause Resolution: $resolution"

    /**
     * 1XX: Initialization Errors
     *
     * @param code The error code.
     * @param message The error message.
     * @param cause The cause of the error.
     * @param resolution The re@SerialNamesolution to the error.
     */
    @Serializable
    sealed class InitializationError(
        @SerialName("initError_code") override val code: String,
        @SerialName("initError_message") override val message: String = "Initialization has failed.",
        @SerialName("initError_cause") override val cause: String,
        @SerialName("initError_resolution") override val resolution: String,
        @SerialName("initError_serverErrorName") override val serverErrorName: String,
    ) : ChartboostMediationError(code, message, cause, resolution, serverErrorName) {
        @Serializable
        object Unknown : InitializationError(
            code = "CM_100",
            cause = "There was an error that was not accounted for.",
            resolution = "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_UNKNOWN",
        )

        @Serializable
        object Aborted : InitializationError(
            code = "CM_101",
            cause = "The initialization process started but was aborted midway prior to completion.",
            resolution = "Contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_ABORTED",
        )

        @Serializable
        object AdBlockerDetected : InitializationError(
            code = "CM_102",
            cause = "An ad blocker was detected.",
            resolution = "N/A.",
            serverErrorName = "CM_SHOW_FAILURE_AD_BLOCKER_DETECTED",
        )

        @Serializable
        object AdapterNotFound : InitializationError(
            code = "CM_103",
            cause = "The adapter instance responsible for initializing this partner is no longer in memory.",
            resolution = "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_ADAPTER_NOT_FOUND",
        )

        @Serializable
        object InvalidAppConfig : InitializationError(
            code = "CM_104",
            cause = "Chartboost Mediation received an invalid app config payload from the ad server.",
            resolution = "If this problem persists, reach out to the Chartboost Mediation team for further assistance. If possible, always forward us a copy of Chartboost Mediation network traffic.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_INVALID_APP_CONFIG",
        )

        @Serializable
        object InvalidCredentials : InitializationError(
            code = "CM_105",
            cause = "Invalid/empty credentials were supplied to initialize the partner.",
            resolution = "Ensure appropriate fields are correctly entered on the Chartboost Mediation dashboard.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_INVALID_CREDENTIALS",
        )

        @Serializable
        object NoConnectivity : InitializationError(
            code = "CM_106",
            cause = "No Internet connectivity was available.",
            resolution = "Ensure there is Internet connectivity and try again.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_NO_CONNECTIVITY",
        )

        @Serializable
        object PartnerNotIntegrated : InitializationError(
            code = "CM_107",
            cause = "The partner adapter and/or SDK might not have been properly integrated.",
            resolution = "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_PARTNER_NOT_INTEGRATED",
        )

        @Serializable
        object Timeout : InitializationError(
            code = "CM_108",
            cause = "The initialization operation has taken too long to complete.",
            resolution = "This should not be a critical error. Typically the partner can continue to finish initialization in the background. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_TIMEOUT",
        )

        @Serializable
        object Skipped : InitializationError(
            code = "CM_109",
            message = "Partner initialization was skipped.",
            cause = "You explicitly skipped initializing the partner.",
            resolution = "N/A.",
            serverErrorName = "CM_INITIALIZATION_SKIPPED",
        )

        @Serializable
        object Exception : InitializationError(
            code = "CM_110",
            cause = "An exception was thrown during initialization.",
            resolution = "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_EXCEPTION",
        )

        @Serializable
        object ActivityNotFound : InitializationError(
            code = "CM_111",
            cause = "There is no Activity with which to initialize the partner.",
            resolution = "Ensure that the Context passed to ChartboostMediationSdk.start() is an Activity.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_ACTIVITY_NOT_FOUND",
        )

        @Serializable
        object NetworkingError : InitializationError(
            code = "CM_112",
            cause = "Init request failed due to a networking error.",
            resolution = "Typically this error should resolve itself. If the error persists, contact Chartboost Mediation support and share a copy of your network traffic logs.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_NETWORKING_ERROR",
        )

        @Serializable
        object OSVersionNotSupported : InitializationError(
            code = "CM_113",
            cause = "The partner does not support this OS version.",
            resolution = "This is an expected error and can be ignored. Devices running newer OS versions should work fine.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_OS_VERSION_NOT_SUPPORTED",
        )

        @Serializable
        object ServerError : InitializationError(
            code = "CM_114",
            cause = "The initialization request failed due to a server error.",
            resolution = "If this problem persists, reach out to Chartboost Mediation Support and/or the mediation partner team for further assistance. If possible, always share a copy of your network traffic logs.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_SERVER_ERROR",
        )

        @Serializable
        object InternalError : InitializationError(
            code = "CM_115",
            cause = "An error occurred within the Chartboost Mediation initialization sequence.",
            resolution = "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_INTERNAL_ERROR",
        )

        @Serializable
        object InProgress : InitializationError(
            code = "CM_116",
            message = "Chartboost Mediation initialization is already in progress.",
            cause = "Multiple initialization requests were made.",
            resolution = "Chartboost Mediation is already initializing. Please wait.",
            serverErrorName = "CM_INITIALIZATION_FAILURE_INITIALIZATION_IN_PROGRESS",
        )
    }

    /**
     * 2XX: Prebid Errors
     *
     * @param code The error code.
     * @param message The error message.
     * @param cause The cause of the error.
     * @param resolution The resolution to the error.
     */
    @Serializable
    sealed class PrebidError(
        @SerialName("prebidError_code") override val code: String,
        @SerialName("prebidError_message") override val message: String = "Partner token fetch has failed.",
        @SerialName("prebidError_cause") override val cause: String,
        @SerialName("prebidError_resolution") override val resolution: String,
        @SerialName("prebidError_serverErrorName") override val serverErrorName: String,
    ) : ChartboostMediationError(code, message, cause, resolution, serverErrorName) {
        @Serializable
        object Unknown : PrebidError(
            "CM_200",
            cause = "There was an error that was not accounted for.",
            resolution = "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs.",
            serverErrorName = "CM_PREBID_FAILURE_UNKNOWN",
        )

        @Serializable
        object AdapterNotFound : PrebidError(
            "CM_201",
            cause = "The adapter instance responsible for this token fetch is no longer in memory.",
            resolution = "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_PREBID_FAILURE_ADAPTER_NOT_FOUND",
        )

        @Serializable
        object InvalidArgument : PrebidError(
            "CM_202",
            cause = "Required data is missing.",
            resolution = "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_PREBID_FAILURE_INVALID_ARGUMENT",
        )

        @Serializable
        object Uninitialized : PrebidError(
            "CM_203",
            cause = "The partner was not able to call its bidding APIs because it was not initialized, either because you have explicitly skipped its initialization or there were issues initializing it.",
            resolution = "If this network supports bidding and you have explicitly skipped its initialization, allow it to initialize. Otherwise, try to re-initialize it.",
            serverErrorName = "CM_PREBID_FAILURE_NOT_INITIALIZED",
        )

        @Serializable
        object PartnerNotIntegrated : PrebidError(
            "CM_204",
            cause = "The partner adapter and/or SDK might not have been properly integrated.",
            resolution = "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build.",
            serverErrorName = "CM_PREBID_FAILURE_PARTNER_NOT_INTEGRATED",
        )

        @Serializable
        object Timeout : PrebidError(
            "CM_205",
            cause = "The token fetch operation has taken too long to complete.",
            resolution = "Try again. Typically, this issue should resolve itself. If the issue persists, contact the mediation partner and provide a copy of your console logs.",
            serverErrorName = "CM_PREBID_FAILURE_TIMEOUT",
        )

        @Serializable
        object Exception : PrebidError(
            "CM_206",
            cause = "An exception was thrown during token fetch.",
            resolution = "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_PREBID_FAILURE_EXCEPTION",
        )

        @Serializable
        object UnsupportedOsVersion : PrebidError(
            "CM_207",
            cause = "The partner does not support this OS version.",
            resolution = "This is an expected error and can be ignored. Devices running newer OS versions should work fine.",
            serverErrorName = "CM_PREBID_FAILURE_OS_VERSION_NOT_SUPPORTED",
        )

        @Serializable
        object NetworkingError : PrebidError(
            "CM_208",
            cause = "Prebid request failed due to a networking error.",
            resolution = "Typically this error should resolve by itself. If the error persists, contact Chartboost Mediation Support and/or the mediation partner team and share a copy of your network traffic logs.",
            serverErrorName = "CM_PREBID_FAILURE_NETWORKING_ERROR",
        )

        @Serializable
        object UnsupportedAdFormat : PrebidError(
            "CM_209",
            cause = "The partner does not support that ad format.",
            resolution = "Try again with a different ad format. If the ad format you are requesting for is supported by the partner, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_PREBID_FAILURE_UNSUPPORTED_AD_FORMAT",
        )
    }

    /**
     * 3XX: Load Errors
     *
     * @param code The error code.
     * @param message The error message.
     * @param cause The cause of the error.
     * @param resolution The resolution to the error.
     */
    @Serializable
    sealed class LoadError(
        @SerialName("loadError_code") override val code: String,
        @SerialName("loadError_message") override val message: String = "Ad load has failed.",
        @SerialName("loadError_cause") override val cause: String,
        @SerialName("loadError_resolution") override val resolution: String,
        @SerialName("loadError_serverErrorName") override val serverErrorName: String,
    ) : ChartboostMediationError(code, message, cause, resolution, serverErrorName) {
        @Serializable
        object Unknown : LoadError(
            "CM_300",
            cause = "There was an error that was not accounted for.",
            resolution = "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_UNKNOWN",
        )

        @Serializable
        object Aborted : LoadError(
            "CM_301",
            cause = "The ad load process started but was aborted midway prior to completion.",
            resolution = "Contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_ABORTED",
        )

        @Serializable
        object AdBlockerDetected : LoadError(
            "CM_302",
            cause = "An ad blocker was detected.",
            resolution = "N/A.",
            serverErrorName = "CM_LOAD_FAILURE_AD_BLOCKER_DETECTED",
        )

        @Serializable
        object AdapterNotFound : LoadError(
            "CM_303",
            cause = "The adapter instance responsible for this load operation is no longer in memory.",
            resolution = "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_ADAPTER_NOT_FOUND",
        )

        @Serializable
        object AuctionNoBid : LoadError(
            "CM_304",
            cause = "The auction for this ad request did not succeed.",
            resolution = "Try again. Typically, this issue should resolve itself. If the issue persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_AUCTION_NO_BID",
        )

        @Serializable
        object AuctionTimeout : LoadError(
            "CM_305",
            cause = "The auction for this ad request has taken too long to complete.",
            resolution = "Try again. Typically, this issue should resolve itself. If the issue persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_AUCTION_TIMEOUT",
        )

        @Serializable
        object InvalidAdMarkup : LoadError(
            "CM_306",
            cause = "The ad markup String is invalid.",
            resolution = "Contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your network traffic logs.",
            serverErrorName = "CM_LOAD_FAILURE_INVALID_AD_MARKUP",
        )

        @Serializable
        object InvalidAdRequest : LoadError(
            "CM_307",
            cause = "The ad request is malformed.",
            resolution = "Contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your network traffic logs.",
            serverErrorName = "CM_LOAD_FAILURE_INVALID_AD_REQUEST",
        )

        @Serializable
        object InvalidBidResponse : LoadError(
            "CM_308",
            cause = "The auction for this ad request succeeded but the bid response is corrupt.",
            resolution = "Try again. Typically, this issue should resolve itself. If the issue persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_INVALID_BID_RESPONSE",
        )

        @Serializable
        object InvalidChartboostMediationPlacement : LoadError(
            "CM_309",
            cause = "The Chartboost placement is invalid or empty.",
            resolution = "Ensure the Chartboost placement is properly defined on the Chartboost Mediation dashboard.",
            serverErrorName = "CM_LOAD_FAILURE_INVALID_CHARTBOOST_MEDIATION_PLACEMENT",
        )

        @Serializable
        object InvalidPartnerPlacement : LoadError(
            "CM_310",
            cause = "The partner placement is invalid or empty.",
            resolution = "Ensure the partner placement is properly defined on the Chartboost Mediation dashboard.",
            serverErrorName = "CM_LOAD_FAILURE_INVALID_PARTNER_PLACEMENT",
        )

        @Serializable
        object MismatchedAdFormat : LoadError(
            "CM_311",
            cause = "A placement for a different ad format was used in the ad request for the current ad format.",
            resolution = "Ensure you are using the correct placement for the correct ad format.",
            serverErrorName = "CM_LOAD_FAILURE_MISMATCHED_AD_FORMAT",
        )

        @Serializable
        object NoConnectivity : LoadError(
            "CM_312",
            cause = "No Internet connectivity was available.",
            resolution = "Ensure there is Internet connectivity and try again.",
            serverErrorName = "CM_LOAD_FAILURE_NO_CONNECTIVITY",
        )

        @Serializable
        object NoFill : LoadError(
            "CM_313",
            cause = "There is no ad inventory at this time.",
            resolution = "Try again but be mindful of CM_LOAD_FAILURE_RATE_LIMITED.",
            serverErrorName = "CM_LOAD_FAILURE_NO_FILL",
        )

        @Serializable
        object PartnerNotInitialized : LoadError(
            "CM_314",
            cause = "The partner was not able to call its load APIs because it was not initialized, either because you have explicitly skipped its initialization or there were issues initializing it.",
            resolution = "If you would like to load and show ads from this partner, allow it to initialize or try to re-initialize it.",
            serverErrorName = "CM_LOAD_FAILURE_PARTNER_NOT_INITIALIZED",
        )

        @Serializable
        object OutOfStorage : LoadError(
            "CM_315",
            cause = "The ad request might have succeeded but there was not enough storage to store the ad. Therefore this is treated as a failure.",
            resolution = "N/A.",
            serverErrorName = "CM_LOAD_FAILURE_OUT_OF_STORAGE",
        )

        @Serializable
        object PartnerNotIntegrated : LoadError(
            "CM_316",
            cause = "The partner adapter and/or SDK might not have been properly integrated.",
            resolution = "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build.",
            serverErrorName = "CM_LOAD_FAILURE_PARTNER_NOT_INTEGRATED",
        )

        @Serializable
        object RateLimited : LoadError(
            "CM_317",
            cause = "Too many ad requests have been made over a short amount of time.",
            resolution = "Avoid continually making ad requests in a short amount of time. You may implement an exponential backoff strategy to help mitigate this issue.",
            serverErrorName = "CM_LOAD_FAILURE_RATE_LIMITED",
        )

        @Serializable
        object ShowInProgress : LoadError(
            "CM_318",
            cause = "An ad is already showing.",
            resolution = "You can only load another ad once the current ad is done showing.",
            serverErrorName = "CM_LOAD_FAILURE_SHOW_IN_PROGRESS",
        )

        @Serializable
        object AdRequestTimeout : LoadError(
            "CM_319",
            cause = "The ad request operation has taken too long to complete.",
            resolution = "If this issue persists, contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_TIMEOUT",
        )

        @Serializable
        object UnsupportedAdFormat : LoadError(
            "CM_320",
            cause = "The partner does not support that ad format.",
            resolution = "Try again with a different ad format. If the ad format you are requesting for is supported by the partner, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_UNSUPPORTED_AD_FORMAT",
        )

        @Serializable
        object PrivacyOptIn : LoadError(
            "CM_321",
            cause = "One or more privacy settings have been opted in.",
            resolution = "N/A.",
            serverErrorName = "CM_LOAD_FAILURE_PRIVACY_OPT_IN",
        )

        @Serializable
        object PrivacyOptOut : LoadError(
            "CM_322",
            cause = "One or more privacy settings have been opted out.",
            resolution = "N/A.",
            serverErrorName = "CM_LOAD_FAILURE_PRIVACY_OPT_OUT",
        )

        @Serializable
        object PartnerInstanceNotFound : LoadError(
            "CM_323",
            cause = "The partner SDK instance is null.",
            resolution = "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build.",
            serverErrorName = "CM_LOAD_FAILURE_PARTNER_INSTANCE_NOT_FOUND",
        )

        @Serializable
        object MismatchedAdParams : LoadError(
            "CM_324",
            cause = "The partner returned an ad with different ad parameters than the one requested.",
            resolution = "This is typically caused by a partner SDK bug. Contact the mediation partner and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_MISMATCHED_AD_PARAMS",
        )

        @Serializable
        object InvalidBannerSize : LoadError(
            "CM_325",
            cause = "The supplied banner size is invalid.",
            resolution = "Ensure the requested banner size is valid.",
            serverErrorName = "CM_LOAD_FAILURE_INVALID_BANNER_SIZE",
        )

        @Serializable
        object Exception : LoadError(
            "CM_326",
            cause = "An exception was thrown during ad load.",
            resolution = "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_EXCEPTION",
        )

        @Serializable
        object LoadInProgress : LoadError(
            "CM_327",
            cause = "An ad is already loading.",
            resolution = "Wait until the current ad load is done before loading another ad.",
            serverErrorName = "CM_LOAD_FAILURE_LOAD_IN_PROGRESS",
        )

        @Serializable
        object ActivityNotFound : LoadError(
            "CM_328",
            cause = "There is no Activity to load the ad in.",
            resolution = "Ensure that the Context passed to ChartboostMediationSdk.start() is an Activity.",
            serverErrorName = "CM_LOAD_FAILURE_ACTIVITY_NOT_FOUND",
        )

        @Serializable
        object NoBannerView : LoadError(
            "CM_329",
            cause = "The partner returns an ad with no banner view to show.",
            resolution = "This is typically caused by a partner adapter bug. Contact the mediation partner and provide a copy of your console logs.",
            serverErrorName = "CM_LOAD_FAILURE_NO_BANNER_VIEW",
        )

        @Serializable
        object NetworkingError : LoadError(
            "CM_330",
            cause = "Ad request failed due to a networking error.",
            resolution = "Typically this error should resolve itself. If the error persists, contact Chartboost Mediation support and share a copy of your network traffic logs.",
            serverErrorName = "CM_LOAD_FAILURE_NETWORKING_ERROR",
        )

        @Serializable
        object ChartboostMediationNotInitialized : LoadError(
            "CM_331",
            cause = "The Chartboost Mediation SDK was not initialized.",
            resolution = "Ensure the Chartboost Mediation SDK is initialized before loading ads.",
            serverErrorName = "CM_LOAD_FAILURE_CHARTBOOST_MEDIATION_NOT_INITIALIZED",
        )

        @Serializable
        object OSVersionNotSupported : LoadError(
            "CM_332",
            cause = "The partner does not support this OS version.",
            resolution = "This is an expected error and can be ignored. Devices running newer OS versions should work fine.",
            serverErrorName = "CM_LOAD_FAILURE_OS_VERSION_NOT_SUPPORTED",
        )

        @Serializable
        object ServerError : LoadError(
            "CM_333",
            cause = "The load request failed due to a server error.",
            resolution = "If this problem persists, reach out to Chartboost Mediation Support and/or the mediation partner team for further assistance. If possible, always share a copy of your network traffic logs.",
            serverErrorName = "CM_LOAD_FAILURE_SERVER_ERROR",
        )

        @Serializable
        object InvalidCredentials : LoadError(
            "CM_334",
            cause = "Invalid/empty credentials were supplied to load the ad.",
            resolution = "Ensure appropriate fields are correctly entered on the partner dashboard.",
            serverErrorName = "CM_LOAD_FAILURE_INVALID_CREDENTIALS",
        )

        @Serializable
        object WaterfallExhaustedNoFill : LoadError(
            "CM_335",
            cause = "All waterfall entries have resulted in an error or no fill.",
            resolution = "Try again. If the problem persists, verify Partner settings in the Chartboost Mediation dashboard.",
            serverErrorName = "CM_LOAD_FAILURE_WATERFALL_EXHAUSTED_NO_FILL",
        )
    }

    /**
     * 4XX: Show Errors
     *
     * @param code The error code.
     * @param message The error message.
     * @param cause The cause of the error.
     * @param resolution The resolution to the error.
     */
    @Serializable
    sealed class ShowError(
        @SerialName("showError_code") override val code: String,
        @SerialName("showError_message") override val message: String = "Ad show has failed.",
        @SerialName("showError_cause") override val cause: String,
        @SerialName("showError_resolution") override val resolution: String,
        @SerialName("showError_serverErrorName") override val serverErrorName: String,
    ) : ChartboostMediationError(code, message, cause, resolution, serverErrorName) {
        @Serializable
        object Unknown : ShowError(
            "CM_400",
            cause = "There was an error that was not accounted for.",
            resolution = "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs.",
            serverErrorName = "CM_SHOW_FAILURE_UNKNOWN",
        )

        @Serializable
        object ActivityNotFound : ShowError(
            "CM_401",
            cause = "There is no Activity to show the ad in.",
            resolution = "Ensure that the Context passed to ChartboostMediationSdk.start() is an Activity.",
            serverErrorName = "CM_SHOW_FAILURE_ACTIVITY_NOT_FOUND",
        )

        @Serializable
        object AdBlockerDetected : ShowError(
            "CM_402",
            cause = "An ad blocker was detected.",
            resolution = "N/A.",
            serverErrorName = "CM_SHOW_FAILURE_AD_BLOCKER_DETECTED",
        )

        @Serializable
        object AdNotFound : ShowError(
            "CM_403",
            cause = "An ad that might have been cached is no longer available to show.",
            resolution = "Try loading another ad but be mindful of CM_LOAD_FAILURE_RATE_LIMITED.",
            serverErrorName = "CM_SHOW_FAILURE_AD_NOT_FOUND",
        )

        @Serializable
        object AdExpired : ShowError(
            "CM_404",
            cause = "The ad was expired by the partner SDK after a set time window.",
            resolution = "Try loading another ad but be mindful of CM_LOAD_FAILURE_RATE_LIMITED.",
            serverErrorName = "CM_SHOW_FAILURE_AD_EXPIRED",
        )

        @Serializable
        object AdNotReady : ShowError(
            "CM_405",
            cause = "There is no ad ready to show.",
            resolution = "Try loading another ad and ensure it is ready before it's shown.",
            serverErrorName = "CM_SHOW_FAILURE_AD_NOT_READY",
        )

        @Serializable
        object AdapterNotFound : ShowError(
            "CM_406",
            cause = "The adapter instance responsible for this show operation is no longer in memory.",
            resolution = "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_SHOW_FAILURE_ADAPTER_NOT_FOUND",
        )

        @Serializable
        object InvalidChartboostMediationPlacement : ShowError(
            "CM_407",
            cause = "The Chartboost placement is invalid or empty.",
            resolution = "Ensure the Chartboost placement is properly defined on the Chartboost Mediation dashboard.",
            serverErrorName = "CM_SHOW_FAILURE_INVALID_CHARTBOOST_MEDIATION_PLACEMENT",
        )

        @Serializable
        object InvalidPartnerPlacement : ShowError(
            "CM_408",
            cause = "The partner placement is invalid or empty.",
            resolution = "Ensure the partner placement is properly defined on the Chartboost Mediation dashboard.",
            serverErrorName = "CM_SHOW_FAILURE_INVALID_PARTNER_PLACEMENT",
        )

        @Serializable
        object MediaBroken : ShowError(
            "CM_409",
            cause = "The media associated with this ad is corrupt and cannot be rendered.",
            resolution = "Try loading another ad. If this problem persists, contact the mediation partner and provide a copy of your console and network traffic logs.",
            serverErrorName = "CM_SHOW_FAILURE_MEDIA_BROKEN",
        )

        @Serializable
        object NoConnectivity : ShowError(
            "CM_410",
            cause = "No Internet connectivity was available.",
            resolution = "Ensure there is Internet connectivity and try again.",
            serverErrorName = "CM_SHOW_FAILURE_NO_CONNECTIVITY",
        )

        @Serializable
        object NoFill : ShowError(
            "CM_411",
            cause = "There is no ad inventory at this time.",
            resolution = "Try loading another ad but be mindful of CM_LOAD_FAILURE_RATE_LIMITED.",
            serverErrorName = "CM_SHOW_FAILURE_NO_FILL",
        )

        @Serializable
        object NotInitialized : ShowError(
            "CM_412",
            cause = "The partner was not able to call its show APIs because it was not initialized, either because you have explicitly skipped its initialization or there were issues initializing it.",
            resolution = "If you would like to load and show ads from this partner, allow it to initialize or try to re-initialize it.",
            serverErrorName = "CM_SHOW_FAILURE_NOT_INITIALIZED",
        )

        @Serializable
        object PartnerNotIntegrated : ShowError(
            "CM_413",
            cause = "The partner adapter and/or SDK might not have been properly integrated.",
            resolution = "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build.",
            serverErrorName = "CM_SHOW_FAILURE_PARTNER_NOT_INTEGRATED",
        )

        @Serializable
        object ShowInProgress : ShowError(
            "CM_414",
            cause = "An ad is already showing.",
            resolution = "You cannot show multiple fullscreen ads simultaneously. Wait until the current ad is done showing before showing another ad.",
            serverErrorName = "CM_SHOW_FAILURE_SHOW_IN_PROGRESS",
        )

        @Serializable
        object Timeout : ShowError(
            "CM_415",
            cause = "The show operation has taken too long to complete.",
            resolution = "If this issue persists, contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs.",
            serverErrorName = "CM_SHOW_FAILURE_TIMEOUT",
        )

        @Serializable
        object VideoPlayerError : ShowError(
            "CM_416",
            cause = "There was an error with the video player.",
            resolution = "Contact Chartboost Mediation Support or the mediation partner and provide details of your integration.",
            serverErrorName = "CM_SHOW_FAILURE_VIDEO_PLAYER_ERROR",
        )

        @Serializable
        object PrivacyOptIn : ShowError(
            "CM_417",
            cause = "One or more privacy settings have been opted in.",
            resolution = "N/A.",
            serverErrorName = "CM_SHOW_FAILURE_PRIVACY_OPT_IN",
        )

        @Serializable
        object PrivacyOptOut : ShowError(
            "CM_418",
            cause = "One or more privacy settings have been opted out.",
            resolution = "N/A.",
            serverErrorName = "CM_SHOW_FAILURE_PRIVACY_OPT_OUT",
        )

        @Serializable
        object WrongResourceType : ShowError(
            "CM_419",
            cause = "A resource was found but it doesn't match the ad type to be shown.",
            resolution = "This is an internal error. Typically, it should resolve itself. If this issue persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_SHOW_FAILURE_WRONG_RESOURCE_TYPE",
        )

        @Serializable
        object UnsupportedAdFormat : ShowError(
            "CM_420",
            cause = "The ad format is not supported by the partner SDK.",
            resolution = "Try again with a different ad format. If the ad format you are requesting for is supported by the partner, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_SHOW_FAILURE_UNSUPPORTED_AD_FORMAT",
        )

        @Serializable
        object Exception : ShowError(
            "CM_421",
            cause = "An exception was thrown during ad show.",
            resolution = "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_SHOW_FAILURE_EXCEPTION",
        )

        @Serializable
        object UnsupportedAdSize : ShowError(
            "CM_422",
            cause = "The ad size is not supported by the partner SDK.",
            resolution = "If this issue persists, contact the mediation partner and provide a copy of your console log.",
            serverErrorName = "CM_SHOW_FAILURE_UNSUPPORTED_AD_SIZE",
        )

        @Serializable
        object InvalidBannerSize : ShowError(
            "CM_423",
            cause = "The supplied banner size is invalid.",
            resolution = "Ensure the requested banner size is valid.",
            serverErrorName = "CM_SHOW_FAILURE_INVALID_BANNER_SIZE",
        )
    }

    /**
     * 5XX: Invalidate Errors
     *
     * @param code The error code.
     * @param message The error message.
     * @param cause The cause of the error.
     * @param resolution The resolution to the error.
     */
    @Serializable
    sealed class InvalidateError(
        @SerialName("invalidateError_code") override val code: String,
        @SerialName("invalidateError_message") override val message: String = "Ad invalidation has failed.",
        @SerialName("invalidateError_cause") override val cause: String,
        @SerialName("invalidateError_resolution") override val resolution: String,
        @SerialName("invalidateError_serverErrorName") override val serverErrorName: String,
    ) : ChartboostMediationError(code, message, cause, resolution, serverErrorName) {
        @Serializable
        object Unknown : InvalidateError(
            "CM_500",
            cause = "There was an error that was not accounted for.",
            resolution = "Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs.",
            serverErrorName = "CM_INVALIDATE_FAILURE_UNKNOWN",
        )

        @Serializable
        object AdNotFound : InvalidateError(
            "CM_501",
            cause = "There is no ad to invalidate.",
            resolution = "N/A.",
            serverErrorName = "CM_INVALIDATE_FAILURE_AD_NOT_FOUND",
        )

        @Serializable
        object AdapterNotFound : InvalidateError(
            "CM_502",
            cause = "The adapter instance responsible for this show operation is no longer in memory.",
            resolution = "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INVALIDATE_FAILURE_ADAPTER_NOT_FOUND",
        )

        @Serializable
        object NotInitialized : InvalidateError(
            "CM_503",
            cause = "The partner was not able to call its invalidate APIs because it was not initialized, either because you have explicitly skipped its initialization or there were issues initializing it.",
            resolution = "If this network supports ad invalidation and you have explicitly skipped its initialization, allow it to initialize. Otherwise, try to re-initialize it.",
            serverErrorName = "CM_INVALIDATE_FAILURE_NOT_INITIALIZED",
        )

        @Serializable
        object PartnerNotIntegrated : InvalidateError(
            "CM_504",
            cause = "The partner adapter and/or SDK might not have been properly integrated.",
            resolution = "Check your adapter/SDK integration. If this error persists, contact Chartboost Mediation Support and provide a minimal reproducible build.",
            serverErrorName = "CM_INVALIDATE_FAILURE_PARTNER_NOT_INTEGRATED",
        )

        @Serializable
        object Timeout : InvalidateError(
            "CM_505",
            cause = "The invalidate operation has taken too long to complete.",
            resolution = "If this issue persists, contact Chartboost Mediation Support and/or the mediation partner and provide a copy of your console logs.",
            serverErrorName = "CM_INVALIDATE_FAILURE_TIMEOUT",
        )

        @Serializable
        object WrongResourceType : InvalidateError(
            "CM_506",
            cause = "A resource was found but it doesn't match the ad type to be invalidated.",
            resolution = "This is an internal error. Typically, it should resolve itself. If this issue persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INVALIDATE_FAILURE_WRONG_RESOURCE_TYPE",
        )

        @Serializable
        object Exception : InvalidateError(
            "CM_507",
            cause = "An exception was thrown during ad invalidation.",
            resolution = "Check your console logs for more details. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INVALIDATE_FAILURE_EXCEPTION",
        )

        @Serializable
        object UnsupportedAdFormat : InvalidateError(
            "CM_508",
            cause = "The ad format is not supported by the partner SDK.",
            resolution = "Try again with a different ad format. If the ad format you are requesting for is supported by the partner, contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INVALIDATE_UNSUPPORTED_AD_FORMAT",
        )
    }

    /**
     * 6XX: Other Errors
     *
     * @param code The error code.
     * @param message The error message.
     * @param cause The cause of the error.
     * @param resolution The resolution to the error.
     */
    @Serializable
    sealed class OtherError(
        @SerialName("otherError_code") override val code: String,
        @SerialName("otherError_message") override val message: String = "An unknown error has occurred.",
        @SerialName("otherError_cause") override val cause: String,
        @SerialName("otherError_resolution") override val resolution: String,
        @SerialName("otherError_serverErrorName") override val serverErrorName: String,
    ) : ChartboostMediationError(code, message, cause, resolution, serverErrorName) {
        @Serializable
        object Unknown : OtherError(
            code = "CM_600",
            message = "It is unclear if it originates from Chartboost Mediation or mediation partner(s).",
            cause = "There is no known cause.",
            resolution = "No information is available about this error.",
            serverErrorName = "CM_UNKNOWN_ERROR",
        )

        @Serializable
        object PartnerError : OtherError(
            code = "CM_601",
            message = "The partner has returned an error.",
            cause = "Unknown.",
            resolution = "The Chartboost Mediation SDK does not have insights into this type of error. Contact the mediation partner and provide details of your integration.",
            serverErrorName = "CM_PARTNER_ERROR",
        )

        @Serializable
        object InternalError : OtherError(
            code = "CM_602",
            message = "An internal error has occurred.",
            cause = "Unknown.",
            resolution = "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INTERNAL_ERROR",
        )

        @Serializable
        object NoConnectivity : OtherError(
            code = "CM_603",
            message = "No Internet connectivity was available.",
            cause = "Unknown.",
            resolution = "Ensure there is Internet connectivity and try again.",
            serverErrorName = "CM_NO_CONNECTIVITY",
        )

        @Serializable
        object AdServerError : OtherError(
            code = "CM_604",
            message = "An ad server issue has occurred.",
            cause = "Unknown.",
            resolution = "This is an internal error. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_AD_SERVER_ERROR",
        )

        @Serializable
        object InvalidArgument : OtherError(
            code = "CM_605",
            message = "Invalid/empty arguments were passed to the function call, which caused the function to terminate prematurely.",
            cause = "Unknown.",
            resolution = "Depending on when this error occurs, it could be due to an issue in Chartboost Mediation or mediation partner(s) or your integration. Contact Chartboost Mediation Support and provide a copy of your console logs.",
            serverErrorName = "CM_INVALID_ARGUMENT",
        )

        @Serializable
        object PreinitializationActionFailed : OtherError(
            "CM_606",
            "Requested action failed because it needs to be performed before initializing Chartboost Mediation.",
            "Requested action needs to be performed before initializing Chartboost Mediation.",
            "Perform the action before initializing Chartboost Mediation.",
            serverErrorName = "CM_PREINITIALIZATION_ACTION_FAILED",
        )
    }
}

/**
 * Automate the process of registering all the error subclasses of each sealed class with their
 * serializers.
 *
 * @param T The sealed class to register
 * @receiver The [SerializersModuleBuilder] to register the serializers with.
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> SerializersModuleBuilder.registerAllSubclasses() {
    val kClass = T::class
    val subClasses = kClass.sealedSubclasses

    for (subClass in subClasses) {
        val serializer = subClass.serializer()

        @Suppress("UNCHECKED_CAST")
        (this::class.members.find { it.name == "subclass" } as? (KClass<*>, KSerializer<*>) -> Unit)
            ?.invoke(subClass, serializer)
    }
}

/**
 * The [SerializersModule] for the [ChartboostMediationError] sealed classes.
 */
val errorSerializersModule =
    SerializersModule {
        polymorphic(ChartboostMediationError::class) {
            registerAllSubclasses<ChartboostMediationError>()
        }
    }
