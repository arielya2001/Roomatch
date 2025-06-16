// MainActivity.java
package com.example.roomatch.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.roomatch.R;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.view.fragments.ApartmentSearchFragment;
import com.example.roomatch.view.fragments.ChatsFragment;
import com.example.roomatch.view.fragments.OwnerApartmentsFragment;
import com.example.roomatch.view.fragments.PartnerFragment;
import com.example.roomatch.view.fragments.SeekerHomeFragment;
import com.example.roomatch.view.fragments.ProfileFragment;
import com.example.roomatch.view.fragments.CreateProfileFragment;
import com.example.roomatch.view.fragments.OwnerFragment;
import com.example.roomatch.view.fragments.SeekerMainFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNav;
    private String userType;
    private ApartmentRepository apartmentRepository; // משתנה עבור ה-Repository

    public static boolean isTestMode = false; // פה


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottom_navigation); // ← מוקדם יותר
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        apartmentRepository = new ApartmentRepository(MainActivity.isTestMode); // ברירת מחדל

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (isTestMode) {
                userType = "owner"; // או "seeker" לפי הצורך
                setupBottomNav(userType);

                String initialFragment = getIntent().getStringExtra("fragment");
                if ("seeker_home".equals(initialFragment)) {
                    replaceFragment(new SeekerHomeFragment());
                } else {
                    replaceFragment(new OwnerApartmentsFragment());
                }
            } else {
                startActivity(new Intent(this, AuthActivity.class));
                finish();
            }
            return;
        }


        bottomNav.setOnItemSelectedListener(this::onNavigationItemSelected);
        bottomNav.setVisibility(BottomNavigationView.GONE);

        // בדיקה אם יש פרמטר מ-CreateProfileFragment
        String initialFragment = getIntent().getStringExtra("fragment");

        if (initialFragment != null) {
            switch (initialFragment) {
                case "owner_apartments":
                    replaceFragment(new OwnerApartmentsFragment());
                    setupBottomNav("owner");
                    break;
                case "seeker_home":
                    replaceFragment(new SeekerHomeFragment());
                    setupBottomNav("seeker");
                    break;
                case "create_profile":
                    replaceFragment(new CreateProfileFragment());
                    break;
                case "menu_apartments":
                    replaceFragment(new ApartmentSearchFragment());
                    setupBottomNav("seeker");
                    break;
            }
        } else {
            checkUserProfile(currentUser.getUid());
        }
    }

    private void checkUserProfile(String uid) {
        Log.d("MainActivity", "Checking user profile for uid: " + uid);
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        auth.signOut();
                        startActivity(new Intent(MainActivity.this, AuthActivity.class));
                        finish();
                        return;
                    }

                    userType = document.getString("userType");
                    Log.d("MainActivity", "User type: " + userType);

                    if (userType == null || userType.isEmpty()) {
                        replaceFragment(new CreateProfileFragment());
                        return;
                    }

                    setupBottomNav(userType);

                    if ("owner".equals(userType)) {
                        replaceFragment(new OwnerApartmentsFragment());
                    } else {
                        replaceFragment(new ApartmentSearchFragment());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error loading profile: " + e.getMessage());
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
        if (id == R.id.nav_profile) {
            replaceFragment(new ProfileFragment());
            return true;
        } else if (id == R.id.nav_logout) {
            auth.signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return true;
        } else if (id == R.id.nav_apartments) {
            replaceFragment(new OwnerApartmentsFragment());
            return true;
        } else if (id == R.id.nav_chats) {
            replaceFragment(new ChatsFragment());
            return true;
        } else if (id == R.id.menu_apartments) {
            replaceFragment(new ApartmentSearchFragment());
            return true;
        } else if (id == R.id.menu_partners) {
            replaceFragment(new PartnerFragment());
            return true;
        }
        return false;
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}