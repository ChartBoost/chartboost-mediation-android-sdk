package com.chartboost.sdk.test;

import com.chartboost.sdk.internal.Model.SdkConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SdkConfigurationBuilder {
    final JSONObject config;

    public SdkConfigurationBuilder() {
        this(new JSONObject());
    }

    public SdkConfigurationBuilder(JSONObject config) {
        this.config = config;
    }

    public JSONObject options() {
        return config;
    }

    public SdkConfiguration build() {
        return new SdkConfiguration(config);
    }

    public SdkConfigurationBuilder withConfigVariant(String configVariant) {
        putConfigValue(config, "configVariant", configVariant);
        return this;
    }

    private void putConfigValue(JSONObject obj, String key, Object value) {
        try {
            obj.put(key, value);
        } catch (JSONException ex) {
            throw new Error(ex);
        }
    }

    public SdkConfigurationBuilder withPrefetchDisable(boolean b) {
        putConfigValue(config, "prefetchDisable", b);
        return this;
    }

    public SdkConfigurationBuilder withPublisherDisable(boolean b) {
        putConfigValue(config, "publisherDisable", b);
        return this;
    }

    public SdkConfigurationBuilder withInvalidateFolderList(List<String> directories) {
        putConfigValue(config, "invalidateFolderList", new JSONArray((Collection) directories));
        return this;
    }

    public SdkConfigurationBuilder withTrackCritical(boolean enabled) {
        putConfigValue(editTrackingLevels(), "critical", enabled);
        return this;
    }


    public SdkConfigurationBuilder withTrackDebug(boolean enabled) {
        putConfigValue(editTrackingLevels(), "debug", enabled);
        return this;
    }


    public SdkConfigurationBuilder withTrackSession(boolean enabled) {
        putConfigValue(editTrackingLevels(), "session", enabled);
        return this;
    }

    public SdkConfigurationBuilder withTrackSystem(boolean enabled) {
        putConfigValue(editTrackingLevels(), "system", enabled);
        return this;
    }

    public SdkConfigurationBuilder withTrackTiming(boolean enabled) {
        putConfigValue(editTrackingLevels(), "timing", enabled);
        return this;
    }

    public SdkConfigurationBuilder withWebViewCacheTTLDays(int days) {
        putConfigValue(editWebView(), "cacheTTLs", TimeUnit.DAYS.toSeconds(days));
        return this;
    }

    public SdkConfigurationBuilder withWebViewCacheMaxUnits(int units) {
        putConfigValue(editWebView(), "cacheMaxUnits", units);
        return this;
    }

    public SdkConfigurationBuilder withWebViewCacheMaxBytes(long bytes) {
        putConfigValue(editWebView(), "cacheMaxBytes", bytes);
        return this;
    }

    public SdkConfigurationBuilder withWebViewDirectories(List<String> directories) {
        putConfigValue(editWebView(), "directories", new JSONArray((Collection) directories));
        return this;
    }

    public SdkConfigurationBuilder withV3DesiredAvailableInterstitialAdUnits(int i) {
        putConfigValue(edit("v3"), "desiredAvailableInterstitialAdUnits", i);
        return this;
    }

    public SdkConfigurationBuilder withGetAdRetryBaseMs(long ms) {
        putConfigValue(config, "getAdRetryBaseMs", ms);
        return this;
    }

    public SdkConfigurationBuilder withGetAdRetryMaxBackoffExponent(int maxExponent) {
        putConfigValue(config, "getAdRetryMaxBackoffExponent", maxExponent);
        return this;
    }

    public SdkConfigurationBuilder withWebViewEnabled(boolean enabled) {
        putConfigValue(editWebView(), "enabled", enabled);
        return this;
    }

    public SdkConfigurationBuilder withWebViewInPlayEnabled(boolean enabled) {
        putConfigValue(editWebView(), "inplayEnabled", enabled);
        return this;
    }

    public SdkConfigurationBuilder withWebViewInterstitialEnabled(boolean enabled) {
        putConfigValue(editWebView(), "interstitialEnabled", enabled);
        return this;
    }

    public SdkConfigurationBuilder withWebViewInvalidatePendingImpressionTTLMinutes(int minutes) {
        putConfigValue(editWebView(), "invalidatePendingImpression", minutes);
        return this;
    }

    public SdkConfigurationBuilder withWebViewLockOrientation(Boolean lock) {
        putConfigValue(editWebView(), "lockOrientation", lock);
        return this;
    }

    public SdkConfigurationBuilder withWebViewPrefetchSession(int v) {
        putConfigValue(editWebView(), "prefetchSession", v);
        return this;
    }

    public SdkConfigurationBuilder withWebViewRewardVideoEnabled(boolean enabled) {
        putConfigValue(editWebView(), "rewardVideoEnabled", enabled);
        return this;
    }

    public SdkConfigurationBuilder withTrackingEnabled(boolean enabled) {
        putConfigValue(edit("tracking"), "enabled", enabled);
        return this;
    }

    public SdkConfigurationBuilder withWebViewVersion(String version) {
        putConfigValue(editWebView(), "version", version);
        return this;
    }

    public SdkConfigurationBuilder withPreCacheMaxBytes(long bytes) {
        putConfigValue(editVideoPreCaching(), "maxBytes", bytes);
        return this;
    }

    public SdkConfigurationBuilder withPreCacheBufferSize(int size) {
        putConfigValue(editVideoPreCaching(), "bufferSize", size);
        return this;
    }

    public SdkConfigurationBuilder withPreCacheMaxUnitsPerTimeWindow(int units) {
        putConfigValue(editVideoPreCaching(), "maxUnitsPerTimeWindow", units);
        return this;
    }

    public SdkConfigurationBuilder withPreCacheMaxUnitsPerTimeWindowCellular(int units) {
        putConfigValue(editVideoPreCaching(), "maxUnitsPerTimeWindowCellular", units);
        return this;
    }

    public SdkConfigurationBuilder withPreCacheTimeWindow(long window) {
        putConfigValue(editVideoPreCaching(), "timeWindow", window);
        return this;
    }

    public SdkConfigurationBuilder withPreCacheTimeWindowCellular(long window) {
        putConfigValue(editVideoPreCaching(), "timeWindowCellular", window);
        return this;
    }

    public SdkConfigurationBuilder withPreCacheTTL(long ttl) {
        putConfigValue(editVideoPreCaching(), "ttl", ttl);
        return this;
    }

    private JSONObject editNative() {
        return edit("native");
    }

    private JSONObject editTrackingLevels() {
        return edit("trackingLevels");
    }

    private JSONObject editWebView() {
        return edit("webview");
    }

    private JSONObject editVideoPreCaching() {
        return edit("videoPreCaching");
    }

    private JSONObject edit(String key) {
        JSONObject result = config.optJSONObject(key);
        if (result == null) {
            result = new JSONObject();
            try {
                config.put(key, result);
            } catch (JSONException ex) {
                throw new Error(ex);
            }
        }
        return result;
    }
}
