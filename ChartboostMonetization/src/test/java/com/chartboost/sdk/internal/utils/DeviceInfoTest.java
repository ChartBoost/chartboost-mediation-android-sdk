package com.chartboost.sdk.internal.utils;

import static android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
import static android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE;
import static android.content.res.Configuration.UI_MODE_TYPE_NORMAL;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.chartboost.sdk.internal.utils.DeviceInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Testing ANDROID_ID is not possible cause secure settings always return null
 */
@RunWith(MockitoJUnitRunner.class)
public class DeviceInfoTest {

    private Context contextMock = mock(Context.class);
    private ContentResolver contentResolverMock = mock(ContentResolver.class);
    private Resources resourcesMock = mock(Resources.class);
    private Configuration configurationMock = mock(Configuration.class);
    private DisplayMetrics metricsMock = mock(DisplayMetrics.class);
    private PackageManager packageManagerMock = mock(PackageManager.class);

    @Before
    public void setup() {
        when(contextMock.getPackageManager()).thenReturn(packageManagerMock);
        lenient().when(contextMock.getContentResolver()).thenReturn(contentResolverMock);
        when(contextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getDisplayMetrics()).thenReturn(metricsMock);
        when(resourcesMock.getConfiguration()).thenReturn(configurationMock);
    }

    @Test
    public void generateUniqueIdTest() {
        boolean isTrackingLimited = false;
        // even though we should get here android id, secure settings will return null and we are getting fallback value of randomUUID
        String id = DeviceInfo.getUniqueId(contextMock, isTrackingLimited);
        assertNotNull(id);
        assertEquals(36, id.length());
    }

    @Test
    public void generateUniqueIdTrackingLimitedTest() {
        boolean isTrackingLimited = true;
        String id = DeviceInfo.getUniqueId(contextMock, isTrackingLimited);
        assertNotNull(id);
        assertEquals(36, id.length());
    }

    @Test
    public void getOpenRTBDeviceTypePhoneTest() {
        metricsMock.heightPixels = 1024;
        metricsMock.widthPixels = 800;
        metricsMock.ydpi = 320; //xhdpi
        metricsMock.xdpi = 320;

        Integer type = DeviceInfo.getOpenRTBDeviceType(contextMock);
        assertNotNull(type);
        // 4 is phone type
        assertEquals(4, type.intValue());
    }

    @Test
    public void getOpenRTBDeviceTypeTabletTest() {
        metricsMock.heightPixels = 2040;
        metricsMock.widthPixels = 1024;
        metricsMock.ydpi = 320; //xhdpi
        metricsMock.xdpi = 320;

        Integer type = DeviceInfo.getOpenRTBDeviceType(contextMock);
        assertNotNull(type);
        // 5 is tablet type
        assertEquals(5, type.intValue());
    }

    @Test
    public void getDeviceTypePhoneTest() {
        configurationMock.screenLayout = SCREENLAYOUT_SIZE_LARGE;
        configurationMock.uiMode = UI_MODE_TYPE_NORMAL;

        when(packageManagerMock.hasSystemFeature("org.chromium.arc.device_management")).thenReturn(false);
        when(packageManagerMock.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false);
        when(packageManagerMock.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false);
        when(packageManagerMock.hasSystemFeature(PackageManager.FEATURE_TELEVISION)).thenReturn(false);

        String type = DeviceInfo.getType(contextMock);
        assertEquals("phone", type);
    }

    @Test
    public void getDeviceTypeTabletTest() {
        configurationMock.screenLayout = SCREENLAYOUT_SIZE_XLARGE;
        configurationMock.uiMode = UI_MODE_TYPE_NORMAL;

        when(packageManagerMock.hasSystemFeature("org.chromium.arc.device_management")).thenReturn(false);
        when(packageManagerMock.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false);
        when(packageManagerMock.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false);
        when(packageManagerMock.hasSystemFeature(PackageManager.FEATURE_TELEVISION)).thenReturn(false);

        String type = DeviceInfo.getType(contextMock);
        assertEquals("tablet", type);
    }

    @Test
    public void getDeviceTypeTVTest() {
        configurationMock.screenLayout = SCREENLAYOUT_SIZE_XLARGE;
        configurationMock.uiMode = UI_MODE_TYPE_NORMAL;

        when(packageManagerMock.hasSystemFeature("org.chromium.arc.device_management")).thenReturn(false);
        when(packageManagerMock.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false);
        when(packageManagerMock.hasSystemFeature(PackageManager.FEATURE_TELEVISION)).thenReturn(true);

        String type = DeviceInfo.getType(contextMock);
        assertEquals("tv", type);
    }

    @Test
    public void getDeviceTypeWatchTest() {
        configurationMock.screenLayout = SCREENLAYOUT_SIZE_XLARGE;
        configurationMock.uiMode = UI_MODE_TYPE_NORMAL;

        when(packageManagerMock.hasSystemFeature("org.chromium.arc.device_management")).thenReturn(false);
        when(packageManagerMock.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(true);

        String type = DeviceInfo.getType(contextMock);
        assertEquals("watch", type);
    }

    @Test
    public void getDeviceTypeChromebookTest() {
        configurationMock.screenLayout = SCREENLAYOUT_SIZE_XLARGE;
        configurationMock.uiMode = UI_MODE_TYPE_NORMAL;

        when(packageManagerMock.hasSystemFeature("org.chromium.arc.device_management")).thenReturn(true);

        String type = DeviceInfo.getType(contextMock);
        assertEquals("chromebook", type);
    }
}
