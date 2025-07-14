package com.example.code_jarvis.chat;

import com.example.code_jarvis.git.GitOperations;
import com.example.code_jarvis.git.GitUiOperationHandler;

import java.io.File;

public class ChatGitOperations {
    public GitUiOperationHandler gitUiOperationHandler;
    public GitOperations gitOperations;

    public ChatGitOperations(GitUiOperationHandler gitUiOperationHandler, GitOperations gitOperations){
        this.gitUiOperationHandler=gitUiOperationHandler;
        this.gitOperations=gitOperations;

    }
    public void addAll(){
        gitUiOperationHandler.runWithUi(callback->{
            gitOperations.callback=callback;
            gitOperations.add(null);
        });
    }
    public void commit(String message){
        gitUiOperationHandler.runWithUi(callback->{
            gitOperations.callback=callback;
            gitOperations.commit(message);
        });
    }
    public void push(){
        gitUiOperationHandler.runWithUi(callback->{
            gitOperations.callback=callback;
            gitOperations.push();
        });
    }
    public void pull(){
        gitUiOperationHandler.runWithUi(callback->{
            gitOperations.callback=callback;
            gitOperations.pull();
        });
    }
}
