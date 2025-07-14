package com.example.code_jarvis.model;

public class Repo {
    private String name;
    private String url; // clone_url

    public Repo(String name, String url) {
        this.name = name;
        this.url = url;
    }
    public String getName() { return name; }
    public String getUrl() { return url; }
}
