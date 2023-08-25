# Initialize the Chartboost Mediation SDK

Import the Chartboost Mediation SDK:

```kotlin Kotlin
import com.chartboost.heliumsdk.HeliumSdk
```
```java Java
import com.chartboost.heliumsdk.HeliumSdk;
```

## Initializing the Chartboost Mediation SDK and setting a HeliumSdkListener

Using the Chartboost Mediation application ID and application signature from your dashboard, initialize the Chartboost Mediation SDK. You may optionally specify partners to skip initialization:

```kotlin Kotlin
HeliumSdk.start(this@MyActivityContext, myHeliumAppId, myHeliumAppSignature, HeliumInitializationOptions(), heliumSdkListener)
```
```java Java
HeliumSdk.start(MyActivityContext.this, myHeliumAppId, myHeliumAppSignature, new HeliumInitializationOptions(), heliumSdkListener);
```

Make sure that these are the Chartboost Mediation `AppId` and `AppSignature` values that you obtain directly from your ChartboostMediation Dashboard for your app as opposed to credentials from Chartboost or any other Ad Network.

In the above example we’re passing in the `heliumSdkListener` object to act as our HeliumSdkListener. Implementing the `HeliumSdkListener` interface allows you to receive notifications about the Chartboost Mediation SDK’s initialization process:

```kotlin Kotlin
val heliumSdkListener = object : HeliumSdk.HeliumSdkListener { error -> {
    error?.let {
        Log.d(TAG, "Helium SDK failed to initialize. Reason: ${it.message}")
    } ?: run {
        // SDK Initialized with no errors. 
        Log.d(TAG, "Helium SDK Initialized successfully")
    }
}
```
```java Java
HeliumSdk.HeliumSdkListener heliumSdkListener = new HeliumSdk.HeliumSdkListener() {
    @Override
    public void didInitialize(Error error) {
        if (error != null) {
            Log.d(TAG,"Helium SDK failed to initialize. Reason: " + error.getMessage());
        } else {
            //SDK Started,
            Log.d(TAG,"Helium SDK initialized successfully");
        }
    }
};
```

The `didInitialize` method will be called when the SDK is ready to handle more calls. If the SDK initialized successfully, then proceed. If the error parameter is not null, it will state the reason why the SDK failed to initialize.
