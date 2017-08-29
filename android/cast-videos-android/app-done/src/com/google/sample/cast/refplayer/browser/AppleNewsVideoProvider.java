package com.google.sample.cast.refplayer.browser;

import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by warenix on 8/28/17.
 */

public class AppleNewsVideoProvider {

    private static final String TAG = AppleNewsVideoProvider.class.getSimpleName();

    public static JSONArray parse(String urlString)
    {
        InputStream is = null;
        try {
            java.net.URL url = new java.net.URL(urlString);
            URLConnection urlConnection = url.openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), "utf-8"), 1024);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String html = sb.toString();
            Log.d(TAG, html);

            Pattern regex = Pattern.compile("videoPlaylistOriginal =(.+?]);", Pattern.DOTALL);
            Matcher matcher = regex.matcher(html);
            if (matcher.find()) {
                Log.d(TAG, matcher.groupCount() + " videos found");
                Log.d(TAG, matcher.group(1));
                String jsonString = matcher.group(1);

                JSONArray json = new JSONArray(jsonString);
                return json;
            }

            // regex to extract html
            return null;

        } catch (Exception e) {
            Log.d(TAG, "Failed to parse the json for media list", e);
            return null;
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
