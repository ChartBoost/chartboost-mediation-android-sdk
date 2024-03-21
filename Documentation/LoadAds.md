# Load Ads

## Creating Interstitial & Rewarded Ads

To show an interstitial or rewarded ad, first create a variable to hold a reference to either the Interstitial or Rewarded Helium Ad:

```kotlin Kotlin
val heliumInterstitialAd: HeliumInterstitialAd
val heliumRewardedAd :HeliumRewardedAd
```
```java Java
private HeliumInterstitialAd heliumInterstitialAd;
private HeliumRewardedAd heliumRewardedAd;
```

Then, using the placement name you set up on your dashboard, get an instance of a Helium Ad:

```kotlin Kotlin
heliumInterstitialAd = HeliumInterstitialAd(MYHELIUMPLACEMENT1, heliumInterstitialAdListener)
heliumRewardedAd = HeliumRewardedAd(MYHELIUMPLACEMENT2, heliumRewardedAdListener)
```
```java Java
heliumInterstitialAd = new HeliumInterstitialAd(MYHELIUMPLACEMENT1, heliumInterstitialAdListener);
heliumRewardedAd = new HeliumRewardedAd(MYHELIUMPLACEMENT2, heliumRewardedAdListener);
```

In the above examples we’re passing in the `heliumInterstitialAdListener` object and `heliumRewardedAdListener` to act as delegates/listeners and receive notifications about the SDK ad activity. See section **Delegate Usage** for more details.

## Loading Interstitial & Rewarded Ads

You will need to create an instance for each placement name you want to use. Finally, make the call to load the ad:

For Helium SDK 3.0.0:

- `load()` now returns a `String` representing the load request identifier. This return value is optional to consume.

Prior to Helium SDK 3.0.0:

- `load()` does not return a `String`.

```kotlin Helium SDK 3.0.0 (Kotlin)
// load() now returns a String representing the load request identifier.
heliumInterstitialAd.load()
heliumRewardedAd.load()
```
```java Helium SDK 3.0.0 (Java)
// load() now returns a String representing the load request identifier.
heliumInterstitialAd.load();
heliumRewardedAd.load();
```
```kotlin Helium SDK 2.11.0 & below (Kotlin)
// load() is a void method.
heliumInterstitialAd.load()
heliumRewarded.load()
```
```java Helium SDK 2.11.0 & below (Java)
// load() is a void method.
heliumInterstitialAd.load();
heliumRewarded.load();
```

## Creating Banner Ads

There are two ways to create banners on Android: programmatically & in XML.

*Note: The following banner sizes can be passed down. Some partners may not fill for some banner sizes.

| Banner Enum | Dimensions (Width & Height) |
| :-----------|:----------------------------:|
| Standard | (320 x 50) |
| Medium | (300 x 250) |
| Leaderboard | (728 x 90) |

## Create Banner Ads Programmatically

To show a banner ad programmatically, first declare a variable to hold a reference to the Banner Helium Ad. Supply the corresponding Placement Name and the Banner Size. Note that we renamed HeliumBannerAd.Size to HeliumBannerAd.HeliumBannerSize.

```kotlin Helium SDK 3.0.0 (Kotlin)
/*
  The following Banner enum Sizes can be passed down:
  HeliumBannerAd.HeliumBannerSize.STANDARD
  HeliumBannerAd.HeliumBannerSize.MEDIUM
  HeliumBannerAd.HeliumBannerSize.LEADERBOARD
*/
val bannerSize = HeliumBannerAd.HeliumBannerSize.STANDARD
val heliumBannerAd = new HeliumBannerAd(context, placementName, bannerSize, bannerListener)
```
```java Helium SDK 3.0.0 (Java)
/*
  The following Banner enum Sizes can be passed down:
  HeliumBannerAd.HeliumBannerSize.STANDARD
  HeliumBannerAd.HeliumBannerSize.MEDIUM
  HeliumBannerAd.HeliumBannerSize.LEADERBOARD
*/
HeliumBannerAd.HeliumBannerSize bannerSize = HeliumBannerAd.HeliumBannerSize.STANDARD;
HeliumBannerAd heliumBannerAd = new HeliumBannerAd(context, placementName, bannerSize, bannerListener);
```
```kotlin Helium SDK 2.11.0 (Kotlin)
/*
  The following Banner enum Sizes can be passed down:
  HeliumBannerAd.Size.STANDARD
  HeliumBannerAd.Size.MEDIUM
  HeliumBannerAd.Size.LEADERBOARD
*/
val bannerSize = HeliumBannerAd.Size.STANDARD
val heliumBannerAd = new HeliumBannerAd(context, placementName, bannerSize, bannerListener)
```
```java Helium SDK 2.11.0 (Java)
/*
  The following Banner enum Sizes can be passed down:
  HeliumBannerAd.Size.STANDARD
  HeliumBannerAd.Size.MEDIUM
  HeliumBannerAd.Size.LEADERBOARD
*/
HeliumBannerAd.Size bannerSize = HeliumBannerAd.Size.STANDARD;
HeliumBannerAd heliumBannerAd = new HeliumBannerAd(context, placementName, bannerSize, bannerListener);
```

## Creating Banner Ads In An XML Layout

To show a banner via an xml layout, you will need to set the `HeliumBannerAd` in the layout where you want to display the banner.

```xml
...
  <com.chartboost.heliumsdk.ad.HeliumBannerAd
      android:id="@+id/bannerAd"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      tools:layout_height="50dp"
      app:heliumBannerSize="STANDARD"
      app:heliumPlacementName="PlacementName" />
...
```

**Note:** Notice that there are two app attributes required.

- `app:heliumBannerSize` -- Please set the heliumBannerSize to either STANDARD, MEDIUM, or LEADERBOARD.
- `app:heliumPlacementName` -- Please set the heliumPlacementName for the banner placement that you have in the dashboard.

## Loading Banner Ads

For Helium SDK 3.0.0:

- `load()` now returns a `String` representing the load request identifier. This return value is optional to consume.
- Banners are now automatically shown. Please add this banner view to the view hierarchy and the ads will automatically be shown.

Prior to Helium SDK 3.0.0:

- `load()` does not return a `String`.
- Banners are not automatically shown and include a `show()` method.

You will need to create an ad instance for each placement name you want to use. Finally, make the call to load the ad:

```kotlin Helium SDK 3.0.0 (Kotlin)
// load() returns a String representing the load request identifier.
heliumBannerAd.load()
```
```java Helium SDK 3.0.0 (Java)
// load() returns a String representing the load request identifier.
heliumBannerAd.load();
```
```kotlin Helium SDK 2.11.0 & below (Kotlin)
// load() is a void method.
heliumBannerAd.load()
```
```java Helium SDK 2.11.0 & below (Java)
// load() is a void method.
heliumBannerAd.load();
```

## Clearing Loaded Ads

Sometimes, you may need to clear loaded ads on existing placements to request another ad (i.e. for an in-house programmatic auction).

### Clearing Interstitial and Rewarded Ads

```kotlin Kotlin
heliumInterstitialAd.clearLoaded()
heliumRewardedAd.clearLoaded()
```
```java Java
heliumInterstitialAd.clearLoaded();
heliumRewardedAd.clearLoaded();
```

### Clearing Banner Ads

For Helium SDK 3.0.0 on banners:

- The `clearLoaded()` method for the `HeliumBannerAd` class was renamed to `clearAd()`. This will clear the currently showing banner ad and any cached banner ad that was loaded for auto refresh. To do this:

```kotlin Helium SDK 3.0.0 (Kotlin)
// To clear currently showing and any cached ad loaded for auto refresh.
heliumBannerAd.clearAd()
```
```java Helium SDK 3.0.0 (Java)
// To clear currently showing and any cached ad loaded for auto refresh.
heliumBannerAd.clearAd();
```
```kotlin Helium SDK 2.11.0 & below (Kotlin)
heliumBannerAd.clearLoaded()
```
```java Helium SDK 2.11.0 & below (Java)
heliumBannerAd.clearLoaded();
```

## Queueing Fullscreen Ads

```kotlin
// Create a Chartboost Mediation fullscreen ad queue with the ad queue manager. The manager returns a ChartboostMediationFullscreenAdQueue.
val queue: ChartboostMediationFullscreenAdQueue = ChartboostMediationFullscreenAdQueueManager.queue(context, placementName)

// To listen to ad queue events, you can use the ChartboostMediationFullscreenAdQueueListener
queue.adQueueListener = object : ChartboostMediationFullscreenAdQueueListener {
    override fun onFullScreenAdQueueUpdated(
        adQueue: ChartboostMediationFullscreenAdQueue,
        result: AdLoadResult,
        numberOfAdsReady: Int,
    ) {
        println(
            "Fullscreen ad queue updated${
                if (result.error != null) {
                    " with error: ${result.error?.cause}"
                } else {
                    ""
                }
            }. Number of ads ready: $numberOfAdsReady",
        )
    }

    override fun onFullscreenAdQueueExpiredAdRemoved(
        adQueue: ChartboostMediationFullscreenAdQueue,
        numberOfAdsReady: Int,
    ) {
        println("Fullscreen ad queue removed an expired ad. Number of ads ready: $numberOfAdsReady")
    }
}

// To start queueing ads, simply use the `start()` method.
queue.start()

// To check if the queue is running, query the read-only `isRunning` property.
queue.isRunning

// To check if there's an ad in the queue, use the `hasNextAd()` method.
// To get an ad from the queue, use the `getNextAd()` method.
CoroutineScope(Main).launch {
  if (queue.hasNextAd) {
    val ad = queue.nextAd()
    // Showing an ad needs to be in a suspend function.
    // Be mindful that ads will need to be reattached to your ChartboostMediationFullscreenAdListener if you are listening to ad cycle events.
    ad?.listener = fullscreenAdListener()
    ad?.show(context)
  }
}

// To stop the queue, simply use the `stop()` method.
queue.stop()

```

```java
// Create a Chartboost Mediation fullscreen ad queue with the ad queue manager. The manager returns a ChartboostMediationFullscreenAdQueue.
ChartboostMediationFullscreenAdQueue queue = ChartboostMediationFullscreenAdQueueManager.queue(context, placementName)

// To listen to ad queue events, you can use the ChartboostMediationFullscreenAdQueueListener
final ChartboostMediationFullscreenAdQueueListener fullscreenAdQueueListener = new ChartboostMediationFullscreenAdQueueListener() {
    @Override
    public void onFullScreenAdQueueUpdated(@NonNull ChartboostMediationFullscreenAdQueue adQueue, @NonNull AdLoadResult result, int numberOfAdsReady) {
        System.out.println("Fullscreen ad queue has been updated for placement " + placementName + ". Number of ads ready to show: " + numberOfAdsReady);
    }

    @Override
    public void onFullscreenAdQueueExpiredAdRemoved(@NonNull ChartboostMediationFullscreenAdQueue adQueue, int numberOfAdsReady) {
        System.out.println("Fullscreen ad queue expired ad has been removed for placement " + placementName + ". Number of ads ready to show: " + numberOfAdsReady);
    }
};

// Attach the created ChartboostMediationFullscreenAdQueueListener with the setAdQueueListener() method.
queue.setAdQueueListener(fullscreenAdQueueListener);

// To start queueing ads, simply use the `start()` method.
queue.start()

// To check if the queue is running, query the `isRunning()` method.
queue.isRunning()

// To check if there's an ad in the queue, use the `hasNextAd()` method.
// To get an ad from the queue, use the `getNextAd()` method.
if (queue.hasNextAd()) {
  ChartboostMediationFullscreenAd ad = queue.getNextAd()
  if (ad != null) {
    // Be mindful that ads will need to be reattached to your ChartboostMediationFullscreenAdListener if you are listening to ad cycle events.
    ad.setListener(fullscreenAdListener())  
    ad.showFullscreenAdFromJava(context, result -> {});
  } else {
    System.out.println("Fullscreen ad is null. Load an ad first.");
  }
}

// To stop the queue, simply use the `stop()` method.
queue.stop()
```
