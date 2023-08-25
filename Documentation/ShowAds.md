# Show Ads

## Showing Interstitial and Rewarded Ads

When you’re ready to show an ad, you can call the `show()` method like so:

```kotlin Kotlin
heliumInterstitialAd.show()
heliumRewardedAd.show()
```
```java Java
heliumInterstitialAd.show();
heliumRewardedAd.show();
```

> Note: While Helium includes a `readyToShow()` method in our ad objects, we suggest calling `show()` without the `readyToShow()` check. The `didShow()` callback can be used for a similar check.`

```kotlin Kotlin
override fun didShow(placementName: String, error: HeliumAdError?) {
  if (error != null) {
    // An error was detected, gracefully exit and attempt a new ad load.
  } else {
    // Ad is successfully shown.
  }
}
```
```java Java
@Override
public void didShow(@NonNull String placementName, @Nullable HeliumAdError error) {
  if (error != null) {
    // An error was detected, gracefully exit and attempt a new ad load.
  } else {
    // Ad is successfully shown.
  }
}
```

You can implement the `InterstitialAdListener` or `RewardedAdListener` interfaces in your project to receive notifications about the lifecycle of the ad display process. See section [Delegate Usage](DelegateUsage.md) for more details.

## Showing Banner Ads

Starting in version 3.0.0 of the Helium SDK, the `show()` method has been removed. Banners are automatically shown after being attached to a view hierarchy.

If you enable auto-refresh for a banner placement in the dashboard, then the Helium SDK will apply that setting when the placement is shown.

Any auto refresh changes made on the dashboard will take approximately one hour to take effect and the SDK must be rebooted in order to pick up the changes once they're available.

## Releasing Helium Ads

To clear resources used by Helium Ads, you can use the destroy method associated with the respective Helium Ad you have used.

```kotlin Kotlin
override fun onDestroy() {
  ...
  // Release interstitial ad
  heliumInterstitialAd?.let {
      it.destroy();
      Log("destroyed an existing interstitial");
  }

  // Release rewarded ad
  heliumRewardedAd?.let {
      it.destroy();
      Log("destroyed an existing rewarded");
  }

  // Release banner ad
  heliumBannerAd?.let {
      it.destroy();
      Log("destroyed an existing banner");
  }
  ...
}
```
```java Java
@Override
public void onDestroy() {
  ...
  // Release interstitial ad
  if (heliumInterstitialAd != null) {
      heliumInterstitialAd.destroy();
      Log("destroyed an existing interstitial");
  }

  // Release rewarded ad
  if (heliumRewardedAd != null) {
      heliumRewardedAd.destroy();
      Log("destroyed an existing rewarded");
  }

  // Release banner ad
  if (heliumBannerAd != null) {
      heliumBannerAd.destroy();
      Log("destroyed an existing banner");
  }
  ...
}
```
