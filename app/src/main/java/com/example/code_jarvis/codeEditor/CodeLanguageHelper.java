package com.example.code_jarvis.codeEditor;

import java.io.File;
import java.util.Locale;

import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.*;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition;

public class CodeLanguageHelper {

    public static GrammarRegistry grammarRegistry = GrammarRegistry.getInstance();

    ;
    public static ThemeRegistry themeRegistry = ThemeRegistry.getInstance();


    public static TextMateLanguage getLanguageForExtension(String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        String scope = "source." + getScopeName("."+filename);

        return TextMateLanguage.create(
                scope,
                grammarRegistry,
                themeRegistry,
                true // disable debug mode
        );
    }

    private static String getScopeName(String filename) {

        if (filename.endsWith(".java")) return "java";
        if (filename.endsWith(".kt")||filename.endsWith("kotlin")) return "kotlin";
        if (filename.endsWith(".py")|| filename.endsWith("python") ) return "python";
        if (filename.endsWith(".js")|| filename.endsWith("javascript")) return "js";
        if (filename.endsWith(".ts")|| filename.endsWith("typescript")) return "ts";
        if (filename.endsWith(".cpp") || filename.endsWith(".cc") || filename.endsWith(".cxx")) return "cpp";
        if (filename.endsWith(".c")) return "c";
        if (filename.endsWith(".cs")|| filename.endsWith("csharp")) return "cs";
        if (filename.endsWith(".php")) return "php";
        if (filename.endsWith(".swift")) return "swift";
        if (filename.endsWith(".go")) return "go";
        if (filename.endsWith(".rs")) return "rust";
        if (filename.endsWith(".html")) return "html";
        if (filename.endsWith(".css")) return "css";
        if (filename.endsWith(".xml")) return "xml";
        if (filename.endsWith(".json")) return "json";


        return "plain";
    }


}
