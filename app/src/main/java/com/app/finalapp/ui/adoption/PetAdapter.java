package com.app.finalapp.ui.adoption;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.finalapp.Pet;
import com.app.finalapp.R;
import com.app.finalapp.databinding.PetItemBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private final List<Pet> petList;
    private final OnPetClickListener listener;

    public PetAdapter(List<Pet> petList, OnPetClickListener listener) {
        this.petList = petList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        PetItemBinding binding = PetItemBinding.inflate(inflater, parent, false);
        return new PetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);
        holder.bind(pet, listener);
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        private final PetItemBinding binding;

        PetViewHolder(PetItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Pet pet, OnPetClickListener listener) {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.groom_icon) // Placeholder image
                    .error(R.drawable.mustacios_thankyou); // Error image

            if (pet.getImageUrls() != null && !pet.getImageUrls().isEmpty()) {
                Glide.with(binding.petImage.getContext())
                        .load(pet.getImageUrls().get(0))
                        .apply(options)
                        .into(binding.petImage);
            } else {
                binding.petImage.setImageResource(R.drawable.mustacios_thankyou);
            }

            binding.petType.setText(pet.getType());
            binding.petAge.setText(pet.getAge());
            binding.petGender.setText(pet.getGender());

            binding.getRoot().setOnClickListener(view -> {
                if (listener != null) {
                    listener.onPetClick(pet);
                }
            });
        }
    }

    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }

    public void setPets(List<Pet> pets) {
        this.petList.clear();
        this.petList.addAll(pets);
        notifyDataSetChanged();
    }
}
