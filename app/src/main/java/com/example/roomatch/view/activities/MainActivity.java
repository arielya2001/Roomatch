package com.example.roomatch.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.roomatch.R;
import com.example.roomatch.view.fragments.OwnerApartmentsFragment;
import com.example.roomatch.view.fragments.SeekerHomeFragment;
import com.example.roomatch.view.fragments.ProfileFragment;
import com.example.roomatch.view.fragments.CreateProfileFragment;
import com.example.roomatch.view.fragments.OwnerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNav;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(this::onNavigationItemSelected);
        bottomNav.setVisibility(BottomNavigationView.GONE);

        checkUserProfile(currentUser.getUid());
    }

    private void checkUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        auth.signOut();
                        startActivity(new Intent(MainActivity.this, AuthActivity.class));
                        finish();
                        return;
                    }

                    userType = document.getString("userType");

                    if (userType == null || userType.isEmpty()) {
                        replaceFragment(new CreateProfileFragment());
                        return;
                    }

                    setupBottomNav(userType);

                    if ("owner".equals(userType)) {
                        replaceFragment(new OwnerApartmentsFragment()); // ← נקודת פתיחה חדשה!
                    } else {
                        replaceFragment(new SeekerHomeFragment());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "שגיאה בטעינת פרופיל", Toast.LENGTH_SHORT).show();
                    auth.signOut();
                    startActivity(new Intent(MainActivity.this, AuthActivity.class));
                    finish();
                });
    }

    private void setupBottomNav(String userType) {
        bottomNav.getMenu().clear();

        if ("seeker".equals(userType)) {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_seeker);
        } else if ("owner".equals(userType)) {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_owner);
        }

        bottomNav.setVisibility(BottomNavigationView.VISIBLE);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            if ("seeker".equals(userType)) {
                replaceFragment(new SeekerHomeFragment());
            } else {
                replaceFragment(new OwnerApartmentsFragment());
            }
            return true;
        } else if (id == R.id.nav_profile) {
            replaceFragment(new ProfileFragment());
            return true;
        } else if (id == R.id.nav_back) {
            getSupportFragmentManager().popBackStack();
            return true;
        } else if (id == R.id.nav_logout) {
            auth.signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return true;
        } else if (id == R.id.nav_apartments) {
            replaceFragment(new OwnerApartmentsFragment());
            return true;
        } else if (id == R.id.nav_publish_apartment) {
            replaceFragment(new OwnerFragment());
            return true;
        }

        return false;
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null) // כדי שכפתור BACK יעבוד כמו שצריך
                .commit();
    }
}
