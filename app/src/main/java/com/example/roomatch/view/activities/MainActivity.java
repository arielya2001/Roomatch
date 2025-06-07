package com.example.roomatch.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.roomatch.R;
import com.example.roomatch.view.fragments.ApartmentSearchFragment;
import com.example.roomatch.view.fragments.CreateProfileFragment;
import com.example.roomatch.view.fragments.OwnerFragment;
import com.example.roomatch.view.fragments.SeekerHomeFragment;  // ← חדש!
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // ודא שקיים fragmentContainer ב-XML

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish(); // סוגר את MainActivity
        } else {
            checkUserProfile(currentUser.getUid());
        }
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

                    String userType = document.getString("userType");
                    if (userType == null || userType.isEmpty()) {
                        replaceFragment(new CreateProfileFragment());
                    } else if ("owner".equals(userType)) {
                        replaceFragment(new OwnerFragment());
                    } else if ("seeker".equals(userType)) {
                        replaceFragment(new SeekerHomeFragment());
                    } else {
                        replaceFragment(new CreateProfileFragment());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "שגיאה בטעינת פרופיל", Toast.LENGTH_SHORT).show();
                    auth.signOut();
                    startActivity(new Intent(MainActivity.this, AuthActivity.class));
                    finish();
                });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
