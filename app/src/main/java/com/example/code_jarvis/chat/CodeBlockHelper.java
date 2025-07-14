package com.example.code_jarvis.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.example.code_jarvis.R;
import com.example.code_jarvis.codeEditor.CodeLanguageHelper;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.widget.CodeEditor;

public class CodeBlockHelper {
    public static TextView createMessageTextView(Context context, ViewGroup parent, String text,int index) {
        LayoutInflater inflater = LayoutInflater.from(context);
        TextView tv = (TextView) inflater.inflate(R.layout.view_message_bubble, parent,false);
        if(index==0){
            tv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.bg_msg_top, null));
        } else if (index==-1) {
            tv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.bg_msg_bottom, null));
        }

        tv.setText(text);
        return tv;
    }

    public static View createCodeEditorView(Context context,ViewGroup parent ,String code, String lang) throws Exception {
        LayoutInflater inflater = LayoutInflater.from(context);
        View root = inflater.inflate(R.layout.view_code_block, parent, false);
        CodeEditor editor = root.findViewById(R.id.editor_codeview);
        ImageButton copyButton = root.findViewById(R.id.btn_copy);
        editor.setText(code);
        editor.setEditorLanguage(CodeLanguageHelper.getLanguageForExtension(lang));
        editor.setWordwrap(true);
        editor.setTextSize(13);
        editor.setLineNumberEnabled(false);
        editor.setTypefaceText(Typeface.MONOSPACE);
        editor.setColorScheme(TextMateColorScheme.create(CodeLanguageHelper.themeRegistry));
        editor.setEditable(false);
        editor.setFocusable(true);
        editor.setFocusableInTouchMode(true);
        editor.setVerticalScrollBarEnabled(true);
        editor.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        editor.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        editor.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Code", editor.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show();
        });
        return root;
    }


}
