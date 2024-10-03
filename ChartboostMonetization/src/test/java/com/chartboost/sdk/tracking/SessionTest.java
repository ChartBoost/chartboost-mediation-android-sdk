package com.chartboost.sdk.tracking;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionTest {

    private SharedPreferences.Editor editMock = mock(SharedPreferences.Editor.class);
    private SharedPreferences prefsMock = mock(SharedPreferences.class);
    private Session session;

    @Before
    public void setup() {
        when(prefsMock.edit()).thenReturn(editMock);
    }

    @Test
    public void sessionCountLoadMaxTest() {
        int sessionsValue = Integer.MAX_VALUE;
        when(prefsMock.getInt(anyString(), anyInt())).thenReturn(sessionsValue);
        session = new Session(prefsMock);
        int counter = session.getSessionCounter();
        assertEquals(sessionsValue, counter);
    }

    @Test
    public void sessionCountLoadDefaultTest() {
        int sessionsValue = 0;
        when(prefsMock.getInt(anyString(), anyInt())).thenReturn(sessionsValue);
        session = new Session(prefsMock);
        int counter = session.getSessionCounter();
        assertEquals(sessionsValue, counter);
    }

    @Test
    public void sessionCountLoadErrorTest() {
        session = new Session(mock(SharedPreferences.class));
        int counter = session.getSessionCounter();
        assertEquals(0, counter);
    }

    @Test
    public void sessionCountAddTest() {
        int sessionsValue = 0;
        when(prefsMock.getInt(anyString(), anyInt())).thenReturn(sessionsValue);
        when(editMock.putInt(anyString(), anyInt())).thenReturn(editMock);
        session = new Session(prefsMock);
        int counter = session.getSessionCounter();
        assertEquals(sessionsValue, counter);
        session.addSession();
        int incrementedCounter = session.getSessionCounter();
        assertEquals(counter + 1, incrementedCounter);
    }
}
