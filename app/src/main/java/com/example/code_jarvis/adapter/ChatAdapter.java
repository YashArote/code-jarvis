package com.example.code_jarvis.adapter;

import static com.example.code_jarvis.chat.CodeBlockHelper.createCodeEditorView;
import static com.example.code_jarvis.chat.CodeBlockHelper.createMessageTextView;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.code_jarvis.R;
import com.example.code_jarvis.chat.ChatMessage;
import com.example.code_jarvis.chat.CodeBlockExtractor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messages = new ArrayList<>();
    private final Context context;

    private final String repo_url;

    public ChatAdapter(Context context,String repo_url) {
        this.context = context;
        this.repo_url=repo_url;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        LinearLayout bubbleContainer;
        TextView messageText, messageTime;
        LinearLayout codeBlockContainer;


        public MessageViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.message_container);
            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
            bubbleContainer=itemView.findViewById(R.id.message_bubble_container);
            codeBlockContainer = itemView.findViewById(R.id.code_block_container);

        }
    }

    public static class TypingViewHolder extends RecyclerView.ViewHolder {
        public TypingViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }
    public File exportChatsToJson() {
        try {
            File dir = context.getFilesDir();
            if (dir == null) return null;

            String base64Url= Base64.encodeToString(repo_url.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
            File file = new File(dir, "chat_export_" + base64Url + ".json");
            Gson gson = new Gson();
            String json = gson.toJson(messages);

            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();

           // Toast.makeText(context, "Chat exported to: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return file;

        } catch (Exception e) {
            e.printStackTrace();
           // Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }
    public void importChatsFromJson() {
        try {
            File dir = context.getFilesDir();
            if (dir == null) return;
            String base64Url= Base64.encodeToString(repo_url.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
            File file = new File(dir, "chat_export_" + base64Url + ".json");
            if(!file.exists()) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            Type type = new TypeToken<List<ChatMessage>>() {}.getType();
            Gson gson = new Gson();
            List<ChatMessage> savedMessages = gson.fromJson(jsonBuilder.toString(), type);
            messages=savedMessages;
            notifyDataSetChanged();
          //  Toast.makeText(context, "Chats imported successfully.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
           // Toast.makeText(context, "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_JARVIS_TYPING) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_typing_message, parent, false);
            TypingViewHolder typingViewHolder=  new TypingViewHolder(view);

            return typingViewHolder;
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_chat_message, parent, false);
            MessageViewHolder messageViewHolder=new MessageViewHolder(view);

            return messageViewHolder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holderBase, int position) {
        ChatMessage message = messages.get(position);
        if (holderBase instanceof MessageViewHolder) {
            MessageViewHolder holder = (MessageViewHolder) holderBase;
            holder.messageText.setText(message.getText());
            holder.messageTime.setText(message.getTimestamp());

            // Align bubble
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.bubbleContainer.getLayoutParams();
            params.gravity = (message.getType() == ChatMessage.TYPE_USER) ? Gravity.END : Gravity.START;
            holder.bubbleContainer.setLayoutParams(params);

            // Background and text color
            int bgRes = (message.getType() == ChatMessage.TYPE_USER)
                    ? R.drawable.bg_message_user : R.drawable.bg_message_jarvis;
            holder.messageText.setBackground(ContextCompat.getDrawable(context, bgRes));
            holder.messageText.setTextColor(Color.WHITE);

            // Clear old code block views to prevent duplication
            holder.codeBlockContainer.removeAllViews();

            if (message.extractionResult != null ) {

                CodeBlockExtractor.ExtractionResult result = message.extractionResult;
                String parsed = result.cleanedMessage;
                List<CodeBlockExtractor.CodeBlock> blocks = result.blocks;

                Pattern pattern = Pattern.compile("<<CODE_BLOCK_(\\d+)>>");
                Matcher matcher = pattern.matcher(parsed);
                int lastEnd = 0;

                while (matcher.find()) {
                    int index = Integer.parseInt(matcher.group(1)) - 1;

                    if (matcher.start() > lastEnd) {
                        String beforeText = parsed.substring(lastEnd, matcher.start()).trim();
                        if (!beforeText.isEmpty()) {
                            TextView textView = createMessageTextView(context, holder.codeBlockContainer, beforeText, index);
                            holder.codeBlockContainer.addView(textView);
                        }
                    }

                    if (index >= 0 && index < blocks.size()) {
                        CodeBlockExtractor.CodeBlock cb = blocks.get(index);
                        View editor;
                        try {
                            editor = createCodeEditorView(context, holder.codeBlockContainer, cb.code, cb.language);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        holder.codeBlockContainer.addView(editor);
                    }

                    lastEnd = matcher.end();
                }

                if (lastEnd < parsed.length()) {
                    String afterText = parsed.substring(lastEnd).trim();
                    if (!afterText.isEmpty()) {
                        TextView textView = createMessageTextView(context, holder.codeBlockContainer, afterText, -1);
                        holder.codeBlockContainer.addView(textView);
                    }
                }

                holder.messageText.setVisibility(View.GONE); // Hide main message
            } else {
                holder.messageText.setVisibility(View.VISIBLE); // Show if not extracted
            }

        } else {
            // Typing animation
            View dot1 = holderBase.itemView.findViewById(R.id.dot1);
            View dot2 = holderBase.itemView.findViewById(R.id.dot2);
            View dot3 = holderBase.itemView.findViewById(R.id.dot3);

            animateDot(dot1, 0);
            animateDot(dot2, 200);
            animateDot(dot3, 400);
        }
    }


    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);

    }
    public void codeBlockExtract(){
        int lastIndex = messages.size() - 1;
        ChatMessage message = messages.get(lastIndex);
        CodeBlockExtractor.ExtractionResult extractionResult= CodeBlockExtractor.extractCodeBlocks(message.getText());
        message.extractionResult=extractionResult;
        if(extractionResult!=null){
            notifyItemChanged(lastIndex);
        }
    }
    public void appendToLastAssistantMessage(String token) {
        if (messages.isEmpty()) return;
        int lastIndex = messages.size() - 1;
        ChatMessage lastMessage = messages.get(lastIndex);
        lastMessage.setText(lastMessage.getText() + token);
        notifyItemChanged(lastIndex);
    }

    public void removeTypingIndicator() {
        int i = messages.size() - 1;
            if (messages.get(i).getType() == ChatMessage.TYPE_JARVIS_TYPING) {
                messages.remove(i);
                notifyItemRemoved(i);

        }
    }
    private void animateDot(View dot, long delay) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setDuration(600);
        animator.setStartDelay(delay);
        animator.start();
    }

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }
}
