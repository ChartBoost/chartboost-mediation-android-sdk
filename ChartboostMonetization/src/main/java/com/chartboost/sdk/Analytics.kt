package com.chartboost.sdk

import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.logging.Logger
import java.util.*

/**
 * The Analytics class is used to track post-install analytics events. We expose the following set
 * of API methods to developers to send and track events on our dashboard:
 *
 * 1) trackInAppPurchaseEvent: (Public: Open to developers)
 * 2) trackMiscRevenueGeneratingEventOfType: (Private: Not open for now, but will be made available soon)
 * 3) trackCustomEventOfType: (Private: Not open for now, but will be made available soon)
 * 4) trackInGameScore: (Private: Not open for now, but will be made available soon)
 * 5) trackPlayerCurrencyBalance: (Private: Not open for now, but will be made available soon)
 */
object Analytics {
    /**
     * Tracks an in-app purchase made on the Google Play Store.
     *
     * @param title Title of the product being purchased (e.g., Sword).
     * @param description Description of the product being purchased (e.g., Platinum sword).
     * @param price Cost of the product being purchased (e.g., "$0.99").
     * @param currency Currency in which the product was purchased (e.g., USD).
     * @param productID Unique productID of the purchased item (e.g., com.chartboost.sword).
     * @param purchaseData Unique string obtained from Google Play upon purchase.
     * @param purchaseSignature Unique string obtained from Google Play upon purchase.
     */
    @JvmStatic
    fun trackInAppGooglePlayPurchaseEvent(
        title: String,
        description: String,
        price: String,
        currency: String,
        productID: String,
        purchaseData: String?,
        purchaseSignature: String?,
    ) {
        if (!Chartboost.isSdkStarted()) {
            Logger.e("You need to call Chartboost.startWithAppId() before tracking in-app purchases")
            return
        }

        ChartboostDependencyContainer.sdkComponent.analyticsApi.trackInAppPurchaseEvent(
            productID,
            title,
            description,
            price,
            currency,
            purchaseData,
            purchaseSignature,
            null,
            null,
            IAPType.GOOGLE_PLAY,
        )
    }

    /**
     * Tracks an in-app purchase made on the Amazon Appstore.
     *
     * @param title Title of the product being purchased (e.g., Sword).
     * @param description Description of the product being purchased (e.g., Platinum sword).
     * @param price Cost of the product being purchased (e.g., "0.99").
     * @param currency Currency in which the product was purchased (e.g., USD).
     * @param productID Unique productID of the purchased item (e.g., com.chartboost.sword).
     * @param userID User ID used in purchasing the product.
     * @param purchaseToken Unique string obtained from the Amazon Appstore upon purchase.
     */
    @JvmStatic
    fun trackInAppAmazonStorePurchaseEvent(
        title: String,
        description: String,
        price: String,
        currency: String,
        productID: String,
        userID: String?,
        purchaseToken: String?,
    ) {
        if (!Chartboost.isSdkStarted()) {
            Logger.e("You need to call Chartboost.startWithAppId() before tracking in-app purchases")
            return
        }

        ChartboostDependencyContainer.sdkComponent.analyticsApi.trackInAppPurchaseEvent(
            productID,
            title,
            description,
            price,
            currency,
            null,
            null,
            userID,
            purchaseToken,
            IAPType.AMAZON,
        )
    }

    /**
     * Tracks an in-app purchase event.
     *
     * @param iAPPurchaseInfoMap Enum hash map where the key is {@link IAPPurchaseInfo} and the value is the purchase information of the product.
     * @param iapType Type of in-app purchase being used, from the enum value.
     *
     * Example: Google IAP
     * ```
     * val map = EnumMap<IAPPurchaseInfo, String>(IAPPurchaseInfo::class.java)
     * map[IAPPurchaseInfo.PRODUCT_ID] = "Provide product id from the purchase made"
     * map[IAPPurchaseInfo.PRODUCT_TITLE] = "Provide product title from the purchase made"
     * map[IAPPurchaseInfo.PRODUCT_DESCRIPTION] = "Provide product description from the purchase made"
     * map[IAPPurchaseInfo.PRODUCT_PRICE] = "Provide product price from the purchase made"
     * map[IAPPurchaseInfo.PRODUCT_CURRENCY_CODE] = "Provide currency code of the purchase"
     * map[IAPPurchaseInfo.GOOGLE_PURCHASE_DATA] = "Provide purchaseData object of the purchase"
     * map[IAPPurchaseInfo.GOOGLE_PURCHASE_SIGNATURE] = "Provide purchaseSignature of the purchase"
     * ```
     *
     * Example: Amazon IAP
     * ```
     * val map = EnumMap<IAPPurchaseInfo, String>(IAPPurchaseInfo::class.java)
     * map[IAPPurchaseInfo.PRODUCT_ID] = "Provide product id from the purchase made"
     * map[IAPPurchaseInfo.PRODUCT_TITLE] = "Provide product title from the purchase made"
     * map[IAPPurchaseInfo.PRODUCT_DESCRIPTION] = "Provide product description from the purchase made"
     * map[IAPPurchaseInfo.PRODUCT_PRICE] = "Provide product price from the purchase made"
     * map[IAPPurchaseInfo.PRODUCT_CURRENCY_CODE] = "Provide currency code of the purchase"
     * map[IAPPurchaseInfo.AMAZON_USER_ID] = "Provide amazon user id of the purchase"
     * map[IAPPurchaseInfo.AMAZON_PURCHASE_TOKEN] = "Provide purchaseToken from the receipt of the purchase"
     * ```
     */
    @JvmStatic
    fun trackInAppPurchaseEvent(
        iAPPurchaseInfoMap: HashMap<IAPPurchaseInfo, String>,
        iapType: IAPType,
    ) {
        if (!Chartboost.isSdkStarted()) {
            Logger.e("You need to call Chartboost.startWithAppId() before tracking in-app purchases")
            return
        }

        // Google receipt parameters
        val purchaseData = iAPPurchaseInfoMap[IAPPurchaseInfo.GOOGLE_PURCHASE_DATA]
        val purchaseSignature = iAPPurchaseInfoMap[IAPPurchaseInfo.GOOGLE_PURCHASE_SIGNATURE]

        // Amazon receipt parameters
        val amzUserId = iAPPurchaseInfoMap[IAPPurchaseInfo.AMAZON_USER_ID]
        val amzPurchaseToken = iAPPurchaseInfoMap[IAPPurchaseInfo.AMAZON_PURCHASE_TOKEN]

        // Non-nullable params
        val productId = iAPPurchaseInfoMap[IAPPurchaseInfo.PRODUCT_ID]
        val productTitle = iAPPurchaseInfoMap[IAPPurchaseInfo.PRODUCT_TITLE]
        val productDescription = iAPPurchaseInfoMap[IAPPurchaseInfo.PRODUCT_DESCRIPTION]
        val productPrice = iAPPurchaseInfoMap[IAPPurchaseInfo.PRODUCT_PRICE]
        val productCurrencyCode = iAPPurchaseInfoMap[IAPPurchaseInfo.PRODUCT_CURRENCY_CODE]

        if (productId.isNullOrEmpty() ||
            productTitle.isNullOrEmpty() ||
            productDescription.isNullOrEmpty() ||
            productPrice.isNullOrEmpty() ||
            productCurrencyCode.isNullOrEmpty()
        ) {
            Logger.e("Null object is passed. Please pass a valid value object")
            return
        }

        ChartboostDependencyContainer.sdkComponent.analyticsApi.trackInAppPurchaseEvent(
            productId,
            productTitle,
            productDescription,
            productPrice,
            productCurrencyCode,
            purchaseData,
            purchaseSignature,
            amzUserId,
            amzPurchaseToken,
            iapType,
        )
    }

    /**
     * Tracks miscellaneous revenue generating events.
     *
     * @param eventType One of the types defined in the MiscRevenueGeneratingEventType enum.
     * @param amount Amount generated.
     * @param currency Three-digit code (ISO 4217) representing the currency used.
     * @param source Source of the revenue.
     */
    /**
     * private static synchronized void trackMiscRevenueGeneratingEvent(MiscRevenueGeneratingEventType eventType, double amount, String currency, String source) {
     *     if (eventType == null || currency == null || source == null) {
     *         Logging.e("Misc revenue event field invalid");
     *         return;
     *     }
     *     JSONWrapper obj = Dictionary(JKV("type", eventType.toString()),
     *                                 JKV("amount", amount),
     *                                 JKV("currency", currency),
     *                                 JKV("source", source));
     *     sendTrackingRequest(obj, Constants.END_POINT_TYPE_PIT_MISC_REVENUE);
     * }
     */

    /**
     * Tracks a custom event.
     *
     * @param eventType One of the types defined in the CustomEventType enum.
     * @param index Use this integer to specify where/when the event happened.
     */
    /**
     * private static synchronized void trackCustomEvent(CustomEventType eventType, int index) {
     *     trackCustomEvent(eventType, index, "default");
     * }
     */

    /**
     * Tracks a custom event with additional information.
     *
     * @param eventType One of the types defined in the CustomEventType enum.
     * @param index Use this integer to specify where/when the event happened.
     * @param key Use this string to add more information about the event.
     */
    /**
     * private static synchronized void trackCustomEvent(CustomEventType eventType, int index, String key) {
     *     if (eventType == null || key == null) {
     *         Logging.e("Custom Event field invalid");
     *         return;
     *     }
     *     JSONWrapper obj = Dictionary(JKV("type", eventType.toString()),
     *                                 JKV("key", key),
     *                                 JKV("index", index));
     *     sendTrackingRequest(obj, Constants.END_POINT_TYPE_PIT_INGAME_EVENT);
     * }
     */

    /**
     * Tracks in-game score.
     *
     * @param score Score achieved.
     */
    /**
     * private static synchronized void trackInGameScore(float score) {
     *     trackInGameScore(score, 0);
     * }
     */

    /**
     * Tracks in-game score with additional information.
     *
     * @param score Score achieved.
     * @param index Use this integer to specify where/when the event happened.
     */
    /**
     * private static synchronized void trackInGameScore(float score, int index) {
     *     JSONWrapper obj = Dictionary(JKV("score", score),
     *                                 JKV("index", index));
     *     sendTrackingRequest(obj, Constants.END_POINT_TYPE_PIT_GAME_SCORE);
     * }
     */

    /**
     * Tracks the current in-game currency balance.
     *
     * @param balance Current in-game currency balance.
     * @param inGameCurrency Type of currency.
     */
    /**
     * private static synchronized void trackPlayerCurrencyBalance(float balance, String inGameCurrency) {
     *     trackPlayerCurrencyBalance(balance, inGameCurrency, 0);
     * }
     */

    /**
     * Tracks the current in-game currency balance with additional information.
     *
     * @param balance Current in-game currency balance.
     * @param inGameCurrency Type of currency.
     * @param index Use this integer to specify where/when the event happened.
     */
    /**
     * private static synchronized void trackPlayerCurrencyBalance(float balance, String inGameCurrency, int index) {
     *     if (inGameCurrency == null) {
     *         Logging.e("Currency Balance field invalid");
     *         return;
     *     }
     *     JSONWrapper obj = Dictionary(JKV("balance", balance),
     *                                 JKV("in-game-currency", inGameCurrency),
     *                                 JKV("index", index));
     *     sendTrackingRequest(obj, Constants.END_POINT_TYPE_PIT_INGAME_CURRENCY);
     * }
     */

    /**
     * Sends current level information details for the game played.
     *
     * @param eventLabel Event label for the level information.
     * @param type Type of level.
     * @param mainLevel Level number for the current level.
     * @param description Description of the level information.
     */
    @JvmStatic
    fun trackLevelInfo(
        eventLabel: String,
        type: LevelType,
        mainLevel: Int,
        description: String,
    ) {
        trackLevelInfo(eventLabel, type, mainLevel, 0, description)
    }

    /**
     * Sends current level information details for the game played.
     *
     * @param eventLabel Event label for the level information.
     * @param type Type of level.
     * @param mainLevel Level number for the current level.
     * @param subLevel Current sub-level number.
     * @param description Description of the level information.
     */
    @JvmStatic
    fun trackLevelInfo(
        eventLabel: String,
        type: LevelType,
        mainLevel: Int,
        subLevel: Int,
        description: String,
    ) {
        if (!Chartboost.isSdkStarted()) {
            Logger.e("You need to call Chartboost.startWithAppId() before tracking in-app purchases")
            return
        }
        ChartboostDependencyContainer.sdkComponent.analyticsApi.trackLevelInfo(
            eventLabel,
            type,
            mainLevel,
            subLevel,
            description,
            System.currentTimeMillis(),
        )
    }

    /** Types of in-app purchases supported. */
    enum class IAPType {
        GOOGLE_PLAY,
        AMAZON,
    }

    /**
     * Enum constants used as keys for sending purchase information during in-app purchases and post-install analytics tracking.
     *
     * PRODUCT_ID :  Product ID for the product (unique SKU id assigned for every product).
     * PRODUCT_TITLE: Title of the product.
     * PRODUCT_DESCRIPTION: Description of the product.
     * PRODUCT_PRICE: Price of the product purchased (e.g., "$0.99").
     * PRODUCT_CURRENCY_CODE: Currency code of the product purchased (e.g., "USD" or "INR").
     * GOOGLE_PURCHASE_DATA: Only applicable to Google Play purchases. This value is obtained from the purchase object received from Google Play upon purchase.
     * GOOGLE_PURCHASE_SIGNATURE: Only applicable to Google Play purchases. This value is obtained from the purchase object received from Google Play upon purchase.
     * AMAZON_PURCHASE_TOKEN: Only applicable to Amazon purchases. This value is the 'purchase token' from the receipt object of the purchase made through Amazon.
     * AMAZON_USER_ID: User ID associated with the product purchase (only applicable to Amazon purchases).
     */
    enum class IAPPurchaseInfo {
        PRODUCT_ID,
        PRODUCT_TITLE,
        PRODUCT_DESCRIPTION,
        PRODUCT_PRICE,
        PRODUCT_CURRENCY_CODE,
        GOOGLE_PURCHASE_DATA,
        GOOGLE_PURCHASE_SIGNATURE,
        AMAZON_PURCHASE_TOKEN,
        AMAZON_USER_ID,
    }

    /**
     * Enum values representing different types of level tracking information.
     */
    enum class LevelType(val levelType: Int) {
        HIGHEST_LEVEL_REACHED(1),
        CURRENT_AREA(2),
        CHARACTER_LEVEL(3),
        OTHER_SEQUENTIAL(4),
        OTHER_NONSEQUENTIAL(5),
    }

    /**
     * Enum values for custom event types.
     */
    enum class CustomEventType {
        CustomEventType1,
        CustomEventType2,
        CustomEventType3,
    }

    /**
     * Enum values for miscellaneous revenue generating event types.
     */
    enum class MiscRevenueGeneratingEventType {
        MiscRevenueGeneratingEventType1,
        MiscRevenueGeneratingEventType2,
        MiscRevenueGeneratingEventType3,
    }
}
