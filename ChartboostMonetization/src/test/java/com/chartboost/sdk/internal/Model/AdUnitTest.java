package com.chartboost.sdk.internal.Model;

import static com.chartboost.sdk.test.TestUtils.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.api.WebView.WebViewGetResponseBuilder;
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit;
import com.chartboost.sdk.internal.AdUnitManager.data.InfoIcon;
import com.chartboost.sdk.internal.AdUnitManager.parsers.AdUnitParser;
import com.chartboost.sdk.internal.utils.Base64Wrapper;
import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.ReferenceResponse;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//TODO redo it with kotlin and redo the tests and remove robolectric dependency
public class AdUnitTest extends BaseTest {

    private final Base64Wrapper base64Wrapper = new Base64Wrapper();
    private final String theAdId = randomString("the ad id");
    private final String theBaseUrl = randomString("the base url");
    private final InfoIcon theInfoIcon = randomInfoIcon();
    private final int theAnimationId = TestUtils.randomAnimationId();
    private final String theCgn = randomString("the cgn");
    private final String theCreative = randomString("the creative");
    private final String theDeepLink = randomString("the deep link");
    private final String videoUrl = "http://" + randomString("the video url");
    private final String theLink = randomString("the link");
    private final String theMediaType = TestUtils.randomMediaType();
    private final String theTemplateId = randomString("the template id");
    private final String theTo = randomString("the advertiser app id (to)");
    private final String theRenderEngine = randomRenderEngine();
    private final List<String> theScripts = randomScriptsArray(3);
    private final String adLandscapeParamName = randomString("{% ad_landscape %}");

    private final String rewardAmountParamName = randomString("{% reward_amount %}");
    private final int theRewardAmount = TestUtils.smallRandomInt();

    private final String rewardCurrencyParamName = randomString("{% reward_currency %}");
    private final String theRewardCurrency = randomString("the reward currency");

    private final AssetDescriptor theTemplateHtmlBody = AssetDescriptor.baseTemplate33cda9;
    private final AssetDescriptor theCloseButton = AssetDescriptor.closeButton60x60;

    private final AssetDescriptor theLandscapeAdImage = AssetDescriptor.imageBustersBoostLandscape;

    private final String trackingEventsRedKey = randomString("tracking events red");
    private final String trackingEventsBlueKey = randomString("tracking events blue");

    private final String trackingEventUrlA = randomString("tracking event url a");
    private final String trackingEventUrlB = randomString("tracking event url b");
    private final String trackingEventUrlC = randomString("tracking event url c");


    @Test
    public void verifyDecodeAllWebViewResponseFields() throws JSONException {
        try (TestContainer tc = new TestContainer()) {
            JSONObject webviewResponse = WebViewGetResponseBuilder.interstitialReturned()
                    .withAdId(theAdId)
                    .withBaseUrl(theBaseUrl)
                    .withInfoIcon(theInfoIcon)
                    .withCgn(theCgn)
                    .withCreative(theCreative)
                    .withDeepLink(theDeepLink)
                    .withLink(theLink)
                    .withMediaType(theMediaType)
                    .withTo(theTo)
                    .withAnimation(theAnimationId)
                    .withTemplateId(theTemplateId)
                    .withHtmlBodyElement(theTemplateHtmlBody)
                    .withPrecacheVideoElement(videoUrl)
                    .withElement(theCloseButton)
                    .withParameterizedElement(theLandscapeAdImage, adLandscapeParamName)
                    .withParameter("reward_amount", String.valueOf(theRewardAmount), rewardAmountParamName)
                    .withParameter("reward_currency", theRewardCurrency, rewardCurrencyParamName)
                    .withRenderEngine(theRenderEngine)
                    .withScripts(theScripts)
                    .withTrackingEvent(trackingEventsRedKey, trackingEventUrlA)
                    .withTrackingEvent(trackingEventsBlueKey, trackingEventUrlB)
                    .withTrackingEvent(trackingEventsBlueKey, trackingEventUrlC)
                    .build();

            AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(webviewResponse);
            verifyWebViewFields(adUnit, webviewResponse, tc);
        }
    }

    private void verifyWebViewFields(AdUnit adUnit, JSONObject webviewResponse, TestContainer tc) {
        //assertThat(adUnit.legacyResponse, equalsJSONObject(webviewResponse));

        assertNotNull(adUnit.getAssets());
        assertThat(adUnit.getAssets().keySet(), containsInAnyOrder(
                "body",
                "60x60.png",
                adLandscapeParamName));

        assertNotNull(adUnit.getParameters());
        assertThat(adUnit.getParameters().keySet(), containsInAnyOrder(
                rewardAmountParamName,
                rewardCurrencyParamName));

        assertThat(adUnit.getTemplate(), is(theTemplateId));

        assertThat(adUnit.getAdId(), is(theAdId));
        assertThat(adUnit.getCgn(), is(theCgn));
        assertThat(adUnit.getCreative(), is(theCreative));
        assertThat(adUnit.getVideoUrl(), is(videoUrl));
        assertThat(adUnit.getDeepLink(), is(theDeepLink));
        assertThat(adUnit.getLink(), is(theLink));
        assertThat(adUnit.getRewardAmount(), is(theRewardAmount));
        assertThat(adUnit.getRewardCurrency(), is(theRewardCurrency));
        assertThat(adUnit.getTo(), is(theTo));

        assertNotNull(adUnit.getEvents());
        assertThat(adUnit.getEvents().keySet(), containsInAnyOrder(
                trackingEventsBlueKey,
                trackingEventsRedKey));
        List<String> redEvents = adUnit.getEvents().get(trackingEventsRedKey);
        assertThat(redEvents, containsInAnyOrder(trackingEventUrlA));
        List<String> blueEvents = adUnit.getEvents().get(trackingEventsBlueKey);
        assertThat(blueEvents, containsInAnyOrder(trackingEventUrlB, trackingEventUrlC));

        assertThat(adUnit.getMediaType(), is(theMediaType));
        assertThat(adUnit.getName(), is("")); // not set for webview

        assertNotNull(adUnit.getBody());
        assertThat(adUnit.getBody().directory, is("html"));
        assertThat(adUnit.getBody().filename, is(theTemplateHtmlBody.filename));
        assertThat(adUnit.getBody().url, is(theTemplateHtmlBody.uri));
    }

    @Test
    public void convertWebViewV2GetEvents() throws JSONException {
        final String url_a_1 = "http://a/event/1";
        final String url_a_2 = "http://a/event/2";
        final String url_b_1 = "http://b/event/1";
        try (TestContainer tc = new TestContainer()) {
            JSONObject v2GetResponse = WebViewGetResponseBuilder.interstitialReturned()
                    .withTrackingEvent("a", url_a_1)
                    .withTrackingEvent("a", url_a_2)
                    .withTrackingEvent("b", url_b_1)
                    .withHtmlBodyElement(AssetDescriptor.baseTemplate2e34e6)
                    .build();
            AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(v2GetResponse);

            assertThat(adUnit.getEvents().keySet(), containsInAnyOrder("a", "b"));
            assertThat(adUnit.getEvents().get("a"), containsInAnyOrder(url_a_1, url_a_2));
            assertThat(adUnit.getEvents().get("b"), containsInAnyOrder(url_b_1));
        }
    }

    @Test
    public void convertRewardGet() throws JSONException {
        try (TestContainer tc = new TestContainer()) {
            JSONObject v2 = ReferenceResponse.webviewV2RewardGetWithResults.asJSONObject();
            AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(v2);
            assertNotNull(adUnit);
            assertThat(adUnit.getRewardAmount(), is(30));
            assertThat(adUnit.getRewardCurrency(), is("Credits"));
            assertThat(adUnit.getParameters().keySet(), hasItem("{% reward_amount %}"));
            assertThat(adUnit.getParameters().keySet(), hasItem("{% reward_currency %}"));

            assertThat(adUnit.getAssets().keySet(), containsInAnyOrder(
                    "body",
                    "{% ad_landscape %}",
                    "{% app_icon %}",
                    "{% close_landscape %}",
                    "{% frame_landscape %}",
                    "{% post_video_button %}",
                    "{% post_video_reward_icon %}",
                    "{% replay_landscape %}",
                    "{% video_click_button %}",
                    "{% video_confirmation_button %}",
                    "{% video_confirmation_icon %}",
                    "{{ video_landscape }}"));
        }
    }

    @Test
    public void convertNonTemplateBodyHtmlElement() throws JSONException {
        try (TestContainer tc = TestContainer.defaultWebView()) {
            JSONObject webviewResponse = WebViewGetResponseBuilder.interstitialReturned()
                    .withHtmlBodyElement(AssetDescriptor.baseTemplate33cda9)
                    .withParameterizedElement("the_html_filename", "html", "http://some_html_file", "param_name_for_html_element")
                    .build();
            AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(webviewResponse);
            assertThat(adUnit.getAssets().keySet(), containsInAnyOrder("body", "param_name_for_html_element"));

        }
    }

    @Test
    public void convertCrossInstall() throws JSONException {
        try (TestContainer tc = new TestContainer()) {
            JSONObject v2 = ReferenceResponse.webviewV2InterstitialGet_CrossInstall.asJSONObject();

            AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(v2);

            assertThat(adUnit.getAssets().keySet(), containsInAnyOrder(
                    "body",
                    "background3_portrait.jpg",
                    "crossinstall_gow_mini_robotron_v1_5_background3_landscape.jpg",
                    "icon.jpg",
                    "loadingBarFrame.png",
                    "loadingBarFull.png",
                    "orientation.jpg",
                    "crossinstall_gow_mini_robotron_v1_5_background2_portrait.jpg",
                    "crossinstall_gow_mini_robotron_v1_5_background3_portrait.jpg",
                    "crossinstall_gow_mini_robotron_v1_5_background_portrait.jpg",
                    "crossinstall_gow_mini_robotron_v1_5_assets.png",
                    "crossinstall_gow_mini_robotron_v1_5_icon.jpg",
                    "crossinstall_gow_mini_robotron_v1_5_background2_landscape.jpg",
                    "background3_landscape.jpg",
                    "background_portrait.jpg",
                    "background2_portrait.jpg",
                    "background2_landscape.jpg",
                    "footerLogo.png",
                    "crossinstall_ms_mini_robotron_v1_5_chartboost_orientation.jpg",
                    "background_landscape.jpg",
                    "crossinstall_gow_mini_robotron_v1_5_background_landscape.jpg",
                    "assets.png",
                    "headerLogo.png"));
        }
    }

    @Test
    public void parseVideoUrlFromFileDefaultTest() throws JSONException {
        String expectedFilename = "sdk-test_video-asset1.mp4";
        JSONObject v2 = ReferenceResponse.interstitialGetWithResultsVideoUrl.asJSONObject();
        AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(v2);
        assertNotNull(adUnit.getVideoUrl());
        assertTrue(adUnit.getVideoUrl().length() > 0);
        assertNotNull(adUnit.getVideoFilename());
        assertTrue(adUnit.getVideoFilename().length() > 0);
        assertEquals(expectedFilename, adUnit.getVideoFilename());
    }

    @Test
    public void parseVideoUrlEmptyNameTest() throws JSONException {
        String expectedFilename = "";
        String overrideVideoUrl = "";
        JSONObject v2 = ReferenceResponse.interstitialGetWithResultsVideoUrl.asJSONObject();
        JSONObject webview = v2.getJSONObject("webview");
        JSONArray elements = webview.getJSONArray("elements");
        int size = elements.length();
        for (int i = 0; i < size; i++) {
            JSONObject obj = (JSONObject) elements.get(i);
            String type = obj.getString("type");
            if ("preCachedVideo".equals(type)) {
                obj.put("value", overrideVideoUrl);
            }
        }
        AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(v2);
        assertNotNull(adUnit.getVideoUrl());
        assertNotNull(adUnit.getVideoFilename());
        assertEquals(expectedFilename, adUnit.getVideoFilename());
    }

    @Test
    public void parseVideoUrlWithoutSubdomainTest() throws JSONException {
        String expectedFilename = "video.mp4";
        String overrideVideoUrl = "http://test.com/video.mp4";
        JSONObject v2 = ReferenceResponse.interstitialGetWithResultsVideoUrl.asJSONObject();
        JSONObject webview = v2.getJSONObject("webview");
        JSONArray elements = webview.getJSONArray("elements");
        int size = elements.length();
        for (int i = 0; i < size; i++) {
            JSONObject obj = (JSONObject) elements.get(i);
            String type = obj.getString("type");
            if ("preCachedVideo".equals(type)) {
                obj.put("value", overrideVideoUrl);
            }
        }
        AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(v2);
        assertNotNull(adUnit.getVideoUrl());
        assertTrue(adUnit.getVideoUrl().length() > 0);
        assertNotNull(adUnit.getVideoFilename());
        assertTrue(adUnit.getVideoFilename().length() > 0);
        assertEquals(expectedFilename, adUnit.getVideoFilename());
    }

    @Test
    public void parseVideoUrlWithSubdomainTest() throws JSONException {
        String expectedFilename = "1_chartboost_video.mp4";
        String overrideVideoUrl = "http://test.com/1/chartboost/video.mp4";
        JSONObject v2 = ReferenceResponse.interstitialGetWithResultsVideoUrl.asJSONObject();
        JSONObject webview = v2.getJSONObject("webview");
        JSONArray elements = webview.getJSONArray("elements");
        int size = elements.length();
        for (int i = 0; i < size; i++) {
            JSONObject obj = (JSONObject) elements.get(i);
            String type = obj.getString("type");
            if ("preCachedVideo".equals(type)) {
                obj.put("value", overrideVideoUrl);
            }
        }
        AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(v2);
        assertNotNull(adUnit.getVideoUrl());
        assertTrue(adUnit.getVideoUrl().length() > 0);
        assertNotNull(adUnit.getVideoFilename());
        assertTrue(adUnit.getVideoFilename().length() > 0);
        assertEquals(expectedFilename, adUnit.getVideoFilename());
    }

    @Test
    public void parseVideoUrlWithoutProtocolTest() throws JSONException {
        String expectedFilename = "1_chartboost_video.mp4";
        String overrideVideoUrl = "test.com/1/chartboost/video.mp4";
        JSONObject v2 = ReferenceResponse.interstitialGetWithResultsVideoUrl.asJSONObject();
        JSONObject webview = v2.getJSONObject("webview");
        JSONArray elements = webview.getJSONArray("elements");
        int size = elements.length();
        for (int i = 0; i < size; i++) {
            JSONObject obj = (JSONObject) elements.get(i);
            String type = obj.getString("type");
            if ("preCachedVideo".equals(type)) {
                obj.put("value", overrideVideoUrl);
            }
        }
        AdUnit adUnit = new AdUnitParser(base64Wrapper).parse(v2);
        assertNotNull(adUnit.getVideoUrl());
        assertTrue(adUnit.getVideoUrl().length() > 0);
        assertNotNull(adUnit.getVideoFilename());
        assertTrue(adUnit.getVideoFilename().length() > 0);
        assertEquals(expectedFilename, adUnit.getVideoFilename());
    }

    public InfoIcon randomInfoIcon() {
        return new InfoIcon(
                randomString("the info icon image url"),
                randomString("the info icon clickthrough url"),
                InfoIcon.Position.Companion.parse(TestUtils.smallRandomInt() % 4),
                new InfoIcon.DoubleSize(randomDouble() % 20.0, randomDouble() % 20.0),
                new InfoIcon.DoubleSize(randomDouble() % 20.0, randomDouble() % 20.0),
                new InfoIcon.DoubleSize(randomDouble() % 20.0, randomDouble() % 20.0)
        );
    }

    public static String randomRenderEngine() {
        String[] renderEngines = {"mraid", "html", "vast"};
        return renderEngines[TestUtils.smallRandomInt() % renderEngines.length];
    }

    public static List<String> randomScriptsArray(int maxElements) {
        return IntStream.rangeClosed(1, maxElements)
                .mapToObj(i -> "script " + i)
                .map(TestUtils::randomString)
                .collect(Collectors.toList());
    }

    public double randomDouble() {
        return (new Random()).nextDouble();
    }
}
