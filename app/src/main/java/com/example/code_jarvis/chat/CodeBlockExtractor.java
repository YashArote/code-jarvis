package com.example.code_jarvis.chat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeBlockExtractor {

    public static class CodeBlock {
        public final String code;
        public final String language;

        public CodeBlock(String code, String language) {
            this.code = code;
            this.language = language;
        }
    }

    public static class ExtractionResult {
        public String cleanedMessage;
        public List<CodeBlock> blocks;

        public ExtractionResult(String cleanedMessage, List<CodeBlock> blocks) {
            this.cleanedMessage = cleanedMessage;
            this.blocks = blocks;
        }
    }

    public static ExtractionResult extractCodeBlocks(String message) {
        if (message == null || !message.contains("```")) {
            return null;
        }

        List<CodeBlock> blocks = new ArrayList<>();
        StringBuilder cleaned = new StringBuilder();
        int codeIndex = 0;

        Pattern pattern = Pattern.compile("(?s)```([a-zA-Z0-9#+\\-_.]*)\\n(.*?)```");
        Matcher matcher = pattern.matcher(message);

        int lastEnd = 0;

        while (matcher.find()) {
            // Append text before the current code block
            cleaned.append(message, lastEnd, matcher.start());

            String lang = matcher.group(1).trim();
            String code = matcher.group(2).trim();

            if (lang.isEmpty()) lang = "plain";

            blocks.add(new CodeBlock(code, lang));
            cleaned.append("<<CODE_BLOCK_").append(++codeIndex).append(">>");

            lastEnd = matcher.end();
        }

        // Append remaining text after last match
        if (lastEnd < message.length()) {
            cleaned.append(message.substring(lastEnd));
        }

        return blocks.isEmpty() ? null : new ExtractionResult(cleaned.toString().trim(), blocks);
    }
}
