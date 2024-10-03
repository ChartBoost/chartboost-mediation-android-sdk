package com.chartboost.sdk.test;

import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;
import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;

import com.chartboost.sdk.internal.Networking.CBNetworkRequest;
import com.chartboost.sdk.internal.Networking.CBNetworkRequest.Method;

import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

public class Endpoint {
    public final CBNetworkRequest.Method method;
    public final String uri;

    public Endpoint(CBNetworkRequest.Method method, String uri) {
        this.method = method;
        this.uri = uri;
    }

    public static final Endpoint apiConfig = post("api/config");
    public static final Endpoint apiInstall = post("api/install");
    public static final Endpoint apiVideoPrefetch = post("api/video-prefetch");
    public static final Endpoint interstitialGet = post("interstitial/get");
    public static final Endpoint interstitialShow = post("interstitial/show");
    public static final Endpoint rewardGet = post("reward/get");

    public static final Endpoint v3Load = post("sdk/v3/load");

    public static final Endpoint webviewV2InterstitialGet = webviewV2("interstitial/get");
    public static final Endpoint webviewV2Prefetch = webviewV2("prefetch");
    public static final Endpoint webviewV2RewardGet = webviewV2("reward/get");

    private static Endpoint webviewV2(String rest) {
        return post("webview/v2/" + rest);
    }

    public static Endpoint post(String rest) {
        return new Endpoint(Method.POST, liveUri(rest));
    }

    private static String liveUri(String uriPart) {
        return com.chartboost.sdk.BuildConfig.API_PROTOCOL + "://live.chartboost.com/" + uriPart;
    }

    public ResponseDescriptor ok(JSONObject responseBody) {
        return respond(HttpsURLConnection.HTTP_OK, responseBody);
    }

    public ResponseDescriptor ok(byte[] data) {
        return respond(HttpsURLConnection.HTTP_OK, data);
    }

    public ResponseDescriptor notFound() {
        return respond(HttpsURLConnection.HTTP_NOT_FOUND);
    }

    public ResponseDescriptor internalError() {
        return respond(HttpsURLConnection.HTTP_INTERNAL_ERROR);
    }

    public ResponseDescriptor respond() {
        return new ResponseDescriptor(this);
    }

    public ResponseDescriptor respond(int statusCode, JSONObject body) {
        return respond(statusCode, body.toString());
    }

    public ResponseDescriptor respond(int statusCode, String body) {
        return respond(statusCode, body.getBytes());
    }

    public ResponseDescriptor respond(int statusCode, byte[] data) {
        return new ResponseDescriptor(this, statusCode, data);
    }

    public ResponseDescriptor respond(int statusCode) {
        return respond(statusCode, new byte[0]);
    }

    public ResponseDescriptor impressionRecorded() {
        return ok(jsonObject(
                JKV("status", 200),
                JKV("message", "Impression Recorded.")));
    }
}
