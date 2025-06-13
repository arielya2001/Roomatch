package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomatch.R;
import com.example.roomatch.viewmodel.ProfileViewModel;

import java.util.Map;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private TextView textName, textAge, textGender, textLifestyle, textInterests;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // חיבור views
        textName = view.findViewById(R.id.textProfileName);
        textAge = view.findViewById(R.id.textProfileAge);
        textGender = view.findViewById(R.id.textProfileGender);
        textLifestyle = view.findViewById(R.id.textProfileLifestyle);
        textInterests = view.findViewById(R.id.textProfileInterests);

        Button updateProfileButton = view.findViewById(R.id.buttonUpdateProfile);
        updateProfileButton.setOnClickListener(v -> viewModel.requestEditProfile());

        // צפייה בפרופיל
        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                textName.setText("שם: " + safe(profile.get("fullName")));
                textAge.setText("גיל: " + safe(profile.get("age")));
                textGender.setText("מגדר: " + safe(profile.get("gender")));
                textLifestyle.setText("סגנון חיים: " + safe(profile.get("lifestyle")));
                textInterests.setText("תחומי עניין: " + safe(profile.get("interests")));
            }
        });

        // צפייה בטוסטים
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        // פתיחת דיאלוג עריכת פרופיל
        viewModel.getEditRequested().observe(getViewLifecycleOwner(), shouldEdit -> {
            if (shouldEdit != null && shouldEdit) {
                showEditProfileDialog();
                viewModel.resetEditRequest(); // קריאה לשיטה ציבורית לאיפוס
            }
        });

        // טעינת נתונים
        viewModel.loadProfile();
    }

    private String safe(Object value) {
        return value != null ? value.toString() : "לא זמין";
    }

    private void showEditProfileDialog() {
        Map<String, Object> current = viewModel.getProfile().getValue();
        if (current == null) {
            Toast.makeText(getContext(), "לא ניתן לערוך פרופיל ריק", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        EditText editFullName = dialogView.findViewById(R.id.editFullName);
        EditText editAge = dialogView.findViewById(R.id.editAge);
        EditText editGender = dialogView.findViewById(R.id.editGender);
        EditText editLifestyle = dialogView.findViewById(R.id.editLifestyle);
        EditText editInterests = dialogView.findViewById(R.id.editInterests);

        editFullName.setText(safe(current.get("fullName")));
        editAge.setText(safe(current.get("age")));
        editGender.setText(safe(current.get("gender")));
        editLifestyle.setText(safe(current.get("lifestyle")));
        editInterests.setText(safe(current.get("interests")));

        builder.setTitle("עדכון פרטים אישיים")
                .setPositiveButton("שמור", (dialog, which) -> {
                    viewModel.updateProfile(
                            editFullName.getText().toString().trim(),
                            editAge.getText().toString().trim(),
                            editGender.getText().toString().trim(),
                            editLifestyle.getText().toString().trim(),
                            editInterests.getText().toString().trim()
                    );
                })
                .setNegativeButton("ביטול", null)
                .create()
                .show();
    }
}