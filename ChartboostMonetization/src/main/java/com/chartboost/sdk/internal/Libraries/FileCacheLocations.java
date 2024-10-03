package com.chartboost.sdk.internal.Libraries;

import java.io.File;

public class FileCacheLocations {
    // Directory locations
    public final File baseDir;

    public final File cssDir;
    public final File htmlDir;
    public final File imagesDir;
    public final File jsDir;
    public final File templatesDir;
    public final File videosDir;
    public final File precacheDir;
    public final File precacheQueueDir;

    public FileCacheLocations(File root) {
        baseDir = new File(root, CBConstants.CACHE_DIR_CHARTBOOST_BASE);
        if (!baseDir.exists())
            baseDir.mkdirs();

        cssDir = createDirectory(baseDir, FileCache.CBDirectoryType.StyleSheets);
        htmlDir = createDirectory(baseDir, FileCache.CBDirectoryType.Html);
        imagesDir = createDirectory(baseDir, FileCache.CBDirectoryType.Images);
        jsDir = createDirectory(baseDir, FileCache.CBDirectoryType.Javascript);
        templatesDir = createDirectory(baseDir, FileCache.CBDirectoryType.TemplateMetaData);
        videosDir = createDirectory(baseDir, FileCache.CBDirectoryType.Videos);
        precacheDir = createDirectory(baseDir, FileCache.CBDirectoryType.Precache);
        precacheQueueDir = createDirectory(baseDir, FileCache.CBDirectoryType.PrecacheQueue);
    }

    public File getBaseDir() {
        return baseDir;
    }

    private static File createDirectory(File directory, @FileCache.CBDirectoryType String filename) {
        File f = new File(directory, filename);
        if (!f.exists()) {
            f.mkdir();
        }
        return f;
    }
}
