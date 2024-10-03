package com.chartboost.sdk.internal.Networking

import com.chartboost.sdk.PlayServices.BaseTest
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Model.RequestBodyFields
import com.chartboost.sdk.internal.Model.toBodyFields
import com.chartboost.sdk.internal.Model.toReachabilityBodyFields
import com.chartboost.sdk.test.TestContainer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import java.util.*

class RequestBodyFieldsTest : BaseTest() {
    @Test
    fun verifyFields() {
        TestContainer().use { tc ->
            val b =
                RequestBodyFields(
                    tc.appId,
                    tc.appSignature,
                    tc.identity.toIdentityBodyFields(),
                    tc.reachability.toReachabilityBodyFields(),
                    tc.carrierBuilder.build(tc.applicationContext),
                    tc.session.toSessionBodyFields(),
                    tc.timeSource.toBodyFields(),
                    tc.privacyApi.toPrivacyBodyFields(),
                    tc.sdkConfig.get().toConfigurationBodyFields(),
                    tc.deviceBodyFieldsFactory.build(),
                    null,
                )
            MatcherAssert.assertThat(
                b.REQUEST_PARAM_APP,
                CoreMatchers.`is`(CoreMatchers.equalTo(tc.appId)),
            )
            MatcherAssert.assertThat(
                b.REQUEST_PARAM_SDK_VERSION,
                CoreMatchers.`is`(CoreMatchers.equalTo(CBConstants.SDK_VERSION)),
            )
            MatcherAssert.assertThat(
                b.REQUEST_PARAM_COUNTRY,
                CoreMatchers.`is`(
                    CoreMatchers.equalTo(
                        Locale.getDefault().country,
                    ),
                ),
            )
            MatcherAssert.assertThat(
                b.REQUEST_PARAM_LANGUAGE,
                CoreMatchers.`is`(
                    CoreMatchers.equalTo(
                        Locale.getDefault().language,
                    ),
                ),
            )
            val constants = tc.control.constants
            MatcherAssert.assertThat(
                b.REQUEST_PARAM_PACKAGE,
                CoreMatchers.`is`(CoreMatchers.equalTo(constants.packageName)),
            )
            MatcherAssert.assertThat(
                b.REQUEST_PARAM_VERSION,
                CoreMatchers.`is`(CoreMatchers.equalTo(constants.packageVersionName)),
            )
            val carrier = b.REQUEST_PARAM_CARRIER_INFO
            MatcherAssert.assertThat(
                carrier.opt("carrier-name") as String,
                CoreMatchers.`is`(
                    CoreMatchers.equalTo(constants.carrierName),
                ),
            )
            MatcherAssert.assertThat(
                carrier.opt("mobile-country-code") as String,
                CoreMatchers.`is`(
                    CoreMatchers.equalTo(constants.carrierMobileCountryCode),
                ),
            )
            MatcherAssert.assertThat(
                carrier.opt("mobile-network-code") as String,
                CoreMatchers.`is`(
                    CoreMatchers.equalTo(constants.carrierMobileNetworkCode),
                ),
            )
            MatcherAssert.assertThat(
                carrier.opt("iso-country-code") as String,
                CoreMatchers.`is`(
                    CoreMatchers.equalTo(constants.carrierIsoCountryCode),
                ),
            )
            MatcherAssert.assertThat(
                carrier.opt("phone-type") as Int,
                CoreMatchers.`is`(CoreMatchers.equalTo(constants.carrierPhoneType)),
            )
        }
    }
}
