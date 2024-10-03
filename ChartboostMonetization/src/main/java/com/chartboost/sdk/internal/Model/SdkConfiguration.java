package com.chartboost.sdk.internal.Model;

import static com.chartboost.sdk.internal.Networking.CBNetworkRequest.API_ENDPOINT_PREFETCH;
import static com.chartboost.sdk.privacy.model.CCPA.CCPA_STANDARD;
import static com.chartboost.sdk.privacy.model.COPPA.COPPA_STANDARD;
import static com.chartboost.sdk.privacy.model.LGPD.LGPD_STANDARD;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.External.Android;
import com.chartboost.sdk.internal.Libraries.CBConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
    This corresponds to:
      - a row from the sdk_configurations collection, which is sometimes
        modified by a row from the device_experiments collection.

      - ApiConfigResponse.ConfigOptions on the adserver-config server
 */
public class SdkConfiguration {
    private static final String PUBLISHER_WARNING_KEY = "publisherWarning";

    public final String configVariant;
    public final boolean prefetchDisable;
    public final boolean publisherDisable;
    public final /*immutable*/ List<String> invalidateFolderList;

    public final boolean trackCritical;
    public final boolean trackError;
    public final boolean trackDebug;
    public final boolean trackSession;
    public final boolean trackSystem;
    public final boolean trackTiming;
    public final boolean trackUser;
    public final boolean includeStackTrace;
    public final int webviewCacheMaxBytes;
    public final int webviewCacheMaxUnits;
    public final int webviewCacheTTLDays;
    public final /*immutable*/ List<String> webviewDirectories;
    public final boolean webviewEnabled;
    public final boolean webviewInPlayEnabled;
    public final boolean webviewInterstitialEnabled;
    public final int webviewInvalidatePendingImpressionTTLMinutes;
    public final boolean webviewLockOrientation;
    public final int webviewPrefetchSession;
    public final boolean webviewRewardVideoEnabled;
    public final String webviewVersion;

    public final String webviewPrefetchEndpoint;

    private final BannerConfig bannerConfig;
    public PrivacyStandardsConfig privacyStandardsConfig;
    private final String publisherWarning;
    private final TrackingConfig trackingConfig;
    private final VideoPreCachingModel precacheConfig;
    private final OmSdkModel omSdkConfig;

    public SdkConfiguration(JSONObject w) {
        configVariant = w.optString(CBConstants.CONFIG_VARIANT_KEY);
        prefetchDisable = w.optBoolean(CBConstants.CONFIG_PREFETCH_DISABLED_KEY);
        publisherDisable = w.optBoolean(CBConstants.CONFIG_PUBLISHER_DISABLE_KEY);
        bannerConfig = BannerConfig.parseBannerConfig(w);

        try {
            privacyStandardsConfig = PrivacyStandardsConfig.parsePrivacyStandardsConfig(w);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //optional cause it will crash otherwise, but if not existing then return null
        publisherWarning = w.optString(PUBLISHER_WARNING_KEY, null);

        List<String> invalidateFolderDirectories = new ArrayList<>();
        JSONArray dir = w.optJSONArray(CBConstants.CONFIG_INVALIDATE_FOLDER_LIST);
        if (dir != null) {
            int n = dir.length();
            for (int i = 0; i < n; ++i) {
                String directory = dir.optString(i);
                if (!directory.isEmpty())
                    invalidateFolderDirectories.add(directory);
            }
        }
        invalidateFolderList = Collections.unmodifiableList(invalidateFolderDirectories);

        // "trackingLevels" sub-object
        JSONObject trackingLevels = w.optJSONObject(CBConstants.CONFIG_TRACKING_LEVELS_KEY);
        if (trackingLevels == null) {
            trackingLevels = new JSONObject();
        }
        trackCritical = trackingLevels.optBoolean(CBConstants.TRACK_EVENT_CONFIGTRACKINGLEVELCRITICAL, true);
        includeStackTrace = trackingLevels.optBoolean(CBConstants.TRACK_EVENT_CONFIGTRACKINGLEVELINCLUDESTACKTRACE, true);
        trackError = trackingLevels.optBoolean(CBConstants.TRACK_EVENT_CONFIGTRACKINGLEVELERROR);
        trackDebug = trackingLevels.optBoolean(CBConstants.TRACK_EVENT_CONFIGTRACKINGLEVELDEBUG);
        trackSession = trackingLevels.optBoolean(CBConstants.TRACK_EVENT_CONFIGTRACKINGLEVELSESSION);
        trackSystem = trackingLevels.optBoolean(CBConstants.TRACK_EVENT_CONFIGTRACKINGLEVELSYSTEM);
        trackTiming = trackingLevels.optBoolean(CBConstants.TRACK_EVENT_CONFIGTRACKINGLEVELTIMING);
        trackUser = trackingLevels.optBoolean(CBConstants.TRACK_EVENT_CONFIGTRACKINGLEVELUSER);
        trackingConfig = TrackingConfigKt.parseTrackingConfig(w);

        JSONObject precachingJson = w.optJSONObject(CBConstants.CONFIG_VIDEO_PRECACHING_KEY);
        if (precachingJson == null) {
            precachingJson = new JSONObject();
        }

        precacheConfig = VideoPreCachingModel.parseVideoPreCachingModelConfig(precachingJson);

        JSONObject omSdkConfigJson = w.optJSONObject(CBConstants.CONFIG_VIDEO_OM_SDK_KEY);
        if (omSdkConfigJson == null) {
            omSdkConfigJson = new JSONObject();
        }

        omSdkConfig = OmSdkModelKt.jsonToOmSdkModel(omSdkConfigJson);

        // "webview" sub-object
        JSONObject webview = w.optJSONObject(CBConstants.CONFIG_WEBVIEW_KEY);
        if (webview == null) {
            webview = new JSONObject();
        }

        webviewCacheMaxBytes = webview.optInt(CBConstants.CONFIG_CACHE_MAX_BYTES, CBConstants.DEFAULT_WEBVIEW_CACHE_MAX_BYTES);

        int cacheMaxUnits = webview.optInt(CBConstants.CONFIG_CACHE_MAX_UNITS, CBConstants.DEFAULT_WEBVIEW_CACHE_MAX_UNITS);
        webviewCacheMaxUnits = cacheMaxUnits > 0 ? cacheMaxUnits : CBConstants.DEFAULT_WEBVIEW_CACHE_MAX_UNITS;
        webviewCacheTTLDays = (int) TimeUnit.SECONDS.toDays(webview.optInt(CBConstants.CONFIG_CACHE_TTLs, CBConstants.DEFAULT_WEBVIEW_CACHE_TTL_SECONDS));

        List<String> directories = new ArrayList<>();
        JSONArray dirArray = webview.optJSONArray(CBConstants.CONFIG_DIRECTORIES);
        if (dirArray != null) {
            int n = dirArray.length();
            for (int i = 0; i < n; ++i) {
                String directory = dirArray.optString(i);
                if (!directory.isEmpty())
                    directories.add(directory);
            }
        }
        webviewDirectories = Collections.unmodifiableList(directories);

        webviewEnabled = webview.optBoolean(CBConstants.CONFIG_ENABLED_KEY, isWebviewEnabledByDefault());
        webviewInPlayEnabled = webview.optBoolean(CBConstants.CONFIG_INPLAY_ENABLED_KEY, true);
        webviewInterstitialEnabled = webview.optBoolean(CBConstants.CONFIG_INTERSTITIAL_ENABLED_KEY, true);

        int invalidatePendingImpression = webview.optInt(CBConstants.CONFIG_INVALIDATE_PENDING_IMPRESSION,
                CBConstants.PENDING_IMPRESSION_EXPIRATION_MINUTES);
        webviewInvalidatePendingImpressionTTLMinutes =
                invalidatePendingImpression > 0
                        ? invalidatePendingImpression
                        : CBConstants.PENDING_IMPRESSION_EXPIRATION_MINUTES;

        webviewLockOrientation = webview.optBoolean(CBConstants.CONFIG_LOCK_ORIENTATION_KEY, true);
        webviewPrefetchSession = webview.optInt(CBConstants.CONFIG_PREFETCH_SESSION, 3);
        webviewRewardVideoEnabled = webview.optBoolean(CBConstants.CONFIG_REWARD_VIDEO_ENABLED_KEY, true);

        webviewVersion = webview.optString(CBConstants.CONFIG_WEBVIEW_VERSION, "v2");
        webviewPrefetchEndpoint = String.format("%s/%s%s", "webview", webviewVersion, API_ENDPOINT_PREFETCH);
    }

    public BannerConfig getBannerConfig() {
        return bannerConfig;
    }

    public boolean getPublisherDisable() {
        return publisherDisable;
    }

    public boolean getPrefetchDisable() {
        return prefetchDisable;
    }

    public String getPublisherWarning() {
        return publisherWarning;
    }

    public TrackingConfig getTrackingConfig() {
        return trackingConfig;
    }

    public VideoPreCachingModel getPrecacheConfig() {
        return precacheConfig;
    }

    public OmSdkModel getOmSdkConfig() {
        return omSdkConfig;
    }

    public boolean isWebviewEnabled() {
        return webviewEnabled;
    }

    public boolean isWebviewLockOrientation() {
        return webviewLockOrientation;
    }

    private static boolean isWebviewEnabledByDefault() {

        final int[] refOSVersionForWebView = {4, 4, 2}; //API 19 Kitkat v4.4.2

        String deviceReleaseVersion = Android.instance().getReleaseVersion();

        if ((deviceReleaseVersion == null) || (deviceReleaseVersion.length() <= 0)) {
            return false;
        }

        //Replace non numbers except decimal with blanks in the reference and device version strings
        deviceReleaseVersion = deviceReleaseVersion.replaceAll("[^\\d.]", "");
        String[] devRelVersion = deviceReleaseVersion.split("\\.");

        for (int index = 0;
             (index < devRelVersion.length) && (index < refOSVersionForWebView.length);
             index++) {
            try {
                if (Integer.parseInt(devRelVersion[index]) > refOSVersionForWebView[index]) {
                    return true;
                } else if (Integer.parseInt(devRelVersion[index]) < refOSVersionForWebView[index]) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }

    public static class PrivacyStandardsConfig {
        private static final String PRIVACY_STANDARDS_KEY = "privacyStandards";

        private HashSet<String> privacyStandards;

        /**
         * No whitelist means we fall back to the default, which is us_privacy
         * empty whitelist means we don’t allow anything
         * something in the whitelist means we allow that something
         *
         * @param config The JSON object containing the privacy standards whitelist
         *
         * @return
         * @throws JSONException
         */
        public static PrivacyStandardsConfig parsePrivacyStandardsConfig(JSONObject config) throws JSONException {
            PrivacyStandardsConfig privacyStandardsConfig = new PrivacyStandardsConfig();

            HashSet<String> defaultSet = new HashSet<>();
            defaultSet.add(CCPA_STANDARD);
            defaultSet.add(COPPA_STANDARD);
            defaultSet.add(LGPD_STANDARD);

            JSONArray json = config.optJSONArray(PRIVACY_STANDARDS_KEY);
            if (json != null) {
                int size = json.length();
                parseJson(json, defaultSet, size);
                clearDefaultSetWhenJsonIsEmpty(defaultSet, size);
            }

            privacyStandardsConfig.privacyStandards = defaultSet;
            return privacyStandardsConfig;
        }

        private static void parseJson(@NonNull JSONArray json, HashSet<String> defaultSet, int size) throws JSONException {
            for (int i = 0; i < size; i++) {
                defaultSet.add(json.getString(i));
            }
        }

        private static void clearDefaultSetWhenJsonIsEmpty(HashSet<String> defaultSet, int size) {
            if (size == 0) {
                defaultSet.clear();
            }
        }

        public HashSet<String> getPrivacyStandardsWhitelist() {
            return privacyStandards;
        }
    }

    public static class BannerConfig {
        private static final String BANNER_ENABLED_KEY = "bannerEnable";
        protected boolean bannerEnabled;

        public static BannerConfig parseBannerConfig(JSONObject config) {
            BannerConfig bannerConfig = new BannerConfig();
            bannerConfig.bannerEnabled = config.optBoolean(BANNER_ENABLED_KEY, true);
            return bannerConfig;
        }

        public boolean isBannerEnabled() {
            return bannerEnabled;
        }
    }

    public ConfigurationBodyFields toConfigurationBodyFields() {
        return new ConfigurationBodyFields(
                configVariant,
                webviewEnabled,
                webviewVersion
        );
    }
}
