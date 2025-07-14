package com.example.code_jarvis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SetupActivity extends AppCompatActivity {

    private TextView wifiStatusText, storageStatusText;
    private ImageView wifiIcon, storageIcon;
    private Button nextButton;
    private TextView skipModel;
    private long selectedModelSize = 1100L * 1024 * 1024;
    private static final long DEFAULT_MIN_STORAGE_BUFFER = 300L * 1024 * 1024; // 300 MB buffer
    private static final long DEFAULT_TRANSFORMER=200L *1024 *1024;
    private boolean isWifiOK = false;
    private boolean isStorageOK = false;
    private static final long REQUIRED_SPACE_BYTES = 1100L * 1024 * 1024; // 2 GB
    private String[] modelUrls = {
            "https://huggingface.co/tensorblock/deepseek-coder-1.3b-instruct-GGUF/resolve/main/deepseek-coder-1.3b-instruct-Q6_K.gguf?download=true",
            "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf?download=true",
            ""
    };

    public void getCustomUrl(ChatActivity.InputCallback inputCallback){
        LayoutInflater inflater = getLayoutInflater();
        EditText editText=new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setHint("gguf direct download link");
        editText.setHintTextColor(getColor(R.color.colorDull));
        editText.setTextColor(getColor(R.color.colorOnSurface));
        View dialogView=inflater.inflate(R.layout.common_dialog, null);
        LinearLayout container= dialogView.findViewById(R.id.common_dialog_container);
        container.removeView(findViewById(R.id.alertMessage));
        container.addView(editText);
        new android.app.AlertDialog.Builder(this).setView(dialogView)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    String userInput= editText.getText().toString();
                    try {
                        inputCallback.onInputConfirmed(userInput);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .setCancelable(false)
                .show();
    }
    public static void getFileSizeAsync(String url, Consumer<Long> callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("HEAD");
                conn.connect();
                long size = conn.getContentLengthLong();
                conn.disconnect();

                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.accept(size);
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.accept(-1L);
                });
            }
        });
    }
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_activity);

        wifiStatusText = findViewById(R.id.wifi_status_text);
        skipModel=findViewById(R.id.skip_model);
        storageStatusText = findViewById(R.id.storage_status_text);
        wifiIcon = findViewById(R.id.wifi_icon);
        storageIcon = findViewById(R.id.storage_icon);
        nextButton = findViewById(R.id.next_button);
        Spinner llmSpinner = findViewById(R.id.llm_model_spinner);
        TextView totalStorageText = findViewById(R.id.total_storage_text);

// Model names and their storage sizes in bytes
        Map<String, Long> modelSizes = new HashMap<>();
        modelSizes.put("DeepSeek-Coder 1.3B", 1200L * 1024 * 1024);
        modelSizes.put("Phi-3 Mini", 2400L * 1024 * 1024);


        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"DeepSeek-Coder 1.3B", "Phi-3 Mini","Custom"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        llmSpinner.setAdapter(adapter);

// Default selection
        llmSpinner.setSelection(0);
        selectedModelSize = modelSizes.get("DeepSeek-Coder 1.3B");
        updateStorageText(selectedModelSize+DEFAULT_MIN_STORAGE_BUFFER);

// Listen for selection changes
        llmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String modelName = (String) parent.getItemAtPosition(position);
                if(position==2){
                    getCustomUrl(url-> {
                        getFileSizeAsync(url, size -> {
                            if (size > 0) {
                                selectedModelSize= size;

                            } else {
                                Toast.makeText(SetupActivity.this, "Failed to fetch model size.", Toast.LENGTH_SHORT).show();
                            }
                            modelUrls[position]=url;
                            updateStorageText(selectedModelSize+DEFAULT_MIN_STORAGE_BUFFER);
                            checkStorageStatus();
                            updateNextButtonState();
                        });

                    });
                } else {
                    selectedModelSize = modelSizes.get(modelName);

                }
                updateStorageText(selectedModelSize+DEFAULT_MIN_STORAGE_BUFFER);
                checkStorageStatus();
                updateNextButtonState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        checkWifiStatus();
        checkStorageStatus();
        updateNextButtonState();
        skipModel.setOnClickListener(view -> {
            startActivity(new Intent(SetupActivity.this, ConnectActivity.class));
        });
        nextButton.setOnClickListener(v -> {
            // Proceed to the next page
            int selectedIndex = llmSpinner.getSelectedItemPosition();
            String selectedModelUrl = modelUrls[selectedIndex];

            Intent intent = new Intent(SetupActivity.this, DownloadActivity.class);
            intent.putExtra("model_url", selectedModelUrl);


            startActivity(intent);
            finish();
        });
    }

    private void checkWifiStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                wifiStatusText.setText("Connected to Wi-Fi.");
                wifiIcon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.status_success)); // ✅ Green
                isWifiOK = true;
            } else {
                wifiStatusText.setText("Not on Wi-Fi. Proceeding with mobile data.");
                wifiIcon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.status_warning)); // ⚠️ Amber
                isWifiOK = true; // allow mobile data, just warn
            }
        } else {
            wifiStatusText.setText("No network connection.");
            wifiIcon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.status_error)); // ❌ Red
            isWifiOK = false;
        }

    }

    private void checkStorageStatus() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        long availableBytes = (long) statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();

        long totalRequired = selectedModelSize + DEFAULT_MIN_STORAGE_BUFFER+ DEFAULT_TRANSFORMER;

        if (availableBytes >= totalRequired) {
            storageStatusText.setText("Sufficient storage available.");
            storageIcon.setColorFilter(getColor(android.R.color.holo_green_dark));
            isStorageOK = true;
        } else {
            storageStatusText.setText("Not enough storage. Please free up space.");
            storageIcon.setColorFilter(getColor(android.R.color.holo_red_dark));
            isStorageOK = false;
        }
    }

    private void updateStorageText(long sizeBytes) {
        TextView totalStorageText = findViewById(R.id.total_storage_text);
        double sizeInGB = sizeBytes / (1024.0 * 1024.0 * 1024.0);
        totalStorageText.setText(String.format("Total storage required: ~%.1f GB", sizeInGB));
    }

    private void updateNextButtonState() {
        // Call this after both checks, or if you want to update reactively later
        nextButton.setEnabled(isWifiOK && isStorageOK);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check in case user comes back
        checkWifiStatus();
        checkStorageStatus();
        updateNextButtonState();
    }
}
