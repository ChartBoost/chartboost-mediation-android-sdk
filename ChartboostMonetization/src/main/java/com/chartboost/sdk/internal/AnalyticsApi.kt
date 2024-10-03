package com.chartboost.sdk.internal

import android.text.TextUtils
import android.util.Base64
import com.chartboost.sdk.Analytics
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.initialization.SdkInitializer
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.EventTracker
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.regex.Pattern

private const val KEY_IAP_PRODUCT_PURCHASE_DATA = "purchaseData"
private const val KEY_IAP_PRODUCT_PURCHASE_SIGNATURE = "purchaseSignature"
private const val KEY_IAP_PRODUCT_PURCHASE_TYPE = "type"

/**
 * IAP KEY Constants
 */
private const val KEY_IAP_PRODUCT_ID = "productID"
private const val KEY_IAP_PRODUCT_TITLE = "localized-title"
private const val KEY_IAP_PRODUCT_DESCRIPTION = "localized-description"
private const val KEY_IAP_PRODUCT_PRICE = "price"
private const val KEY_IAP_PRODUCT_CURRENCY = "currency"
private const val KEY_IAP_PRODUCT_RECEIPT = "receipt"

private const val KEY_IAP_PRODUCT_USERID = "userID"
private const val KEY_IAP_PRODUCT_PURCHASE_TOKEN = "purchaseToken"

/**
 * PIA LEVEL TRACKING CONSTANTS
 */
private const val KEY_PIA_LEVEL_TRACKING = "level_info"
private const val KEY_PIA_LEVEL_TRACKING_EVENT_LABEL = "event_label"
private const val KEY_PIA_LEVEL_TRACKING_EVENT_FIELD = "event_field"
private const val KEY_PIA_LEVEL_TRACKING_MAIN_LEVEL = "main_level"
private const val KEY_PIA_LEVEL_TRACKING_SUB_LEVEL = "sub_level"
private const val KEY_PIA_LEVEL_TRACKING_DESCRIPTION = "description"
private const val KEY_PIA_LEVEL_TRACKING_TIMESTAMP = "timestamp"
private const val KEY_PIA_LEVEL_TRACKING_DATA_TYPE = "data_type"

internal class AnalyticsApi(
    private val sdkInitializer: SdkInitializer,
    private val networkService: CBNetworkService,
    private val requestBodyBuilder: RequestBodyBuilder,
    private val eventTracker: EventTracker,
) {
    fun trackLevelInfo(
        eventLabel: String,
        type: Analytics.LevelType,
        mainLevel: Int,
        subLevel: Int,
        description: String,
        timestamp: Long,
    ) {
        try {
            if (!isSdkStarted()) {
                Logger.e(
                    "You need call Chartboost.startWithAppId() before tracking in-app purchases",
                )
                return
            }

            if (eventLabel.isEmpty()) {
                Logger.e("Invalid value: event label cannot be empty or null")
                return
            }

            if (mainLevel < 0 || subLevel < 0) {
                Logger.e("Invalid value: Level number should be > 0")
                return
            }

            if (description.isEmpty()) {
                Logger.e("Invalid value: description cannot be empty or null")
                return
            }

            val levelInfoArray = JSONArray()
            val levelInfo = JSONObject()
            levelInfo.put(KEY_PIA_LEVEL_TRACKING_EVENT_LABEL, eventLabel)
            levelInfo.put(KEY_PIA_LEVEL_TRACKING_EVENT_FIELD, type.levelType)
            levelInfo.put(KEY_PIA_LEVEL_TRACKING_MAIN_LEVEL, mainLevel)
            levelInfo.put(KEY_PIA_LEVEL_TRACKING_SUB_LEVEL, subLevel)
            levelInfo.put(KEY_PIA_LEVEL_TRACKING_DESCRIPTION, description)
            levelInfo.put(KEY_PIA_LEVEL_TRACKING_TIMESTAMP, timestamp)
            levelInfo.put(KEY_PIA_LEVEL_TRACKING_DATA_TYPE, KEY_PIA_LEVEL_TRACKING)
            levelInfoArray.put(levelInfo)
            sendTrackingRequestTrack(levelInfoArray)
        } catch (e: Exception) {
            Logger.e("", e)
        }
    }

    fun trackInAppPurchaseEvent(
        productID: String,
        title: String,
        description: String,
        price: String,
        currency: String,
        purchaseData: String?,
        purchaseSignature: String?,
        userID: String?,
        purchaseToken: String?,
        iapType: Analytics.IAPType,
    ) {
        try {
            if (!isSdkStarted()) {
                Logger.e(
                    "You need call Chartboost.startWithAppId() before tracking in-app purchases",
                )
                return
            }

            val cost = calculateCost(price)
            if (cost == -1f) {
                return
            }

            // Do the receipt check and create a receipt object based on the type of In-App purchase
            val receipt =
                when (iapType) {
                    Analytics.IAPType.GOOGLE_PLAY -> {
                        buildReceiptGooglePlay(purchaseData, purchaseSignature)
                    }
                    Analytics.IAPType.AMAZON -> {
                        buildReceiptAmazon(userID, purchaseToken)
                    }
                }

            // Encode the receipt into bas64 encrypted string
            if (receipt.length() == 0) {
                Logger.e("Error while parsing the receipt to a JSON Object")
                return
            }

            val receiptString = Base64.encodeToString(receipt.toString().toByteArray(), Base64.NO_WRAP)

            // Create a JSON object to send all the info to the server
            val iapEvent = JSONObject()
            iapEvent.put(KEY_IAP_PRODUCT_TITLE, title)
            iapEvent.put(KEY_IAP_PRODUCT_DESCRIPTION, description)
            iapEvent.put(KEY_IAP_PRODUCT_PRICE, cost)
            iapEvent.put(KEY_IAP_PRODUCT_CURRENCY, currency)
            iapEvent.put(KEY_IAP_PRODUCT_ID, productID)
            iapEvent.put(KEY_IAP_PRODUCT_RECEIPT, receiptString)
            sendTrackingRequestIap(iapEvent)
        } catch (e: Exception) {
            Logger.e("", e)
        }
    }

    private fun isSdkStarted(): Boolean {
        return sdkInitializer.isSdkInitialized()
    }

    private fun buildReceiptGooglePlay(
        purchaseData: String?,
        purchaseSignature: String?,
    ): JSONObject {
        return if (purchaseData.isNullOrEmpty() || purchaseSignature.isNullOrEmpty()) {
            Logger.e("Null object is passed for for purchase data or purchase signature")
            JSONObject()
        } else {
            val receiptJson = JSONObject()
            receiptJson.put(KEY_IAP_PRODUCT_PURCHASE_DATA, purchaseData)
            receiptJson.put(KEY_IAP_PRODUCT_PURCHASE_SIGNATURE, purchaseSignature)
            receiptJson.put(KEY_IAP_PRODUCT_PURCHASE_TYPE, Analytics.IAPType.GOOGLE_PLAY.ordinal)
            receiptJson
        }
    }

    private fun buildReceiptAmazon(
        userID: String?,
        purchaseToken: String?,
    ): JSONObject {
        return if (userID.isNullOrEmpty() || purchaseToken.isNullOrEmpty()) {
            Logger.e("Null object is passed for for amazon user id or amazon purchase token")
            JSONObject()
        } else {
            val receiptJson = JSONObject()
            receiptJson.put(KEY_IAP_PRODUCT_USERID, userID)
            receiptJson.put(KEY_IAP_PRODUCT_PURCHASE_TOKEN, purchaseToken)
            receiptJson.put(KEY_IAP_PRODUCT_PURCHASE_TYPE, Analytics.IAPType.AMAZON.ordinal)
            receiptJson
        }
    }

    /** Extract the price as a number from the string using regex */
    private fun calculateCost(price: String): Float {
        /** Extract the price as a number from the string using regex */
        val cost: Float =
            try {
                val pattern = Pattern.compile("(\\d+\\.\\d+)|(\\d+)")
                val matcher = pattern.matcher(price)
                matcher.find()
                val result = matcher.group()
                if (TextUtils.isEmpty(result)) {
                    Logger.e("Invalid price object")
                    return -1f
                }
                result.toFloat()
            } catch (e: IllegalStateException) {
                Logger.e("Invalid price object", e)
                return -1f
            }
        return cost
    }

    private fun sendTrackingRequestIap(requestData: JSONObject) {
        val endpointType = CBConstants.END_POINT_TYPE_PIT_IAP
        val path = String.format(Locale.US, "%s%s", CBConstants.API_ENDPOINT_POST_INSTALL, endpointType)
        val request =
            CBRequest(
                CBConstants.API_ENDPOINT,
                path,
                requestBodyBuilder.build(),
                Priority.NORMAL,
                endpointType,
                null,
                eventTracker,
            )
        request.appendBodyArgument(endpointType, requestData)
        request.checkStatusInResponseBody = true
        networkService.submit(request)
    }

    private fun sendTrackingRequestTrack(requestData: JSONArray) {
        val path = CBConstants.API_ENDPOINT_POST_INSTALL + CBConstants.END_POINT_TYPE_PIA_TRACKING
        val request =
            CBRequest(
                CBConstants.API_ENDPOINT,
                path,
                requestBodyBuilder.build(),
                Priority.NORMAL,
                CBConstants.END_POINT_TYPE_PIA_TRACKING,
                null,
                eventTracker,
            )
        request.appendBodyArgument(CBConstants.KEY_PIA_LEVEL_TRACKING_TRACK_INFO, requestData)
        request.checkStatusInResponseBody = true
        networkService.submit(request)
    }
}
