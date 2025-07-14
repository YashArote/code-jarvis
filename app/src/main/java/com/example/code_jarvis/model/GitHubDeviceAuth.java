package com.example.code_jarvis.model;
import android.content.Context;

import android.os.Handler;
import android.os.Looper;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

public class GitHubDeviceAuth {

    private static final String TAG = "GitHubDeviceAuth";
    private static final String CLIENT_ID = "Ov23liQhbVysrvNT1Fav"; // TODO: Replace with your GitHub OAuth App client_id
    private static final String SCOPE = "repo user"; // Customize as needed
    public static String deviceCode;
    public static int interval;
    public static boolean shouldResumePolling=false;

    public interface AuthCallback {
        void onAuthSuccess(String accessToken);
        void onAuthError(String error);
        void onUserCodeReady(String userCode, String verificationUri);
    }

    public static void authenticate(Context context, AuthCallback callback) {
        new Thread(() -> {
            try {
                // Step 1: Request device and user code
                URL url = new URL("https://github.com/login/device/code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Accept", "application/json");

                conn.setDoOutput(true);

                String params = "client_id=" + CLIENT_ID + "&scope=" + SCOPE;
                conn.getOutputStream().write(params.getBytes());

                String response = readResponse(conn);
                JSONObject json = new JSONObject(response);

                deviceCode = json.getString("device_code");
                String userCode = json.getString("user_code");
                String verificationUri = json.getString("verification_uri");
                interval = json.getInt("interval");

                // Notify UI to show userCode and verificationUri
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onUserCodeReady(userCode, verificationUri)
                );

                // Step 2: Poll for token
                resumePolling(context, callback, deviceCode, interval);

            }catch (UnknownHostException e){
                e.printStackTrace();
                shouldResumePolling = true;
            } catch(Exception e) {

                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onAuthError("Exception: " + e.getMessage())
                );
            }
        }).start();
    }

    public static void resumePolling(Context context, AuthCallback callback, String deviceCode, int interval) throws InterruptedException, IOException, JSONException {
        boolean tokenReceived = false;
        while (!tokenReceived) {

            Thread.sleep(interval * 1000L);

            URL tokenUrl = new URL("https://github.com/login/oauth/access_token");
            HttpURLConnection tokenConn = (HttpURLConnection) tokenUrl.openConnection();
            tokenConn.setRequestMethod("POST");

            tokenConn.setRequestProperty("Accept", "application/json");
            tokenConn.setDoOutput(true);

            String body = "client_id=" + CLIENT_ID +
                    "&device_code=" + deviceCode +
                    "&grant_type=urn:ietf:params:oauth:grant-type:device_code";
            tokenConn.getOutputStream().write(body.getBytes());

            String tokenResponse = readResponse(tokenConn);
            JSONObject tokenJson = new JSONObject(tokenResponse);

            if (tokenJson.has("access_token")) {
                String accessToken = tokenJson.getString("access_token");

                // Step 3: Validate access token
                if (isTokenValid(accessToken)) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onAuthSuccess(accessToken)
                    );
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onAuthError("Token validation failed")
                    );
                }

                tokenReceived = true;
            } else if (tokenJson.has("error")) {
                String error = tokenJson.getString("error");
                if (error.equals("authorization_pending")) {
                    // continue polling
                } else if (error.equals("slow_down")) {
                    interval += 5;
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onAuthError("Error: " + error)
                    );
                    break;
                }
            }
        }
    }

    private static boolean isTokenValid(String accessToken) {
        try {
            URL url = new URL("https://api.github.com/user");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("User-Agent", "MyAndroidApp");

            int responseCode = conn.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    private static String readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            response.append(line);
        reader.close();
        return response.toString();
    }
}
