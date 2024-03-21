# Use Proguard

For Helium SDK 3.1.0+:

- The usage of Proguard is no longer required for the Helium SDK and its adapters; however, partner network SDKs may still require Proguard. Please refer to their respective SDK documentation as to whether you need to ammend proguard rules to your application.

Prior to Helium SDK 3.1.0:

- If using proguard, add the following to your `proguard-rules.pro` file.

```java
-keep class com.chartboost.** { *; }
```
