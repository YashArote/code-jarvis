package com.example.code_jarvis;

import com.example.code_jarvis.git.GitOperations;
import com.example.code_jarvis.git.GitUiOperationHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.code_jarvis.adapter.RepoAdapter;
import com.example.code_jarvis.model.GitHubApiHelper;
import com.example.code_jarvis.model.GitHubDeviceAuth;
import com.example.code_jarvis.model.Repo;
import com.google.gson.reflect.TypeToken;


import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConnectActivity extends AppCompatActivity {
    LinearLayout gitHubContainer;
    String accessToken;
    SharedPreferences prefs;
    TabLayout tabLayout;
    LinearLayout blankView;
    FloatingActionButton actionButton;
    ProgressBar progressBar;
    RecyclerView recyclerView;
    GitUiOperationHandler gitUiOperationHandler;
    RepoAdapter repoAdapter;
    RepoAdapter downloadAdapter;
    GitHubDeviceAuth.AuthCallback gitHubDeviceAuth;

    void disconnect(){
        View dialogView = getLayoutInflater().inflate(R.layout.github_logout, null);
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    prefs = getSharedPreferences("github_auth", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor=prefs.edit();
                    editor.putString("access_token", "").apply();
                    View githubStatusDot = findViewById(R.id.github_status_dot);
                    githubStatusDot.setBackgroundResource(R.drawable.dot_red);
                    accessToken=prefs.getString("access_token", "");
                    editor.putString("repo_list", "");
                    editor.apply();
                    getReposFromStorage();
                    actionButton.setVisibility(View.GONE);
                }).setNegativeButton("CANCEL", null)
                .show();
    }
    void showBuilder(String verificationUri,String userCode){
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_gihub_auth, null);
        // Set the verification URI as a clickable link
        TextView tvVerificationUri = dialogView.findViewById(R.id.tv_verification_uri);
        tvVerificationUri.setText(verificationUri);
        tvVerificationUri.setAutoLinkMask(Linkify.WEB_URLS);
        tvVerificationUri.setMovementMethod(LinkMovementMethod.getInstance());

        // Set the user code
        TextView tvUserCode = dialogView.findViewById(R.id.tv_user_code);
        tvUserCode.setText(userCode);

        // Copy button logic
        ImageButton btnCopy = dialogView.findViewById(R.id.btn_copy);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("GitHub User Code", userCode);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show();
        });

        // Build and show the dialog
        new AlertDialog.Builder(ConnectActivity.this)
                .setCancelable(false)
                .setTitle("GitHub Authorization")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }
    void getReposApi(){

        // Show loading, hide RecyclerView
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        GitHubApiHelper apiHelper = new GitHubApiHelper();
        if(!accessToken.isEmpty()){
        apiHelper.fetchRepos(accessToken, new GitHubApiHelper.RepoCallback() {
            @Override
            public void onSuccess(List<Repo> repos) {
                // Hide loading, show RecyclerView
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                // Save repos to SharedPreferences
                SharedPreferences prefs = getSharedPreferences("github_auth", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                Gson gson = new Gson();
                String json = gson.toJson(repos);
                editor.putString("repo_list", json);
                editor.apply();

                // Set up RecyclerView
                recyclerView.setLayoutManager(new LinearLayoutManager(ConnectActivity.this));
                repoAdapter = new RepoAdapter(repos, repo -> {
                    extracted(repo);

                });
                recyclerView.setAdapter(repoAdapter);
            }

            @Override
            public void onError(Exception e) {
                // Hide loading even on error
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                // Handle error (show a message, etc.)
            }
        });}
    }

    private void extracted(Repo repo) {
        File dirToClone=new File(getFilesDir(),"github_repos");
        File[] repoDirs = dirToClone.listFiles(File::isDirectory);

        if (repoDirs != null) {
            for (File dir : repoDirs) {
                Log.d("Repo", "Cloned repo: " + dir.getName());
            }
        } else {
            Log.d("Repo", "No cloned repos found.");
        }
        String base64Url= Base64.encodeToString(repo.getUrl().getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
        File githubDir=new File(dirToClone,base64Url);
        GitOperations gitOperations=new GitOperations(repo.getUrl(),githubDir, accessToken);
        gitUiOperationHandler.runWithUi((callback ->{
            gitOperations.callback=callback;
            gitOperations.cloneRepo();
        }));

    }

    void getReposFromStorage(){
        SharedPreferences prefs = getSharedPreferences("github_auth", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        if(accessToken.isEmpty()){
            List<Repo> repoList= new ArrayList<>();
            repoAdapter.repoList=repoList;
            repoAdapter.notifyDataSetChanged();
            return;
        };
        String savedJson = prefs.getString("repo_list", "");
        if(savedJson.isEmpty()){
            getReposApi();
        }else{
            Type type = new TypeToken<List<Repo>>() {}.getType();
            List<Repo> savedRepos = gson.fromJson(savedJson, type);
            recyclerView.setLayoutManager(new LinearLayoutManager(ConnectActivity.this));
            repoAdapter = new RepoAdapter(savedRepos, repo -> {
                extracted(repo);
            });

            recyclerView.setAdapter(repoAdapter);

        }

    }
    void getDownloadedRepos(){
        File githubRepos=new File(getFilesDir(), "github_repos");
        List<Repo> repos= new ArrayList<>();
        File[] files = githubRepos.listFiles();
        if (files != null && files.length > 0) {

            for (File file : files) {
                if (file.isDirectory() && !file.isHidden()) {
                    String repoUrl=file.getName();
                    String decodedUrl=new String(Base64.decode(repoUrl, Base64.URL_SAFE | Base64.NO_WRAP));
                    String repoName = decodedUrl.substring(decodedUrl.lastIndexOf('/') + 1).replace(".git", "");
                    repos.add(new Repo(repoName, decodedUrl));
                }
            }
            downloadAdapter=new RepoAdapter(repos,repo -> {
                Intent intent = new Intent(this,ChatActivity.class);
                intent.putExtra("repo_url", repo.getUrl());
                intent.putExtra("access_token", accessToken);
                startActivity(intent);
            });
            downloadAdapter.isDownload=true;
            recyclerView.setAdapter(downloadAdapter);
        }else {
            downloadAdapter=new RepoAdapter(repos, repo -> {});
            recyclerView.setAdapter(downloadAdapter);
        }

    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.github_connect);
        prefs = getSharedPreferences("github_auth", Context.MODE_PRIVATE);
        gitHubContainer= findViewById(R.id.github_status_container);
        gitUiOperationHandler=new GitUiOperationHandler(this);
        accessToken=prefs.getString("access_token", "");
        tabLayout = findViewById(R.id.tab_layout);
        blankView = findViewById(R.id.blank_view);
        progressBar = findViewById(R.id.progress_bar);
        recyclerView = findViewById(R.id.recycler_view);
        actionButton=findViewById(R.id.fab_refresh);
        actionButton.setOnClickListener(view -> getReposApi());
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Downloaded"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    if(!accessToken.isEmpty()){
                        recyclerView.setAdapter(repoAdapter);
                        recyclerView.setVisibility(View.VISIBLE);
                        actionButton.setVisibility(View.VISIBLE);
                    }

                    blankView.setVisibility(View.GONE);

                } else {
                    getDownloadedRepos();
                    actionButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        int drawableRes = accessToken.isEmpty()?R.drawable.dot_red: R.drawable.dot_green;
        if(!accessToken.isEmpty()){
            getReposFromStorage();
            actionButton.setVisibility(View.VISIBLE);
            File dirToClone=new File(getFilesDir(), "crowdfunding");
            File[] files = dirToClone.listFiles();
            if (files != null && files.length > 0) {
                File firstFile = null;
                for (File file : files) {
                    if (file.isFile() && !file.isHidden()) {
                        firstFile = file;
                        break;
                    }
                }
                if (firstFile != null) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(firstFile))) {
                        StringBuilder content = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                        // Print file name and contents
                        Log.d("RepoClone", "First file: " + firstFile.getName());
                        Log.d("RepoClone", "Contents:\n" + content.toString());
                    } catch (IOException e) {
                        Log.e("RepoClone", "Error reading file: " + e.getMessage());
                    }
                } else {
                    Log.d("RepoClone", "No regular file found in " + dirToClone.getAbsolutePath());
                }
            } else {
                Log.d("RepoClone", "Directory is empty or does not exist: " + dirToClone.getAbsolutePath());
            }
        }
        View githubStatusDot = findViewById(R.id.github_status_dot);
        githubStatusDot.setBackgroundResource(drawableRes);
        gitHubDeviceAuth=new GitHubDeviceAuth.AuthCallback() {
            @Override
            public void onAuthSuccess(String access_Token) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ConnectActivity.this, "Authenticated!", Toast.LENGTH_SHORT).show();
                githubStatusDot.setBackgroundResource(R.drawable.dot_green);
                prefs.edit().putString("access_token", access_Token).apply();
                accessToken=prefs.getString("access_token", "");
                getReposFromStorage();
                actionButton.setVisibility(View.VISIBLE);
                // Now you can use the accessToken to call GitHub APIs
            }

            @Override
            public void onAuthError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ConnectActivity.this, "Auth failed: " + error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onUserCodeReady(String userCode, String verificationUri) {
                progressBar.setVisibility(View.GONE);
                runOnUiThread(() -> {
                    // Show these in a TextView, or copy to clipboard, or open verificationUri
                    showBuilder(verificationUri, userCode);

                });
            }
        };
        gitHubContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(accessToken.isEmpty()){
                    progressBar.setVisibility(View.VISIBLE);
                    GitHubDeviceAuth.authenticate(view.getContext(),gitHubDeviceAuth);

                }else{
                    disconnect();

                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (GitHubDeviceAuth.shouldResumePolling) {

                new Thread(()->{
                    try {
                        GitHubDeviceAuth.resumePolling(this, gitHubDeviceAuth, GitHubDeviceAuth.deviceCode, GitHubDeviceAuth.interval);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }).start();

        }
    }
}