package com.chartboost.sdk.internal.AssetLoader;

//public class AssetLoaderHelper {
//
//    /*
//        Template parameter replacement, independent from any production code,
//        since we'll use this method to test that production code.
//     */
//    public static String replaceTemplateParameters(AssetDescriptor htmlDescriptor,
//                                                   ResponseDescriptor webviewGetResponse) {
//
//        Map<String, String> parameters = getReplacementParameters(webviewGetResponse);
//
//        String html = new String(htmlDescriptor.copyContents());
//        for (Map.Entry<String, String> entry : parameters.entrySet()) {
//            html = html.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
//        }
//        return html;
//    }
//
//    @NonNull
//    public static Map<String, String> getReplacementParameters(ResponseDescriptor webviewGetResponse) {
//        Map<String, String> parameters = new HashMap<>();
//        JSONObject getResponse = webviewGetResponse.asJSONObject();
//
//        try {
//            JSONArray elements = getResponse.getJSONObject("webview").getJSONArray("elements");
//            for (int i = 0; i < elements.length(); i++) {
//                JSONObject element = elements.getJSONObject(i);
//                String param = element.optString("param");
//                if (!param.isEmpty()) {
//                    String type = element.getString("type");
//                    if (type.equals("param")) {
//                        parameters.put(param, element.getString("value"));
//
//                    } else {
//                        parameters.put(param, element.getString("name"));
//                    }
//                }
//
//            }
//        } catch (JSONException ex) {
//            throw new Error(ex);
//        }
//        return parameters;
//    }
//}
