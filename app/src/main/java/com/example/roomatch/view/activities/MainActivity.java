package com.example.roomatch.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.roomatch.R;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.view.fragments.ApartmentSearchFragment;
import com.example.roomatch.view.fragments.ChatsFragment;
import com.example.roomatch.view.fragments.ContactsFragment;
import com.example.roomatch.view.fragments.CreateProfileFragment;
import com.example.roomatch.view.fragments.GroupChatFragment;
import com.example.roomatch.view.fragments.GroupChatsListFragment;
import com.example.roomatch.view.fragments.MatchRequestsFragment;
import com.example.roomatch.view.fragments.OwnerApartmentsFragment;
import com.example.roomatch.view.fragments.PartnerFragment;
import com.example.roomatch.view.fragments.ProfileFragment;
import com.example.roomatch.view.fragments.SeekerHomeFragment;
import com.example.roomatch.view.fragments.SharedGroupsFragment;
import com.google.android.libraries.places.api.Places;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNav;
    private String userType;
    private ApartmentRepository apartmentRepository;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // הפעלת Google Places API אם טרם הופעל
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key), Locale.getDefault());
        }

        bottomNav = findViewById(R.id.bottom_navigation);
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        apartmentRepository = new ApartmentRepository();

        navigationView.setNavigationItemSelectedListener(this);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        bottomNav.setOnItemSelectedListener(this::onBottomNavItemSelected);
        bottomNav.setVisibility(BottomNavigationView.GONE);

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


    // הסרת onCreateOptionsMenu ו-onOptionsItemSelected כי אין צורך ב-Toolbar

    private void checkUserProfile(String uid) {
        Log.d("MainActivity", "Checking user profile for uid: " + uid);
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Log.e("MainActivity", "User profile does not exist for uid: " + uid);
                        auth.signOut();
                        startActivity(new Intent(MainActivity.this, AuthActivity.class));
                        finish();
                        return;
                    }

                    UserProfile userProfile = document.toObject(UserProfile.class);
                    if (userProfile == null) {
                        Log.e("MainActivity", "Failed to parse user profile for uid: " + uid);
                        auth.signOut();
                        startActivity(new Intent(MainActivity.this, AuthActivity.class));
                        finish();
                        return;
                    }

                    userType = userProfile.getUserType();
                    Log.d("MainActivity", "User type: " + userType);

                    if ("owner".equals(userType)) {
                        toolbar.setVisibility(Toolbar.GONE); // הסתרת ה־Toolbar מה-Activity
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    } else if ("seeker".equals(userType)) {
                        setSupportActionBar(toolbar); // שימוש בתפריט
                        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                        drawerLayout.addDrawerListener(drawerToggle);
                        drawerToggle.syncState();
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    }


                    if (userType == null || userType.isEmpty()) {
                        replaceFragment(new CreateProfileFragment());
                        return;
                    }

                    setupBottomNav(userType);

                    if ("owner".equals(userType)) {
                        replaceFragment(new OwnerApartmentsFragment());
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    } else if ("seeker".equals(userType)) {
                        replaceFragment(new ApartmentSearchFragment());
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error loading profile: " + e.getMessage(), e);
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
            // הסרת "התנתקות" מהסרגל התחתון אם קיימת
            bottomNav.getMenu().removeItem(R.id.nav_logout);
        } else if ("owner".equals(userType)) {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_owner);
        }

        bottomNav.setVisibility(BottomNavigationView.VISIBLE);
    }

    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile) {
            replaceFragment(new ProfileFragment());
            return true;
        } else if (id == R.id.nav_apartments) {
            replaceFragment(new OwnerApartmentsFragment());
            return true;
        } else if (id == R.id.nav_chats) {
            replaceFragment(new ChatsFragment());
            return true;
        } else if (id == R.id.nav_group_chats) {
            replaceFragment(new GroupChatsListFragment()); // שינוי לרשימת צ'אטים
            return true;
        } else if (id == R.id.nav_logout) {
            auth.signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_contacts) {
            replaceFragment(new ContactsFragment());
        } else if (id == R.id.nav_shared_groups) {
            replaceFragment(new SharedGroupsFragment());
        } else if (id == R.id.nav_match_requests) {
            replaceFragment(new MatchRequestsFragment());
        } else if (id == R.id.nav_group_chats) {
            replaceFragment(new GroupChatsListFragment()); // שינוי גם כאן
        } else if (id == R.id.nav_logout) {
            auth.signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return true;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (drawerToggle != null) {
            drawerLayout.removeDrawerListener(drawerToggle);
        }
    }

}