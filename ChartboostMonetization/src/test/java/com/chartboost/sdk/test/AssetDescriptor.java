package com.chartboost.sdk.test;

import static com.chartboost.sdk.test.TestUtils.readResourceToByteArray;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.Model.Asset;
import com.chartboost.sdk.internal.Networking.CBNetworkRequest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AssetDescriptor {
    public final Endpoint endpoint;
    public final CBNetworkRequest.Method method;
    public final String uri;
    public final String cacheDir;
    public final String filename;
    private final byte[] contents;

    public AssetDescriptor(String uri, String cacheDir) {
        this(uri, cacheDir, readResourceToByteArray(resourcePathFromUri(uri)));
    }

    public AssetDescriptor(String uri, String cacheDir, byte[] contents) {
        this.method = CBNetworkRequest.Method.GET;
        this.uri = uri;
        this.endpoint = new Endpoint(method, uri);
        this.cacheDir = cacheDir;
        this.filename = filenameFromResourcePath(resourcePathFromUri(uri));
        this.contents = contents;
    }

    public byte[] copyContents() {
        return contents.clone();
    }

    public File writeResourceToCache(File cacheBaseDir) {
        File outputFile = getCacheFile(cacheBaseDir);
        TestUtils.writeByteArrayToFile(contents, outputFile);
        return outputFile;
    }

    public void deleteFromCache(File cacheBaseDir) {
        File cacheFile = getCacheFile(cacheBaseDir);
        assertTrue(cacheFile.delete());
    }

    public void assertAssetInCache(File cacheBaseDir) {
        File cacheFile = getCacheFile(cacheBaseDir);
        TestUtils.assertFileContentsMatchByteArray(cacheFile, contents);
    }

    @NonNull
    public File getCacheFile(File cacheBaseDir) {
        return new File(new File(cacheBaseDir, cacheDir), filename);
    }

    public static Map<String, Asset> toAssets(AssetDescriptor[] assetDescriptors) {
        Map<String, Asset> assets = new HashMap<>();
        for (AssetDescriptor d : assetDescriptors) {
            assets.put(d.cacheDir + "/" + d.filename, new Asset(d.cacheDir, d.filename, d.uri));
        }
        return assets;
    }

    private static String resourcePathFromUri(String uri) {
        String prefix = "https://";
        if (!uri.startsWith(prefix))
            throw new Error("Expected uri to start with " + prefix + "  (uri=" + uri + ")");

        return "com/chartboost/cdn/" + uri.substring(prefix.length());
    }

    public static boolean resourceExistsForURI(String uri) {
        String resourcePath = resourcePathFromUri(uri);
        boolean exists = TestUtils.class.getClassLoader().getResource(resourcePath) != null;
        return exists;
    }

    private static String filenameFromResourcePath(String resourcePath) {
        File f = new File(resourcePath);
        return f.getName();
    }

    public static AssetDescriptor template(String uri) {
        return new AssetDescriptor(uri, "html");
    }

    private static AssetDescriptor video(String uri) {
        return new AssetDescriptor(uri, "videos");
    }

    private static AssetDescriptor image(String uri) {
        return new AssetDescriptor(uri, "images");
    }

    private static AssetDescriptor inPlayIcon(String uri) {
        return new AssetDescriptor(uri, "inPlayIcons");
    }

    private static AssetDescriptor videoWithDirectory(String uri, String subDir) {
        return new AssetDescriptor(uri, "videos" + "/" + subDir);
    }

    private static byte[] placeholderTemplateContents =
            readResourceToByteArray("com/chartboost/cdn/t.chartboost.com/base_templates/html/2e34e65ca1f91ee30d0953e00632abf6b4158240");
    private static byte[] placeholderVideoContents =
            readResourceToByteArray("com/chartboost/cdn/v.chartboost.com/videoads/5408018bc26ee4365868486b_568-1411069049.mp4");
    private static byte[] placeholderImageContents =
            readResourceToByteArray("com/chartboost/cdn/a.chartboost.com/creatives/568dd74788380904ad4959b3/c761294c2f1411d5b52c25662ee64d23b2d4dfcc.jpeg");

    private static AssetDescriptor placeholderTemplate(String uri) {
        return new AssetDescriptor(uri, "html", placeholderTemplateContents);
    }

    static AssetDescriptor placeholderVideo(String uri) {
        return new AssetDescriptor(uri, "videos", placeholderVideoContents);
    }

    public static AssetDescriptor placeholderImage(String uri) {
        return new AssetDescriptor(uri, "images", placeholderImageContents);
    }

    public static AssetDescriptor placeholderInPlayIcon(String uri) {
        return new AssetDescriptor(uri, "inPlayIcons", placeholderImageContents);
    }

    public static AssetDescriptor placeholder(String uri, String type) {
        switch (type) {
            case "videos":
                return placeholderVideo(uri);
            case "images":
                return placeholderImage(uri);
            default:
                throw new RuntimeException("No placeholder for type " + type);
        }
    }

    public static final AssetDescriptor baseTemplate2e34e6 =
            template("https://t.chartboost.com/base_templates/html/2e34e65ca1f91ee30d0953e00632abf6b4158240");

    public static final AssetDescriptor baseTemplate33cda9 =
            template("https://t.chartboost.com/base_templates/html/33cda9530711983713a77eff494dd126f2fe86b5");

    public static final AssetDescriptor baseTemplateff6e78 =
            template("https://t.chartboost.com/base_templates/html/ff6e789d706aa4fc97ff7ea0d906029caf7642d6");

    public static final AssetDescriptor baseTemplate8790ab =
            template("https://t.chartboost.com/base_templates/html/8790abc997d7b9b65111f4e569888084ba93b40f");

    public static final AssetDescriptor baseTemplatee770e6 =
            template("https://t.chartboost.com/base_templates/html/e770e67d3c8e2c38f3e7efc811a4a2c9c224ef3e");

    public static final AssetDescriptor baseTemplatee70814 =
            placeholderTemplate("https://t.chartboost.com/base_templates/html/e70814a038075cb9c149f5849966249d8634a746");

    public static final AssetDescriptor videoAd56ff66 =
            video("https://v2.chartboost.com/videoads/56ff669ac909a64395ce6ad5_568-1459578522.mp4");

    public static final AssetDescriptor videoAd55fb49 =
            video("https://v.chartboost.com/videoads/55fb49585b1453669ffdd4da_568-1442531710.mp4");

    public static final AssetDescriptor videoAdMagicWars1 =
            video("https://v2.chartboost.com/videoads/56f43b318838095f6461e6c4_568-1458846513.mp4");

    public static final AssetDescriptor videoAd567a70 =
            video("https://v2.chartboost.com/videoads/567a7039a8b63c7fe2161f05_568-1450864697.mp4");

    public static final AssetDescriptor videoAdMagicWars2 =
            video("https://v2.chartboost.com/videoads/56f43b46f6cd4503c0eaed4b_568-1458846534.mp4");

    public static final AssetDescriptor videoAdMagicWars3 =
            video("https://v2.chartboost.com/videoads/56f43b042fdf3406b92d996f_568-1458846468.mp4");

    public static final AssetDescriptor videoAd571459 =
            video("https://v2.chartboost.com/videoads/5714596543150f360ce290ce_568-1460951397.mp4");

    public static final AssetDescriptor videoAd571b10 =
            video("https://v.chartboost.com/videoads/571b101ff6cd4576b02b3098_568-1461391391.mp4");

    public static final AssetDescriptor videoAd57148e =
            video("https://v2.chartboost.com/videoads/57158efd04b01630fe7bb412_568-1461030653.mp4");

    public static final AssetDescriptor videoAd5704d9 =
            video("https://v2.chartboost.com/videoads/5704d94e2fdf3401a1e4ce88_568-1459935566.mp4");

    public static final AssetDescriptor videoAd55f35e =
            video("https://v.chartboost.com/videoads/55f35e3e5b1453665ef35820_568-1442012772.mp4");

    public static final AssetDescriptor videoAd56a719 =
            video("https://v2.chartboost.com/videoads/56a7190b0d60252acfeb4987_568-1453791500.mp4");

    public static final AssetDescriptor videoAd5721b5 =
            video("https://v.chartboost.com/videoads/5721b5f7f6cd4551cc0891d8_568-1461827063.mp4");

    public static final AssetDescriptor videoAd5697d4 =
            video("https://v.chartboost.com/videoads/5697d40bda152702e9e76193_568-1452791467.mp4");

    public static final AssetDescriptor videoAd570593 =
            video("https://v.chartboost.com/videoads/5705939ff6cd4560a9d4d1e9_568-1459983263.mp4");

    public static final AssetDescriptor videoAd5668ca =
            video("https://v2.chartboost.com/videoads/5668ca11a8b63c2588b4816d_568-1449708049.mp4");

    public static final AssetDescriptor videoAd56abc8 =
            video("https://v.chartboost.com/videoads/56abc8b8a8b63c559b6c7be9_568-1454098616.mp4");

    public static final AssetDescriptor videoAd56f439 =
            video("https://v.chartboost.com/videoads/56f439f5a8b63c1196588441_568-1458846197.mp4");

    public static final AssetDescriptor videoAdLandscape5705a3 =
            video("https://v.chartboost.com/videoads/5705a36a04b0166bfbfc7419_568-1459987306.mp4");

    public static final AssetDescriptor videoAdCookingFeverPizzeria =
            placeholderVideo("https://v.chartboost.com/videoads/56718adba8b63c3224cf3089_568-1450281691.mp4");

    public static final AssetDescriptor videoAdWordTrek =
            placeholderVideo("https://v2.chartboost.com/videoads/574ea86d04b0161bc69a559b_568-1464772717.mp4");

    public static final AssetDescriptor videoAdRollingSky =
            placeholderVideo("https://v2.chartboost.com/videoads/57721f9c43150f39a14db63c_568-1467096988.mp4");

    public static final AssetDescriptor videoAdGardenscapesLandscape =
            placeholderVideo("https://v2.chartboost.com/videoads/57bd94ce04b01659e33484a2_568-1472042190.mp4");

    public static final AssetDescriptor videoAdVikings =
            placeholderVideo("https://v2.chartboost.com/videoads/57ac705bf6cd456952da64c9_568-1470918747.mp4");

    public static final AssetDescriptor videoAdOperationNewEarth =
            placeholderVideo("https://v2.chartboost.com/videoads/57a2099c04b016758a9b2ba8_568-1470237084.mp4");

    public static final AssetDescriptor videoAdGameOfDiceLandscape =
            placeholderVideo("https://v.chartboost.com/videoads/56a615ec0d60251fe1add741_568-1453725164.mp4");

    public static final AssetDescriptor videoAdChartboost =
            video("https://v.chartboost.com/videoads/5408018bc26ee4365868486b_568-1411069049.mp4");

    public static final AssetDescriptor closeButton60x60 =
            image("https://a.chartboost.com/static-assets/interstitials-v2/close-buttons/60x60.png");

    public static final AssetDescriptor closeButtonVideo60x60 =
            image("https://a.chartboost.com/static-assets/interstitials-v2/close-buttons/video-60x60.png");

    public static final AssetDescriptor closeButtonWebView60x60 =
            placeholderImage("https://a.chartboost.com/static-assets/interstitials-v2/close-buttons/webview-60x60.png");

    public static final AssetDescriptor videoDefaultAssetWatch =
            image("https://a.chartboost.com/video/default_assets/watch_button.png");

    public static final AssetDescriptor videoDefaultAssetDownload =
            image("https://a.chartboost.com/video/default_assets/download.png");

    public static final AssetDescriptor videoDefaultAssetPlayFree =
            image("https://a.chartboost.com/video/default_assets/playfree.png");

    public static final AssetDescriptor videoDefaultAssetReplay =
            image("https://a.chartboost.com/video/default_assets/replay.png");

    public static final AssetDescriptor videoDefaultAssetReplayWhite =
            placeholderImage("https://a.chartboost.com/video/default_assets/replay_white.png");

    public static final AssetDescriptor videoIconRewardedCoin =
            image("https://a.chartboost.com/apps/video_icon/rewarded_coin.png");

    public static final AssetDescriptor videoFrame960x640 =
            image("https://a.chartboost.com/static-assets/interstitials-v2/frames/16-9/video-960x640.png");

    public static final AssetDescriptor interstitialFrame1088x640 =
            image("https://a.chartboost.com/static-assets/interstitials-v2/frames/16-9/1088x640.png");

    public static final AssetDescriptor interstitialFrame640x1088 =
            image("https://a.chartboost.com/static-assets/interstitials-v2/frames/9-16/640x1088.png");

    public static final AssetDescriptor interstitialFrameVideo1152x2048 =
            placeholderImage("https://a.chartboost.com/static-assets/interstitials-v2/frames/9-16/video-1152x2048.png");

    public static final AssetDescriptor interstitialFrameVideo2048x1536 =
            placeholderImage("https://a.chartboost.com/static-assets/interstitials-v2/frames/4-3/video-2048x1536.png");

    public static final AssetDescriptor interstitialFrame1536x2048 =
            placeholderImage("https://a.chartboost.com/static-assets/interstitials-v2/frames/3-4/1536x2048.png");

    public static final AssetDescriptor interstitialFrame2048x1536 =
            placeholderImage("https://a.chartboost.com/static-assets/interstitials-v2/frames/4-3/2048x1536.png");

    public static final AssetDescriptor appIcon568dd7 =
            image("https://a.chartboost.com/apps/icons/568dd74788380904ad4959b3.114.png");

    public static final AssetDescriptor inPlayIcon568dd7 =
            inPlayIcon("https://a.chartboost.com/apps/icons/568dd74788380904ad4959b3.114.png");

    public static final AssetDescriptor appIcon5620c4 =
            placeholderImage("https://a.chartboost.com/apps/icons/5620c4970d60256598edc1f3.114.png");

    public static final AssetDescriptor inPlayIcon5620c4 =
            placeholderInPlayIcon("https://a.chartboost.com/apps/icons/5620c4970d60256598edc1f3.114.png");

    public static final AssetDescriptor imageSwordOfChaos =
            image("https://a.chartboost.com/creatives/568dd74788380904ad4959b3/c761294c2f1411d5b52c25662ee64d23b2d4dfcc.jpeg");

    public static final AssetDescriptor videoSwordOfChaos =
            video("https://v.chartboost.com/videoads/5714774e04b01630fe7aa6c3_568-1460959055.mp4");

    public static final AssetDescriptor imageJewelCrack =
            image("https://a.chartboost.com/creatives/56f3d75bf6cd450398f699ec/0e0e81750d00e5e7721ddcf461baa06fbdd353e2.jpeg");

    public static final AssetDescriptor imageLevel23 =
            image("https://a.chartboost.com/creatives/5089cc9016ba476935000000/aec19913ad09298d9ce38f3ef3da33f610d949cc.jpeg");

    public static final AssetDescriptor videoAd55fb49WithSubDir =
            videoWithDirectory("https://v.chartboost.com/videoads/55fb49585b1453669ffdd4da_568-1442531710.mp4", "chartboost/subdir1");

    public static final AssetDescriptor imageBirdBubblzLandscape =
            placeholderImage("https://a.chartboost.com/creatives/57b5403d04b01675dda1bca8/4fa500449965604e5469a182ac904400df8f792d.jpeg");

    public static final AssetDescriptor imageBirdBubblzPortrait =
            placeholderImage("https://a.chartboost.com/creatives/57b5403d04b01675dda1bca8/62fddfb546595952866b19809e0a9b65dbb754b9.jpeg");

    public static final AssetDescriptor imageGameOfDicePortrait =
            placeholderImage("https://a.chartboost.com/creatives/5620c4970d60256598edc1f3/7ac13eaf07890fc38ac9814ceef0596389db15cb.jpeg");

    public static final AssetDescriptor imageGameOfDiceLandscape =
            placeholderImage("https://a.chartboost.com/creatives/5620c4970d60256598edc1f3/be234a37803b7156ab1a0fcf0a5b35162ebc5ace.jpeg");

    public static final AssetDescriptor imageBustersBoostLandscape =
            placeholderImage("https://a.chartboost.com/creatives/4f3586b4cd1cb2c25400001c/241f704ef5ee85333bf40516890926db510cada5.jpeg");
    public static final AssetDescriptor imageBustersBoostPortrait =
            placeholderImage("https://a.chartboost.com/creatives/4f3586b4cd1cb2c25400001c/10b182e3a2c48da620083b0bb8173618cf4ac9cc.jpeg");
    public static final AssetDescriptor imageBustersBoostIcon =
            placeholderImage("https://a.chartboost.com/apps/icons/4f3586b4cd1cb2c25400001c.114.png");

    public ResponseDescriptor notFound() {
        return endpoint.notFound();
    }

    public Asset toAsset() {
        return new Asset(cacheDir, filename, uri);
    }

    public ResponseDescriptor respond() {
        return new ResponseDescriptor(this);
    }
}
