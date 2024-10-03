package com.chartboost.sdk.mock.android;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Map;

class SharedPreferencesEditorMockWrapper {
    public final SharedPreferences.Editor mockEditor;
    private final SharedPreferencesMockWrapper sharedPreferencesMockWrapper;

    private final Map<String, Object> newValues;

    SharedPreferencesEditorMockWrapper(SharedPreferencesMockWrapper sharedPreferenceMockWrapper) {
        this.mockEditor = mock(SharedPreferences.Editor.class);
        this.sharedPreferencesMockWrapper = sharedPreferenceMockWrapper;

        this.newValues = sharedPreferenceMockWrapper.cloneValues();

        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenAnswer(new PutValueAnswer());
        when(mockEditor.putFloat(anyString(), anyFloat())).thenAnswer(new PutValueAnswer());
        when(mockEditor.putInt(anyString(), anyInt())).thenAnswer(new PutValueAnswer());
        when(mockEditor.putLong(anyString(), anyLong())).thenAnswer(new PutValueAnswer());
        when(mockEditor.putString(anyString(), anyString())).thenAnswer(new PutValueAnswer());
        doAnswer(new ApplyAnswer()).when(mockEditor).apply();
        doAnswer(new CommitAnswer()).when(mockEditor).commit();
    }

    private class ApplyAnswer implements Answer<Void> {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            sharedPreferencesMockWrapper.replaceValues(newValues);
            return null;
        }
    }

    private class CommitAnswer implements Answer<Void> {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            sharedPreferencesMockWrapper.replaceValues(newValues);
            return null;
        }
    }

    private class PutValueAnswer implements Answer<SharedPreferences.Editor> {
        @Override
        public SharedPreferences.Editor answer(InvocationOnMock invocation) throws Throwable {
            String key = invocation.getArgument(0, String.class);
            Object value = invocation.getArgument(1, Object.class);
            newValues.put(key, value);
            return mockEditor;
        }
    }
}
