package com.chartboost.sdk.internal.Model;

import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;
import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.internal.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Asset {
    public final String directory;
    public final String filename;
    public final String url;

    public Asset(String directory, String filename, String url) {
        this.directory = directory;
        this.filename = filename;
        this.url = url;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    private static Map<String, Asset> deserializeAssets(JSONObject assetsJson) throws JSONException {
        Map<String, Asset> assets = new HashMap<>();
        if (assetsJson == null) {
            Logger.d("deserializeAssets assetsJson is null", null);
            return assets;
        }

        for (Iterator<String> directories = assetsJson.keys(); directories.hasNext(); ) {
            String dirname = directories.next();
            JSONObject dir = assetsJson.getJSONObject(dirname);
            for (Iterator<String> paramNames = dir.keys(); paramNames.hasNext(); ) {
                String paramName = paramNames.next();
                JSONObject assetJson = dir.getJSONObject(paramName);
                String filename = assetJson.getString("filename");
                String url = assetJson.getString("url");
                assets.put(paramName, new Asset(dirname, filename, url));
            }
        }
        return assets;
    }

    public static Map<String, Asset> deserializeNativeVideos(JSONObject response) {
        Map<String, Asset> assets = new HashMap<>();
        try {
            JSONArray videos = response.getJSONArray("videos");
            int length = videos.length();
            for (int i = 0; i < length; ++i) {
                try {
                    JSONObject video = videos.getJSONObject(i);
                    String filename = video.getString("id");
                    String uri = video.getString("video");

                    assets.put(filename, new Asset("videos", filename, uri));
                } catch (JSONException e) {
                    Logger.e("deserializeNativeVideos (file): " + e, null);
                }
            }
        } catch (JSONException e) {
            Logger.e("deserializeNativeVideos (videos array): " + e, null);
        }
        return assets;
    }

    private static JSONObject v2TemplateElementsToV3TemplateAssets(JSONArray elements) throws JSONException {
        JSONObject assets = jsonObject();
        if (elements == null) {
            return assets;
        }

        for (int j = 0; j < elements.length(); j++) {
            JSONObject element = elements.getJSONObject(j);
            String name = element.optString("name");
            String type = element.optString("type");
            String value = element.optString("value");
            String param = element.optString("param");

            if (!"param".equals(type) && param.isEmpty()) {
                JSONObject assetDirectory = assets.optJSONObject(type);
                if (assetDirectory == null) {
                    assetDirectory = jsonObject();
                    assets.put(type, assetDirectory);
                }
                assetDirectory.put("html".equals(type) ? "body" : name, jsonObject(
                        JKV("filename", name),
                        JKV("url", value)));
            }
        }
        return assets;
    }

    public static Map<String, Asset> v2PrefetchToAssets(JSONObject response, int maxTemplates) {
        Map<String, Asset> assets = new HashMap<>();
        if (response == null) {
            return assets;
        }

        try {
            JSONObject cacheAssets = response.getJSONObject("cache_assets");
            for (Iterator<String> keys = cacheAssets.keys(); keys.hasNext(); ) {
                String type = keys.next();
                if ("templates".equals(type)) {
                    assets.putAll(parseTemplateAssetsToAssetsMap(cacheAssets, maxTemplates));
                } else {
                    assets.putAll(parseCachedJsonToAssetsMap(cacheAssets, type));
                }
            }
        } catch (JSONException e) {
            Logger.e("v2PrefetchToAssets: " + e, null);
        }
        return assets;
    }

    private static Map<String, Asset> parseTemplateAssetsToAssetsMap(JSONObject cacheAssets, int maxTemplates) throws JSONException {
        Map<String, Asset> assets = new HashMap<>();
        if (cacheAssets == null) {
            return assets;
        }

        JSONArray v2Templates = cacheAssets.optJSONArray("templates");
        if (v2Templates != null) {
            int nTemplates = Math.min(maxTemplates, v2Templates.length());
            for (int i = 0; i < nTemplates; i++) {
                JSONObject v2Template = v2Templates.getJSONObject(i);
                JSONObject templateAssetsJson = null;
                if (v2Template != null) {
                    templateAssetsJson = v2TemplateElementsToV3TemplateAssets(v2Template.getJSONArray("elements"));
                }

                Map<String, Asset> templateAssets = deserializeAssets(templateAssetsJson);
                for (Map.Entry<String, Asset> entry : templateAssets.entrySet()) {
                    Asset asset = entry.getValue();
                    // Each template will have an entry with key=="body",
                    // so use the filename since that will be unique.
                    assets.put(asset.filename, asset);
                }
            }
        }
        return assets;
    }

    private static Map<String, Asset> parseCachedJsonToAssetsMap(JSONObject cacheAssets, String type) throws JSONException {
        Map<String, Asset> assets = new HashMap<>();
        if (cacheAssets == null || type == null) {
            return assets;
        }

        JSONArray entries = cacheAssets.getJSONArray(type);
        int entriesLength = entries.length();
        for (int i = 0; i < entriesLength; i++) {
            JSONObject entry = entries.getJSONObject(i);
            String filename = entry.getString("name");
            String url = entry.getString("value");
            assets.put(filename, new Asset(type, filename, url));
        }
        return assets;
    }

    public File getFile(File baseDir) {
        File file = null;
        if (directory != null && filename != null) {
            String relativePath = directory + "/" + filename;
            try {
                file = new File(baseDir, relativePath);
            } catch (Exception e) {
                Logger.d("Cannot create file for path: " + relativePath + ". Error: " + e, null);
            }
        } else {
            Logger.d("Cannot create file. Directory or filename is null.", null);
        }
        return file;
    }

    @NonNull
    @Override
    public String toString() {
        return "Asset{" +
                "directory='" + directory + '\'' +
                ", filename='" + filename + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
