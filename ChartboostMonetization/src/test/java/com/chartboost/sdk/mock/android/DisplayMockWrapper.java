package com.chartboost.sdk.mock.android;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import android.view.Display;

import com.chartboost.sdk.test.TestContainerControl;

import org.mockito.invocation.InvocationOnMock;

public class DisplayMockWrapper {
    public final Display mockDisplay = mock(Display.class);
    private final TestContainerControl control;

    public DisplayMockWrapper(TestContainerControl control) {
        this.control = control;
        lenient().when(mockDisplay.getHeight()).thenAnswer(new GetHeightAnswer());
        lenient().when(mockDisplay.getWidth()).thenAnswer(new GetWidthAnswer());
    }

    private class GetHeightAnswer implements org.mockito.stubbing.Answer<Integer> {
        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            return control.displayMetrics().height();
        }
    }

    private class GetWidthAnswer implements org.mockito.stubbing.Answer<Integer> {
        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            return control.displayMetrics().width();
        }
    }
}
