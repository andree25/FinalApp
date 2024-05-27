package com.app.finalapp.ui.home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.finalapp.R;
import com.app.finalapp.ui.home.SearchResponse;

import java.util.ArrayList;
import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    private Context context;
    private List<SearchResponse.Item> articles;

    public ArticleAdapter(Context context) {
        this.context = context;
        this.articles = new ArrayList<>();
    }

    public void updateArticles(List<SearchResponse.Item> newArticles) {
        this.articles.clear();
        this.articles.addAll(newArticles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        SearchResponse.Item article = articles.get(position);
        holder.titleTextView.setText(article.getTitle());
        holder.snippetTextView.setText(article.getSnippet());
        holder.itemView.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getLink()));
            context.startActivity(browserIntent);
        });
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView snippetTextView;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.article_title);
            snippetTextView = itemView.findViewById(R.id.article_snippet);
        }
    }
}
