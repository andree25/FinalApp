package com.app.finalapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private Context context;
    private List<Pet> petList;
    private OnPetClickListener listener;

    public PetAdapter(Context context, List<Pet> petList, OnPetClickListener listener) {
        this.context = context;
        this.petList = petList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.pet_item, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);
        // Check if imageUrls is not null and not empty
        if (pet.getImageUrls() != null && !pet.getImageUrls().isEmpty()) {
            Glide.with(context).load(pet.getImageUrls().get(0)).into(holder.petImage);
        } else {
            holder.petImage.setImageResource(R.drawable.mustacios_thankyou); // default_image is a placeholder in your drawable resources.
        }
        holder.petType.setText(pet.getType());
        holder.petAge.setText(pet.getAge());
        holder.petGender.setText(pet.getGender());
        holder.petDescription.setText(pet.getDescription());

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onPetClick(pet);
            }
        });
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        ImageView petImage;
        TextView petType, petAge, petGender, petDescription;

        PetViewHolder(View itemView) {
            super(itemView);
            petImage = itemView.findViewById(R.id.pet_image);
            petType = itemView.findViewById(R.id.pet_type);
            petAge = itemView.findViewById(R.id.pet_age);
            petGender = itemView.findViewById(R.id.pet_gender);
            petDescription = itemView.findViewById(R.id.pet_description);
        }
    }

    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }
}

