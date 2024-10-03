package com.chartboost.sdk.internal.Model;

import static com.chartboost.sdk.internal.Libraries.CBConstants.REQUEST_PARAM_BID_REQUEST_GPP_CONSENT;
import static com.chartboost.sdk.internal.Libraries.CBConstants.REQUEST_PARAM_BID_REQUEST_GPP_CONSENT_SID;
import static com.chartboost.sdk.privacy.model.COPPA.COPPA_STANDARD;

import android.os.Build;

import com.chartboost.sdk.internal.Libraries.CBJSON;
import com.chartboost.sdk.internal.Networking.AdParameters;
import com.chartboost.sdk.internal.Networking.requests.NetworkType;
import com.chartboost.sdk.internal.WebView.UserAgentHelper;
import com.chartboost.sdk.internal.adType.AdType;
import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.iab.omid.library.chartboost.adsession.Partner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class OpenRTBRequestModel {
    private static Integer REQUEST_PARAM_OPENRTB_DEVICE_TYPE;
    private static final String REQUEST_PARAM_OPENRTB_OSV = Build.VERSION.RELEASE;
    private static final String REQUEST_PARAM_OPENRTB_OS = "Android";
    private static final String REQUEST_PARAM_OPENRTB_DISPLAY_MANAGER = "Chartboost-Android-SDK";
    private static final String REQUEST_PARAM_OPENRTB_CURRENCY = "USD";
    private static final int REQUEST_PARAM_OPENRTB_SECURE = 1;
    private static final int OPENRTB_INSTL_IS_FULLSCREEN = 1;
    private static final int OPENRTB_INSTL_IS_NOT_FULLSCREEN = 0;

    private final JSONObject jsonRepresentation;

    private final JSONObject device;
    private final JSONArray imp;
    private final JSONObject app;
    private final JSONObject regs;
    private final JSONObject user;

    private final RequestBodyFields requestBodyFields;
    private final AdParameters adParameters;
    private final OpenMeasurementManager omManager;

    public OpenRTBRequestModel(RequestBodyFields requestBodyFields,
                               AdParameters adParameters,
                               OpenMeasurementManager omManager) {
        REQUEST_PARAM_OPENRTB_DEVICE_TYPE = requestBodyFields.getOrtbDeviceType();
        this.requestBodyFields = requestBodyFields;
        this.adParameters = adParameters;
        this.omManager = omManager;

        this.device = new JSONObject();
        this.imp = new JSONArray();
        this.app = new JSONObject();
        this.regs = new JSONObject();
        this.user = new JSONObject();
        this.jsonRepresentation = new JSONObject();

        populateRootJSON();
        populateDeviceJSON();
        populateImpJSON();
        populateAppJSON();
        populateRegsJSON();
        populateUserJSON();
    }

    private void populateRootJSON() {
        CBJSON.put(jsonRepresentation, "id", JSONObject.NULL); // DA will set this
        CBJSON.put(jsonRepresentation, "test", JSONObject.NULL);
        CBJSON.put(jsonRepresentation, "cur", new JSONArray().put("USD"));
        CBJSON.put(jsonRepresentation, "at", 2); // 2nd price auction
    }

    public JSONObject getJsonRepresentation() {
        return jsonRepresentation;
    }

    private void populateDeviceJSON() {
        IdentityBodyFields identity = requestBodyFields.getIdentityBodyFields();
        CBJSON.put(device, "devicetype", REQUEST_PARAM_OPENRTB_DEVICE_TYPE);
        CBJSON.put(device, "w", requestBodyFields.getDeviceBodyFields().getDeviceWidth());
        CBJSON.put(device, "h", requestBodyFields.getDeviceBodyFields().getDeviceHeight());
        CBJSON.put(device, "ifa", identity.getGaid());
        CBJSON.put(device, "osv", REQUEST_PARAM_OPENRTB_OSV);
        CBJSON.put(device, "lmt", identity.getTrackingState().getValue());
        CBJSON.put(device, "connectiontype", getNetworkTypeValue());
        CBJSON.put(device, "os", REQUEST_PARAM_OPENRTB_OS);
        CBJSON.put(device, "geo", generateGeo());
        CBJSON.put(device, "ip", JSONObject.NULL);
        CBJSON.put(device, "language", requestBodyFields.REQUEST_PARAM_LANGUAGE);
        CBJSON.put(device, "ua", UserAgentHelper.INSTANCE.getWebViewUserAgentValue());
        CBJSON.put(device, "make", requestBodyFields.REQUEST_PARAM_DEVICE_MAKE);
        CBJSON.put(device, "model", requestBodyFields.REQUEST_PARAM_MODEL);
        CBJSON.put(device, "carrier", requestBodyFields.REQUEST_CARRIER_NAME);
        CBJSON.put(device, "ext", generateExtWithIdentityAndOm(identity, omManager));
        CBJSON.put(this.jsonRepresentation, "device", device);
    }

    private void populateImpJSON() {
        JSONObject impression = new JSONObject();
        CBJSON.put(impression, "id", JSONObject.NULL);
        JSONObject banner = new JSONObject();
        CBJSON.put(banner, "w", adParameters.width);
        CBJSON.put(banner, "h", adParameters.height);
        CBJSON.put(banner, "btype", JSONObject.NULL);
        CBJSON.put(banner, "battr", JSONObject.NULL);
        CBJSON.put(banner, "pos", JSONObject.NULL);
        CBJSON.put(banner, "topframe", JSONObject.NULL);
        CBJSON.put(banner, "api", JSONObject.NULL);
        JSONObject ext = new JSONObject();
        CBJSON.put(ext, "placementtype", getPlacementType());
        CBJSON.put(ext, "playableonly", JSONObject.NULL);
        CBJSON.put(ext, "allowscustomclosebutton", JSONObject.NULL);
        CBJSON.put(banner, "ext", ext);

        CBJSON.put(impression, "banner", banner);
        CBJSON.put(impression, "instl", isFullscreen());
        CBJSON.put(impression, "tagid", adParameters.location);
        CBJSON.put(impression, "displaymanager", REQUEST_PARAM_OPENRTB_DISPLAY_MANAGER);
        CBJSON.put(impression, "displaymanagerver", requestBodyFields.REQUEST_PARAM_SDK_VERSION);
        CBJSON.put(impression, "bidfloor", JSONObject.NULL);
        CBJSON.put(impression, "bidfloorcur", REQUEST_PARAM_OPENRTB_CURRENCY);
        CBJSON.put(impression, "secure", REQUEST_PARAM_OPENRTB_SECURE);

        imp.put(impression);

        CBJSON.put(this.jsonRepresentation, "imp", imp);
    }

    private void populateAppJSON() {
        CBJSON.put(app, "id", requestBodyFields.REQUEST_PARAM_APP);
        CBJSON.put(app, "name", JSONObject.NULL);
        CBJSON.put(app, "bundle", requestBodyFields.REQUEST_PARAM_PACKAGE);
        CBJSON.put(app, "storeurl", JSONObject.NULL);
        JSONObject publisher = new JSONObject();
        CBJSON.put(publisher, "id", JSONObject.NULL);
        CBJSON.put(publisher, "name", JSONObject.NULL);
        CBJSON.put(app, "publisher", publisher);
        CBJSON.put(app, "cat", JSONObject.NULL);
        CBJSON.put(this.jsonRepresentation, "app", app);
    }

    /**
     * The “Regs” object will signal whether or not the request is subject to GDPR regulations.
     * It will do so via the extension attribute “gdpr” which is an optional integer that indicates:
     * 0 = No, 1 = Yes. Under OpenRTB conventions for optional attributes, omission indicates Unknown.
     */
    private void populateRegsJSON() {
        Integer coppaValue = getCOPPA();
        if (coppaValue != null) {
            CBJSON.put(regs, "coppa", coppaValue);
        }
        JSONObject ext = new JSONObject();
        CBJSON.put(ext, "gdpr", getGDPR());
        CBJSON.put(ext, REQUEST_PARAM_BID_REQUEST_GPP_CONSENT, getGppString());
        CBJSON.put(ext, REQUEST_PARAM_BID_REQUEST_GPP_CONSENT_SID, getGppSid());
        for (DataUseConsent data : getDataUseConsentList()) {
            // COPPA is added as separate field
            if (!data.getPrivacyStandard().equals(COPPA_STANDARD)) {
                CBJSON.put(ext, data.getPrivacyStandard(), data.getConsent());
            }
        }
        CBJSON.put(regs, "ext", ext);
        CBJSON.put(this.jsonRepresentation, "regs", regs);
    }

    /**
     * consent openRTB value defines if user agrees for BEHAVIORAL tracking
     * for consent Merge UNKNOWN and NO_BEHAVIORAL
     */
    private void populateUserJSON() {
        CBJSON.put(user, "id", JSONObject.NULL);
        CBJSON.put(user, "geo", generateGeo());
        String tcfv2String = getTCFv2String();
        if (tcfv2String != null) {
            CBJSON.put(user, "consent", tcfv2String);
        }
        JSONObject ext = new JSONObject();
        CBJSON.put(ext, "consent", getConsent());
        CBJSON.put(ext, "impdepth", adParameters.impDepth);
        CBJSON.put(user, "ext", ext);
        CBJSON.put(this.jsonRepresentation, "user", user);
    }

    private JSONObject generateGeo() {
        JSONObject geo = new JSONObject();
        CBJSON.put(geo, "lat", JSONObject.NULL);
        CBJSON.put(geo, "lon", JSONObject.NULL);
        CBJSON.put(geo, "country", requestBodyFields.REQUEST_PARAM_COUNTRY);
        CBJSON.put(geo, "type", 2); // IP inferred
        return geo;
    }

    private JSONObject generateExtWithIdentityAndOm(IdentityBodyFields identity,
                                                    OpenMeasurementManager omManager) {
        JSONObject ext = new JSONObject();
        if (identity.getSetId() != null) {
            CBJSON.put(ext, "appsetid", identity.getSetId());
        }
        if (identity.getSetIdScope() != null) {
            CBJSON.put(ext, "appsetidscope", identity.getSetIdScope());
        }

        Partner omPartner = omManager.getOmidPartner();
        if (omManager.isOmSdkEnabled() && omPartner != null) {
            CBJSON.put(ext, "omidpn", omPartner.getName());
            CBJSON.put(ext, "omidpv", omPartner.getVersion());
        }
        return ext;
    }

    private String getPlacementType() {
        //this will be a when in kotlin
        if (adParameters.adType == AdType.Interstitial.INSTANCE) {
            Logger.e("INTERSTITIAL NOT COMPATIBLE WITH OPENRTB", null);
        } else if (adParameters.adType == AdType.Rewarded.INSTANCE) {
            Logger.e("REWARDED_VIDEO NOT COMPATIBLE WITH OPENRTB", null);
        }
        return adParameters.adType.getName().toLowerCase(Locale.ROOT);
    }

    private Integer isFullscreen() {
        return adParameters.adType.isFullScreen()
                ? OPENRTB_INSTL_IS_FULLSCREEN : OPENRTB_INSTL_IS_NOT_FULLSCREEN;
    }

    private Collection<DataUseConsent> getDataUseConsentList() {
        if (requestBodyFields != null) {
            return requestBodyFields.getPrivacyBodyFields().getWhitelistedPrivacyStandardsList();
        }
        return new ArrayList<>();
    }

    private String getTCFv2String() {
        if (requestBodyFields != null) {
            return requestBodyFields.getPrivacyBodyFields().getTcfString();
        }
        return null;
    }

    private String getGppString() {
        if (requestBodyFields != null) {
            return requestBodyFields.getPrivacyBodyFields().getGppString();
        }
        return null;
    }

    private String getGppSid() {
        if (requestBodyFields != null) {
            return requestBodyFields.getPrivacyBodyFields().getGppSid();
        }
        return null;
    }

    private int getConsent() {
        if (requestBodyFields != null && requestBodyFields.getPrivacyBodyFields().getOpenRtbConsent() != null) {
            return requestBodyFields.getPrivacyBodyFields().getOpenRtbConsent();
        }
        return 0;
    }

    private int getGDPR() {
        if (requestBodyFields != null && requestBodyFields.getPrivacyBodyFields().getOpenRtbGdpr() != null) {
            return requestBodyFields.getPrivacyBodyFields().getOpenRtbGdpr();
        }
        return 0;
    }

    private Integer getCOPPA() {
        if (requestBodyFields != null) {
            return requestBodyFields.getPrivacyBodyFields().getOpenRtbCoppa();
        }
        return null;
    }

    private int getNetworkTypeValue() {
        NetworkType networkType = requestBodyFields.getReachabilityBodyFields().getOpenRTBConnectionType();
        return networkType.getValue();
    }
}
