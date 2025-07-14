package com.example.code_jarvis;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    String[] fileNames = {"model.gguf"};
    boolean anyFileDosentExist=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        for (String name : fileNames) {
            File f = new File(getFilesDir(), name);
            if (!f.exists()) {
                anyFileDosentExist = true;
            }
        }
        if (!anyFileDosentExist) {
            startActivity(new Intent(MainActivity.this, ConnectActivity.class));
        } else if (anyFileDosentExist) {
            startActivity(new Intent(MainActivity.this, SetupActivity.class));
        } else {
            SharedPreferences prefs = getSharedPreferences("downloadPrefs", Context.MODE_PRIVATE);
            if (prefs.getBoolean("model.gguf" + "started", false)) {
                Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
                intent.putExtra("model_url", prefs.getString("model_url", null));
                startActivity(intent);
            }
        }
        finish();
    }
}