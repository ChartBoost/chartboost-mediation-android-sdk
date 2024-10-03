package com.chartboost.sdk.internal.Networking.requests;

import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Model.NetworkParameters;
import com.chartboost.sdk.internal.Model.OpenRTBRequestModel;
import com.chartboost.sdk.internal.Networking.AdParameters;
import com.chartboost.sdk.internal.Networking.CBNetworkRequestResult;
import com.chartboost.sdk.internal.Networking.CBNetworkServerResponse;
import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager;
import com.chartboost.sdk.tracking.EventTracker;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenRTBRequest extends CBRequest {
    public OpenRTBRequest(
            NetworkParameters networkParameters,
            AdParameters adParameters,
            OpenMeasurementManager omManager,
            EventTracker eventTracker
    ) {
        super(
                networkParameters.method,
                networkParameters.endpoint,
                networkParameters.path,
                networkParameters.requestBodyFields,
                networkParameters.priority,
                null,
                networkParameters.callback,
                eventTracker
        );
        this.body = new OpenRTBRequestModel(
                networkParameters.requestBodyFields,
                adParameters,
                omManager
        ).getJsonRepresentation();
    }

    @Override
    public void appendRequestBodyInfoParams() {
    }

    @Override
    public CBNetworkRequestResult<JSONObject> parseServerResponse(@NotNull CBNetworkServerResponse response) {
        try {
            final JSONObject json = new JSONObject(new String(response.getData()));
            return CBNetworkRequestResult.success(json);
        } catch (JSONException e) {
            Logger.e("parseServerResponse", e);
            return CBNetworkRequestResult.failure(
                    new CBError(
                            CBError.Internal.HTTP_NOT_FOUND,
                            "No Bid"
                    )
            );
        }
    }
}
