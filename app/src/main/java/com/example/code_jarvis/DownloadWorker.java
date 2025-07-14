package com.example.code_jarvis;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadWorker extends Worker {

    private static final String TAG = "DownloadWorker";

    public DownloadWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        String url = getInputData().getString("url");
        String fileName = getInputData().getString("fileName");

        Log.i(TAG, "Starting download task");
        Log.i(TAG, "Download URL: " + url);
        Log.i(TAG, "Target file name: " + fileName);

        try {
            File downloadDir = getApplicationContext().getFilesDir();
            File outFile = new File(downloadDir, fileName);
            long downloadedSize = outFile.exists() ? outFile.length() : 0;

            Log.i(TAG, "Download directory: " + downloadDir.getAbsolutePath());
            Log.i(TAG, "File exists: " + outFile.exists());
            Log.i(TAG, "Already downloaded size: " + downloadedSize + " bytes");

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            if (downloadedSize > 0) {
                connection.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
            }

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            Log.i(TAG, "HTTP response code: " + responseCode);

            // Handle auth failure
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.e(TAG, "Authentication failed. Check your URL or headers.");
                return Result.failure();
            }

            // Handle 416: Range Not Satisfiable
            if (responseCode == 416) {
                Log.w(TAG, "HTTP 416 received. Deleting file and restarting download.");
                if (outFile.exists()) {
                    boolean deleted = outFile.delete();
                    Log.i(TAG, "Deleted file: " + deleted);
                }
                return Result.retry();
            }

            // Acceptable responses
            if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Unexpected HTTP response. Cancelling download.");
                return Result.failure();
            }

            InputStream input = new BufferedInputStream(connection.getInputStream());
            FileOutputStream output = new FileOutputStream(outFile, downloadedSize > 0);

            Log.i(TAG, "Starting file write...");

            byte[] buffer = new byte[8192];
            int count;
            long totalWritten = 0;
            long fileSize = connection.getContentLengthLong() + downloadedSize;
            long totalBytesRead = downloadedSize;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                totalWritten += count;
                totalBytesRead += count;

                int progress = (int) ((totalBytesRead * 100) / fileSize);
                setProgressAsync(new Data.Builder().putInt("progress", progress).build());
            }

            output.flush();
            output.close();
            input.close();

            long expectedSize = fileSize;
            long actualSize = outFile.length();

            Log.i(TAG, "Expected total file size: " + expectedSize + " bytes");
            Log.i(TAG, "Actual downloaded file size: " + actualSize + " bytes");

            if (actualSize != expectedSize) {
                Log.e(TAG, "File size mismatch. Will retry...");
                return Result.retry();
            }

            Log.i(TAG, "Download completed successfully.");
            return Result.success();

        } catch (IOException e) {
            Log.e(TAG, "IOException during download. Network issue, retrying...", e);
            return Result.retry();

        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception during download", e);
            return Result.failure();
        }
    }
}
