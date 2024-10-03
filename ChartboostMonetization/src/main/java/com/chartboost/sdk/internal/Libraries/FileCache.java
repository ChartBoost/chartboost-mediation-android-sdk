package com.chartboost.sdk.internal.Libraries;

import android.content.Context;
import androidx.annotation.StringDef;

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit;
import com.chartboost.sdk.internal.Model.Asset;
import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.internal.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FileCache {
    private final AtomicReference<SdkConfiguration> sdkConfig;

    // File locations on internal storage
    private final FileCacheLocations internal;

    @StringDef({
            CBDirectoryType.TemplateMetaData,
            CBDirectoryType.Videos,
            CBDirectoryType.Precache,
            CBDirectoryType.Images,
            CBDirectoryType.StyleSheets,
            CBDirectoryType.Javascript,
            CBDirectoryType.Html,
            CBDirectoryType.VideoCompletion,
            CBDirectoryType.Session,
            CBDirectoryType.Track,
            CBDirectoryType.RequestManager,
            CBDirectoryType.PrecacheQueue
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CBDirectoryType {
        String TemplateMetaData = "templates";
        String Videos = "videos";
        String Precache = "precache";
        String Images = "images";
        String StyleSheets = "css";
        String Javascript = "js";
        String Html = "html";
        String VideoCompletion = "videoCompletionEvents";
        String Session = "session";
        String Track = "track";
        String RequestManager = "requests";
        String PrecacheQueue = "precache_queue";
    }

    /**
     * Public constructor for creating CBFileCache instance
     */
    public FileCache(Context context, AtomicReference<SdkConfiguration> sdkConfig) {
        internal = new FileCacheLocations(context.getCacheDir());
        this.sdkConfig = sdkConfig;

        // clean up the "templates" metadata directory.  These are from the
        // old AssetPrefetcher.  They contain a subdirectory per template,
        // and each subdirectory contains files with names corresponding
        // to asset filenames.  They have been replaced with a single
        // file called (template id).json in the templates/ subdirectory.
        try {
            long expireAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(sdkConfig.get().webviewCacheTTLDays);
            File templatesDir = new File(internal.baseDir, "templates");
            if (templatesDir.exists()) {
                File[] templatesSubDirs = templatesDir.listFiles();
                cleanTemplateSubdirectories(templatesSubDirs, expireAt);
                deleteBlacklistedFiles(internal);
            }
        } catch (Exception e) {
            Logger.e("Exception while cleaning up templates directory at " + internal.templatesDir.getPath(), e);
            e.printStackTrace();
        }
    }

    public File[] getPrecacheFiles() {
        File dir = getPrecacheDir();
        if (dir != null) {
            return dir.listFiles();
        }
        return null;
    }

    public boolean deleteFile(File file) {
        if (file != null && file.exists()) {
            return file.delete();
        }
        return false;
    }

    public File getFileIfCached(File destDir, String filename) {
        final File cachedFile;
        if (destDir != null && filename != null) {
            cachedFile = new File(destDir, filename);
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return cachedFile;
            }
        }
        return null;
    }

    public boolean isFileCached(File file) {
        if (file != null) {
            return file.exists() && file.length() > 0;
        }
        return false;
    }

    private void cleanTemplateSubdirectories(File[] templatesSubDirs, long expireAt) {
        if (templatesSubDirs != null) {
            for (File subdirectory : templatesSubDirs) {
                if (subdirectory.isDirectory()) {
                    File[] metadataFiles = subdirectory.listFiles();
                    deleteMetadataFiles(metadataFiles, expireAt);
                    File[] filesAfterDelete = subdirectory.listFiles();
                    deleteSubdirectoryAfterInitialMetadataDelete(filesAfterDelete, subdirectory);
                }
            }
        }
    }

    private void deleteMetadataFiles(File[] metadataFiles, long expireAt) {
        if (metadataFiles != null) {
            for (File file : metadataFiles) {
                if (file.lastModified() < expireAt) {
                    if (!file.delete())
                        Logger.e("Unable to delete " + file.getPath(), null);
                }
            }
        }
    }

    private void deleteSubdirectoryAfterInitialMetadataDelete(File[] filesAfterDelete, File subdirectory) {
        if (filesAfterDelete != null && filesAfterDelete.length == 0 && !subdirectory.delete()) {
            Logger.e("Unable to delete " + subdirectory.getPath(), null);
        }
    }

    private void deleteBlacklistedFiles(FileCacheLocations fileLocations) {
        File blacklistAdIdFile = new File(fileLocations.baseDir, ".adId");
        if (blacklistAdIdFile.exists() && !blacklistAdIdFile.delete()) {
            Logger.e("Unable to delete " + blacklistAdIdFile.getPath(), null);
        }
    }

    /**
     * Retrieves the byte array data from the give file path thats stored in the specified directory in sand box
     */
    synchronized byte[] readByteArrayFromDisk(File file) {
        if (file == null)
            return null;

        byte[] contents = null;
        try {
            contents = CommonsIO.INSTANCE.readFileToByteArray(file);
        } catch (Exception e) {
            Logger.e("Error loading cache from disk", e);
        }
        return contents;
    }


    public String getVideoPath(String id) {
        File file = new File(currentLocations().videosDir, id);
        if (file.exists()) {
            return file.getPath();
        }
        return null;
    }

    /**
     * Check if file exists in the directory path
     */
    public boolean isNativeImageFileExists(String fileName) {
        if (currentLocations().imagesDir == null || fileName == null)
            return false;
        File file = new File(currentLocations().imagesDir, fileName);
        return file.exists();
    }

    public JSONArray getNativeVideoList() {
        JSONArray videoList = new JSONArray();
        String[] fNames = currentLocations().videosDir.list();
        if (fNames != null) {
            for (String fName : fNames) {
                if (!fName.equals(".nomedia") && !fName.endsWith(".tmp"))
                    videoList.put(fName);
            }
        }
        return videoList;
    }

    public JSONObject getWebViewCacheAssets() {
        JSONObject cacheAssets = new JSONObject();
        try {
            final File baseDir = currentLocations().baseDir;
            for (String dirName : sdkConfig.get().webviewDirectories) {
                if (!dirName.equals("templates")) {
                    File path = new File(baseDir, dirName);
                    JSONArray filenames = new JSONArray();
                    if (path.exists()) {
                        String[] fNames = path.list();
                        if (fNames != null) {
                            for (String fName : fNames) {
                                if (!fName.equals(".nomedia") && !fName.endsWith(".tmp"))
                                    filenames.put(fName);
                            }
                        }
                    }
                    CBJSON.put(cacheAssets, dirName, filenames);
                }
            }
        } catch (Exception ex) {
            Logger.e("getWebViewCacheAssets: " + ex, null);
        }

        return cacheAssets;
    }

    public FileCacheLocations currentLocations() {
        return internal;
    }

    public File getPrecacheDir() {
        return internal.precacheDir;
    }

    public File getPrecacheQueueDir() {
        return internal.precacheQueueDir;
    }

    public long getFolderSize(File dir) {
        long size = 0;
        try {
            if (dir != null && dir.isDirectory()) {
                File[] list = dir.listFiles();
                if (list != null) {
                    for (File file : list) {
                        size += getFolderSize(file);
                    }
                }
            } else if (dir != null) {
                size = dir.length();
            }
        } catch (Exception e) {
            Logger.e("getFolderSize: " + e, null);
        }
        return size;
    }

    public JSONObject getFolderInfo() {
        JSONObject meta = new JSONObject();
        CBJSON.put(meta, CBConstants.FILE_CACHE_ROOT_INTERNAL_SPACE, getFolderSize(internal.baseDir));
        File pathDir = currentLocations().baseDir;
        String[] parentDirList = pathDir.list();

        if (parentDirList != null && parentDirList.length > 0) {
            for (String dirName : parentDirList) {
                File subDir = new File(pathDir, dirName);
                JSONObject dirObj = new JSONObject();
                CBJSON.put(dirObj, subDir.getName() + "-size", getFolderSize(subDir));
                String[] list = subDir.list();
                if (list != null)
                    CBJSON.put(dirObj, "count", list.length);

                CBJSON.put(meta, subDir.getName(), dirObj);
            }
        }
        return meta;
    }

    // This should use setLastModified, but it doesn't work with some devices, hence this hack
    public void touch(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(0);
            int firstByte = raf.read();
            raf.seek(0);
            raf.write(firstByte);
        } catch (FileNotFoundException e) {
            Logger.e("File not found when attempting to touch", e);
        } catch (IOException e) {
            Logger.e("IOException when attempting to touch file", e);
        }
    }

    public Boolean isAssetsAvailable(AdUnit adUnit) {
        Map<String, Asset> assets = adUnit.getAssets();
        FileCacheLocations cacheLocations = currentLocations();
        if (cacheLocations == null) {
            return false;
        }
        File baseDir = cacheLocations.baseDir;
        Collection<Asset> assetsCollection = assets.values();
        for (Asset asset : assetsCollection) {
            File assetFile = asset.getFile(baseDir);
            if (assetFile == null) {
                return false;
            }

            if (!assetFile.exists()) {
                Logger.e("Asset does not exist: " + asset.filename, null);
                return false;
            }
        }
        return true;
    }
}
