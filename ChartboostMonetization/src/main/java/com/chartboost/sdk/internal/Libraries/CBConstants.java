package com.chartboost.sdk.internal.Libraries;

import com.chartboost.sdk.BuildConfig;

import java.util.concurrent.TimeUnit;

public interface CBConstants {
    String ASYNC_THREAD_PREFIX = "Chartboost Thread #";
    int ASYNC_MAX_THREADS = 2;
    int REQUEST_TIME_OUT = 1000 * 10;
    int NETWORK_REQUEST_SERVICE_THREADS = 4;
    int PENDING_IMPRESSION_EXPIRATION_MINUTES = 3;
    int DEFAULT_WEBVIEW_CACHE_MAX_BYTES = 100 * 1024 * 1024;
    int DEFAULT_WEBVIEW_CACHE_MAX_UNITS = 10;
    int DEFAULT_WEBVIEW_CACHE_TTL_SECONDS = (int) TimeUnit.DAYS.toSeconds(7);

    /**
     * CACHE DIRECTORY NAME CONSTANTS
     */

    String CACHE_DIR_CHARTBOOST_BASE = ".chartboost";

    /**
     * PROTOCOL CONSTANTS
     */

    String SDK_USERAGENT_BASE = "Chartboost-Android-SDK";
    String API_PROTOCOL = BuildConfig.API_PROTOCOL;

    /**
     * VERSION CONSTANTS
     */

    String API_VERSION = BuildConfig.API_VERSION;
    String SDK_VERSION = BuildConfig.SDK_VERSION;
    String RELEASE_COMMIT_HASH = BuildConfig.RELEASE_COMMIT_HASH;

    /**
     * API ENDPOINT CONSTANTS
     */

    String API_ENDPOINT = API_PROTOCOL + "://live.chartboost.com";

    String API_ENDPOINT_WEBVIEW_INTERSTITIAL_GET_FORMAT = "webview/%s/interstitial/get";
    String API_ENDPOINT_WEBVIEW_REWARD_GET_FORMAT = "webview/%s/reward/get";
    String API_ENDPOINT_TRACKING_DEFAULT = "https://ssp-events.chartboost.com/track/sdk";
    String API_ENDPOINT_POST_INSTALL = "/post-install-event/";

    /* END POINT TYPES*/

    /**
     * POST INSTALL TRACKING (PIT)
     */
    String END_POINT_TYPE_PIT_IAP = "iap";
    String END_POINT_TYPE_PIA_TRACKING = "tracking";

    /**
     * PIA REQUEST PARAM KEY TYPE
     */
    String KEY_PIA_LEVEL_TRACKING_TRACK_INFO = "track_info";

    /**
     * SHARED PREFERENCES CONSTANTS
     */
    String PREFERENCES_FILE_TRACKING = "cbPrefsTracking";
    String PREFERENCES_FILE_DEFAULT = "cbPrefs";
    String PREFERENCES_KEY_UUID = "cbUUID";
    String FILE_CACHE_ROOT_INTERNAL_SPACE = ".chartboost-internal-folder-size";

    /**
     * HTTP REQUEST CONSTANTS
     */

    /* Header key */
    String REQUEST_PARAM_APP_HEADER_KEY = "X-Chartboost-App";
    String REQUEST_PARAM_REACHABILITY_HEADER_KEY = "X-Chartboost-Reachability";
    String REQUEST_PARAM_SIGNATURE_HEADER_KEY = "X-Chartboost-Signature";
    String REQUEST_PARAM_ACCEPT_HEADER_KEY = "Accept";
    String REQUEST_PARAM_CLIENT_HEADER_KEY = "X-Chartboost-Client";
    String REQUEST_PARAM_HEADER_KEY = "X-Chartboost-API";
    String REQUEST_PARAM_HEADER_TEST_KEY = "X-Chartboost-Test";
    String REQUEST_PARAM_HEADER_DSP_KEY = "X-Chartboost-DspDemoApp";

    /* Header value */
    String REQUEST_PARAM_HEADER_VALUE = "application/json";

    /* Body param containers */
    String WEB_REQUEST_PARAM_CONTAINER_AD = "ad";
    String WEB_REQUEST_PARAM_CONTAINER_APP = "app";
    String WEB_REQUEST_PARAM_BID_REQUEST = "bidrequest";
    String WEB_REQUEST_PARAM_CONTAINER_DEVICE = "device";
    String WEB_REQUEST_PARAM_CONTAINER_SDK = "sdk";
    String WEB_REQUEST_PARAM_CARRIER_NAME = "carrier_name";
    String WEB_REQUEST_PARAM_MCC = "mobile_country_code";
    String WEB_REQUEST_PARAM_MNC = "mobile_network_code";
    String WEB_REQUEST_PARAM_ISO = "iso_country_code";
    String WEB_REQUEST_PARAM_PHONE_TYPE = "phone_type";
    String WEB_REQUEST_PARAM_CACHE = "cache";
    String WEB_REQUEST_PARAM_AMOUNT = "amount";
    String WEB_REQUEST_PARAM_RETRY_COUNT = "retry_count";
    String WEB_REQUEST_PARAM_SESSION_ID = "session_id";
    String WEB_REQUEST_PARAM_DEVICE_FAMILY = "device_family";
    String WEB_REQUEST_PARAM_RETINA = "retina";
    String WEB_REQUEST_PARAM_UI = "ui";
    String WEB_REQUEST_PARAM_TEST_MODE = "test_mode";

    /* Body param */
    String REQUEST_PARAM_VIDEO_CACHED = "video_cached";
    String REQUEST_PARAM_IMP_DEPTH = "imp_depth";
    String REQUEST_PARAM_LOCATION = "location";
    String REQUEST_PARAM_IDENTITY = "identity";
    String REQUEST_PARAM_LIMIT_AD_TRACKING = "limit_ad_tracking";
    String REQUEST_PARAM_PUBLISHER_LIMIT_AD_TRACKING = "pidatauseconsent";
    String REQUEST_PARAM_TCFV2_CONSENT = "consent";
    String REQUEST_PARAM_PRIVACY = "privacy";
    String REQUEST_PARAM_MODEL = "model";
    String REQUEST_PARAM_OS = "os";
    String REQUEST_PARAM_COUNTRY = "country";
    String REQUEST_PARAM_LANGUAGE = "language";
    String REQUEST_PARAM_VERSION = "bundle";
    String REQUEST_PARAM_PACKAGE = "bundle_id";
    String REQUEST_PARAM_SDK = "sdk";
    String REQUEST_PARAM_WIDTH = "w";
    String REQUEST_PARAM_HEIGHT = "h";
    String REQUEST_PARAM_DEVICE_WIDTH = "dw";
    String REQUEST_PARAM_DEVICE_HEIGHT = "dh";
    String REQUEST_PARAM_DEVICE_DPI = "dpi";
    String REQUEST_PARAM_TIMESTAMP = "timestamp";
    String REQUEST_PARAM_SCALE = "scale";
    String REQUEST_PARAM_APP = "app";
    String REQUEST_PARAM_DEVICE_MAKE = "make";
    String REQUEST_PARAM_DEVICE_TYPE = "device_type";
    String REQUEST_PARAM_ACTUAL_DEVICE_TYPE = "actual_device_type";
    String REQUEST_PARAM_REACHABILITY = "reachability";
    String REQUEST_PARAM_SESSION = "session";
    String REQUEST_PARAM_CARRIER = "carrier";
    String REQUEST_PARAM_CARRIER_NAME = "carrier-name";
    String REQUEST_PARAM_MCC = "mobile-country-code";
    String REQUEST_PARAM_MNC = "mobile-network-code";
    String REQUEST_PARAM_ISO = "iso-country-code";
    String REQUEST_PARAM_PHONE_TYPE = "phone-type";
    String REQUEST_PARAM_AD_ID = "ad_id";
    String REQUEST_PARAM_SET_ID_SCOPE = "appsetidscope";
    String REQUEST_PARAM_CACHED = "cached";
    String REQUEST_PARAM_ASSET_LIST = "cache_assets";
    String REQUEST_PARAM_IS_PORTRAIT = "is_portrait";
    String REQUEST_PARAM_MEDIATION = "mediation";
    String REQUEST_PARAM_MEDIATION_VERSION = "mediation_version";
    String REQUEST_PARAM_ADAPTER_VERSION = "adapter_version";
    String REQUEST_PARAM_TIMEZONE = "timezone";
    String REQUEST_PARAM_COMMIT_HASH = "commit_hash";
    String REQUEST_PARAM_CONFIG_VARIANT = "config_variant";
    String REQUEST_PARAM_USER_AGENT = "user_agent";
    String REQUEST_PARAM_RETARGET_REINSTALL = "retarget_reinstall";
    String REQUEST_PARAM_BID_REQUEST_APP = "app";
    String REQUEST_PARAM_BID_REQUEST_APP_VER = "ver";
    String REQUEST_PARAM_BID_REQUEST_REGS = "regs";
    String REQUEST_PARAM_BID_REQUEST_EXT = "ext";
    String REQUEST_PARAM_BID_REQUEST_GPP_CONSENT = "gpp";
    String REQUEST_PARAM_BID_REQUEST_GPP_CONSENT_SID = "gpp_sid";

    /**
     * 1 - Ethernet; Wired Connection
     * 2 - WIFI
     * 3 - Cellular Network - Unknown Generation
     * 4 - Cellular Network - 2G
     * 5 - Cellular Network - 3G
     * 6 - Cellular Network - 4G
     * 7 - Cellular Network - 5G
     */
    String REQUEST_PARAM_CONNECTION_TYPE = "connectiontype";

    String TRACK_EVENT_CONFIGTRACKINGLEVELSESSION = "session";
    String TRACK_EVENT_CONFIGTRACKINGLEVELUSER = "user";
    String TRACK_EVENT_CONFIGTRACKINGLEVELSYSTEM = "system";
    String TRACK_EVENT_CONFIGTRACKINGLEVELTIMING = "timing";
    String TRACK_EVENT_CONFIGTRACKINGLEVELDEBUG = "debug";
    String TRACK_EVENT_CONFIGTRACKINGLEVELERROR = "error";
    String TRACK_EVENT_CONFIGTRACKINGLEVELCRITICAL = "critical";
    String TRACK_EVENT_CONFIGTRACKINGLEVELINCLUDESTACKTRACE = "includeStackTrace";

    /**
     * CONFIG SETTINGS CONSTANTS
     */
    String CONFIG_KEY = "config";
    String CONFIG_ENABLED_KEY = "enabled";
    String CONFIG_PREFETCH_SESSION = "prefetchSession";
    String CONFIG_TRACKING_LEVELS_KEY = "trackingLevels";
    String CONFIG_VIDEO_PRECACHING_KEY = "videoPreCaching";
    String CONFIG_VIDEO_OM_SDK_KEY = "omSdk";
    String CONFIG_DIRECTORIES = "directories";
    String CONFIG_CACHE_TTLs = "cacheTTLs";
    String CONFIG_CACHE_MAX_UNITS = "cacheMaxUnits";
    String CONFIG_CACHE_MAX_BYTES = "cacheMaxBytes";
    String CONFIG_WEBVIEW_KEY = "webview";
    String CONFIG_INTERSTITIAL_ENABLED_KEY = "interstitialEnabled";
    String CONFIG_REWARD_VIDEO_ENABLED_KEY = "rewardVideoEnabled";
    String CONFIG_INPLAY_ENABLED_KEY = "inplayEnabled";
    String CONFIG_LOCK_ORIENTATION_KEY = "lockOrientation";
    String CONFIG_WEBVIEW_VERSION = "version";
    String CONFIG_PUBLISHER_DISABLE_KEY = "publisherDisable";
    String CONFIG_PREFETCH_DISABLED_KEY = "prefetchDisable";
    String CONFIG_INVALIDATE_PENDING_IMPRESSION = "invalidatePendingImpression";
    String CONFIG_VARIANT_KEY = "configVariant";
    String CONFIG_INVALIDATE_FOLDER_LIST = "invalidateFolderList";

    /**
     * AD UNIT MANAGER CONSTANTS
     */
    String REASON_CACHE = "cache";


    /**
     * STRING CONSTANTS
     */
    String CBIMPRESSIONACTIVITY_IDENTIFIER = "isChartboost";
}
