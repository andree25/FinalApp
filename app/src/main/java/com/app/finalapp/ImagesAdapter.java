package com.app.finalapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ImagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Uri> imagesList;
    private Context context;
    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_EMPTY = 1;

    public ImagesAdapter(Context context, List<Uri> imagesList) {
        this.context = context;
        this.imagesList = imagesList;
    }

    @Override
    public int getItemViewType(int position) {
        return imagesList.isEmpty() ? VIEW_TYPE_EMPTY : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
            return new ImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_placeholder, parent, false);
            return new EmptyViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ImageViewHolder) {
            Uri imageUri = imagesList.get(position);
            Glide.with(context).load(imageUri).into(((ImageViewHolder) holder).imageView);

            ((ImageViewHolder) holder).deleteImage.setOnClickListener(v -> {
                imagesList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, imagesList.size());
            });
        } else if (holder instanceof EmptyViewHolder) {
            Log.d("ImagesAdapter", "Displaying empty view placeholder.");
            ((EmptyViewHolder) holder).textView.setText(context.getString(R.string.image_hint));
        }
    }

    @Override
    public int getItemCount() {
        return imagesList.isEmpty() ? 1 : imagesList.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView deleteImage;

        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            deleteImage = itemView.findViewById(R.id.delete_image);
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        EmptyViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.placeholder_text);
        }
    }
}
