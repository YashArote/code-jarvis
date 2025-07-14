package com.example.code_jarvis.model;

import android.os.Handler;
import android.os.Looper;
import okhttp3.*;
import org.json.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitHubApiHelper {

    private static final String BASE_URL = "https://api.github.com";
    private OkHttpClient client = new OkHttpClient();

    public interface RepoCallback {
        void onSuccess(List<Repo> repos);

        void onError(Exception e);
    }

    public void fetchRepos(String accessToken, RepoCallback callback) {
        Request userRequest = new Request.Builder()
                .url(BASE_URL + "/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .build();

        client.newCall(userRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject userJson = new JSONObject(response.body().string());
                        String username = userJson.getString("login");
                        fetchUserRepos(username, accessToken, callback);
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                } else {
                    callback.onError(new Exception("User fetch failed"));
                }
            }
        });
    }

    private void fetchUserRepos(String username, String accessToken, RepoCallback callback) {
        Request reposRequest = new Request.Builder()
                .url(BASE_URL + "/users/" + username + "/repos")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .build();

        client.newCall(reposRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray reposArray = new JSONArray(response.body().string());
                        List<Repo> repoList = new ArrayList<>();
                        for (int i = 0; i < reposArray.length(); i++) {
                            JSONObject repoObj = reposArray.getJSONObject(i);
                            String name = repoObj.getString("name");
                            String cloneUrl = repoObj.getString("clone_url");
                            repoList.add(new Repo(name, cloneUrl));
                        }
                        // Ensure callback runs on main thread
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(repoList));
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(new Exception("Repo fetch failed")));
                }
            }
        });
    }
}
