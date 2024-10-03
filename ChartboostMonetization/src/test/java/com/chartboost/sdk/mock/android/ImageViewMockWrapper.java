package com.chartboost.sdk.mock.android;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ImageViewMockWrapper {
    public final ImageView mockImageView = mock(ImageView.class);

    private Drawable drawable = null;

    public ImageViewMockWrapper() {
        // slightly different form required for Answer<Void>
        Mockito.doAnswer(new SetImageDrawableAnswer()).when(mockImageView).setImageDrawable(any(Drawable.class));

        when(mockImageView.getDrawable()).thenAnswer(new GetDrawableAnswer());
    }

    private class SetImageDrawableAnswer implements Answer<Void> {
        @Override
        public Void answer(InvocationOnMock invocation) {
            drawable = invocation.getArgument(0, Drawable.class);
            return null;
        }
    }

    private class GetDrawableAnswer implements Answer<Drawable> {

        @Override
        public Drawable answer(InvocationOnMock invocation) {
            return drawable;
        }
    }
}
