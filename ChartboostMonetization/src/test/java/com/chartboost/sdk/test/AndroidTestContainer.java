package com.chartboost.sdk.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.MockUtil.isMock;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.chartboost.sdk.internal.External.Android;
import com.chartboost.sdk.internal.UiPoster;
import com.chartboost.sdk.legacy.Factory;
import com.chartboost.sdk.mock.android.UiPosterScheduledExecutionQueue;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.List;

public class AndroidTestContainer implements AutoCloseable {
    public final TempDirectory tempDirectory = new TempDirectory();
    public final File cacheDir = new File(tempDirectory.directory, "cache");

    public final TestAndroid android;
    public final TestFactory factory;
    final List<Runnable> runnablesToRunAfterAnyRunnable;

    final TestContainerControl control;

    final UiPosterScheduledExecutionQueue handlerMockWrapper;

    final UiPoster uiPoster;
    final TestTimeSource testTimeSource;
    public final Context applicationContext;

    public final NetworkInfo activeNetworkInfo;
    public final NetworkCapabilities activeNetworkCapability;
    public final ConnectivityManager connectivityManager;
    final PackageManager packageManager;
    public final Application application;
    public final Activity activity;

    public final WindowManager windowManager;
    public final DisplayMetrics displayMetrics;

    public AndroidTestContainer() {
        this(new AndroidTestContainerBuilder());
    }

    AndroidTestContainer(AndroidTestContainerBuilder builder) {
        control = builder.control;
        final TestContainerControl.Constants constants = control.constants;

        this.factory = builder.factory;

        android = builder.android;
        runnablesToRunAfterAnyRunnable = builder.runnablesToRunAfterAnyRunnable;
        handlerMockWrapper = builder.handlerMockWrapper;
        testTimeSource = builder.testTimeSource;
        uiPoster = builder.uiPoster;

        Android.initialize(android);
        Factory.install(factory);

        displayMetrics = mock(DisplayMetrics.class);
        displayMetrics.density = control.displayMetrics().density();
        displayMetrics.densityDpi = control.displayMetrics().densityDpi();
        displayMetrics.widthPixels = 768;
        displayMetrics.heightPixels = 1024;
        displayMetrics.xdpi = 234.46153f;
        displayMetrics.ydpi = 234.46153f;

        Configuration configuration = mock(Configuration.class);
        configuration.orientation = (control.displayMetrics().height() > control.displayMetrics().width())
                ? Configuration.ORIENTATION_PORTRAIT
                : Configuration.ORIENTATION_LANDSCAPE;
        Resources resources = mock(Resources.class);
        lenient().when(resources.getDisplayMetrics()).thenReturn(displayMetrics);
        lenient().when(resources.getConfiguration()).thenReturn(configuration);

        activeNetworkInfo = mock(NetworkInfo.class);
        lenient().when(activeNetworkInfo.isConnected()).thenReturn(true);
        activeNetworkCapability = mock(NetworkCapabilities.class);
        lenient().when(activeNetworkCapability.hasCapability(anyInt())).thenReturn(true);

        connectivityManager = mock(ConnectivityManager.class);
        lenient().when(connectivityManager.getActiveNetworkInfo()).thenReturn(activeNetworkInfo);
        if (Build.VERSION.SDK_INT >= 23)
            lenient().when(connectivityManager.getActiveNetwork()).thenReturn(mock(Network.class));
        lenient().when(connectivityManager.getNetworkCapabilities(any(Network.class))).thenReturn(activeNetworkCapability);

        WindowMetrics windowMetrics = mock(WindowMetrics.class);
        Rect bounds = mock(Rect.class);
        lenient().when(bounds.width()).thenReturn(720);
        lenient().when(bounds.height()).thenReturn(1024);
        lenient().when(windowMetrics.getBounds()).thenReturn(bounds);
        windowManager = mock(WindowManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when(windowManager.getCurrentWindowMetrics()).thenReturn(windowMetrics);
        }

        PackageInfo packageInfoMock = new PackageInfo();
        packageInfoMock.versionName = "1.0";
        packageManager = mock(PackageManager.class);
        try {
            lenient().when(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfoMock);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        applicationContext = builder.applicationContext;
        if (isMock(applicationContext)) {
            lenient().when(applicationContext.getPackageName()).thenReturn(control.constants.packageName);
            lenient().when(applicationContext.getCacheDir()).thenReturn(cacheDir);
            lenient().when(applicationContext.getResources()).thenReturn(resources);
            lenient().when(applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
            lenient().when(applicationContext.getPackageManager()).thenReturn(packageManager);

        }

        android.advertisingIdClientInfo = new AdvertisingIdClient.Info(
                "01234567-abcd-01ab-23cd-0123456789ab",
                false
        );

        android.environmentExternalStorageState = Environment.MEDIA_MOUNTED;
        Android.initialize(android);

        application = mock(Application.class);

        control.grantPermission(Manifest.permission.INTERNET);
        control.grantPermission(Manifest.permission.ACCESS_NETWORK_STATE);

        activity = createMockActivity();

        PackageInfo metaDataPackageInfo = mock(PackageInfo.class);
        metaDataPackageInfo.versionName = constants.packageVersionName;
        try {
            lenient().when(packageManager.getPackageInfo(constants.packageName, PackageManager.GET_META_DATA)).thenReturn(metaDataPackageInfo);
        } catch (PackageManager.NameNotFoundException ex) {
            throw new Error(ex);
        }
    }

    public Activity createMockActivity() {
        Activity activity = mock(Activity.class, RETURNS_DEEP_STUBS);
        when(activity.getApplication()).thenReturn(application);
        when(activity.getApplicationContext()).thenReturn(applicationContext);
        when(activity.getPackageManager()).thenReturn(packageManager);

        Answer<Integer> checkPermissionAnswer = new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                String permission = invocation.getArgument(0, String.class);
                return control.hasPermission(permission)
                        ? PackageManager.PERMISSION_GRANTED
                        : PackageManager.PERMISSION_DENIED;
            }
        };

        if (Build.VERSION.SDK_INT >= 23)
            lenient().when(activity.checkSelfPermission(anyString())).thenAnswer(checkPermissionAnswer);
        lenient().when(activity.checkCallingOrSelfPermission(anyString())).thenAnswer(checkPermissionAnswer);

        return activity;
    }

    public void grantPermission(String permission) {
        control.grantPermission(permission);
    }

    public void revokePermission(String permission) {
        control.revokePermission(permission);
    }

    @Override
    public void close() {
        tempDirectory.close();
        cacheDir.delete();
    }

    public File getCacheFile(String filename) {
        return new File(cacheDir, filename);
    }
}
