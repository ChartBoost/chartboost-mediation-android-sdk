package com.chartboost.sdk.internal.AssetLoader;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.chartboost.sdk.BuildConfig;
import com.chartboost.sdk.internal.Model.Asset;
import com.chartboost.sdk.internal.Networking.CBNetworkRequest;
import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Networking.ConnectionType;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer;
import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestContainerBuilder;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

public class AssetRequestTest extends BaseTest {
    @Test
    public void testCDNHeaders() {
        ChartboostDependencyContainer.INSTANCE.start("appid", "signature");
        final AssetDescriptor assetDescriptor = AssetDescriptor.baseTemplate2e34e6;
        for (ConnectionType connectionType : ConnectionType.values()) {
            try (TestContainer tc = TestContainerBuilder.defaultWebView()
                    .withNetworkService()
                    .withDownloader()
                    .withResponse(assetDescriptor)
                    .withReachabilityConnectionType(connectionType)
                    .build()) {

                Map<String, Asset> assets = new HashMap<>();
                final Asset asset = assetDescriptor.toAsset();
                assets.put("a", asset);
                tc.downloader.downloadAssets(Priority.NORMAL, assets, new AtomicInteger(0), null, "");
                tc.run();

                String expectedClient = String.format("Chartboost-Android-SDK  %s", BuildConfig.SDK_VERSION);
                HttpsURLConnection assetConn = tc.mockNetworkFactory.getMockConnectionReturnedForRequest(CBNetworkRequest.Method.GET, asset.url);
                verify(assetConn).addRequestProperty("X-Chartboost-App", tc.appId);
                verify(assetConn).addRequestProperty("X-Chartboost-Client", expectedClient);
                verify(assetConn).addRequestProperty("X-Chartboost-Reachability", Integer.toString(connectionType.getValue()));
                verify(assetConn, times(3)).addRequestProperty(anyString(), anyString()); // and nothing else
            }
        }
    }
}
