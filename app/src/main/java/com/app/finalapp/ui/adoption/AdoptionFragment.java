package com.app.finalapp.ui.adoption;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.app.finalapp.NavigationManager;
import com.app.finalapp.R;
import com.app.finalapp.databinding.FragmentAdoptionBinding;
import com.app.finalapp.ui.BaseFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class AdoptionFragment extends BaseFragment {
    private AdoptionViewModel mViewModel;
    private NavController navController;
    private NavigationManager navigationManager;
    private FragmentAdoptionBinding binding;
    private PetAdapter petAdapter;

    public static AdoptionFragment newInstance() {
        return new AdoptionFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdoptionBinding.inflate(inflater, container, false);
        mViewModel = new ViewModelProvider(this).get(AdoptionViewModel.class);
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navigationManager = NavigationManager.getInstance();

        setupUI();
        setupRecyclerView();
        observeViewModel();

        mViewModel.checkUserLoggedIn();
        return binding.getRoot();
    }

    private void setupUI() {
        binding.dogAddButton.setOnClickListener(view -> navController.navigate(R.id.action_nav_adopt_to_nav_pet));
        binding.allButton.setOnClickListener(v -> filterPets("All"));
        binding.dogsButton.setOnClickListener(v -> filterPets("Dog"));
        binding.catsButton.setOnClickListener(v -> filterPets("Cat"));
        binding.bunnyButton.setOnClickListener(v -> filterPets("Bunny"));
        binding.hamsterButton.setOnClickListener(v -> filterPets("Hamster"));
        binding.guineaButton.setOnClickListener(v -> filterPets("Guinea Pig"));
        binding.parrotButton.setOnClickListener(v -> filterPets("Parrot"));
        binding.fishButton.setOnClickListener(v -> filterPets("Fishes"));
        binding.otherButton.setOnClickListener(v -> filterPets("Other"));

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mViewModel.searchPets(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mViewModel.searchPets(newText);
                return true;
            }
        });
    }

    private void filterPets(String type) {
        mViewModel.filterPetsByType(type);
        binding.searchView.setQuery("", false);
    }

    private void setupRecyclerView() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        petAdapter = new PetAdapter(new ArrayList<>(), pet -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("pet", pet);
            navController.navigate(R.id.action_nav_adopt_to_petDetailFragment, bundle);
        });
        binding.recyclerView.setAdapter(petAdapter);
    }

    private void observeViewModel() {
        mViewModel.getPets().observe(getViewLifecycleOwner(), pets -> {
            if (pets != null) {
                petAdapter.setPets(pets);
            }
        });

        mViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        mViewModel.isUserLoggedIn().observe(getViewLifecycleOwner(), isLoggedIn -> {
            if (!isLoggedIn) {
                Bundle args = new Bundle();
                args.putInt("targetFragmentId", R.id.nav_adopt);
                navController.navigate(R.id.nav_login, args);
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navigationManager.pushFragmentId(R.id.nav_adopt);
    }
}
