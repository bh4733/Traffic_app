package com.example.trafficapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrafficRepository {
    private static final String TAG = "TrafficRepository";

    private static final String UNPLANNED_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";

    private static final String ROADWORKS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    private static final String UNPLANNED_CACHE = "unplanned.xml";
    private static final String ROADWORKS_CACHE = "roadworks.xml";

    private final Context context;
    private final parser xmlParser = new parser();
    private String clientId = "";
    private String clientKey = "";
    private boolean offline;

    public TrafficRepository(Context context) {
        this.context = context.getApplicationContext();
        readKeys();
    }

    private void readKeys() {
        try {
            Bundle metaData = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
                    .metaData;
            clientId = metaData.getString("com.example.trafficapp.CLIENT_ID", "");
            clientKey = metaData.getString("com.example.trafficapp.CLIENT_KEY", "");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not read API keys", e);
        }
    }

    public List<Situation> fetchAll() {
        offline = false;
        List<Situation> all = new ArrayList<>();
        all.addAll(fetchFeed(UNPLANNED_URL, UNPLANNED_CACHE, Situation.INCIDENT));
        if (ROADWORKS_URL.startsWith("https")) {
            all.addAll(fetchFeed(ROADWORKS_URL, ROADWORKS_CACHE, Situation.ROADWORKS));
        } else {
            Log.w(TAG, "Roadworks URL not set yet - skipping roadworks feed");
        }
        return all;
    }

    public String getStatus() {
        if (offline) return "No connection - showing last saved data";
        String time = new SimpleDateFormat("HH:mm", Locale.UK).format(new Date());
        return "Updated at " + time;
    }

    private List<Situation> fetchFeed(String url, String cacheFile, String category) {
        String xml;
        try {
            xml = download(url);
            save(cacheFile, xml);
        } catch (IOException e) {
            Log.w(TAG, category + " download failed (" + e.getMessage() + "), using cache");
            offline = true;
            xml = load(cacheFile);
        }
        if (xml == null) return new ArrayList<>();
        return xmlParser.parse(xml, category);
    }

    private String download(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Authorization", Base64.getEncoder()
                .encodeToString((clientId + ":" + clientKey).getBytes()));
        try {
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code);

            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) sb.append(line);
            }
            String result = sb.toString();
            int i = result.indexOf("<?");          // clean up leading garbage (as in skeleton)
            if (i < 0) throw new IOException("Response is not XML");
            return result.substring(i);
        } finally {
            conn.disconnect();
        }
    }

    private void save(String name, String data) {
        try (FileOutputStream out = context.openFileOutput(name, Context.MODE_PRIVATE)) {
            out.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Could not save cache " + name, e);
        }
    }

    private String load(String name) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                context.openFileInput(name), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (IOException e) {
            return null;                           // nothing cached yet
        }
    }
}
