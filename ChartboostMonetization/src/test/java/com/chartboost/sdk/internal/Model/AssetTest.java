package com.chartboost.sdk.internal.Model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.ReferenceResponse;
import com.chartboost.sdk.test.TestContainer;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssetTest extends BaseTest {
    @Test
    public void v2PrefetchToAssetsAndTemplates() throws JSONException {
        try (TestContainer tc = new TestContainer()) {
            JSONObject v2PrefetchResponse = ReferenceResponse.webviewV2PrefetchWithResults.asJSONObject();

            Map<String, Asset> assets = Asset.v2PrefetchToAssets(v2PrefetchResponse, 10);

            List<String> expectedAssetFilenames = new ArrayList<>();
            for (AssetDescriptor d : ReferenceResponse.webviewV2PrefetchWithResultsAssetDescriptors) {
                expectedAssetFilenames.add(d.filename);
            }

            assertThat(assets.keySet(), containsInAnyOrder(expectedAssetFilenames.toArray(new String[0])));
            for (AssetDescriptor d : ReferenceResponse.webviewV2PrefetchWithResultsAssetDescriptors) {
                Asset a = assets.get(d.filename);
                assertThat(a.filename, is(d.filename));
                assertThat(a.directory, is(d.cacheDir));
                assertThat(a.url, is(d.uri));
            }
        }
    }
}
