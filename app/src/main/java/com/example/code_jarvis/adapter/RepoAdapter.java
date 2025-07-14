package com.example.code_jarvis.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.code_jarvis.R;
import com.example.code_jarvis.model.Repo;

import java.util.List;

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.RepoViewHolder> {
    public List<Repo> repoList;
    private OnDownloadClickListener listener;
    public boolean isDownload=false;

    public interface OnDownloadClickListener {
        void onDownloadClick(Repo repo);
    }

    public RepoAdapter(List<Repo> repoList, OnDownloadClickListener listener) {
        this.repoList = repoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RepoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.repo_card, parent, false);
        if(isDownload) {
            ImageButton imageButton = view.findViewById(R.id.download_icon);
            imageButton.setImageResource(R.drawable.ic_open);
        }
        return new RepoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RepoViewHolder holder, int position) {
        Repo repo = repoList.get(position);
        holder.repoName.setText(repo.getName());
        holder.downloadIcon.setOnClickListener(v -> listener.onDownloadClick(repo));
    }

    @Override
    public int getItemCount() {
        return repoList.size();
    }

    static class RepoViewHolder extends RecyclerView.ViewHolder {
        TextView repoName;
        ImageButton downloadIcon;

        RepoViewHolder(View itemView) {
            super(itemView);
            repoName = itemView.findViewById(R.id.repo_name);
            downloadIcon = itemView.findViewById(R.id.download_icon);
        }
    }
}
