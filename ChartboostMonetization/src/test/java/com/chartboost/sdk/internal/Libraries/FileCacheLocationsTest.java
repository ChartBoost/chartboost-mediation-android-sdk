package com.chartboost.sdk.internal.Libraries;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.test.TempDirectory;

import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileCacheLocationsTest extends BaseTest {
    @Test
    public void testInitialization() {
        try (TempDirectory tempDir = new TempDirectory()) {
            assertThat(tempDir.directory.list(), is(new String[0]));

            final FileCacheLocations fcl = new FileCacheLocations(tempDir.directory);

            Map<String, File> expectedDirs = new HashMap<String, File>() {{
                put("css", fcl.cssDir);
                put("html", fcl.htmlDir);
                put("images", fcl.imagesDir);
                put("js", fcl.jsDir);
                put("templates", fcl.templatesDir);
                put("precache", fcl.precacheDir);
                put("videos", fcl.videosDir);
                put("precache", fcl.precacheDir);
                put("precache_queue", fcl.precacheQueueDir);
            }};

            // only created one top-level directory
            assertThat(tempDir.directory.list().length, is(1));

            // The top-level directory it created is ".chartboost"
            assertThat(fcl.baseDir, is(new File(tempDir.directory, ".chartboost")));

            // created exactly as many subdirectories as we are expecting
            assertThat(fcl.baseDir.list().length, is(equalTo(expectedDirs.size())));

            // each directory we expected was created and is empty
            for (Map.Entry<String, File> entry : expectedDirs.entrySet()) {
                String name = entry.getKey();
                File file = entry.getValue();

                assertThat(file, is(new File(fcl.baseDir, name)));
                assertTrue(file.exists());
                assertTrue(file.isDirectory());
                assertThat(file.list(), is(new String[0]));
            }
        }
    }
}
