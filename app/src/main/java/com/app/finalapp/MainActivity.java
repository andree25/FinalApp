package com.app.finalapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.app.finalapp.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        mAuth = FirebaseAuth.getInstance();
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_adopt, R.id.nav_chat, R.id.nav_donate, R.id.nav_groom, R.id.nav_shelter, R.id.nav_vet)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_login) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setDisplayShowHomeEnabled(false);
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        });

        updateNavigationHeader();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            mAuth.signOut();
            updateNavigationHeader();
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_home);
            return true;
        } else if (id == R.id.action_admin) {
            navigateToAdminLogin();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void navigateToAdminLogin() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.nav_login);
        DrawerLayout drawer = binding.drawerLayout;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    private void updateNavigationHeader() {
        FirebaseUser user = mAuth.getCurrentUser();
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView navUsername = headerView.findViewById(R.id.textView_name_navigation_header);
        TextView navEmail = headerView.findViewById(R.id.textView_email_navigation_header);
        ImageView navImageView = headerView.findViewById(R.id.imageView_navigation_header);

        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        String email = user.getEmail(); // Assuming email is not stored in Realtime Database
                        String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                        navUsername.setText(name);
                        navEmail.setText(email);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(MainActivity.this).load(imageUrl).into(navImageView);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(MainActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Reset to default text and image when not logged in or user data is not available
            navUsername.setText(getString(R.string.nav_header_title));
            navEmail.setText(getString(R.string.nav_header_subtitle));
            navImageView.setImageResource(R.drawable.groom_icon);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavigationHeader(); // Ensure the user information is updated every time the user returns to the app
    }
}
