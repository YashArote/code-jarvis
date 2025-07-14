package com.example.code_jarvis.indexing;

import org.json.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
public class CodeIndexer {
    static {

        System.loadLibrary("TreeSitterJNI");
    }

    // Declare native functions
    public static native String parseFile(String code, String language);

    // Java method to parse multiple files
    public static JSONObject parseDirectory(File root) {
        JSONObject projectSymbols = new JSONObject();
        Queue<File> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            File current = queue.poll();
            if (current.isDirectory()) {
                Collections.addAll(queue, Objects.requireNonNull(current.listFiles()));
            } else if (current.getName().endsWith(".py") || current.getName().endsWith(".java")) {
                try {
                    String code = new String(Files.readAllBytes(current.toPath()), StandardCharsets.UTF_8);
                    String lang = current.getName().endsWith(".py") ? "python" : "java";
                    String json = parseFile(code, lang);

                    JSONArray symbolArray = new JSONArray(json);
                    JSONObject fileMap = new JSONObject();

                    for (int i = 0; i < symbolArray.length(); i++) {
                        JSONObject symbol = symbolArray.getJSONObject(i);
                        //&&symbol.getString("type").endsWith("_definition")
                        if (symbol.has("name")&&(symbol.getString("type").endsWith("_definition")||symbol.getString("type").endsWith("_declaration"))) {
                            String name = symbol.getString("name");
                            JSONObject symbolDetails = new JSONObject();
                            symbolDetails.put("type", symbol.getString("type"));
                            symbolDetails.put("startLine", symbol.getInt("startLine"));
                            symbolDetails.put("endLine", symbol.getInt("endLine"));
                            fileMap.put(name, symbolDetails);
                        }
                    }

                    projectSymbols.put(current.getName(), fileMap);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return projectSymbols;
    }

}