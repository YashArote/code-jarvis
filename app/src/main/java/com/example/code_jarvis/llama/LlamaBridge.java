package com.example.code_jarvis.llama;

public class LlamaBridge {

    // Load native library
    static {
        System.loadLibrary("smollm"); // name without lib prefix and .so suffix
    }

    // Native methods
    public static native boolean initModel(String modelPath);
    public static native void chatPrompt(String prompt, TokenCallback callback);
    public static native void releaseModel();
    public static native void requestInterrupt();

    // Callback interface for streaming tokens
    public interface TokenCallback {
        void onTokenGenerated(String token);
        void onComplete();
    }
}