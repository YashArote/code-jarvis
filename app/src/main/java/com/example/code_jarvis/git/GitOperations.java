package com.example.code_jarvis.git;

import android.util.Log;
import android.widget.ProgressBar;

import com.example.code_jarvis.indexing.CodeIndexer;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.EmptyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GitOperations {
    public interface GitCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
        void onProgess(String task,int completed);
    }
    public class GitResult {
        public final String message;
        public GitResult(String message) { this.message = message; }
    }

    private final String accessToken;
    private final ProgressMonitor progressMonitor;
    private final String repoUrl;
    private final File localDir;
    public GitCallback<GitResult> callback;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GitOperations(String repoUrl, File localDir, String accessToken) {
        this.repoUrl = repoUrl;
        this.localDir = localDir;
        this.accessToken = accessToken;

        this.progressMonitor=new ProgressMonitor() {
            @Override
            public void start(int totalTasks) {

            }

            @Override
            public void beginTask(String title, int totalWork) {
                callback.onProgess(title, 0);
            }

            @Override
            public void update(int completed) {

            }

            @Override
            public void endTask() {

            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

    }
    public void indexRepo() {
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(repoUrl.getBytes(StandardCharsets.UTF_8));
        File indexFolder = new File(localDir, "index"+encoded);
        indexFolder.mkdir();
        File gitignore = new File(localDir, ".gitignore");
        try {
            List<String> lines = new ArrayList<>();
            if (gitignore.exists()) {
                lines = Files.readAllLines(gitignore.toPath(), StandardCharsets.UTF_8);
            }
            if (!lines.contains("index"+encoded+"/")) {
                lines.add("index"+encoded+"/");
                Files.write(gitignore.toPath(), lines, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        File indexFile = new File(indexFolder, "indexFile_" + sdf.format(new Date()));
        JSONObject projectSymbols = CodeIndexer.parseDirectory(localDir);
        Log.d("json", projectSymbols.toString());
        try (FileWriter file = new FileWriter(indexFile)) {
            file.write(projectSymbols.toString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String getRelativePath(File repoRoot, File targetFile) {
        Path rootPath = repoRoot.toPath().toAbsolutePath().normalize();
        Path filePath = targetFile.toPath().toAbsolutePath().normalize();
        return rootPath.relativize(filePath).toString();
    }
    public void cloneRepo() {
        executor.execute(() -> {
            try {
                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(localDir)
                        .setProgressMonitor(progressMonitor)
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken, ""))
                        .call();
                indexRepo();
                callback.onSuccess(new GitResult("Clone successful"));
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void pull() {
        executor.execute(() -> {
            try (Git git = Git.open(localDir)) {
                git.pull()
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken, ""))
                        .call();
                callback.onSuccess(new GitResult("Pull successful"));
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }
    public void add(List<String> filePatterns) {
        executor.execute(() -> {
            try (Git git = Git.open(localDir)) {
                AddCommand addCmd = git.add();
                if (filePatterns == null || filePatterns.isEmpty()) {
                    addCmd.addFilepattern("."); // Add all files
                } else {
                    for (String pattern : filePatterns) {
                        File fileToAdd=new File(pattern);
                        String relative = getRelativePath(localDir, fileToAdd);
                        addCmd.addFilepattern(relative);
                    }
                }
                addCmd.call();
                callback.onSuccess(new GitResult("Add successful"));
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void commit(String message) {
        executor.execute(() -> {
            try (Git git = Git.open(localDir)) {
                Status status = git.status().call();

                // Only commit if there are staged changes
                if (!status.getAdded().isEmpty()
                        || !status.getChanged().isEmpty()
                        || !status.getRemoved().isEmpty()
                        || !status.getModified().isEmpty()
                        || !status.getMissing().isEmpty()) {

                    git.commit().setMessage(message).call();
                    callback.onSuccess(new GitResult("Commit successful"));
                } else {
                    callback.onFailure(new EmptyCommitException("No changes staged for commit"));
                }
            } catch (EmptyCommitException ece) {
                callback.onFailure(ece);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void push() {
        executor.execute(() -> {
            try (Git git = Git.open(localDir)) {
                git.push()
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken, ""))
                        .call();
                callback.onSuccess(new GitResult("Push successful"));
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }
}
