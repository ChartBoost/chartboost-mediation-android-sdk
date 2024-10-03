package com.chartboost.sdk.api.Native;

import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;
import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;
import static com.chartboost.sdk.internal.Libraries.CBJSON.put;
import static com.chartboost.sdk.test.TestUtils.randomString;

import androidx.annotation.NonNull;

import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.JSONObjectBuilder;

import org.json.JSONObject;

public class NativeGetResponseBuilder extends JSONObjectBuilder {
    public NativeGetResponseBuilder() {
        super();
    }

    public NativeGetResponseBuilder(JSONObject built) {
        super(built);
    }

    public NativeGetResponseBuilder withAdId(String adId) {
        return withField("ad_id", adId);
    }

    public NativeGetResponseBuilder withTo(String to) {
        return withField("to", to);
    }

    public NativeGetResponseBuilder withLink(String link) {
        return withField("link", link);
    }

    public NativeGetResponseBuilder withDeepLink(String link) {
        return withField("deep-link", link);
    }

    public NativeGetResponseBuilder withCgn(String cgn) {
        return withField("cgn", cgn);
    }

    public NativeGetResponseBuilder withCreative(String creative) {
        return withField("creative", creative);
    }

    public NativeGetResponseBuilder withUx(JSONObject ux) {
        return withField("ux", ux);
    }

    public NativeGetResponseBuilder withMinimalUx() {
        return withUx(jsonObject(
                JKV("video-controls-background", jsonObject(
                        JKV("color", "#6000"),
                        JKV("border-color", "#6000")))));
    }

    @NonNull
    public NativeGetResponseBuilder withMediaType(String mediaType) {
        return withField("media-type", mediaType);
    }

    @NonNull
    public NativeGetResponseBuilder withMediaTypeImage() {
        return withMediaType("image");
    }

    @NonNull
    public NativeGetResponseBuilder withMediaTypeVideo() {
        return withMediaType("video");
    }

    @NonNull
    public NativeGetResponseBuilder withAnimation(int animationId) {
        return withField("animation", animationId);
    }

    public NativeGetResponseBuilder withVideoAsset(String name, AssetDescriptor d) {
        return withVideoAsset(name, d, null);
    }

    public NativeGetResponseBuilder withVideoAsset(String name, AssetDescriptor d, String checksum) {
        return withVideoAsset(name,
                d.uri,
                d.filename,
                checksum,
                0.0,
                0, 0,
                0.5,
                d.filename);
    }

    public NativeGetResponseBuilder withImageAsset(String name, AssetDescriptor d, String checksum) {
        return withImageAsset(name, d.uri, checksum, 1.0, 0, 0);
    }

    public NativeGetResponseBuilder withImageAsset(String name,
                                                   String url,
                                                   String checksum,
                                                   double scale,
                                                   int xOffset,
                                                   int yOffset) {
        JSONObject newResponse = copy();

        JSONObject assets = subObject(newResponse, "assets");
        put(assets, name, jsonObject(
                JKV("url", url),
                JKV("checksum", checksum),
                JKV("scale", scale),
                JKV("offset", jsonObject(
                        JKV("x", xOffset),
                        JKV("y", yOffset)))));

        return new NativeGetResponseBuilder(newResponse);
    }

    public NativeGetResponseBuilder withVideoAsset(String name,
                                                   String url,
                                                   String filename,
                                                   String checksum,
                                                   double scale,
                                                   int xOffset,
                                                   int yOffset,
                                                   double buffer,
                                                   String id) {
        JSONObject newResponse = copy();

        JSONObject assets = subObject(newResponse, "assets");
        put(assets, name, jsonObject(
                JKV("url", url),
                JKV("checksum", checksum),
                JKV("scale", scale),
                JKV("offset", jsonObject(
                        JKV("x", xOffset),
                        JKV("y", yOffset))),
                JKV("local-file", filename),
                JKV("buffer", buffer),
                JKV("id", id)));

        return new NativeGetResponseBuilder(newResponse);
    }

    private NativeGetResponseBuilder withType(String type) {
        return withField("type", type);
    }

    private NativeGetResponseBuilder withMessage(String message) {
        return withField("message", message);
    }

    public NativeGetResponseBuilder withStatus(double status) {
        return withField("status", status);
    }

    public static NativeGetResponseBuilder interstitialReturned() {
        return new NativeGetResponseBuilder()
                .withStatus(200)
                .withMessage("Interstitial returned.")
                .withType("native")
                .withRequiredFields();
    }

    public static NativeGetResponseBuilder rewardedReturned() {
        return new NativeGetResponseBuilder()
                .withStatus(200)
                .withMessage("Rewarded returned.")
                .withType("native")
                .withRequiredFields();
    }

    private NativeGetResponseBuilder withRequiredFields() {
        return withAdId(randomString("the ad id"))
                .withField("assets", jsonObject())
                .withTo(randomString("the advertiser app id (to)"))
                .withLink(randomString("the click link"))
                .withCgn(randomString("the cgn"))
                .withCreative(randomString("the creative"))
                .withMinimalUx();
    }

    @NonNull
    private NativeGetResponseBuilder withField(String name, Object value) {
        JSONObject newResponse = copy();

        put(newResponse, name, value);

        return new NativeGetResponseBuilder(newResponse);
    }

    public static NativeGetResponseBuilder minimalParseable() {
        return new NativeGetResponseBuilder()
                .withAdId("")
                .withCgn("")
                .withCreative("")
                .withLink("")
                .withTo("")
                .withField("assets", jsonObject());
    }

    public NativeGetResponseBuilder withRewardAmount(int amount) {
        return withField("reward", amount);
    }

    public NativeGetResponseBuilder withRewardCurrency(String currency) {
        return withField("currency-name", currency);
    }
}
