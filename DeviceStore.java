package com.seuprojeto.nexus;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DeviceStore {
    private static final String PREFS = "nexus_devices";
    private static final String KEY = "device_list";

    public static class HttpDevice {
        public String name;
        public String onUrl;
        public String offUrl;

        public HttpDevice(String name, String onUrl, String offUrl) {
            this.name = name;
            this.onUrl = onUrl;
            this.offUrl = offUrl;
        }
    }

    public static List<HttpDevice> load(Context context) {
        List<HttpDevice> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new HttpDevice(o.optString("name"), o.optString("onUrl"), o.optString("offUrl")));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void save(Context context, List<HttpDevice> devices) {
        JSONArray arr = new JSONArray();
        try {
            for (HttpDevice d : devices) {
                JSONObject o = new JSONObject();
                o.put("name", d.name);
                o.put("onUrl", d.onUrl);
                o.put("offUrl", d.offUrl);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY, arr.toString()).apply();
    }

    public static void sendCommand(String urlStr, Runnable onDone, Runnable onError) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) onDone.run(); else onError.run();
            } catch (Exception e) {
                onError.run();
            }
        }).start();
    }
}
