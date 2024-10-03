package com.chartboost.sdk.internal.Networking.requests;

import static com.chartboost.sdk.internal.Libraries.CBConstants.REQUEST_PARAM_BID_REQUEST_GPP_CONSENT;
import static com.chartboost.sdk.internal.Libraries.CBConstants.REQUEST_PARAM_BID_REQUEST_GPP_CONSENT_SID;
import static com.chartboost.sdk.internal.Libraries.CBConstants.REQUEST_PARAM_PRIVACY;
import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;
import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;
import static com.chartboost.sdk.internal.Networking.CBNetworkRequest.Method.POST;

import com.chartboost.sdk.ChartboostDSP;
import com.chartboost.sdk.SandboxBridgeSettings;
import com.chartboost.sdk.internal.ChartboostDSPHelper;
import com.chartboost.sdk.internal.External.Android;
import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Libraries.CBCrypto;
import com.chartboost.sdk.internal.Libraries.CBJSON;
import com.chartboost.sdk.internal.Libraries.CBUtility;
import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Model.IdentityBodyFields;
import com.chartboost.sdk.internal.Model.MediationBodyFields;
import com.chartboost.sdk.internal.Model.PrivacyBodyFields;
import com.chartboost.sdk.internal.Model.RequestBodyFields;
import com.chartboost.sdk.internal.Networking.CBNetworkRequest;
import com.chartboost.sdk.internal.Networking.CBNetworkRequestInfo;
import com.chartboost.sdk.internal.Networking.CBNetworkRequestResult;
import com.chartboost.sdk.internal.Networking.CBNetworkServerResponse;
import com.chartboost.sdk.internal.Networking.NetworkHelper;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.internal.WebView.UserAgentHelper;
import com.chartboost.sdk.internal.identity.TrackingState;
import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.tracking.CriticalEvent;
import com.chartboost.sdk.tracking.EventTracker;
import com.chartboost.sdk.tracking.TrackingEventName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CBRequest extends CBNetworkRequest<JSONObject> {
    /**
     * Default content type for request.
     */
    private static final String PROTOCOL_CONTENT_TYPE = "application/json";

    private final String path;
    private final String eventType;
    // Public :(
    public final CBAPINetworkResponseCallback callback;
    protected final RequestBodyFields requestBodyFields;
    private final EventTracker eventTracker;

    public JSONObject body;
    public JSONArray bodyArray; // only used for tracking to save body when requests fails

    // If true, response body must contain "status" field 200 <= x <= 299 to be successful.
    // TODO What is the use-case for this flag?
    public boolean checkStatusInResponseBody = false;

    public CBRequest(
            String endpoint,
            String path,
            RequestBodyFields requestBodyFields,
            Priority priority,
            CBAPINetworkResponseCallback callback,
            EventTracker eventTracker
    ) {
        this(endpoint, path, requestBodyFields, priority, null, callback, eventTracker);
    }

    public CBRequest(
            String endpoint,
            String path,
            RequestBodyFields requestBodyFields,
            Priority priority,
            String eventType,
            CBAPINetworkResponseCallback callback,
            EventTracker eventTracker
    ) {
        this(POST, endpoint, path, requestBodyFields, priority, eventType, callback, eventTracker);
    }

    public CBRequest(
            CBNetworkRequest.Method method,
            String endpoint,
            String path,
            RequestBodyFields requestBodyFields,
            Priority priority,
            String eventType,
            CBAPINetworkResponseCallback callback,
            EventTracker eventTracker
    ) {
        super(method, NetworkHelper.normalizedUrl(endpoint, path), priority, null);
        this.body = new JSONObject();
        this.path = path;
        this.requestBodyFields = requestBodyFields;
        this.eventType = eventType;
        this.callback = callback;
        this.eventTracker = eventTracker;
    }

    public void appendBodyArgument(String key, Object value) {
        CBJSON.put(body, key, value);
    }

    /**
     * Append information about device
     */
    public void appendRequestBodyInfoParams() {
        appendBodyArgument(CBConstants.REQUEST_PARAM_APP, requestBodyFields.REQUEST_PARAM_APP);
        appendBodyArgument(CBConstants.REQUEST_PARAM_MODEL, requestBodyFields.REQUEST_PARAM_MODEL);
        appendBodyArgument(CBConstants.REQUEST_PARAM_DEVICE_MAKE, requestBodyFields.REQUEST_PARAM_DEVICE_MAKE);
        appendBodyArgument(CBConstants.REQUEST_PARAM_DEVICE_TYPE, requestBodyFields.REQUEST_PARAM_DEVICE_TYPE);
        appendBodyArgument(CBConstants.REQUEST_PARAM_ACTUAL_DEVICE_TYPE, requestBodyFields.REQUEST_PARAM_ACTUAL_DEVICE_TYPE);
        appendBodyArgument(CBConstants.REQUEST_PARAM_OS, requestBodyFields.REQUEST_PARAM_OS);
        appendBodyArgument(CBConstants.REQUEST_PARAM_COUNTRY, requestBodyFields.REQUEST_PARAM_COUNTRY);
        appendBodyArgument(CBConstants.REQUEST_PARAM_LANGUAGE, requestBodyFields.REQUEST_PARAM_LANGUAGE);
        appendBodyArgument(CBConstants.REQUEST_PARAM_SDK, requestBodyFields.REQUEST_PARAM_SDK_VERSION);
        appendBodyArgument(CBConstants.REQUEST_PARAM_USER_AGENT, UserAgentHelper.INSTANCE.getWebViewUserAgentValue());

        String timestamp = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(requestBodyFields.getTimeSourceBodyField().getCurrentTimeMillis()));
        appendBodyArgument(CBConstants.REQUEST_PARAM_TIMESTAMP, timestamp);
        appendBodyArgument(CBConstants.REQUEST_PARAM_SESSION, requestBodyFields.getSessionCount());
        appendBodyArgument(CBConstants.REQUEST_PARAM_REACHABILITY, requestBodyFields.getReachabilityBodyFields().getConnectionTypeFromActiveNetwork());
        appendBodyArgument(CBConstants.REQUEST_PARAM_IS_PORTRAIT, requestBodyFields.getDeviceBodyFields().isPortrait());
        appendBodyArgument(CBConstants.REQUEST_PARAM_SCALE, requestBodyFields.getDeviceBodyFields().getScale());
        appendBodyArgument(CBConstants.REQUEST_PARAM_VERSION, requestBodyFields.REQUEST_PARAM_VERSION);
        appendBodyArgument(CBConstants.REQUEST_PARAM_PACKAGE, requestBodyFields.REQUEST_PARAM_PACKAGE);
        appendBodyArgument(CBConstants.REQUEST_PARAM_CARRIER, requestBodyFields.REQUEST_PARAM_CARRIER_INFO);

        MediationBodyFields mediationBodyFields = requestBodyFields.getMediationBodyFields();
        if (mediationBodyFields != null) {
            appendBodyArgument(CBConstants.REQUEST_PARAM_MEDIATION, mediationBodyFields.getMediationName());
            appendBodyArgument(CBConstants.REQUEST_PARAM_MEDIATION_VERSION, mediationBodyFields.getLibraryVersion());
            appendBodyArgument(CBConstants.REQUEST_PARAM_ADAPTER_VERSION, mediationBodyFields.getAdapterVersion());
        }

        appendBodyArgument(CBConstants.REQUEST_PARAM_TIMEZONE, requestBodyFields.REQUEST_PARAM_TIMEZONE);
        appendBodyArgument(
                CBConstants.REQUEST_PARAM_CONNECTION_TYPE,
                requestBodyFields.getReachabilityBodyFields().getOpenRTBConnectionType().getValue()
        );
        appendBodyArgument(CBConstants.REQUEST_PARAM_DEVICE_WIDTH, requestBodyFields.getDeviceBodyFields().getDeviceWidth());
        appendBodyArgument(CBConstants.REQUEST_PARAM_DEVICE_HEIGHT, requestBodyFields.getDeviceBodyFields().getDeviceHeight());
        appendBodyArgument(CBConstants.REQUEST_PARAM_DEVICE_DPI, requestBodyFields.getDeviceBodyFields().getDpi());
        appendBodyArgument(CBConstants.REQUEST_PARAM_WIDTH, requestBodyFields.getDeviceBodyFields().getWidth());
        appendBodyArgument(CBConstants.REQUEST_PARAM_HEIGHT, requestBodyFields.getDeviceBodyFields().getHeight());
        appendBodyArgument(CBConstants.REQUEST_PARAM_COMMIT_HASH, CBConstants.RELEASE_COMMIT_HASH);

        IdentityBodyFields identity = requestBodyFields.getIdentityBodyFields();
        if (identity != null) {
            appendBodyArgument(CBConstants.REQUEST_PARAM_IDENTITY, identity.getIdentifiers());
            TrackingState trackingState = identity.getTrackingState();
            if (trackingState != TrackingState.TRACKING_UNKNOWN) {
                boolean limitAdTracking = trackingState == TrackingState.TRACKING_LIMITED;
                appendBodyArgument(CBConstants.REQUEST_PARAM_LIMIT_AD_TRACKING, limitAdTracking);
            }

            Integer setIdScope = identity.getSetIdScope();
            if (setIdScope != null) {
                appendBodyArgument(CBConstants.REQUEST_PARAM_SET_ID_SCOPE, setIdScope);
            }
        } else {
            Logger.e("Missing identity in the CB SDK. This will affect ads performance.", null);
        }

        PrivacyBodyFields privacyBodyFields = requestBodyFields.getPrivacyBodyFields();
        String tcfString = privacyBodyFields.getTcfString();
        if (tcfString != null) {
            appendBodyArgument(CBConstants.REQUEST_PARAM_TCFV2_CONSENT, tcfString);
        }

        appendBodyArgument(CBConstants.REQUEST_PARAM_PUBLISHER_LIMIT_AD_TRACKING, privacyBodyFields.getPiDataUseConsent());

        String configVariant = requestBodyFields.getConfigurationFields().getConfigVariant();
        if (!Android.instance().isEmpty(configVariant)) {
            appendBodyArgument(CBConstants.REQUEST_PARAM_CONFIG_VARIANT, configVariant);
        }

        final JSONObject privacyList = privacyBodyFields.getPrivacyListAsJson();
        final String gppString = privacyBodyFields.getGppString();
        final String gppSid = privacyBodyFields.getGppSid();

        if (privacyList != null) {
            try {
                privacyList.put(REQUEST_PARAM_BID_REQUEST_GPP_CONSENT, gppString);
                privacyList.put(REQUEST_PARAM_BID_REQUEST_GPP_CONSENT_SID, gppSid);
            } catch (JSONException e) {
                Logger.e("Failed to add GPP and/or GPP SID to request body", e);
            }
        }

        appendBodyArgument(REQUEST_PARAM_PRIVACY, privacyList);
    }

    /**
     * Return the URI of this request based on its path and any query parameters.
     */
    public String uri() {
        return getPath();
    }

    /**
     * SETTERS AND GETTERS
     */

    public String getPath() {
        return (path == null) ? "/" : ((path.startsWith("/") ? "" : "/") + path);
    }

    /**
     * Send the session logs for every {@link CBRequest} thats processed
     */
    private void sendToSessionLogs(CBNetworkServerResponse response, CBError error) {
        /*Create the metaData*/
        JSONObject metaData = jsonObject(JKV("endpoint", getPath()),
                JKV("statuscode", response == null ? "None" : response.getStatusCode()),
                JKV("error", error == null ? "None" : error.getType().toString()),
                JKV("errorDescription", error == null ? "None" : error.getErrorDesc()),
                JKV("retryCount", 0));

        Logger.d("sendToSessionLogs: " + metaData, null);
    }

    public String getEventType() {
        return eventType;
    }

    /* CBNetworkRequest overrides */
    @Override
    public CBNetworkRequestInfo buildRequestInfo() {

        appendRequestBodyInfoParams();

        String body = this.body.toString();

        String appId = requestBodyFields.REQUEST_PARAM_APP;
        String appSignature = requestBodyFields.REQUEST_PARAM_SIGNATURE;
        String description = String.format(Locale.US, "%s %s\n%s\n%s",
                getMethod(), uri(), appSignature, body);
        String signature = CBCrypto.getSha1Hex(description);

        Map<String, String> headers = new HashMap<>();
        headers.put(CBConstants.REQUEST_PARAM_ACCEPT_HEADER_KEY, CBConstants.REQUEST_PARAM_HEADER_VALUE);
        headers.put(CBConstants.REQUEST_PARAM_CLIENT_HEADER_KEY, CBUtility.getUserAgent());
        headers.put(CBConstants.REQUEST_PARAM_HEADER_KEY, CBConstants.API_VERSION);
        headers.put(CBConstants.REQUEST_PARAM_APP_HEADER_KEY, appId);
        headers.put(CBConstants.REQUEST_PARAM_SIGNATURE_HEADER_KEY, signature);

        if (SandboxBridgeSettings.INSTANCE.isSandboxMode()) {
            String creativeHeader = SandboxBridgeSettings.getHeader();
            if (!creativeHeader.isEmpty()) {
                headers.put(CBConstants.REQUEST_PARAM_HEADER_TEST_KEY, creativeHeader);
            }

            String customHeader = SandboxBridgeSettings.getCustomHeader();
            if (customHeader != null) {
                headers.put(CBConstants.REQUEST_PARAM_HEADER_TEST_KEY, customHeader);
            }
        }

        if (ChartboostDSP.INSTANCE.isDSP()) {
            String dspHeader = buildDspHeader();
            if (dspHeader != null && !dspHeader.isEmpty()) {
                headers.put(CBConstants.REQUEST_PARAM_HEADER_DSP_KEY, dspHeader);
            }
        }

        return new CBNetworkRequestInfo(headers, body.getBytes(), PROTOCOL_CONTENT_TYPE);
    }

    /**
     * Create DSP header only for the DSP Sample application
     *
     * @return
     */
    private String buildDspHeader() {
        String dspCode = ChartboostDSPHelper.INSTANCE.getDspCode();
        int[] creativeTypes = ChartboostDSPHelper.INSTANCE.getDspCreatives();

        JSONObject dspHeaderJson = new JSONObject();
        if (!dspCode.isEmpty() && creativeTypes != null && creativeTypes.length > 0) {
            try {
                JSONArray creativeTypesArrayJson = new JSONArray();
                for (int creativeType : creativeTypes) {
                    creativeTypesArrayJson.put(creativeType);
                }
                dspHeaderJson.put("exchangeMode", 2);
                dspHeaderJson.put("bidFloor", 0.01);
                dspHeaderJson.put("code", dspCode);
                dspHeaderJson.put("forceCreativeTypes", creativeTypesArrayJson);
            } catch (JSONException e) {
                return null;
            }
        }
        return dspHeaderJson.toString();
    }

    @Override
    public CBNetworkRequestResult<JSONObject> parseServerResponse(CBNetworkServerResponse response) {
        try {
            final JSONObject json = new JSONObject(new String(response.getData()));

            Logger.v(
                    "Request " + getPath() + " succeeded. " +
                            "Response code: " + response.getStatusCode()
                            + ", body: " + json.toString(4),
                    null
            );

            if (checkStatusInResponseBody) {
                final int innerStatus = json.optInt("status");
                final String innerMessage = json.optString("message");

                if (innerStatus == 404) {
                    return getHTTPNotFoundError(innerMessage);
                }

                if (innerStatus < 200 || innerStatus > 299) {
                    final String errorMessage = "Request failed due to status code " + innerStatus + " in message";
                    Logger.e(errorMessage, null);
                    return getHttpNotOkError(innerStatus, innerMessage);
                }
            }

            return CBNetworkRequestResult.success(json);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null) {
                errorMessage = "";
            }
            trackJSONSerializationError(errorMessage);
            Logger.e("parseServerResponse", e);
            return getMiscError(e);
        }
    }

    private CBNetworkRequestResult<JSONObject> getHTTPNotFoundError(String errorMessage) {
        final JSONObject errorJson = getJSONErrorDescriptionFromServer(404, errorMessage);
        return CBNetworkRequestResult.failure(
                new CBError(
                        CBError.Internal.HTTP_NOT_FOUND,
                        errorJson.toString()
                )
        );
    }

    private CBNetworkRequestResult<JSONObject> getHttpNotOkError(int status, String errorMessage) {
        final JSONObject errorJson = getJSONErrorDescriptionFromServer(status, errorMessage);
        return CBNetworkRequestResult.failure(
                new CBError(
                        CBError.Internal.HTTP_NOT_OK,
                        errorJson.toString()
                )
        );
    }

    private JSONObject getJSONErrorDescriptionFromServer(int status, String message) {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("status", status);
            jsonObject.put("message", message);
        } catch (JSONException e) {
            Logger.e("Error creating JSON", e);
        }
        return jsonObject;
    }

    private CBNetworkRequestResult<JSONObject> getMiscError(Exception e) {
        return CBNetworkRequestResult.failure(
                new CBError(CBError.Internal.MISCELLANEOUS, e.getLocalizedMessage())
        );
    }

    private void trackJSONSerializationError(String errorMessage) {
        eventTracker.track(
                CriticalEvent.instance(
                        TrackingEventName.Network.RESPONSE_JSON_SERIALIZATION_ERROR,
                        errorMessage
                )
        );
    }

    /**
     * respond to a successful network request on the UI thread
     */
    @Override
    public void deliverResponse(JSONObject json, CBNetworkServerResponse serverResponse) {
        int statusCode = -1;
        if (serverResponse != null) {
            statusCode = serverResponse.getStatusCode();
        }

        Logger.v( "Request success: " + getUri() + " status: " + statusCode, null);

        if (callback != null && json != null) {
            callback.onSuccess(this, json);
        }

        sendToSessionLogs(serverResponse, null);
    }

    /**
     * respond to a failed network request on the UI thread
     */
    @Override
    public void deliverError(CBError error, CBNetworkServerResponse serverResponse) {
        if (error == null) return;

        Logger.v( "Request failure: " + getUri() + " status: " + error.getErrorDesc(), null);

        if (callback != null) {
            callback.onFailure(this, error);
        }

        sendToSessionLogs(serverResponse, error);
    }

    /**
     * NETWORK RESPONSE CALLBACK  CLASS AND INTERFACES
     */

    public interface CBAPINetworkResponseCallback {
        void onSuccess(CBRequest request, JSONObject response);

        void onFailure(CBRequest request, CBError error);
    }

    /**
     * DEBUGGING
     */

    public String getDescription() {
        return String.format(Locale.US, "<\n%s %s\n%s\n>",
                getMethod(),
                getPath(),
                body.toString()).replace("\n", "\n ").replace("\n >", "\n>");
    }
}
