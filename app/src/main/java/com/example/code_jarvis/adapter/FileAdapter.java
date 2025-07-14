package com.example.code_jarvis.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.code_jarvis.ChatActivity;
import com.example.code_jarvis.R;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    List<File> files;
    File githubDir;
    Context context;
    public onFileClickListener clickListener;
    private LinkedList<File> directoryHistory = new LinkedList<>();
    public interface onFileClickListener{
        void onFileClicked(File file);
    }
    private Runnable hideUndoButton;
    public FileAdapter(File githubDir,List<File> files, Context context,onFileClickListener clickListener, Runnable hideUndoButton) {
        this.githubDir=githubDir;
        this.files = files;
        this.context = context;
        this.clickListener=clickListener;
        this.hideUndoButton=hideUndoButton;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView filename;
        ImageView icon;

        public ViewHolder(View itemView) {
            super(itemView);
            filename = itemView.findViewById(R.id.filename);
            icon = itemView.findViewById(R.id.icon);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.file_item, parent,false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        File file = files.get(position);
        holder.filename.setText(file.getName());
        holder.icon.setImageResource(file.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_file);

        holder.itemView.setOnClickListener(v -> {
            clickListener.onFileClicked(file);
        });
    }
    public void reloadFiles(List<File> files,File parentFile){
        this.files=files;
        directoryHistory.addLast(parentFile);
        notifyDataSetChanged();
    }

    public void goToPevious(){
        File currentFile= directoryHistory.removeLast();
        if( currentFile.getParentFile().getPath().equals(githubDir.getPath())) hideUndoButton.run();
        this.files=ChatActivity.listFiles( currentFile.getParentFile());
        notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
        return files.size();
    }

}

