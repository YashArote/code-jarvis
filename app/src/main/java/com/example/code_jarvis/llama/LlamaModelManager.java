package com.example.code_jarvis.llama;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class LlamaModelManager {
    private static boolean isInitialized = false;
    private static boolean isLoading = false;

    public interface InitCallback {
        void onSuccess();
        void onFailure();
    }

    public static synchronized boolean isReady() {
        return isInitialized;
    }

    public static synchronized boolean isLoading() {
        return isLoading;
    }

    public static void initModelAsync(String modelPath, Context context, InitCallback callback) {
        if (isInitialized) {
            callback.onSuccess();
            return;
        }

        if (isLoading) {
            return;
        }

        isLoading = true;

        new Thread(() -> {
            boolean success = LlamaBridge.initModel(modelPath);

            new Handler(Looper.getMainLooper()).post(() -> {
                isLoading = false;
                if (success) {
                    isInitialized = true;
                    callback.onSuccess();
                } else {
                    callback.onFailure();
                }
            });
        }).start();
    }

    public static synchronized void release() {
        if (isInitialized) {
            LlamaBridge.releaseModel();
            isInitialized = false;
        }
    }
}
