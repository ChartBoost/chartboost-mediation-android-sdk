package com.chartboost.sdk.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.chartboost.sdk.internal.UiPoster;
import com.chartboost.sdk.internal.di.DependencyContainerInternalImpl;
import com.chartboost.sdk.mock.android.UiPosterScheduledExecutionQueue;

import java.util.ArrayList;
import java.util.List;

public class AndroidTestContainerBuilder {
    final TestContainerControl control;
    final TestFactory factory;

    final List<Runnable> runnablesToRunAfterAnyRunnable;
    final UiPosterScheduledExecutionQueue handlerMockWrapper;
    final UiPoster uiPoster;

    final TestTimeSource testTimeSource;

    TestAndroid android;
    Application applicationContext;

    public AndroidTestContainerBuilder() {
        this(new TestContainerControl());
    }

    public AndroidTestContainerBuilder(TestContainerControl control) {
        this.control = control;

        factory = new TestFactory();

        this.testTimeSource = new TestTimeSource();
        runnablesToRunAfterAnyRunnable = new ArrayList<>();

        handlerMockWrapper = new UiPosterScheduledExecutionQueue(testTimeSource);
        uiPoster = handlerMockWrapper.getMockHandler();

        android = new TestAndroid();
        android.sdkVersion = Build.VERSION_CODES.O_MR1;
        android.osReleaseVersion = "8.1";

        applicationContext = mock(Application.class);
        when(applicationContext.getApplicationContext()).thenReturn(applicationContext);
        DependencyContainerInternalImpl dependencyContainerInternal = new DependencyContainerInternalImpl();
        dependencyContainerInternal.initialize(applicationContext);
    }

    public AndroidTestContainer build() {
        return new AndroidTestContainer(this);
    }

    public AndroidTestContainerBuilder withSpyOnAndroid() {
        android = spy(android);
        return this;
    }

    public AndroidTestContainerBuilder withSdkVersion(int sdkVersion) {
        android.sdkVersion = sdkVersion;
        return this;
    }

    public AndroidTestContainerBuilder withOsReleaseVersion(String version) {
        android.osReleaseVersion = version;
        return this;
    }

    public TestContainerBuilder toTestContainerBuilder() {
        return new TestContainerBuilder(build());
    }

    public AndroidTestContainerBuilder withRobolectricContext() {
        applicationContext = ApplicationProvider.getApplicationContext();
        return this;
    }
}
