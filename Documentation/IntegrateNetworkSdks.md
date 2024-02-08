# Integrate Network SDKs

## Latest Supported SDK Versions

| Network               | Support         | Banner | Interstitial | Rewarded |
| :-------------------- | :-------------- | :----: | :----------: | :------: |
| AdMob                 | Android 20.6.0  |    ✅   |      ✅      |     ✅    |
| AppLovin              | Android 11.3.1  |    ✅   |      ✅      |     ✅    |
| Chartboost            | Android 8.3.1   |    ✅   |      ✅      |     ✅    |
| Digital Turbine       | Android 8.1.3   |    ✅   |      ✅      |     ✅    |
| Google Bidding        | Android 20.6.0  |    ✅   |      ✅      |     ✅    |
| InMobi                | Android 10.0.5  |    ✅   |      ✅      |     ✅    |
| ironSource            | Android 7.2.1   |    🚫   |      ✅      |     ✅    |
| Meta Audience Network | Android 6.8.0   |    ✅   |      ✅      |     ✅    |
| Mintegral             | Android 16.0.31 |    ✅   |      ✅      |     ✅    |
| Tapjoy                | Android 12.9.1  |    🚫   |      ✅      |     ✅    |
| Unity Ads             | Android 4.2.1   |    ✅   |      ✅      |     ✅    |
| Vungle                | Android 6.10.5  |    ✅   |      ✅      |     ✅    |

## Adding Ad Network SDKs

To integrate other ad networks via mediation, you will need to include the Helium adapters as well.

```gradle
repositories {
  ...
  mavenCentral()
  maven {
      name "Mintegral's maven repo"
      url "https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea"
  }
  maven {
      name "ironSource's maven repo"
      url "https://android-sdk.is.com/"
  }
  maven {
      name "Tapjoy's maven repo"
      url "https://sdk.tapjoy.com/"
  }
  ...
}
  
// Helium Adapters
implementation 'com.chartboost:helium-adcolony:3.3.1.0'
implementation 'com.chartboost:helium-applovin:3.3.1.0'
implementation 'com.chartboost:helium-admob:3.3.1.0'
implementation 'com.chartboost:helium-facebook:3.3.1.0'
implementation 'com.chartboost:helium-fyber:3.3.1.0'
implementation 'com.chartboost:helium-googlebidding:3.3.1.0'
implementation 'com.chartboost:helium-inmobi:3.3.1.0'
implementation 'com.chartboost:helium-ironsource:3.3.1.0'
implementation 'com.chartboost:helium-mintegral:3.3.1.0'
implementation 'com.chartboost:helium-tapjoy:3.3.1.0'
implementation 'com.chartboost:helium-unityads:3.3.1.0'
implementation 'com.chartboost:helium-vungle:3.3.1.0'
```

## AdColony

To integrate with AdColony Android SDK, simply follow AdColony’s instructions for adding the AdColony SDK via gradle ([Adcolony Android SDK documentation](https://github.com/AdColony/AdColony-Android-SDK/wiki)).

## AdMob

To integrate with AdMob Android SDK, simply follow AdMob’s instructions for adding the AdMob SDK via gradle ([AdMob Android SDK documentation](https://developers.google.com/admob/android/quick-start)).

Note: Latest versions of AdMob now require the usage of [AndroidX support libraries](https://developer.android.com/jetpack/androidx/migrate).

## AppLovin

To integrate with AppLovin Android SDK, simply follow AppLovin’s instructions of adding the AppLovin via gradle ([AppLovin Android SDK documentation](https://dash.applovin.com/docs/integration#androidIntegration)).

## Chartboost

The Chartboost Android SDK is already bundled with the Helium SDK. No need to integrate the Chartboost Android SDK separately.

## Digital Turbine

To integrate with Digital Turbine (formerly Fyber) Android SDK, simply follow the Digital Turbine instructions for adding the Digital Turbine SDK ([Digital Turbine Android SDK documentation](https://support.inmobi.com/monetize/android-guidelines)).

## ironSource

To integrate with ironSource Android SDK, simply follow the ironSource instructions for adding the IronSource SDK ([ironSource Android SDK documentation](https://developers.ironsrc.com/ironsource-mobile/android/android-sdk)).

>_Note: ironSource does not use Maven Central for distributing their SDKs, since they provide their own Maven Repository. See the above example for where it can be found._

## Meta Audience Network

To integrate with Meta Android SDK, simply follow Meta's instructions for adding the Meta Audience Network SDK via gradle ([Meta Android SDK documentation](https://developers.facebook.com/docs/audience-network/android)).

In addition, publishers may be needed to include the following in their network security config file (`network_security_config.xml`).

```xml network_security_config.xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">127.0.0.1</domain>
  </domain-config>
</network-security-config>
```

And update `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest ... >
<application android:networkSecurityConfig="@xml/network_security_config"
... >
...
</application>
</manifest>
```

For more information on Facebook network security configuration check [Network Security Config](https://developers.facebook.com/docs/audience-network/android-network-security-config)

## Mintegral

To integrate with Mintegral Android SDK, simply follow the Mintegral instructions for adding the Mintegral SDK ([Mintegral Android SDK documentation](https://cdn-adn.rayjump.com/cdn-adn/v2/markdown_v2/index.html?file=sdk-m_sdk-android&lang=en)).

>_Note: Mintegral does not use Maven Central for distributing their SDKs, since they provide their own Maven Repository. See the above example for where it can be found._

## Tapjoy

To integrate with Tapjoy Android SDK, simply follow Tapjoy’s instructions for adding the Tapjoy SDK via gradle ([Tapjoy Android SDK documentation](https://dev.tapjoy.com/sdk-integration/android/getting-started-guide-publishers-android)).

>_Note: Tapjoy does not use Maven Central for distributing their SDKs, since they provide their own Maven Repository. See the above example for where it can be found._

## Unity Ads

To integrate with Unity Ads Android SDK, simply follow the Unity’s instructions for adding the Unity Ads SDK ([Unity Android SDK documentation](https://unityads.unity3d.com/help/android/integration-guide-android)).

## Vungle

To integrate with Vungle Android SDK, simply follow Vungle’s instructions for adding the Vungle SDK via gradle ([Vungle Android SDK documentation](https://support.vungle.com/hc/en-us/articles/360002922871)).
