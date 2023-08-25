# Android Chartboost Mediation SDK

![badge](https://img.shields.io/endpoint?url=https%3A%2F%2Fchartboost.s3.amazonaws.com%2Fchartboost-mediation%2Fsdk%2Fandroid%2Fcode-coverage%2Fcoverage-percent.json)

The Android Chartboost Mediation SDK, by Chartboost, is a Unified-Auction & Mediated solution which helps developers increase their mobile apps' revenue with the inclusion of other supported Programmatic & Mediated SDKs.

## Get Started on Android

To keep our Chartboost Mediation Android SDK documentation consistent with our public [Developer Docs](https://developers.chartboost.com) site, we will organize our README.md similarly.

- [Get Started on Android](#get-started-on-android)
- [Integrate Network SDKs](Documentation/IntegrateNetworkSdks.md)
- [Use Proguard](Documentation/UseProguard.md)
- [Initialize ChartboostMediation](Documentation/InitializeChartboostMediation.md)
- [Configure Helium](Documentation/ConfigureHelium.md)
- [Load Ads](Documentation/LoadAds.md)
- [Show Ads](Documentation/ShowAds.md)
- [Delegate Usage](Documentation/DelegateUsage.md)
- [Error Codes](Documentation/ErrorCodes.md)
- [Android Changelog](CHANGELOG.md)

----

## Before You Begin

- Have you signed up for a Chartboost Mediation Account?
- Did you [add an app](https://developers.chartboost.com/docs/import-apps-into-helium) to the Chartboost Mediation Dashboard?
- Have you [set up Ad Placements](https://developers.chartboost.com/docs/manage-placements) in the Chartboost Mediation Dashboard?

## Minimum Supported Development Tools

| Software       | Version             |
| :------------- | :------------------ |
| Android Studio | 2020.3.1+           |
| Android OS     | 5.0+ (API level 21) |

## Add the Chartboost Mediation SDK to your project

For `build.gradle`

```gradle
repositories {
  maven {
    name "Chartboost Mediation's maven repo"
    url "https://cboost.jfrog.io/artifactory/chartboost-mediation"
  }
}
 
dependencies {
   implementation 'com.chartboost:chartboost-mediation-sdk:4.5.0'
}
```

## Add 3rd-Party Dependencies

```gradle
implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.20")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
implementation("com.squareup.okhttp3:okhttp:4.10.0")
implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
```

## Add Google Play Services

Add the Google Play Services Library as a dependency of your project. For detailed instructions, reference the official integration instructions for [Google Play Services](https://developers.google.com/android/guides/setup).

As opposed to referencing the entire Google Play Services package, you only need `play-services-base`, `play-services-ads-identifier`, and `play-services-appset`:

```gradle
implementation 'com.google.android.gms:play-services-base:18.1.0'
implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
implementation 'com.google.android.gms:play-services-appset:16.0.2'
```

### OPTIONAL STEP: Download the Chartboost Mediation Android Sample App

The Chartboost Mediation Android Sample App is no longer distributed via a package and has been moved to a public GitHub repo.

- [Chartboost Mediation Android SDK Sample App](https://github.com/ChartBoost/android-chartboost-mediation-sdk-example)
