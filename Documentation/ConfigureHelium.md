# Configure Helium

## Test Mode

The Helium SDK includes a Test Mode method that will allow you to test your partner integrations and get their test ads. To enable Test Mode, simply set the following method to `true` after starting the Helium SDK. Remember to remove it or set the method to `false` before releasing your app.

```kotlin Kotlin
HeliumSdk.setTestMode(true)
HeliumSdk.setTestMode(false)
```
```java Java
HeliumSdk.setTestMode(true);
HeliumSdk.setTestMode(false);
```

## Privacy Methods

The following privacy methods are used to set different privacy settings:

## COPPA

### `setSubjectToCoppa`

```kotlin Kotlin
// Indicates that you want your content treated as child-directed for purposes of COPPA. We will take steps to disable interest-based advertising for such ad requests.
HeliumSdk.setSubjectToCoppa(true)
```
```java Java
// Indicates that you want your content treated as child-directed for purposes of COPPA. We will take steps to disable interest-based advertising for such ad requests.
HeliumSdk.setSubjectToCoppa(true);
```

```kotlin Kotlin
// Indicates that you don't want your content treated as child-directed for purposes of COPPA. You represent and warrant that your applications and services are not directed towards children and that you will not provide any information to Helium from a user under the age of 13
HeliumSdk.setSubjectToCoppa(false)
```
```java Java
// Indicates that you don't want your content treated as child-directed for purposes of COPPA. You represent and warrant that your applications and services are not directed towards children and that you will not provide any information to Helium from a user under the age of 13
HeliumSdk.setSubjectToCoppa(false);
```

## GDPR

### `setSubjectToGDPR`

```kotlin Kotlin
// Indicates that GDPR is applied to this user from your application.
HeliumSdk.setSubjectToGDPR(true)
```
```java Java
// Indicates that GDPR is applied to this user from your application.
HeliumSdk.setSubjectToGDPR(true);
```

```kotlin Kotlin
// Indicates that GDPR is not applied to this user from your application.
HeliumSdk.setSubjectToGDPR(false)
```
```java Java
// Indicates that GDPR is not applied to this user from your application.
HeliumSdk.setSubjectToGDPR(false);
```

### `setUserHasGivenConsent`
```kotlin Kotlin
// Indicates that this user from your application has given consent to share personal data for behavioral targeted advertising under GDPR regulation.
HeliumSdk.setUserHasGivenConsent(true)
```
```java Java
// Indicates that this user from your application has given consent to share personal data for behavioral targeted advertising under GDPR regulation.
HeliumSdk.setUserHasGivenConsent(true);
```

```kotlin Kotlin
// Indicates that this user from your application has not given consent to use its personal data for behavioral targeted advertising under GDPR regulation, so only contextual advertising is allowed.
HeliumSdk.setUserHasGivenConsent(false)
```
```java Java
// Indicates that this user from your application has not given consent to use its personal data for behavioral targeted advertising under GDPR regulation, so only contextual advertising is allowed.
HeliumSdk.setUserHasGivenConsent(false);
```

## CCPA

### `setCCPAConsent`

```kotlin Kotlin
// Indicates that this user from your application has given consent to share personal data for behavioral targeted advertising under CCPA regulation.
HeliumSdk.setCCPAConsent(true)
```
```java Java
// Indicates that this user from your application has given consent to share personal data for behavioral targeted advertising under CCPA regulation.
HeliumSdk.setCCPAConsent(true);
```

```kotlin Kotlin
// Indicates that this user from your application has not given consent to allow sharing personal data for behavioral targeted advertising under CCPA regulation.
HeliumSdk.setCCPAConsent(false)
```
```java Java
// Indicates that this user from your application has not given consent to allow sharing personal data for behavioral targeted advertising under CCPA regulation.
HeliumSdk.setCCPAConsent(false);
```

Note: The Helium SDK will send CCPA information to the bidding networks via the mediation adapters, where the information is stored.

## Keywords

Starting in version 2.9.0, the Helium SDK introduces keywords—key-value pairs that can be set to enable real-time targeting on line items.

## Set Keywords

To set keywords, you will need to first create a Helium ad object. Then, use the `set()` method to add key-value keywords pair. The set method returns a boolean indicating whether or not the keyword has been successfully set.

```kotlin Kotlin
// Create a Helium ad object.
val interstitialAd = HeliumInterstitialAd(placementName, heliumInterstitialAdListener)
  
val rewardedAd = HeliumRewardedAd(placementName, heliumRewardedAdListener)
  
val bannerAd = HeliumBannerAd(context, placementName, bannerSize, heliumBannerAdListener)
  
interstitialAd.keywords["i12_keyword1"] = "i12_value1"
  
rewardedAd.keywords["rwd_keyword1"] = "rwd_value1"
  
bannerAd.keywords["bnr_keyword1"] = "bnr_value1"
```
```java Java
// Create a Helium ad object.
HeliumInterstitialAd interstitialAd = new HeliumInterstitialAd(placementName, heliumInterstitialAdListener);
  
HeliumRewardedAd rewardedAd = new HeliumRewardedAd(placementName, heliumRewardedAdListener);
  
HeliumBannerAd bannerAd = new HeliumBannerAd(context, placementName, bannerSize, heliumBannerAdListener);

interstitialAd.getKeywords().set("i12_keyword1", "i12_value1");
  
rewardedAd.getKeywords().set("rwd_keyword1", "rwd_value1");
  
bannerAd.getKeywords().set("bnr_keyword1", "bnr_value1");
```

## Remove Keywords

To remove keywords that have been set, simply use the `remove()` method and pass in the key whose value you would like to remove. The remove method returns the value of the key removed.

```kotlin Kotlin
// Remove keyword
interstitialAd.remove("i12_keyword1")
rewardedAd.remove("rwd_keyword1")
bannerAd.remove("bnr_keyword1")
```
```java Java
// Remove keyword
interstitialAd.remove("i12_keyword1");
rewardedAd.remove("rwd_keyword1");
bannerAd.remove("bnr_keyword1");
```

Note: The keywords API has restrictions for setting keys and values. The maximum number of characters allowed for each key name is 64 characters. For values, it is 256 characters.

## ILRD

Impression Level Revenue Data (ILRD) is data we already collect on the server-side and store in our database today. This feature allows  publishers to get access to this data.

An ILRD event occurs every time an impression is tracked. The fields that a publisher is interested in are broken down as follows:

| ILRD Key                  | Value         | ILRD Description                                                  |
| :--------                 | :----------:  | :-----------------                                                |
| `"ad_revenue"`            | double        | Double precision floating point                                   |
| `"currency_type"`         | String        | Always USD                                                        |
| `"country"`               | USA           | Three letter country code ISO_3166-1_alpha-3                      |
| `"impression_id"`         | String        | ID                                                                |
| `"line_item_name"`        | String        | Helium line item name                                             |
| `"line_item_id"`          | String        | Helium line item id                                               |
| `"network_name`"          | String        | Network Name                                                      |
| `"network_placement_id"`  | String        | Partner placement name                                            |
| `"network_type"`          | String        | Either `bidding` or `mediation`                                    |
| `"placement_name"`        | String        | Helium placement name                                             |
| `"placement_type"`        | String        | Ad type: <br/>- `interstitial`<br/>- `rewarded`</br>- `banner`    |
| `"precision"`             | String        | One of the following: <br/>- `estimated`, <br/>- `exact`, <br/>- `publisher_defined`, <br/>- `undisclosed`  |

```json ILRD JSON example
{
 "ad_revenue": 0.055,
 "currency_type": "USD",
 "country": "USA",
 "impression_id": "ae112f3dccf90c705f2d3b1324605e9d16687725",
 "line_item_name": "helium_rv_T1_tapjoy_high",
 "line_item_id": "33f6b0ca-1b3c-4e69-80dd-b57db13db159",
 "network_name": "tapjoy",
 "network_placement_id": "tapjoy_RV_0_25",
 "network_type": "mediation",
 "placement_name": "heliumTapjoyTest",
 "placement_type": "rewarded",
 "precision": "publisher_defined"
}
```

Publishers can receive this data in two ways:

1. On Android, we provide a global notification that gives publishers access to ILRD data on each impression shown event in real-time.
2. For API access, we can provide an ILRD reporting API where publishers can request a custom report (CSV) of all their ILRD data filtered by `appId`, `min_date`, `max_date`.

Publishers can use ILRD data in several ways:

- You can listen to the data via SDK notifications in real time and ship it to your own servers or to other MMPs such as AppsFlyer or Adjust.
- You can manually request a report via the API endpoint and analyze at your convenience.

> Note: Meta Audience Network Ad Revenue
>
> - Unlike other networks, Meta prohibits Helium from sharing the real ad_revenue value of Meta Audience Network impressions with publishers.
> - Instead, Helium is only allowed to share the average of aggregated eCPMs of Meta Audience Network line items.

## Implementation

Create a `HeliumIlrdObserver` object and implement its `onImpression` method. Afterwards, pass the object to the `HeliumSdk.subscribe` and `HeliumSdk.unsubscribe` methods.

```kotlin Kotlin
val ilrdObserver = object: HeliumIlrdObserver {
  override fun onImpression(impData: HeliumImpressionData) {
      // Placement name
      val placement = impData.placementId
      // JSON
      val json = impData.ilrdInfo
  }
}

override fun onCreate(savedInstanceState: Bundle?) {
  ...
  // Subscribe to ILRD on app startup, e.g. in onCreate()
  HeliumSdk.subscribeIlrd(ilrdObserver)
  ...
}

override fun onDestroy() {
  ...
  // Unsubscribe from ILRD on app terminations, e.g. in onDestroy()
  HeliumSdk.unsubscribeIlrd(ilrdObserver)
  ...
}
```
```java Java
HeliumIlrdObserver ilrdObserver = new HeliumIlrdObserver() {
    @Override
    public void onImpression(@NonNull HeliumImpressionData heliumImpressionData) {
        // Placement name
        String placementId = heliumImpressionData.getPlacementId();
        // JSON
        JSONObject json = heliumImpressionData.getIlrdInfo();
    }
};

@Override
protected void onCreate(Bundle savedInstanceState) {
  ...
  // Subscribe to ILRD on app startup, e.g. in onCreate()
  HeliumSdk.subscribeIlrd(ilrdObserver);
  ...
}

@Override
protected void onDestroy() {
  ...
  // Unsubscribe from ILRD on app terminations, e.g. in onDestroy()
  HeliumSdk.unsubscribeIlrd(ilrdObserver);
  ...
}
```
