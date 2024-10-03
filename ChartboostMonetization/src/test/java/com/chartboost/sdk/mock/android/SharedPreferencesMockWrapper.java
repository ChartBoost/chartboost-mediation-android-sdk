package com.chartboost.sdk.mock.android;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import org.mockito.invocation.InvocationOnMock;

import java.util.HashMap;
import java.util.Map;

public class SharedPreferencesMockWrapper {
    public final SharedPreferences mockSharedPreferences = mock(SharedPreferences.class);

    private final Map<String, Object> values;

    public SharedPreferencesMockWrapper(Map<String, Object> values) {
        this.values = values;
        registerMockAnswers();
    }

    @SuppressLint("CommitPrefEdits")
    private void registerMockAnswers() {
        lenient().when(mockSharedPreferences.getInt(anyString(), anyInt())).thenAnswer(new GetIntAnswer());
        lenient().when(mockSharedPreferences.getString(anyString(), anyString())).thenAnswer(new GetStringAnswer());
        lenient().when(mockSharedPreferences.getBoolean(anyString(), anyBoolean())).thenAnswer(new GetBooleanAnswer());
        lenient().when(mockSharedPreferences.edit()).thenAnswer(new EditAnswer());
    }

    public Map<String, Object> cloneValues() {
        HashMap<String, Object> clonedValues = new HashMap<>();
        clonedValues.putAll(values);
        return clonedValues;
    }

    public void replaceValues(Map<String, Object> newValues) {
        values.clear();
        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();

            values.put(k, v);
        }
    }

    private class GetStringAnswer implements org.mockito.stubbing.Answer<String> {
        @Override
        public String answer(InvocationOnMock invocation) throws Throwable {
            String key = invocation.getArgument(0, String.class);
            String defaultValue = invocation.getArgument(1, String.class);

            String value = (String) values.get(key);
            return value != null ? value : defaultValue;
        }
    }

    private class GetBooleanAnswer implements org.mockito.stubbing.Answer<Boolean> {
        @Override
        public Boolean answer(InvocationOnMock invocation) throws Throwable {
            String key = invocation.getArgument(0, String.class);
            Boolean defaultValue = invocation.getArgument(1, Boolean.class);

            Boolean value = (Boolean) values.get(key);
            return value != null ? value : defaultValue;
        }
    }

    private class EditAnswer implements org.mockito.stubbing.Answer<SharedPreferences.Editor> {
        @Override
        public SharedPreferences.Editor answer(InvocationOnMock invocation) throws Throwable {
            SharedPreferencesEditorMockWrapper editorMockWrapper =
                    new SharedPreferencesEditorMockWrapper(SharedPreferencesMockWrapper.this);
            return editorMockWrapper.mockEditor;
        }
    }

    private class GetIntAnswer implements org.mockito.stubbing.Answer<Integer> {

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            String key = invocation.getArgument(0, String.class);
            Integer defaultValue = invocation.getArgument(1, Integer.class);

            Integer value = (Integer) values.get(key);
            return value != null ? value : defaultValue;
        }
    }
}
