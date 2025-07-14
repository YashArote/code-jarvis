package com.example.code_jarvis;


import static com.example.code_jarvis.codeEditor.CodeLanguageHelper.grammarRegistry;
import static com.example.code_jarvis.codeEditor.CodeLanguageHelper.themeRegistry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.code_jarvis.adapter.ChatAdapter;
import com.example.code_jarvis.adapter.FileAdapter;
import com.example.code_jarvis.chat.ChatGitOperations;
import com.example.code_jarvis.chat.ChatMessage;
import com.example.code_jarvis.codeEditor.CodeEditorDialog;
import com.example.code_jarvis.git.GitOperations;
import com.example.code_jarvis.git.GitUiOperationHandler;
import com.example.code_jarvis.llama.LlamaBridge;
import com.example.code_jarvis.llama.LlamaModelManager;

import org.eclipse.jgit.api.Git;
import org.eclipse.tm4e.core.registry.IThemeSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.Locale;

import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;

public class ChatActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    EditText chatInput;
    ImageButton sendButton;
    View dialogProgress;
    AlertDialog progressDialog;
    Boolean isGenerating=false;
    ChatAdapter chatAdapter;
    FileAdapter fileAdapter;
    RecyclerView fileCycler;
    ImageButton undoButton;
    String repo_url;
    Toolbar toolbar;
    String access_token;
    File githubDir;
    GitUiOperationHandler gitUiOperationHandler;
    GitOperations gitOperations;
    ChatGitOperations chatGitOperations;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    final String SYSTEM_PROMPT = """
Follow these rules strictly when answering:

1. Whenever you output code, always use fenced code blocks (three backticks).
2. Always specify the programming language immediately after the opening triple backticks.
""";
    private void showProgressDialog(Activity activity,String message) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.dialog_progress, null);

        TextView progressMessage = view.findViewById(R.id.progress_message);
        progressMessage.setText(message); // Optional: set message dynamically

        progressDialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    public static String getShortTimeDateString() {
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(Locale.getDefault());
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(Locale.getDefault());

        return timeFormatter.format(now) + " · " + dateFormatter.format(now);
    }


    private void loadModel(String modelPath) {
        showProgressDialog(this, "Loading model...");

        LlamaModelManager.initModelAsync(modelPath, this, new LlamaModelManager.InitCallback() {
            @Override
            public void onSuccess() {
                dismissProgressDialog();
                Toast.makeText(ChatActivity.this, "Model loaded", Toast.LENGTH_SHORT).show();
                // Proceed with chat
            }

            @Override
            public void onFailure() {
                dismissProgressDialog();
                Toast.makeText(ChatActivity.this, "Failed to load model", Toast.LENGTH_LONG).show();
                finish(); // Or show error UI
            }
        });
    }

    private void sendPrompt(){
        sendButton.setEnabled(false);
        chatInput.setEnabled(false);
        String prompt= String.valueOf(chatInput.getText());
        chatAdapter.addMessage(new ChatMessage(prompt,ChatMessage.TYPE_USER,getShortTimeDateString()));
        chatAdapter.addMessage(new ChatMessage(prompt,ChatMessage.TYPE_JARVIS_TYPING,getShortTimeDateString()));
        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount()-1);
        new Thread(() -> {
            LlamaBridge.chatPrompt(prompt, new LlamaBridge.TokenCallback() {
                StringBuilder response = new StringBuilder();
                boolean typingRemoved = false;

                @Override
                public void onTokenGenerated(String token) {
                    runOnUiThread(() -> {
                        if (!typingRemoved) {
                            isGenerating=true;
                            sendButton.setImageResource(R.drawable.ic_stop);
                            sendButton.setEnabled(true);

                            sendButton.setOnClickListener(view -> {LlamaBridge.requestInterrupt();});
                            chatAdapter.removeTypingIndicator();
                            chatAdapter.addMessage(new ChatMessage("",ChatMessage.TYPE_JARVIS,getShortTimeDateString()));
                            typingRemoved = true;
                        }
                        response.append(token);
                        chatAdapter.appendToLastAssistantMessage(token);
                    });
                }

                @Override
                public void onComplete() {
                    runOnUiThread(() -> {
                        chatAdapter.codeBlockExtract();
                        isGenerating=false;
                        sendButton.setOnClickListener(view -> {sendPrompt();});
                        sendButton.setImageResource(R.drawable.ic_send);
                        sendButton.setEnabled(true);
                        chatInput.setEnabled(true);
                    });
                }
            });
        }).start();


    }

    @Override
    protected void onDestroy() {
        chatAdapter.exportChatsToJson();
        if(isGenerating){
            LlamaBridge.requestInterrupt();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);
        dialogProgress=getLayoutInflater().inflate(R.layout.dialog_progress, null);
        repo_url=getIntent().getStringExtra("repo_url");
        chatAdapter= new ChatAdapter(getApplicationContext(), repo_url);
        access_token=getIntent().getStringExtra("access_token");
        String modelPath=getFilesDir().getPath()+"/model.gguf";
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            Log.e("ModelLoad", "Model file not found at: " + modelPath);
            Toast.makeText(this, "Model file missing!", Toast.LENGTH_LONG).show();
        }
        fileCycler=findViewById(R.id.file_recycler_view);
        recyclerView= findViewById(R.id.chat_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
        chatAdapter.importChatsFromJson();
        chatInput= findViewById(R.id.chat_input);
        sendButton= findViewById(R.id.send_button);
        undoButton= findViewById(R.id.undo);
        sendButton.setEnabled(false);
        editorSetup();
        if(modelFile.exists()) loadModel(modelPath);

        sendButton.setOnClickListener(view -> {
            sendPrompt();
        });
        chatInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        setToolbar();
        getReposPopulate();
        gitUiOperationHandler=new GitUiOperationHandler(this);
        gitOperations=new GitOperations(repo_url, githubDir, access_token);
        chatGitOperations=new ChatGitOperations(gitUiOperationHandler,gitOperations);

    }

    private void editorSetup() {
        FileProviderRegistry.getInstance().addFileProvider(
                new AssetsFileResolver(
                        getApplicationContext().getAssets() // use application context
                )
        );
        var themeAssetsPath = "theme/monokai.json";
        var model = new ThemeModel(
                IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath), themeAssetsPath, null
                ),
                "monokai"
        );
// If the theme is dark
// model.setDark(true);
        try {
            themeRegistry.loadTheme(model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        themeRegistry.setTheme("monokai");
        grammarRegistry.loadGrammars("registry/languages.json");
    }

    private String readFileAsString(File file) {
        StringBuilder text = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }
    public static List<File> listFiles(File repoDir) {
        File gitignoreFile = new File(repoDir, ".gitignore");

        List<String> ignorePatterns = new ArrayList<>();
        if (gitignoreFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(gitignoreFile.toPath());
                for (String pattern : lines) {
                    pattern = pattern.trim();
                    if (pattern.isEmpty() || pattern.startsWith("#")) continue;

                    ignorePatterns.add(pattern.replace("\\", "/"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File[] filteredFiles = repoDir.listFiles(file -> {
            if (file.getName().startsWith(".")) return false;

            // Get path relative to repoDir and normalize to forward slashes
            String relativePath = repoDir.toPath().relativize(file.toPath()).toString().replace("\\", "/");

            // Check if path starts with any ignore pattern
            for (String ignore : ignorePatterns) {
                if (ignore.startsWith(relativePath)) {
                    return false;
                }
            }

            return true;
        });

        return Arrays.asList(filteredFiles != null ? filteredFiles : new File[0]);
    }

    private void getReposPopulate(){
        File repoDir=new File(getFilesDir(),"github_repos");
        String base64Url= Base64.encodeToString(repo_url.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
        String repoName = repo_url.substring(repo_url.lastIndexOf('/') + 1).replace(".git", "");
        TextView textView=findViewById(R.id.toolbar_title);
        textView.setText(repoName);
        githubDir=new File(repoDir,base64Url);
        List<File> files= listFiles(githubDir);
        fileAdapter=new FileAdapter(githubDir,files, ChatActivity.this, file -> {
            if(file.isDirectory()){
                fileAdapter.reloadFiles(Arrays.asList(file.listFiles()),file);
                undoButton.setVisibility(View.VISIBLE);
            }else{
                String code = readFileAsString(file);
                String lang = file.getName();

                CodeEditorDialog dialog = CodeEditorDialog.newInstance(githubDir,file,code, lang);
                dialog.show(getSupportFragmentManager(), "code_editor");
            }
        },()->{
            undoButton.setVisibility(View.GONE);
        });
        undoButton.setOnClickListener(view ->{
            fileAdapter.goToPevious();
        });
        fileCycler.setLayoutManager(new LinearLayoutManager(ChatActivity.this));
        fileCycler.setAdapter(fileAdapter);
    }
    private void setToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(
                ChatActivity.this,
                drawerLayout,
                toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close
        );

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }
    public interface InputCallback{
        void onInputConfirmed(String commitMessage) throws Exception;
    }
    public void getCommitMessage(InputCallback inputCallback){
        LayoutInflater inflater = getLayoutInflater();
        EditText editText=new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setHint("message...");
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
                .show();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        Log.d("ToolbarPopupTheme", String.valueOf(toolbar.getPopupTheme()));

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.option_one) {
            chatGitOperations.addAll();
            return true;
        } else if (item.getItemId()==R.id.option_two) {
            getCommitMessage(commitMessage -> {
                if(!commitMessage.isEmpty()) chatGitOperations.commit(commitMessage);
            });
           return true;
        }else if(item.getItemId()==R.id.option_three){
            chatGitOperations.push();
            return true;
        }else if(item.getItemId()==R.id.option_four) {
            chatGitOperations.pull();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
