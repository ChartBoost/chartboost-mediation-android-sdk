package com.chartboost.sdk.test;

import static org.mockito.Mockito.mock;

import android.telephony.TelephonyManager;

import com.chartboost.sdk.privacy.model.DataUseConsent;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO This should be removed. There should be no need for dependency injection in unit testing,
//  all dependencies should be passed through constructor.
//  Singletons and statics should be mocked or refactored to be passed as dependencies.
public class TestContainerControl {
    public final Constants constants = new Constants();

    private final SdkConfigurationBuilder sdkConfigurationBuilder;
    private TestDisplayMetrics testDisplayMetrics;
    public final Map<String, Object> sharedPreferenceValues = new HashMap<>();
    private final Set<String> grantedPermissions = new HashSet<>();

    public static TestContainerControl emptyConfig() {
        return new TestContainerControl();
    }

    public static TestContainerControl defaultWebView() {
        SdkConfigurationBuilder optionsBuilder = new SdkConfigurationBuilder()
                .withWebViewEnabled(true)
                .withWebViewDirectories(Arrays.asList("templates", "videos"));
        return new TestContainerControl(optionsBuilder);
    }

    public static TestContainerControl defaultNative() {
        SdkConfigurationBuilder optionsBuilder = new SdkConfigurationBuilder()
                .withWebViewEnabled(false);
        return new TestContainerControl(optionsBuilder);
    }

    public static TestContainerControl webViewPublisherDisabled() {
        return new TestContainerControl(
                new SdkConfigurationBuilder()
                        .withWebViewEnabled(true)
                        .withPublisherDisable(true));
    }

    public TestContainerControl() {
        this(new SdkConfigurationBuilder());
    }

    public TestContainerControl(SdkConfigurationBuilder optionsBuilder) {
        sdkConfigurationBuilder = optionsBuilder;
        testDisplayMetrics = TestDisplayMetrics.portrait();
    }

    public TestDisplayMetrics displayMetrics() {
        return testDisplayMetrics;
    }

    public void setDisplayMetrics(TestDisplayMetrics displayMetrics) {
        testDisplayMetrics = displayMetrics;
    }

    public SdkConfigurationBuilder configure() {
        return sdkConfigurationBuilder;
    }

    public class Constants {
        public final String packageName = "com.some.package.name";
        public final String packageVersionName = "3.2.1";

        public final int networkType = TelephonyManager.NETWORK_TYPE_CDMA;

        public final String carrierName = "Verizon Wireless";
        public final String carrierMobileCountryCode = "311";
        public final String carrierMobileNetworkCode = "480";
        public final String carrierIsoCountryCode = "us";
        public final int carrierPhoneType = TelephonyManager.PHONE_TYPE_CDMA;
        public final int simState = TelephonyManager.SIM_STATE_READY;
        public final int signalStrengthLevel = 4;

        public final JSONObject privacyListJson = mock(JSONObject.class);
        public final List<DataUseConsent> privacyWhitelist = mock(List.class);
        public final int openRTBConsent = 1;
        public final int openRTBCGDPR = 1;
    }

    public void grantPermission(String permission) {
        grantedPermissions.add(permission);
    }

    public void revokePermission(String permission) {
        grantedPermissions.remove(permission);
    }

    public boolean hasPermission(String permission) {
        return grantedPermissions.contains(permission);
    }
}
