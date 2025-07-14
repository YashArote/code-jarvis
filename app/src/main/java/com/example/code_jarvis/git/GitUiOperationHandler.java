package com.example.code_jarvis.git;
import  com.example.code_jarvis.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.code_jarvis.R;

public class GitUiOperationHandler {
    private final Activity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AlertDialog progressDialog;
    private TextView progressMessage;
    public GitUiOperationHandler(Activity activity) {
        this.activity = activity;
    }

    public void runWithUi(GitOperationStarter starter) {
        showProgressBar();
        // Call the starter, which should trigger GitOperations (already backgrounded)
        starter.start(new GitOperations.GitCallback<>() {
            @Override
            public void onSuccess(GitOperations.GitResult result) {
                mainHandler.post(() -> {
                    hideProgressBar();
                    showResultDialog();
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    hideProgressBar();
                    showErrorDialog(e.getMessage());
                });
            }

            @Override
            public void onProgess(String task, int completed) {
                mainHandler.post(() -> {
                    progressMessage.setText("Task: " + task);
                });
            }
        });
    }


    private void showProgressBar() {
        int sizeInDp = 100;
        int sizeInPx = (int) (sizeInDp * activity.getResources().getDisplayMetrics().density);
        FrameLayout layout = new FrameLayout(activity);
        layout.setPadding(50, 50, 50, 50);

        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleLarge);
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        barParams.gravity = Gravity.CENTER;
        layout.addView(progressBar, barParams);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(sizeInPx, sizeInPx);
        layout.setLayoutParams(frameParams);
        progressMessage = new TextView(activity);
        progressMessage.setText("Preparing...");
        progressMessage.setTextColor(Color.WHITE);
        progressMessage.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        textParams.bottomMargin = 30;

        layout.addView(progressMessage, textParams);

        progressDialog = new AlertDialog.Builder(activity)
                .setView(layout)
                .setCancelable(false)
                .create();
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.show();
    }


    private void hideProgressBar() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showResultDialog() {
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView=inflater.inflate(R.layout.common_dialog, null);
        TextView dialogText=dialogView.findViewById(R.id.alertMessage);
        dialogText.setText("Operation completed");
        new AlertDialog.Builder(activity).setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showErrorDialog(String message) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView=inflater.inflate(R.layout.common_dialog, null);
        TextView dialogText=dialogView.findViewById(R.id.alertMessage);
        dialogText.setText(message);
        new AlertDialog.Builder(activity).setView(dialogView)

                .setPositiveButton("OK", null)
                .show();
    }

    // Interface for starting a Git operation with a callback
    public interface GitOperationStarter {
        void start(GitOperations.GitCallback<GitOperations.GitResult> callback);
    }
}
