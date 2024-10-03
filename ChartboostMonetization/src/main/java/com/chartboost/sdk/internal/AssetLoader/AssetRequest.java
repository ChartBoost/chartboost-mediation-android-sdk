package com.chartboost.sdk.internal.AssetLoader;

import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Libraries.CBUtility;
import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Networking.CBNetworkRequest;
import com.chartboost.sdk.internal.Networking.CBNetworkRequestInfo;
import com.chartboost.sdk.internal.Networking.CBNetworkServerResponse;
import com.chartboost.sdk.internal.Networking.CBReachability;
import com.chartboost.sdk.internal.Priority;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

class AssetRequest extends CBNetworkRequest<Void> {

    private final Downloader downloader;
    private final CBReachability reachability;
    final AssetInfo assetInfo;
    private final String appId;

    AssetRequest(
            Downloader downloader,
            CBReachability reachability,
            AssetInfo assetInfo,
            File outputFile,
            String appId
    ) {
        super(CBNetworkRequest.Method.GET, assetInfo.uri, Priority.NORMAL, outputFile);
        this.dispatch = Dispatch.ASYNC;
        this.downloader = downloader;
        this.reachability = reachability;
        this.assetInfo = assetInfo;
        this.appId = appId;
    }

    @Override
    public CBNetworkRequestInfo buildRequestInfo() {
        Map<String, String> headers = new HashMap<>();

        headers.put(CBConstants.REQUEST_PARAM_APP_HEADER_KEY, appId);
        headers.put(CBConstants.REQUEST_PARAM_CLIENT_HEADER_KEY, CBUtility.getUserAgent());
        headers.put(CBConstants.REQUEST_PARAM_REACHABILITY_HEADER_KEY, Integer.toString(reachability.connectionTypeFromActiveNetwork().getValue()));

        return new CBNetworkRequestInfo(headers, null, null);
    }

    @Override
    public void deliverResponse(Void response, CBNetworkServerResponse serverResponse) {
        downloader.onAssetDownloadResult(this, null, null);
    }

    @Override
    public void deliverError(CBError error, CBNetworkServerResponse response) {
        downloader.onAssetDownloadResult(this, error, response);
    }
}
