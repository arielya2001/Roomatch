package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomatch.R;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.viewmodel.ProfileViewModel;

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        textName = view.findViewById(R.id.textProfileName);
        textAge = view.findViewById(R.id.textProfileAge);
        textGender = view.findViewById(R.id.textProfileGender);
        textLifestyle = view.findViewById(R.id.textProfileLifestyle);
        textInterests = view.findViewById(R.id.textProfileInterests);

        LinearLayout layoutLifestyleAndInterests = view.findViewById(R.id.layoutLifestyleAndInterests);
        Button updateProfileButton = view.findViewById(R.id.buttonUpdateProfile);

        updateProfileButton.setOnClickListener(v -> viewModel.requestEditProfile());

        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                textName.setText("שם: " + safe(profile.getFullName()));
                textAge.setText("גיל: " + profile.getAge());
                textGender.setText("מגדר: " + safe(profile.getGender()));

                if ("owner".equals(profile.getUserType())) {
                    layoutLifestyleAndInterests.setVisibility(View.GONE);
                } else {
                    layoutLifestyleAndInterests.setVisibility(View.VISIBLE);
                    textLifestyle.setText("סגנון חיים: " + safe(profile.getLifestyle()));
                    textInterests.setText("תחומי עניין: " + safe(profile.getInterests()));
                }
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getEditRequested().observe(getViewLifecycleOwner(), shouldEdit -> {
            if (shouldEdit != null && shouldEdit) {
                showEditProfileDialog();
                viewModel.resetEditRequest();
            }
        });

        viewModel.loadProfile();
    }


    private String safe(String value) {
        return value != null ? value : "לא זמין";
    }

    private void showEditProfileDialog() {
        UserProfile current = viewModel.getProfile().getValue();
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
        LinearLayout layoutLifestyleAndInterests = dialogView.findViewById(R.id.layoutLifestyleAndInterests);


        // הזנת ערכים
        editFullName.setText(safe(current.getFullName()));
        editAge.setText(String.valueOf(current.getAge()));
        editGender.setText(safe(current.getGender()));
        editLifestyle.setText(safe(current.getLifestyle()));
        editInterests.setText(safe(current.getInterests()));

        boolean isOwner = viewModel.isCurrentUserOwner(); // ← ודא שיש מתודה כזו ב־ViewModel

        // הסתרת שדות אם המשתמש בעל דירה
        if (isOwner) {
            editLifestyle.setVisibility(View.GONE);
            editInterests.setVisibility(View.GONE);
        }

        if (isOwner) {
            layoutLifestyleAndInterests.setVisibility(View.GONE);
        } else {
            layoutLifestyleAndInterests.setVisibility(View.VISIBLE);
        }


        builder.setTitle("עדכון פרטים אישיים")
                .setPositiveButton("שמור", (dialog, which) -> {
                    String ageStr = editAge.getText().toString().trim();

                    String lifestyle = isOwner ? null : editLifestyle.getText().toString().trim();
                    String interests = isOwner ? null : editInterests.getText().toString().trim();

                    viewModel.updateProfile(
                            editFullName.getText().toString().trim(),
                            ageStr.isEmpty() ? "0" : ageStr,
                            editGender.getText().toString().trim(),
                            lifestyle,
                            interests
                    );
                })
                .setNegativeButton("ביטול", null)
                .create()
                .show();
    }

}