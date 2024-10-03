package com.chartboost.sdk.internal.Model;

import static com.chartboost.sdk.tracking.SessionKt.ERROR_SESSION_LOAD;

import android.os.Build;
import androidx.annotation.NonNull;
import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Libraries.CBUtility;
import com.chartboost.sdk.internal.Telephony.Carrier;
import com.chartboost.sdk.internal.Telephony.CarrierParser;

import org.json.JSONObject;

import java.util.Locale;

/**
 * RequestBodyFields stores several values of fields sent with every request, that never change.
 * It also holds references to some services that CBRequest needs in order to get dynamic field values.
 */
public class RequestBodyFields {
    public final String REQUEST_PARAM_MODEL;
    public final String REQUEST_PARAM_OS;
    public final String REQUEST_PARAM_COUNTRY;
    public final String REQUEST_PARAM_LANGUAGE;
    public final String REQUEST_PARAM_VERSION;
    public final String REQUEST_PARAM_PACKAGE;
    public final String REQUEST_PARAM_SDK_VERSION;
    public final String REQUEST_PARAM_APP;
    public final String REQUEST_PARAM_SIGNATURE;
    public final String REQUEST_PARAM_DEVICE_TYPE;
    public final String REQUEST_PARAM_DEVICE_MAKE;
    public final String REQUEST_PARAM_ACTUAL_DEVICE_TYPE;
    public final JSONObject REQUEST_PARAM_CARRIER_INFO;
    public final String REQUEST_CARRIER_NAME;
    public final String REQUEST_PARAM_TIMEZONE;
    public final Integer REQUEST_PARAM_MOBILE_NETWORK_TYPE;
    public final Carrier carrier;
    public final PrivacyBodyFields privacyBodyFields;
    private final SessionBodyFields session;
    private final IdentityBodyFields identity;
    private final ReachabilityBodyFields reachability;
    private final TimeSourceBodyFields timeSourceBodyFields;
    private final ConfigurationBodyFields configurationBodyFields;
    private final DeviceBodyFields deviceBodyFields;
    private final MediationBodyFields mediationBodyFields;

    public RequestBodyFields(String appId,
                             String appSignature,
                             IdentityBodyFields identity,
                             ReachabilityBodyFields reachability,
                             Carrier carrier,
                             SessionBodyFields session,
                             TimeSourceBodyFields timeSourceBodyFields,
                             PrivacyBodyFields privacyBodyFields,
                             ConfigurationBodyFields configurationBodyFields,
                             DeviceBodyFields deviceBodyFields,
                             MediationBodyFields mediationBodyFields) {
        this.identity = identity;
        this.reachability = reachability;
        this.carrier = carrier;
        this.session = session;
        this.timeSourceBodyFields = timeSourceBodyFields;
        this.privacyBodyFields = privacyBodyFields;
        this.REQUEST_PARAM_APP = appId;
        this.REQUEST_PARAM_SIGNATURE = appSignature;
        this.configurationBodyFields = configurationBodyFields;
        this.deviceBodyFields = deviceBodyFields;
        this.mediationBodyFields = mediationBodyFields;

        if ("sdk".equals(Build.PRODUCT) || "google_sdk".equals(Build.PRODUCT) ||
                (Build.MANUFACTURER != null && Build.MANUFACTURER.contains("Genymotion"))) {
            this.REQUEST_PARAM_MODEL = "Android Simulator";
        } else { // device
            this.REQUEST_PARAM_MODEL = Build.MODEL;
        }
        this.REQUEST_PARAM_DEVICE_MAKE = (Build.MANUFACTURER == null) ? "unknown" : Build.MANUFACTURER;
        this.REQUEST_PARAM_DEVICE_TYPE = Build.MANUFACTURER + " " + Build.MODEL;
        this.REQUEST_PARAM_ACTUAL_DEVICE_TYPE = deviceBodyFields.getDeviceType();
        this.REQUEST_PARAM_OS = "Android " + Build.VERSION.RELEASE;
        this.REQUEST_PARAM_COUNTRY = Locale.getDefault().getCountry();
        this.REQUEST_PARAM_LANGUAGE = Locale.getDefault().getLanguage();
        this.REQUEST_PARAM_SDK_VERSION = CBConstants.SDK_VERSION;
        this.REQUEST_PARAM_VERSION = deviceBodyFields.getVersionName();
        this.REQUEST_PARAM_PACKAGE = deviceBodyFields.getPackageName();
        this.REQUEST_CARRIER_NAME = getCarrierParamName(carrier);
        this.REQUEST_PARAM_CARRIER_INFO = getCarrierParamInfo(carrier);
        this.REQUEST_PARAM_TIMEZONE = CBUtility.getCurrentTimezone();
        this.REQUEST_PARAM_MOBILE_NETWORK_TYPE = reachability.getCellularConnectionType();
    }

    public @NonNull
    PrivacyBodyFields getPrivacyBodyFields() {
        return privacyBodyFields;
    }

    public boolean isPortrait() {
        return deviceBodyFields.isPortrait();
    }

    public IdentityBodyFields getIdentityBodyFields() {
        return identity;
    }

    public ReachabilityBodyFields getReachabilityBodyFields() {
        return reachability;
    }

    private String getCarrierParamName(Carrier carrier) {
        if (carrier != null) {
            return carrier.getNetworkOperatorName();
        }
        return "";
    }

    private JSONObject getCarrierParamInfo(Carrier carrier) {
        JSONObject carrierInfo;
        if (carrier != null) {
            carrierInfo = parseCarrierToJson(carrier, new CarrierParser());
        } else {
            carrierInfo = new JSONObject();
        }
        return carrierInfo;
    }

    public JSONObject parseCarrierToJson(Carrier carrier, CarrierParser parser) {
        if (parser != null) {
            return parser.parseCarrierToJsonObject(carrier);
        }
        return new JSONObject();
    }

    public SessionBodyFields getSession() {
        return session;
    }

    public TimeSourceBodyFields getTimeSourceBodyField() {
        return timeSourceBodyFields;
    }

    public int getSessionCount() {
        if (session != null) {
            return session.getSessionCounter();
        }
        return ERROR_SESSION_LOAD;
    }

    public Integer getOrtbDeviceType() {
        return deviceBodyFields.getOrtbDeviceType();
    }

    public ConfigurationBodyFields getConfigurationFields() {
        return configurationBodyFields;
    }

    public DeviceBodyFields getDeviceBodyFields() {
        return deviceBodyFields;
    }

    public MediationBodyFields getMediationBodyFields() { return mediationBodyFields; }
}
