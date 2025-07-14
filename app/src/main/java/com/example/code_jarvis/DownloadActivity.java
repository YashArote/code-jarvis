package com.example.code_jarvis;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.downloader.*;
import com.downloader.Error;

import java.io.File;
import java.util.*;

public class DownloadActivity extends AppCompatActivity {

    Button startButton;
    TextView currentFileText, statusLog;
    LinearLayout fileProgressContainer;

    String modelUrl, embeddingUrl, vocabUrl;
    String[] urls;
    String[] fileNames = {"model.gguf"};

    int completed = 0;
    Map<String, Integer> fileProgressMap = new HashMap<>();
    Map<String, Integer> downloadIds = new HashMap<>();
    SharedPreferences sharedPref;
    // Map to hold per-file progress bar and percent label
    Map<String, ViewHolder> fileViewMap = new HashMap<>();

    static class ViewHolder {
        TextView fileNameView;
        ProgressBar progressBar;
        TextView percentView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_activity);
        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(true)
                .build();
        PRDownloader.initialize(getApplicationContext(),config);
        sharedPref= getSharedPreferences("downloadPrefs", Context.MODE_PRIVATE);
        modelUrl = getIntent().getStringExtra("model_url");

        urls = new String[]{modelUrl};

        startButton = findViewById(R.id.start_download_button);
        currentFileText = findViewById(R.id.current_file_text);
        statusLog = findViewById(R.id.status_log);
        fileProgressContainer = findViewById(R.id.file_progress_container);

        setupFileProgressViews();

        boolean anyFileExistsAndNotEmpty = false;
        for (String name : fileNames) {
            File f = new File(getFilesDir(), name);
            if (f.exists() && f.length() > 0) {
                anyFileExistsAndNotEmpty = true;
                break;
            }
        }
        if (anyFileExistsAndNotEmpty) {
            startButton.setText("Resume Download");
        } else {
            startButton.setText("Start Download");
        }

        startButton.setOnClickListener(v -> startDownload());
    }

    // Dynamically add per-file progress UI
    private void setupFileProgressViews() {
        fileProgressContainer.removeAllViews();
        fileViewMap.clear();
        for (String fileName : fileNames) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView fileNameView = new TextView(this);
            fileNameView.setText(fileName);
            fileNameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

            ProgressBar fileProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            fileProgressBar.setMax(100);
            fileProgressBar.setProgress(0);
            fileProgressBar.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f));

            TextView percentView = new TextView(this);
            percentView.setText("0%");
            percentView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            percentView.setGravity(View.TEXT_ALIGNMENT_TEXT_END);

            row.addView(fileNameView);
            row.addView(fileProgressBar);
            row.addView(percentView);

            fileProgressContainer.addView(row);

            ViewHolder holder = new ViewHolder();
            holder.fileNameView = fileNameView;
            holder.progressBar = fileProgressBar;
            holder.percentView = percentView;
            fileViewMap.put(fileName, holder);
        }
    }

    private void startDownload() {
        startButton.setEnabled(false);
        statusLog.setVisibility(View.VISIBLE);
        currentFileText.setVisibility(View.VISIBLE);

        completed = 0;
        fileProgressMap.clear();
        downloadIds.clear();

        int totalFiles = fileNames.length;

        for (int i = 0; i < 1; i++) {
            final String url = urls[i];
            final String fileName = fileNames[i];
            final File targetFile = new File(getFilesDir(), fileName);
            boolean isPresent=sharedPref.getBoolean(fileName, false);
            if(isPresent) {
                fileProgressMap.put(fileName, 100);
                continue;
            }
            int downloadId = PRDownloader.download(url, targetFile.getParent(), targetFile.getName())
                    .build()
                    .setOnStartOrResumeListener(() -> {
                        sharedPref.edit().putBoolean(fileName+"started",true ).apply();
                        sharedPref.edit().putString("model_url", url).apply();
                        statusLog.append("⏳ Downloading: " + fileName + "\n");
                    })
                    .setOnProgressListener(progress -> {
                        double downloadedMB = progress.currentBytes / (1024.0 * 1024.0);
                        double totalMB = progress.totalBytes / (1024.0 * 1024.0);
                        int percent = (int) ((progress.currentBytes * 100L) / progress.totalBytes);
                        fileProgressMap.put(fileName, percent);
                        updateFileProgress(fileName, percent,totalMB,downloadedMB);
                        currentFileText.setText("Downloading: " + fileName + " (" + percent + "%)");
                    })
                    .start(new OnDownloadListener() {
                        @Override
                        public void onDownloadComplete() {
                            fileProgressMap.put(fileName, 100);
                            updateFileProgress(fileName, 100,0,0);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean(fileName, true);
                            editor.apply();
                            statusLog.append("✔ Downloaded: " + fileName + "\n");
                            completed++;
                            Intent intent = new Intent(DownloadActivity.this,ConnectActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(Error error) {
                            updateFileProgress(fileName, 0,0,0);
                            statusLog.append("❌ Failed: " + fileName + "\n");
                            currentFileText.setText("Download failed for: " + fileName);
                            startButton.setEnabled(true);
                        }
                    });

            downloadIds.put(fileName, downloadId);
        }
    }

    private void updateFileProgress(String fileName, int percent,double total,double downloaded) {
        ViewHolder holder = fileViewMap.get(fileName);
        if (holder != null) {
            holder.progressBar.setProgress(percent);
            holder.percentView.setText(String.format("%.2f/%.2f MB", downloaded, total));
        }
    }


}
