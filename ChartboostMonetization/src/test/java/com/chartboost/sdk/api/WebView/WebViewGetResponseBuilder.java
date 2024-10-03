package com.chartboost.sdk.api.WebView;

import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;
import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;
import static com.chartboost.sdk.internal.Libraries.CBJSON.put;
import static com.chartboost.sdk.test.TestUtils.randomString;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit;
import com.chartboost.sdk.internal.AdUnitManager.data.InfoIcon;
import com.chartboost.sdk.internal.AdUnitManager.parsers.AdUnitParser;
import com.chartboost.sdk.internal.utils.Base64Wrapper;
import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.JSONObjectBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class WebViewGetResponseBuilder extends JSONObjectBuilder {
    public WebViewGetResponseBuilder() {
        super();
    }

    public WebViewGetResponseBuilder(JSONObject built) {
        super(built);
    }

    public static WebViewGetResponseBuilder interstitialReturned() {
        return new WebViewGetResponseBuilder()
                .withStatus(200)
                .withMessage("Interstitial returned.")
                .withRequiredFields()
                .withMediaType("image");
    }

    public static WebViewGetResponseBuilder rewardedReturned() {
        return new WebViewGetResponseBuilder()
                .withStatus(200)
                .withMessage("Rewarded returned.")
                .withRequiredFields()
                .withMediaType("video");
    }

    public WebViewGetResponseBuilder withMediaType(String mediaType) {
        return withField("media-type", mediaType);
    }

    private WebViewGetResponseBuilder withMessage(String message) {
        return withField("message", message);
    }

    public WebViewGetResponseBuilder withStatus(double status) {
        return withField("status", status);
    }

    public WebViewGetResponseBuilder withAdId(String adId) {
        return withField("ad_id", adId);
    }

    public WebViewGetResponseBuilder withBaseUrl(String baseUrl) {
        return withField("baseurl", baseUrl);
    }

    public WebViewGetResponseBuilder withInfoIcon(InfoIcon infoIcon) {
        return withField("infoicon", infoIcon);
    }

    public WebViewGetResponseBuilder withImpressionId(String impressionId) {
        return withParameter("impression_id", impressionId, "{% impression_id %}");
    }

    public WebViewGetResponseBuilder withTo(String to) {
        return withField("to", to);
    }

    public WebViewGetResponseBuilder withDeepLink(String deepLink) {
        return withField("deep-link", deepLink);
    }

    public WebViewGetResponseBuilder withLink(String link) {
        return withField("link", link);
    }

    public WebViewGetResponseBuilder withCgn(String cgn) {
        return withField("cgn", cgn);
    }

    public WebViewGetResponseBuilder withCreative(String creative) {
        return withField("creative", creative);
    }

    public WebViewGetResponseBuilder withTemplateId(String templateId) {
        JSONObject newResponse = copy();

        JSONObject webview = subObject(newResponse, "webview");
        put(webview, "template", templateId);

        return new WebViewGetResponseBuilder(newResponse);

    }

    private WebViewGetResponseBuilder withRequiredFields() {
        return withAdId(randomString("the ad id"))
                .withField("webview", new JSONObject())
                .withTo(randomString("the advertiser app id (to)"))
                .withLink(randomString("the click link"))
                .withCgn(randomString("the cgn"))
                .withCreative(randomString("the creative"))
                .withEmptyElements()
                .withTemplateId(randomString("the template id"))
                .withField("enable_appsheet_animation", false)
                .withField("show_loading", false);
    }

    public WebViewGetResponseBuilder withAnimation(int animationId) {
        return withField("animation", animationId);
    }

    public WebViewGetResponseBuilder withRenderEngine(String renderEngine) {
        return withField("renderengine", renderEngine);
    }

    public WebViewGetResponseBuilder withScripts(List<String> scripts) {
        return withField("scripts", scripts);
    }

    private WebViewGetResponseBuilder withEmptyElements() {
        JSONObject newResponse = copy();

        subArray(newResponse, "webview", "elements");

        return new WebViewGetResponseBuilder(newResponse);
    }

    public WebViewGetResponseBuilder withPrecacheVideoElement(String url) {
        return withElement("test", "preCachedVideo", url);
    }

    public WebViewGetResponseBuilder withHtmlBodyElement(AssetDescriptor d) {
        return withElement(d.filename, "html", d.uri);
    }

    public WebViewGetResponseBuilder withElement(AssetDescriptor d) {
        return withElement(d.filename, d.cacheDir, d.uri);
    }


    public WebViewGetResponseBuilder withParameter(String name, String value, String paramName) {
        return withParameterizedElement(name, "param", value, paramName);
    }

    public WebViewGetResponseBuilder withElement(String name, String type, String value) {
        JSONObject newResponse = copy();

        JSONArray elements = subArray(newResponse, "webview", "elements");

        elements.put(jsonObject(
                JKV("name", name),
                JKV("type", type),
                JKV("value", value)));

        return new WebViewGetResponseBuilder(newResponse);
    }

    public WebViewGetResponseBuilder withParameterizedElement(AssetDescriptor d, String param) {
        return withParameterizedElement(d.filename, d.cacheDir, d.uri, param);
    }

    public WebViewGetResponseBuilder withParameterizedElement(String name, String type, String value, String param) {
        JSONObject newResponse = copy();

        JSONArray elements = subArray(newResponse, "webview", "elements");

        elements.put(jsonObject(
                JKV("name", name),
                JKV("type", type),
                JKV("value", value),
                JKV("param", param)));

        return new WebViewGetResponseBuilder(newResponse);
    }

    public WebViewGetResponseBuilder withElements(AssetDescriptor[] descriptors) {
        WebViewGetResponseBuilder result = this;
        for (AssetDescriptor d : descriptors) {
            result = result.withElement(d.filename, d.cacheDir, d.uri);
        }
        return result;
    }

    @NonNull
    private WebViewGetResponseBuilder withField(String name, Object value) {
        JSONObject newResponse = copy();

        put(newResponse, name, value);

        return new WebViewGetResponseBuilder(newResponse);
    }

    public WebViewGetResponseBuilder withTrackingEvent(String key, String url) {
        JSONObject newResponse = copy();

        JSONArray events = subArray(newResponse, "events", key);
        events.put(url);

        return new WebViewGetResponseBuilder(newResponse);
    }

    @NonNull
    public AdUnit toAdUnit() {
        try {
            return new AdUnitParser(new Base64Wrapper()).parse(toJSONObject());
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

}
