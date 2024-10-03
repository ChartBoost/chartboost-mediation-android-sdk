package com.chartboost.sdk.mock.android;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.util.DisplayMetrics;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DisplayMetricsMockWrapper {
    public final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);

    public DisplayMetricsMockWrapper() {
        doAnswer(new SetToAnswer()).when(mockDisplayMetrics).setTo(any(DisplayMetrics.class));
    }

    private class SetToAnswer implements Answer<Void> {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            DisplayMetrics other = invocation.getArgument(0, DisplayMetrics.class);
            mockDisplayMetrics.heightPixels = other.heightPixels;
            mockDisplayMetrics.widthPixels = other.widthPixels;
            mockDisplayMetrics.densityDpi = other.densityDpi;
            mockDisplayMetrics.density = other.density;
            mockDisplayMetrics.scaledDensity = other.scaledDensity;
            return null;
        }
    }
}
