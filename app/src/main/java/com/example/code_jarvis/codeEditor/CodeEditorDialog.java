package com.example.code_jarvis.codeEditor;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.code_jarvis.R;
import com.example.code_jarvis.git.GitOperations;
import com.example.code_jarvis.git.GitUiOperationHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Stack;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.widget.EditorSearcher;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import okhttp3.internal.http2.Http2Reader;

public class CodeEditorDialog extends DialogFragment {

    private CodeEditor editor;
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private String currentCode = "";
    private String currentLang = "";
    private String currentFile="";
    private String currentDir="";

    public static final String ARG_CODE = "code_arg";
    public static final String ARG_LANG = "lang_arg";
    public static final String ARG_FILE="file_arg";
    public static final String ARG_DIR="github_dir";


    public static CodeEditorDialog newInstance(File githubDir,File file,String code, String language) {
        CodeEditorDialog dialog = new CodeEditorDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CODE, code);
        args.putString(ARG_LANG, language);
        args.putString(ARG_DIR, githubDir.getAbsolutePath());
        args.putString(ARG_FILE,file.getAbsolutePath());
        dialog.setArguments(args);
        return dialog;
    }
    public void writeStringToFile(File file, String data) {
        try {
            FileWriter writer = new FileWriter(file, false);
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.dialog_editor, container, false);

        editor = root.findViewById(R.id.editor_codeview);
        ImageButton closeBtn = root.findViewById(R.id.close_editor);
        ImageButton undoBtn = root.findViewById(R.id.btn_undo);
        ImageButton redoBtn = root.findViewById(R.id.btn_redo);
        Button searchBtn = root.findViewById(R.id.btn_search);
        Button gitAddBtn= root.findViewById(R.id.git_add);
        ImageButton saveButton=root.findViewById(R.id.save_file);
        EditText searchInput = root.findViewById(R.id.search_input);
        EditorSearcher.SearchOptions options = new EditorSearcher.SearchOptions(EditorSearcher.SearchOptions.TYPE_NORMAL,false);

        if (getArguments() != null) {
            currentCode = getArguments().getString(ARG_CODE, "");
            currentLang = getArguments().getString(ARG_LANG, "java");
            currentFile=getArguments().getString(ARG_FILE,"");
            currentDir=getArguments().getString(ARG_DIR,"");

        }
        File currentFileObj=new File(currentFile);
        File currentFolder=new File(currentDir);
        GitOperations gitOperations=new GitOperations(null,currentFolder,null );

        try {
            setupEditor(currentCode, currentLang);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        saveButton.setOnClickListener(v->{
            String currentCode=editor.getText().toString();
            writeStringToFile(currentFileObj, currentCode);
            Toast.makeText(getContext(), "saved!", Toast.LENGTH_SHORT).show();

        });
        gitAddBtn.setOnClickListener(v->{

            gitOperations.callback= new GitOperations.GitCallback<GitOperations.GitResult>() {
                @Override
                public void onSuccess(GitOperations.GitResult result) {
                    new Handler(Looper.getMainLooper()).post(()->{
                        Toast.makeText(getContext(), "success", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override
                public void onFailure(Exception e) {
                    new Handler(Looper.getMainLooper()).post(()->{
                        Toast.makeText(getContext(), "failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onProgess(String task, int completed) {

                }
            };
            gitOperations.add(Arrays.asList(currentFile) );
        });

        searchBtn.setOnClickListener(v -> {
            if (searchInput.getVisibility() == View.GONE) {
                searchInput.setVisibility(View.VISIBLE);
                searchInput.requestFocus();
            } else {
                String keyword = searchInput.getText().toString().trim();
                if (!TextUtils.isEmpty(keyword)) {
                    editor.getSearcher().search(keyword, options);
                } else {
                    editor.getSearcher().stopSearch();
                    searchInput.setVisibility(View.GONE);
                }
            }
        });
        closeBtn.setOnClickListener(v -> dismiss());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.length() == 0) {
                    editor.getSearcher().stopSearch();
                }else{
                    editor.getSearcher().search(s.toString(), options);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        undoBtn.setOnClickListener(v -> {
            if (editor.canUndo()) editor.undo();
        });

        redoBtn.setOnClickListener(v -> {
            if (editor.canRedo()) editor.redo();
        });



        return root;
    }

    private void setupEditor(String code, String langExt) throws Exception {
        editor.setEditorLanguage(CodeLanguageHelper.getLanguageForExtension(langExt));
        editor.setColorScheme(TextMateColorScheme.create(CodeLanguageHelper.themeRegistry));
        editor.setText(code);
        editor.setLineNumberEnabled(true);

        editor.setWordwrap(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
}
