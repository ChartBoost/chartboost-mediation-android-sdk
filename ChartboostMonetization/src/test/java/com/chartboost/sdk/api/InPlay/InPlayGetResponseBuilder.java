package com.chartboost.sdk.api.InPlay;

import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;
import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;
import static com.chartboost.sdk.internal.Libraries.CBJSON.put;
import static com.chartboost.sdk.test.TestUtils.randomString;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.Libraries.CBJSON;
import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.JSONObjectBuilder;

import org.json.JSONObject;

public class InPlayGetResponseBuilder extends JSONObjectBuilder {
    public InPlayGetResponseBuilder() {
        super();
    }

    public InPlayGetResponseBuilder(JSONObject built) {
        super(built);
    }

    @NonNull
    public static InPlayGetResponseBuilder interstitialReturned() {
        return new InPlayGetResponseBuilder()
                .withStatus(200)
                .withMessage("Interstitial returned.")
                .withRequiredFields();
    }

    @NonNull
    private InPlayGetResponseBuilder withMessage(String message) {
        return withField("message", message);
    }

    @NonNull
    private InPlayGetResponseBuilder withRequiredFields() {
        return withAdId(randomString("the ad id"))
                .withTo(randomString("advertiser app id"))
                .withLink(randomString("the link"))
                .withCgn(randomString("the cgn"))
                .withCreative(randomString("the creative"))
                .withName(randomString("the name"));
    }

    @NonNull
    private InPlayGetResponseBuilder withStatus(double status) {
        return withField("status", status);
    }

    @NonNull
    public InPlayGetResponseBuilder withAdId(String adId) {
        return withField("ad_id", adId);
    }

    @NonNull
    public InPlayGetResponseBuilder withTo(String to) {
        return withField("to", to);
    }

    @NonNull
    public InPlayGetResponseBuilder withLink(String link) {
        return withField("link", link);
    }

    @NonNull
    public InPlayGetResponseBuilder withDeepLink(String link) {
        return withField("deep-link", link);
    }

    @NonNull
    public InPlayGetResponseBuilder withCgn(String cgn) {
        return withField("cgn", cgn);
    }

    @NonNull
    public InPlayGetResponseBuilder withCreative(String creative) {
        return withField("creative", creative);
    }

    @NonNull
    public InPlayGetResponseBuilder withName(String name) {
        return withField("name", name);
    }

    @NonNull
    private InPlayGetResponseBuilder withField(String name, Object value) {
        JSONObject newResponse = copy();

        put(newResponse, name, value);

        return new InPlayGetResponseBuilder(newResponse);
    }

    @NonNull
    public InPlayGetResponseBuilder withIcon(AssetDescriptor icon) {
        JSONObject newResponse = copy();

        CBJSON.put(newResponse, "icons", jsonObject(
                JKV("sm", icon.uri),
                JKV("md", icon.uri),
                JKV("lg", icon.uri)));

        return new InPlayGetResponseBuilder(newResponse);
    }
}
