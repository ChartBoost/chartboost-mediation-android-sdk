package com.chartboost.sdk.internal.Libraries;

import static com.chartboost.sdk.test.TestUtils.assertFileContentsMatchByteArray;
import static com.chartboost.sdk.test.TestUtils.createNewFile;
import static com.chartboost.sdk.test.TestUtils.openOutputStream;
import static com.chartboost.sdk.test.TestUtils.readResourceToByteArray;
import static com.chartboost.sdk.test.TestUtils.writeResourceToFile;
import static com.chartboost.sdk.test.TestUtils.writeStringToFile;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.legacy.CBConfig;
import com.chartboost.sdk.test.AndroidTestContainer;
import com.chartboost.sdk.test.AndroidTestContainerBuilder;
import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.SdkConfigurationBuilder;
import com.chartboost.sdk.test.TestContainerControl;
import com.chartboost.sdk.test.TestUtils;

import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FileCacheTest extends BaseTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void getPrecacheFilesTest() throws IOException {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            File baseDir = h.internalBaseDir;
            File precacheDir = new File(baseDir, "precache");
            File file = new File(precacheDir, "video.mp4");
            file.createNewFile();
            File[] files = fc.getPrecacheFiles();
            assertNotNull(files);
            assertTrue(files.length > 0);
        }
    }

    @Test
    public void deleteFileTest() throws IOException {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            File baseDir = h.internalBaseDir;
            File precacheDir = new File(baseDir, "precache");
            File file = new File(precacheDir, "video.mp4");
            file.createNewFile();
            fc.deleteFile(file);
            File[] files = fc.getPrecacheFiles();
            assertNotNull(files);
            assertTrue(files.length == 0);
        }
    }

    @Test
    public void getFileIfCachedTrueTest() throws IOException {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            File baseDir = h.internalBaseDir;
            File precacheDir = new File(baseDir, "precache");
            File file = new File(precacheDir, "video.mp4");
            writeByteArrayToFile(file, "testdata".getBytes(), false);

            File precachedVideo = fc.getFileIfCached(precacheDir, "video.mp4");
            assertNotNull(precachedVideo);
        }
    }

    @Test
    public void getFileIfCachedFalseTest() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            File baseDir = h.internalBaseDir;
            File precacheDir = new File(baseDir, "precache");
            File file = new File(precacheDir, "video.mp4");
            File precachedVideo = fc.getFileIfCached(precacheDir, "video.mp4");
            assertNull(precachedVideo);
        }
    }

    @Test
    public void isFileCachedTrueTest() throws IOException {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            File baseDir = h.internalBaseDir;
            File precacheDir = new File(baseDir, "precache");
            File file = new File(precacheDir, "video.mp4");
            writeByteArrayToFile(file, "testdata".getBytes(), false);
            boolean isCached = fc.isFileCached(file);
            assertTrue(isCached);
        }
    }

    @Test
    public void isFileCachedFalseTest() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            File baseDir = h.internalBaseDir;
            File precacheDir = new File(baseDir, "precache");
            File file = new File(precacheDir, "video.mp4");
            boolean isCached = fc.isFileCached(file);
            assertFalse(isCached);
        }
    }

    /*
        Verify that FileCache.get*{Dir,Path}() return internal paths when external caching
        is not allowed.
     */
    @Test
    public void testInternalPaths() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            assertAllPathsAreMostlyInternal(h, fc);
        }
    }

    /*
        Verify that FileCache.get*{Dir,Path}() return external paths when external caching
        is enabled and available.
     */
    @Test
    public void testExternalPathsWithExternalStorageEnabled() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            assertAllPathsAreMostlyExternal(h, fc);
        }
    }

    private void assertAllPathsAreMostlyInternal(TestHarness h, FileCache fc) {
        // "Mostly" because session, request, and track directories are always internal

        File internalBaseDir = h.internalBaseDir;
        File internalVideosDir = h.tc.getCacheFile(".chartboost/videos");
        File internalImagesDir = h.tc.getCacheFile(".chartboost/images");
        File internalTemplatesDir = h.tc.getCacheFile(".chartboost/templates");
        File internalPrecacheDir = h.tc.getCacheFile(".chartboost/precache");
        File internalPrecacheQueueDir = h.tc.getCacheFile(".chartboost/precache_queue");

        assertThat(fc.currentLocations().baseDir.getAbsolutePath(), is(equalTo(internalBaseDir.getAbsolutePath())));
        assertThat(fc.currentLocations().videosDir, is(equalTo(internalVideosDir)));
        assertThat(fc.currentLocations().imagesDir, is(equalTo(internalImagesDir)));
        assertThat(fc.currentLocations().templatesDir, is(equalTo(internalTemplatesDir)));
        assertThat(fc.currentLocations().precacheDir, is(equalTo(internalPrecacheDir)));
        assertThat(fc.currentLocations().precacheQueueDir, is(equalTo(internalPrecacheQueueDir)));

        assertTrue(internalBaseDir.exists());
        assertTrue(internalVideosDir.exists());
        assertTrue(internalImagesDir.exists());
        assertTrue(internalTemplatesDir.exists());
        assertTrue(internalPrecacheDir.exists());
        assertTrue(internalPrecacheQueueDir.exists());
    }

    private void assertAllPathsAreMostlyExternal(TestHarness h, FileCache fc) {
        // "Mostly" because session, request, and track directories are always internal

        File externalBaseDir = h.internalBaseDir;
        File externalVideosDir = h.tc.getCacheFile(".chartboost/videos");
        File externalImagesDir = h.tc.getCacheFile(".chartboost/images");
        File externalTemplatesDir = h.tc.getCacheFile(".chartboost/templates");
        File externalPrecacheDir = h.tc.getCacheFile(".chartboost/precache");
        File externalPrecacheQueueDir = h.tc.getCacheFile(".chartboost/precache_queue");

        assertThat(fc.currentLocations().baseDir.getAbsolutePath(), is(equalTo(externalBaseDir.getAbsolutePath())));
        assertThat(fc.currentLocations().videosDir, is(equalTo(externalVideosDir)));
        assertThat(fc.currentLocations().imagesDir, is(equalTo(externalImagesDir)));
        assertThat(fc.currentLocations().templatesDir, is(equalTo(externalTemplatesDir)));
        assertThat(fc.currentLocations().precacheDir, is(equalTo(externalPrecacheDir)));
        assertThat(fc.currentLocations().precacheQueueDir, is(equalTo(externalPrecacheQueueDir)));

        assertTrue(externalBaseDir.exists());
        assertTrue(externalVideosDir.exists());
        assertTrue(externalImagesDir.exists());
        assertTrue(externalTemplatesDir.exists());
        assertTrue(externalPrecacheQueueDir.exists());
    }

    /*
        Verify basic functionality of FileCache.getBasePathDir:
         - returns ".chartboost" within the cache directory if there is no external storage.
         - returns ".chartboost" within the external directory if external storage is available.
     */
    @Test
    public void testGetBasePathDir() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            assertThat(fc.currentLocations().baseDir, is(equalTo(h.internalBaseDir)));
            assertThat(fc.currentLocations().baseDir, is(equalTo(h.internalBaseDir)));

            h.tc.revokePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            // Note that this doesn't change until after calling CBConfig.validatePermissions:
            assertThat(fc.currentLocations().baseDir, is(equalTo(h.internalBaseDir)));
            CBConfig.validatePermissions(h.tc.activity);
            assertThat(fc.currentLocations().baseDir, is(equalTo(h.internalBaseDir)));
        }
    }

    /*
        Verify basic operation of FileCache.writeToDisk(File, byte[]):
         - write a new file
         - verify that the contents were written as expected
     */
    @Test
    public void testWriteToDisk_File_String_ByteArray_Success() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            String contents = "abcdef12345678";

            File path = fc.currentLocations().baseDir;
            String filename = "test.json";
            File file = new File(path, filename);

            assertFalse(file.exists());

            try {
                writeByteArrayToFile(file, contents.getBytes(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }

            assertTrue(file.exists());

            File actualFile = h.tc.getCacheFile(".chartboost/test.json");
            assertFileContentsMatchByteArray(actualFile, contents.getBytes());
        }
    }

    /*
        Verify failure handling for:
            - FileCache.writeToDisk(File path, byte[] obj)
         - returns immediately if path == null
         - returns immediately if obj == null
         - swallows exceptions encountered while writing the file
     */
    @Test
    public void testWriteToDisk_File_FileOrString_ByteArray_Failure() {
        // If trackCritical is enabled, the trackException calls write
        // to cb_previous_session_info into the session directory in releaseTest
        TestContainerControl control = new TestContainerControl(
                new SdkConfigurationBuilder()
                        .withTrackCritical(false));

        try (TestHarness h = new TestHarness(control)) {
            FileCache fc = h.createFileCache();

            String contents = "abcdef12345678";

            File path = fc.currentLocations().baseDir;
            String filename = "test.json";
            File file = new File(path, filename);

            assertFalse(file.exists());

            // create a file where a directory is supposed to be, so that the directory can't be created
            File supposedSubdirectory = new File(path, "supposedSubdirectory");
            writeStringToFile(supposedSubdirectory, "garbage");

            // swallows exceptions and logs if an IOException occurs
            File cannotCreate = new File(supposedSubdirectory, "cannot_create.json");

            try {
                writeByteArrayToFile(cannotCreate, contents.getBytes(), false);
            } catch (IOException e) {

            }

            // todo: verify log output

            assertFalse(file.exists());
            assertFalse(cannotCreate.exists());
            File[] files = path.listFiles();
            HashSet<File> fileSet = new HashSet<>(Arrays.asList(files));
            assertTrue(fileSet.contains(supposedSubdirectory));
        }
    }

    @Test
    public void testReadByteArrayFromDisk() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            String resourcePath = "com/chartboost/cdn/a.chartboost.com/static-assets/interstitials-v2/close-buttons/60x60.png";

            byte[] reference = readResourceToByteArray(resourcePath);

            File testFile = h.tc.getCacheFile("test.png");
            writeResourceToFile(resourcePath, testFile);

            byte[] read = fc.readByteArrayFromDisk(testFile);
            assertThat(read, is(equalTo(reference)));
        }
    }

    @Test
    public void testReadByteArrayFromDisk_BadParameters() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            assertThat(fc.readByteArrayFromDisk(null), is(nullValue()));
        }
    }

    @Test
    public void testReadByteArrayFromDisk_FileNotFoundException() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            File testFile = h.tc.getCacheFile("test.png");
            assertFalse(testFile.exists());
            assertThat(fc.readByteArrayFromDisk(testFile), is(nullValue()));
        }
    }

    /*
        Verify normal operation of FileCache.getVideoPath(String):
         - returns the path if the file is found in the video directory
         - looks up files in external storage if available, internal storage if not
         - returns null if it is not
     */
    @Test
    public void testGetVideoPath() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            // Setup
            File internalFile1 = h.tc.getCacheFile(".chartboost/videos/file1");
            File internalFile2 = h.tc.getCacheFile(".chartboost/videos/file2");

            assertNull(fc.getVideoPath("file1"));
            assertNull(fc.getVideoPath("file2"));
            assertNull(fc.getVideoPath("file5"));

            createNewFile(internalFile1);
            createNewFile(internalFile2);

            assertThat(fc.getVideoPath("file1"), is(equalTo(internalFile1.getPath())));
            assertThat(fc.getVideoPath("file2"), is(equalTo(internalFile2.getPath())));
            assertNull(fc.getVideoPath("file5"));
        }
    }

    /*
        Verify normal operation of FileCache.getVideoPath(String) when external storage is available:
         - returns the path if the file is found in the video directory
         - returns null if it is not
     */
    @Test
    public void testGetVideoPath_ExternalStorageAvailable() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            File file1 = h.tc.getCacheFile(".chartboost/videos/file1");
            File file2 = h.tc.getCacheFile(".chartboost/videos/file2");
            createNewFile(file1);
            createNewFile(file2);

            assertThat(fc.getVideoPath("file1"), is(equalTo(file1.getPath())));
            assertThat(fc.getVideoPath("file2"), is(equalTo(file2.getPath())));
            assertNull(fc.getVideoPath("file3"));
        }
    }

    /*
        Verify normal operation of FileCache.getVideoPath(String) when external storage is available:
         - returns the path if the file is found in the video directory
         - returns null if it is not
     */
    @Test
    public void testGetVideoPath_ExternalStorageAllowedButUnavailable() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            File internalFile1 = h.tc.getCacheFile(".chartboost/videos/file1");
            createNewFile(internalFile1);

            assertThat(fc.getVideoPath("file1"), is(equalTo(internalFile1.getPath())));
            assertNull(fc.getVideoPath("file3"));
        }
    }

    //<editor-fold desc="getNativeVideoList tests">
    /*
        getNativeVideoList should return an empty JSONArray if there are no files
     */
    @Test
    public void getNativeVideoList_returns_empty_JSONArray() {
        try (TestHarness h = TestHarness.defaultNative()) {
            FileCache fc = h.createFileCache();
            JSONArray videos = fc.getNativeVideoList();
            assertThat(videos.length(), is(0));
        }
    }

    /*
        getNativeVideoList should return videos in the JSONArray
     */
    @Test
    public void getNativeVideoList_returns_videos() {
        try (TestHarness h = TestHarness.defaultNative()) {
            FileCache fc = h.createFileCache();

            AssetDescriptor.videoAd56a719.writeResourceToCache(h.internalBaseDir);
            AssetDescriptor.videoAd571b10.writeResourceToCache(h.internalBaseDir);

            JSONArray videosArray = fc.getNativeVideoList();
            List<String> videos = TestUtils.toStringList(videosArray);
            assertThat(videos,
                    hasItems("56a7190b0d60252acfeb4987_568-1453791500.mp4",
                            "571b101ff6cd4576b02b3098_568-1461391391.mp4"));
            assertThat(videos.size(), Matchers.is(2));
        }
    }

    /*
        getNativeVideoList should not include ".nomedia" files
     */
    @Test
    public void getNativeVideoList_ignores_nomedia() {
        try (TestHarness h = TestHarness.defaultNative()) {
            FileCache fc = h.createFileCache();

            AssetDescriptor.videoAd56a719.writeResourceToCache(h.internalBaseDir);
            AssetDescriptor.videoAd571b10.writeResourceToCache(h.internalBaseDir);
            TestUtils.writeStringToFile(new File(h.internalBaseDir, "videos/.nomedia"), "anything");
            JSONArray videosArray = fc.getNativeVideoList();
            List<String> videos = TestUtils.toStringList(videosArray);

            assertThat(videos, not(hasItem(containsString("nomedia"))));
        }
    }

    /*
        getNativeVideoList should not include files that end in ".tmp" (downloads in progress)
     */
    @Test
    public void getNativeVideoList_ignores_tmp() {
        try (TestHarness h = TestHarness.defaultNative()) {
            FileCache fc = h.createFileCache();

            AssetDescriptor.videoAd56a719.writeResourceToCache(h.internalBaseDir);
            AssetDescriptor.videoAd571b10.writeResourceToCache(h.internalBaseDir);
            TestUtils.writeStringToFile(new File(h.internalBaseDir, "videos/something.tmp"), "anything");
            JSONArray videosArray = fc.getNativeVideoList();
            List<String> videos = TestUtils.toStringList(videosArray);

            assertThat(videos, not(hasItem(endsWith(".tmp"))));
        }
    }

    //</editor-fold>

    //<editor-fold desc="getWebViewCacheAssets tests">
    /*
        getWebViewCacheAssets should log any exceptions to track, and return a JSONObject anyway.
     */
    @Test
    public void getWebViewCacheAssets_tracks_exceptions() {
        try (TestHarness h = TestHarness.defaultWebView()) {
            FileCache fc = h.createFileCache();

            h.sdkConfig.set(null);
            JSONObject cacheAssets;
            cacheAssets = fc.getWebViewCacheAssets();
            assertNotNull(cacheAssets);
            assertNull(cacheAssets.names());
        }
    }

    /*
        getWebViewCacheAssets() should include videos in the cache immediately after startup.
     */
    @Test
    public void getWebViewCacheAssets_includes_videos() throws JSONException {
        try (TestHarness h = TestHarness.defaultWebView()) {
            FileCache fc = h.createFileCache();

            AssetDescriptor.videoAd56a719.writeResourceToCache(h.internalBaseDir);
            AssetDescriptor.videoAd56abc8.writeResourceToCache(h.internalBaseDir);

            JSONObject cacheAssets = fc.getWebViewCacheAssets();

            List<String> videos = getCacheAssetsVideoFilenames(cacheAssets);
            assertThat(videos, hasItem("56a7190b0d60252acfeb4987_568-1453791500.mp4"));
            assertThat(videos, hasItem("56abc8b8a8b63c559b6c7be9_568-1454098616.mp4"));
            assertThat(videos.size(), Matchers.is(2));
        }
    }

    @NonNull
    private static List<String> getCacheAssetsVideoFilenames(JSONObject cacheAssets) {
        try {
            return TestUtils.toStringList(cacheAssets.getJSONArray("videos"));
        } catch (JSONException ex) {
            throw new Error(ex);
        }
    }

    /*
        Verify normal operation of FileCache.isNativeImageFileExists:
         - returns true if the filename exists within the images directory.
     */
    @Test
    public void testIsNativeImageFileExists() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();
            String filename = "test.png";
            assertFalse(fc.isNativeImageFileExists(filename));

            String resourcePath = "com/chartboost/cdn/a.chartboost.com/static-assets/interstitials-v2/close-buttons/60x60.png";

            File testFile = h.tc.getCacheFile(".chartboost/images/" + filename);
            writeResourceToFile(resourcePath, testFile);

            assertTrue(fc.isNativeImageFileExists(filename));

            assertTrue(testFile.delete());

            assertFalse(fc.isNativeImageFileExists(filename));
        }
    }

    /*
        Verify that FileCache.isNativeImageFileExists(null) returns false
     */
    @Test
    public void testIsNativeImageFileExists_BadParameters() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            assertFalse(fc.isNativeImageFileExists(null));
        }
    }

    @Test
    public void testBasePathDirVsPathDir() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            File expectedInternal = h.internalBaseDir;

            assertThat(fc.currentLocations().baseDir, is(equalTo(expectedInternal)));

            assertThat(fc.currentLocations().baseDir, is(equalTo(expectedInternal)));

            assertThat(fc.currentLocations().baseDir, is(equalTo(expectedInternal)));
        }
    }

    /*
        Verify normal operation of FileCache.getFolderSize:
            - returns sum of file sizes of all files in directory and subdirectory.
     */
    @Test
    public void testGetFolderSize() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            String resourcePath = "com/chartboost/cdn/a.chartboost.com/static-assets/interstitials-v2/close-buttons/60x60.png";

            File testFile = h.tc.getCacheFile("test.png");
            writeResourceToFile(resourcePath, testFile);

            File d1 = h.tc.getCacheFile("d1");
            assertTrue(d1.mkdir());

            File d2 = new File(d1, "d2");
            assertTrue(d2.mkdir());


            writeResourceToFile(resourcePath, testFile);
            writeResourceToFile(resourcePath, new File(d1, "a.png"));
            writeResourceToFile(resourcePath, new File(d2, "b.png"));

            Long resourceSize = 5001L;

            assertThat(fc.getFolderSize(d2), is(resourceSize));
            assertThat(fc.getFolderSize(d1), is(resourceSize * 2));
            assertThat(fc.getFolderSize(h.tc.cacheDir), is(resourceSize * 3));
        }
    }

    /*
        Verify normal operation of FileCache.getFolderInfo:
         - returns JSON object
         - includes sizes for internal and external folders
         - includes sizes for top-level files and directories within the active (internal or external)
         - (can't yet test external folder stuff)
     */
    @Test
    public void testGetFolderInfo() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            String resourcePath = "com/chartboost/cdn/a.chartboost.com/static-assets/interstitials-v2/close-buttons/60x60.png";

            File testFile = h.tc.getCacheFile(".chartboost/test.png");

            File d1 = h.tc.getCacheFile(".chartboost/d1");
            assertTrue(d1.mkdir());

            File d2 = new File(d1, "d2");
            assertTrue(d2.mkdir());

            writeResourceToFile(resourcePath, testFile);
            writeResourceToFile(resourcePath, new File(d1, "a.png"));
            writeResourceToFile(resourcePath, new File(d2, "b.png"));
            writeResourceToFile(resourcePath, h.tc.getCacheFile(".chartboost/external-test.png"));

            JSONObject w = fc.getFolderInfo();

            Long resourceSize = 5001L;
            assertThat((Long) w.opt(".chartboost-internal-folder-size"), is(resourceSize * 4));
            assertThat((Long) w.optJSONObject("test.png").opt("test.png-size"), is(resourceSize));
            assertThat((Long) w.optJSONObject("d1").opt("d1-size"), is(resourceSize * 2));
        }
    }

    /*
        Verify normal operation of FileCache.getFolderInfo:
         - returns JSON object
         - includes sizes for internal and external folders
         - includes sizes for top-level files and directories within the active (internal or external)
         - (can't yet test external folder stuff)
     */
    @Test
    public void testGetFolderInfo_ExternalStorageAvailable() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            String resourcePath = "com/chartboost/cdn/a.chartboost.com/static-assets/interstitials-v2/close-buttons/60x60.png";

            File testFile = h.tc.getCacheFile(".chartboost/test.png");

            File d1 = h.tc.getCacheFile(".chartboost/d1");
            assertTrue(d1.mkdir());

            File d2 = new File(d1, "d2");
            assertTrue(d2.mkdir());

            writeResourceToFile(resourcePath, testFile);
            writeResourceToFile(resourcePath, new File(d1, "a.png"));
            writeResourceToFile(resourcePath, new File(d2, "b.png"));
            writeResourceToFile(resourcePath, h.tc.getCacheFile(".chartboost/external-test.png"));

            JSONObject w = fc.getFolderInfo();

            Long resourceSize = 5001L;
            assertThat((Long) w.opt(".chartboost-internal-folder-size"), is(resourceSize * 4));
            assertThat((Long) w.optJSONObject("test.png").opt("test.png-size"), is(resourceSize));
            assertThat((Long) w.optJSONObject("d1").opt("d1-size"), is(resourceSize * 2));
        }
    }

    @Test
    public void testTouch() {
        try (TestHarness h = new TestHarness()) {
            FileCache fc = h.createFileCache();

            long initialDate = System.currentTimeMillis() - 1000; // lastModified gives precision of a second, setting start time to 1 second before present
            File testFile = h.tc.getCacheFile("fileToTouch");
            fc.touch(testFile);
            long afterTouchDate = testFile.lastModified();
            long finalDate = System.currentTimeMillis();
            String message = "Touch failed to update timestamp. Value should be between " + initialDate + " and " + finalDate + ", but was " + afterTouchDate;
            assertThat(message, afterTouchDate, is(greaterThan(initialDate)));
            assertThat(message, afterTouchDate, is(lessThanOrEqualTo(finalDate)));
        }
    }

    public static ArrayList<String> deserializeArrayListOfString(File inputFile) {
        try (FileInputStream fis = new FileInputStream(inputFile);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            //noinspection unchecked
            return (ArrayList<String>) ois.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw new Error(ex);
        }
    }

    /*
        initialize should delete from the internal cache:
          - files in subdirectories of templates/
          - empty subdirectories of templates
          - templates/
          - .adId
     */
    @Test
    public void initialize_DeletesObsoleteFiles_Internal_TTL_Days_0() throws InterruptedException {
        try (TestHarness h = TestHarness.defaultWebView()) {
            SdkConfiguration sdk = new SdkConfigurationBuilder().withWebViewCacheTTLDays(0).build();
            h.sdkConfig.getAndSet(sdk);
            h.createFileCache();

            File baseDir = h.internalBaseDir;
            File templatesDir = new File(baseDir, "templates");

            File adId = new File(baseDir, ".adId");
            TestUtils.writeStringToFile(adId, "sadkjfhadld");

            File t1 = new File(baseDir, "templates/3478562895672");
            File t2 = new File(baseDir, "templates/34895274852434325");
            File t3 = new File(baseDir, "templates/43895252");
            File f1a = new File(t1, "23459874298572");
            File f1b = new File(t1, "2348756238756247895.mp4");
            File f2a = new File(t2, "34789654297836578245");
            File f2b = new File(t2, "23452857409_2.png");

            File[] dirs = {t1, t2, t3};
            File[] files = {f1a, f1b, f2a, f2b};

            for (File dir : dirs) {
                assertTrue(dir.mkdir());
            }
            for (File file : files) {
                TestUtils.writeStringToFile(file, "sakfhaslfhlkajhklg");
            }

            Thread.sleep(100);
            h.createFileCache();

            assertFalse(adId.exists());
            for (File dir : dirs) {
                assertFalse(dir.exists());
            }

            for (File file : files) {
                assertFalse(file.exists());
            }
            assertTrue(templatesDir.exists());
        }
    }

    @Test
    public void initialize_DeletesOnlyOldFiles_External() {
        try (TestHarness h = TestHarness.defaultWebView()) {
            h.createFileCache();

            File baseDir = h.internalBaseDir;
            File templatesDir = new File(baseDir, "templates");

            File oldAdId = new File(baseDir, ".adId");

            long oldTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8);

            File recentTemplate1 = new File(baseDir, "templates/3478562895672");
            File recentTemplate2 = new File(baseDir, "templates/44895274852434325");
            File oldTemplate3 = new File(baseDir, "templates/53895252");
            File recentFile1a = new File(recentTemplate1, "63459874298572");
            File recentFile1b = new File(recentTemplate1, "7348756238756247895.mp4");
            File recentFile2a = new File(recentTemplate2, "84789654297836578245");
            File oldFile3a = new File(oldTemplate3, "93452857409_2.png");

            File[] recentDirs = {recentTemplate1, recentTemplate2};
            File[] recentFiles = {recentFile1a, recentFile1b, recentFile2a};
            File[] oldDirs = {oldTemplate3};
            File[] oldFiles = {oldFile3a, oldAdId};

            for (File dir : recentDirs) {
                assertTrue(dir.mkdir());
            }
            for (File dir : oldDirs) {
                assertTrue(dir.mkdir());
            }
            for (File file : recentFiles) {
                TestUtils.writeStringToFile(file, "sakfhaslfhlkajhklg");
            }
            for (File file : oldFiles) {
                TestUtils.writeStringToFile(file, "sakfhaslfhlkajhklg");
                assertTrue(file.setLastModified(oldTimestamp));
            }

            h.createFileCache();

            assertFalse(oldAdId.exists());
            for (File dir : recentDirs) {
                assertTrue("Should exist: " + dir.getPath(), dir.exists());
            }
            for (File file : recentFiles) {
                assertTrue(file.exists());
            }
            for (File dir : oldDirs) {
                assertFalse(dir.exists());
            }
            for (File file : oldFiles) {
                assertFalse(file.exists());
            }
            assertTrue(templatesDir.exists());
        }
    }

    static class TestHarness implements AutoCloseable {
        final AndroidTestContainer tc;
        final AtomicReference<SdkConfiguration> sdkConfig;

        final File internalBaseDir;

        TestHarness() {
            this(new TestContainerControl());
        }

        public static TestHarness defaultNative() {
            return new TestHarness(TestContainerControl.defaultNative());
        }

        public static TestHarness defaultWebView() {
            return new TestHarness(TestContainerControl.defaultWebView());
        }

        public TestHarness(TestContainerControl control) {
            tc = new AndroidTestContainerBuilder().withSpyOnAndroid().build();
            sdkConfig = new AtomicReference<>(control.configure().build());
            CBConfig.validatePermissions(tc.activity);

            internalBaseDir = tc.getCacheFile(".chartboost");
        }

        @Override
        public void close() {
            tc.close();
        }

        FileCache createFileCache() {
            return new FileCache(tc.applicationContext, sdkConfig);
        }
    }

    /**
     * Writes a byte array to a file creating the file if it does not exist.
     *
     * @param file   the file to write to
     * @param data   the content to write to the file
     * @param append if {@code true}, then bytes will be added to the
     *               end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.1
     */
    public static void writeByteArrayToFile(final File file, final byte[] data, final boolean append)
            throws IOException {
        writeByteArrayToFile(file, data, 0, data.length, append);
    }

    public static void writeByteArrayToFile(final File file, final byte[] data, final int off, final int len,
                                            final boolean append) throws IOException {
        try (OutputStream out = openOutputStream(file, append)) {
            out.write(data, off, len);
        }
    }
}
