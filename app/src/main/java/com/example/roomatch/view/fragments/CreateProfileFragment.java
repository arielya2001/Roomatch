package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.roomatch.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateProfileFragment extends Fragment {

    private EditText editFullName, editAge, editGender, editLifestyle, editInterests;
    private RadioGroup userTypeGroup;
    private Button saveProfileButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public CreateProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editFullName = view.findViewById(R.id.editFullName);
        editAge = view.findViewById(R.id.editAge);
        editGender = view.findViewById(R.id.editGender);
        editLifestyle = view.findViewById(R.id.editLifestyle);
        editInterests = view.findViewById(R.id.editInterests);
        userTypeGroup = view.findViewById(R.id.userTypeGroup);
        saveProfileButton = view.findViewById(R.id.buttonSaveProfile);

        saveProfileButton.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        String fullName = editFullName.getText().toString().trim();
        String ageStr = editAge.getText().toString().trim();
        String gender = editGender.getText().toString().trim();
        String lifestyle = editLifestyle.getText().toString().trim();
        String interests = editInterests.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(ageStr)) {
            Toast.makeText(getContext(), "אנא מלא את כל השדות החובה", Toast.LENGTH_SHORT).show();
            return;
        }

        // קבלת סוג המשתמש מה-RadioGroup
        int selectedId = userTypeGroup.getCheckedRadioButtonId();
        String userType;

        if (selectedId == R.id.radioSeeker) {
            userType = "seeker";
        } else if (selectedId == R.id.radioOwner) {
            userType = "owner";
        } else {
            Toast.makeText(getContext(), "בחר סוג משתמש", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", fullName);
        profile.put("age", tryParseInt(ageStr));
        profile.put("gender", gender);
        profile.put("lifestyle", lifestyle);
        profile.put("interests", interests);
        profile.put("userType", userType);

        db.collection("users").document(uid)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "הפרופיל נשמר בהצלחה!", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "שגיאה בשמירת הפרופיל: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
