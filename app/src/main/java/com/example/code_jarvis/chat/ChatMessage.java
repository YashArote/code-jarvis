package com.example.code_jarvis.chat;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_JARVIS = 1;
    public static final int TYPE_JARVIS_TYPING = 2;

    private String text;
    private final int type;
    private final String timestamp;
    public CodeBlockExtractor.ExtractionResult extractionResult;

    public ChatMessage(String text, int type, String timestamp) {
        this.text = text;
        this.type = type;
        this.timestamp = timestamp;
    }
    public void setText(String text){
        this.text=text;
    }
    public String getText() { return text; }

    public int getType() { return type; }

    public String getTimestamp() { return timestamp; }
}
