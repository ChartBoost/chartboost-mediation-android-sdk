package com.chartboost.sdk.internal.Networking.requests;

import static com.chartboost.sdk.internal.Libraries.CBConstants.REQUEST_PARAM_BID_REQUEST_GPP_CONSENT;
import static com.chartboost.sdk.internal.Libraries.CBConstants.REQUEST_PARAM_BID_REQUEST_GPP_CONSENT_SID;
import static com.chartboost.sdk.internal.Libraries.CBConstants.REQUEST_PARAM_PRIVACY;
import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;

import com.chartboost.sdk.internal.External.Android;
import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Libraries.CBJSON;
import com.chartboost.sdk.internal.Model.IdentityBodyFields;
import com.chartboost.sdk.internal.Model.PrivacyBodyFields;
import com.chartboost.sdk.internal.Model.RequestBodyFields;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.internal.WebView.UserAgentHelper;
import com.chartboost.sdk.internal.identity.TrackingState;
import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.tracking.Environment;
import com.chartboost.sdk.tracking.EventTracker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public final class CBWebViewRequest extends CBRequest {
    private final JSONObject sdkBody;
    private final JSONObject appBody;
    private final JSONObject deviceBody;
    private final JSONObject adBody;
    private final JSONObject bidRequestBody;

    public CBWebViewRequest(
            String path,
            RequestBodyFields requestBodyFields,
            Priority priority,
            CBAPINetworkResponseCallback callback,
            EventTracker eventTracker
    ) {
        this(
                Method.POST,
                CBConstants.API_ENDPOINT,
                path,
                requestBodyFields,
                priority,
                null,
                callback,
                eventTracker
        );
    }

    public CBWebViewRequest(
            Method method,
            String host,
            String path,
            RequestBodyFields requestBodyFields,
            Priority priority,
            String eventType,
            CBAPINetworkResponseCallback callback,
            EventTracker eventTracker
    ) {
        super(method, host, path, requestBodyFields, priority, eventType, callback, eventTracker);
        sdkBody = new JSONObject();
        appBody = new JSONObject();
        deviceBody = new JSONObject();
        adBody = new JSONObject();
        bidRequestBody = new JSONObject();
    }


    @Override
    public void appendRequestBodyInfoParams() {
        /* App Body Information*/
        CBJSON.put(appBody, CBConstants.REQUEST_PARAM_APP, requestBodyFields.REQUEST_PARAM_APP);
        CBJSON.put(appBody, CBConstants.REQUEST_PARAM_VERSION, requestBodyFields.REQUEST_PARAM_VERSION);
        CBJSON.put(appBody, CBConstants.REQUEST_PARAM_PACKAGE, requestBodyFields.REQUEST_PARAM_PACKAGE);
        CBJSON.put(appBody, CBConstants.WEB_REQUEST_PARAM_SESSION_ID, "");
        CBJSON.put(appBody, CBConstants.WEB_REQUEST_PARAM_UI, -1);
        CBJSON.put(appBody, CBConstants.WEB_REQUEST_PARAM_TEST_MODE, false);

        appendBodyArgument(CBConstants.WEB_REQUEST_PARAM_CONTAINER_APP, appBody);

        /* Bid Request Body Information */
        final JSONObject bidRequestAppBody = CBJSON.jsonObject(
                JKV(CBConstants.REQUEST_PARAM_BID_REQUEST_APP_VER, Environment.Companion.getAppVersion())
        );

        CBJSON.put(bidRequestBody, CBConstants.REQUEST_PARAM_BID_REQUEST_APP, bidRequestAppBody);

        appendBodyArgument(CBConstants.WEB_REQUEST_PARAM_BID_REQUEST, bidRequestBody);

        /* Device Body Information */
        JSONObject newCarrierInfo = CBJSON.jsonObject(
                JKV(CBConstants.WEB_REQUEST_PARAM_CARRIER_NAME, requestBodyFields.REQUEST_PARAM_CARRIER_INFO.optString(CBConstants.REQUEST_PARAM_CARRIER_NAME)),
                JKV(CBConstants.WEB_REQUEST_PARAM_MCC, requestBodyFields.REQUEST_PARAM_CARRIER_INFO.optString(CBConstants.REQUEST_PARAM_MCC)),
                JKV(CBConstants.WEB_REQUEST_PARAM_MNC, requestBodyFields.REQUEST_PARAM_CARRIER_INFO.optString(CBConstants.REQUEST_PARAM_MNC)),
                JKV(CBConstants.WEB_REQUEST_PARAM_ISO, requestBodyFields.REQUEST_PARAM_CARRIER_INFO.optString(CBConstants.REQUEST_PARAM_ISO)),
                JKV(CBConstants.WEB_REQUEST_PARAM_PHONE_TYPE, requestBodyFields.REQUEST_PARAM_CARRIER_INFO.optInt(CBConstants.REQUEST_PARAM_PHONE_TYPE)));
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_CARRIER, newCarrierInfo);

        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_MODEL, requestBodyFields.REQUEST_PARAM_MODEL);
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_DEVICE_MAKE, requestBodyFields.REQUEST_PARAM_DEVICE_MAKE);
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_DEVICE_TYPE, requestBodyFields.REQUEST_PARAM_DEVICE_TYPE);
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_ACTUAL_DEVICE_TYPE, requestBodyFields.REQUEST_PARAM_ACTUAL_DEVICE_TYPE);
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_OS, requestBodyFields.REQUEST_PARAM_OS);
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_COUNTRY, requestBodyFields.REQUEST_PARAM_COUNTRY);
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_LANGUAGE, requestBodyFields.REQUEST_PARAM_LANGUAGE);
        String timestamp = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(requestBodyFields.getTimeSourceBodyField().getCurrentTimeMillis()));
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_TIMESTAMP, timestamp);
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_REACHABILITY, requestBodyFields.getReachabilityBodyFields().getConnectionTypeFromActiveNetwork());
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_IS_PORTRAIT, requestBodyFields.getDeviceBodyFields().isPortrait());
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_SCALE, requestBodyFields.getDeviceBodyFields().getScale());
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_TIMEZONE, requestBodyFields.REQUEST_PARAM_TIMEZONE);
        CBJSON.put(
                deviceBody,
                CBConstants.REQUEST_PARAM_CONNECTION_TYPE,
                requestBodyFields.getReachabilityBodyFields().getOpenRTBConnectionType().getValue()
        );
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_DEVICE_WIDTH, requestBodyFields.getDeviceBodyFields().getDeviceWidth());
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_DEVICE_HEIGHT, requestBodyFields.getDeviceBodyFields().getDeviceHeight());
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_DEVICE_DPI, requestBodyFields.getDeviceBodyFields().getDpi());
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_WIDTH, requestBodyFields.getDeviceBodyFields().getWidth());
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_HEIGHT, requestBodyFields.getDeviceBodyFields().getHeight());
        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_USER_AGENT, UserAgentHelper.INSTANCE.getWebViewUserAgentValue());
        CBJSON.put(deviceBody, CBConstants.WEB_REQUEST_PARAM_DEVICE_FAMILY, "");
        CBJSON.put(deviceBody, CBConstants.WEB_REQUEST_PARAM_RETINA, false);

        IdentityBodyFields identity = requestBodyFields.getIdentityBodyFields();
        if (identity != null) {
            CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_IDENTITY, identity.getIdentifiers());
            TrackingState trackingState = identity.getTrackingState();
            if (trackingState != TrackingState.TRACKING_UNKNOWN) {
                boolean limitAdTracking = trackingState == TrackingState.TRACKING_LIMITED;
                CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_LIMIT_AD_TRACKING, limitAdTracking);
            }

            Integer setIdScope = identity.getSetIdScope();
            if (setIdScope != null) {
                CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_SET_ID_SCOPE, setIdScope);
            }
        } else {
            Logger.e("Missing identity in the CB SDK. This will affect ads performance.", null);
        }

        final PrivacyBodyFields privacyBodyFields = requestBodyFields.getPrivacyBodyFields();
        final String gppString = privacyBodyFields.getGppString();
        final String gppSid = privacyBodyFields.getGppSid();

        String tcfString = privacyBodyFields.getTcfString();
        if (tcfString != null) {
            CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_TCFV2_CONSENT, tcfString);
        }

        CBJSON.put(deviceBody, CBConstants.REQUEST_PARAM_PUBLISHER_LIMIT_AD_TRACKING, privacyBodyFields.getPiDataUseConsent());
        final JSONObject privacyList = privacyBodyFields.getPrivacyListAsJson();

        if (privacyList != null) {
            try {
                privacyList.put(REQUEST_PARAM_BID_REQUEST_GPP_CONSENT, gppString);
                privacyList.put(REQUEST_PARAM_BID_REQUEST_GPP_CONSENT_SID, gppSid);
            } catch (JSONException e) {
                Logger.e("Failed to add GPP and/or GPP SID to request body", e);
            }
        }

        CBJSON.put(deviceBody, REQUEST_PARAM_PRIVACY, privacyList);
        appendBodyArgument(CBConstants.WEB_REQUEST_PARAM_CONTAINER_DEVICE, deviceBody);

        /*SDK Body Information*/
        CBJSON.put(sdkBody, CBConstants.REQUEST_PARAM_SDK, requestBodyFields.REQUEST_PARAM_SDK_VERSION);

        if (requestBodyFields.getMediationBodyFields() != null) {
            CBJSON.put(sdkBody, CBConstants.REQUEST_PARAM_MEDIATION, requestBodyFields.getMediationBodyFields().getMediationName());
            CBJSON.put(sdkBody, CBConstants.REQUEST_PARAM_MEDIATION_VERSION, requestBodyFields.getMediationBodyFields().getLibraryVersion());
            CBJSON.put(sdkBody, CBConstants.REQUEST_PARAM_ADAPTER_VERSION, requestBodyFields.getMediationBodyFields().getAdapterVersion());
        }

        CBJSON.put(sdkBody, CBConstants.REQUEST_PARAM_COMMIT_HASH, CBConstants.RELEASE_COMMIT_HASH);

        String configVariant = requestBodyFields.getConfigurationFields().getConfigVariant();
        if (!Android.instance().isEmpty(configVariant)) {
            CBJSON.put(sdkBody, CBConstants.REQUEST_PARAM_CONFIG_VARIANT, configVariant);
        }

        appendBodyArgument(CBConstants.WEB_REQUEST_PARAM_CONTAINER_SDK, sdkBody);

        /*Ad Body Information*/
        CBJSON.put(adBody, CBConstants.REQUEST_PARAM_SESSION, requestBodyFields.getSessionCount());
        if (adBody.isNull(CBConstants.WEB_REQUEST_PARAM_CACHE))
            CBJSON.put(adBody, CBConstants.WEB_REQUEST_PARAM_CACHE, false);
        if (adBody.isNull(CBConstants.WEB_REQUEST_PARAM_AMOUNT))
            CBJSON.put(adBody, CBConstants.WEB_REQUEST_PARAM_AMOUNT, 0);
        if (adBody.isNull(CBConstants.WEB_REQUEST_PARAM_RETRY_COUNT))
            CBJSON.put(adBody, CBConstants.WEB_REQUEST_PARAM_RETRY_COUNT, 0);
        if (adBody.isNull(CBConstants.REQUEST_PARAM_LOCATION))
            CBJSON.put(adBody, CBConstants.REQUEST_PARAM_LOCATION, "");

        appendBodyArgument(CBConstants.WEB_REQUEST_PARAM_CONTAINER_AD, adBody);
    }

    public void appendWebViewBodyArgument(String key, Object value) {
        CBJSON.put(adBody, key, value);
        appendBodyArgument(CBConstants.WEB_REQUEST_PARAM_CONTAINER_AD, adBody);
    }

    public void appendWebViewBodySdkArgument(String key, Object value) {
        CBJSON.put(sdkBody, key, value);
        appendBodyArgument(CBConstants.WEB_REQUEST_PARAM_CONTAINER_SDK, sdkBody);
    }
}
