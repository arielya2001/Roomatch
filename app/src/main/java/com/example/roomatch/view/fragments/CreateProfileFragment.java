package com.example.roomatch.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomatch.R;
import com.example.roomatch.view.activities.MainActivity;
import com.example.roomatch.viewmodel.CreateProfileViewModel;
import com.example.roomatch.viewmodel.ProfileViewModel;

import java.util.ArrayList;
import java.util.List;

public class CreateProfileFragment extends Fragment {

    private EditText editFullName, editAge;
    private RadioGroup userTypeGroup, genderGroup;
    private Button saveProfileButton;
    private CheckBox checkboxClean, checkboxSmoker, checkboxNightOwl, checkboxQuiet, checkboxParty;
    private CheckBox checkboxMusic, checkboxSports, checkboxTravel, checkboxCooking, checkboxReading;
    private TextView lifestyleLabel, interestsLabel;
    private LinearLayout lifestyleCheckboxes, interestsCheckboxes;

    private CreateProfileViewModel viewModel;


    public CreateProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CreateProfileViewModel.class);   // <-- שונה

        editFullName = view.findViewById(R.id.editFullName);
        editAge = view.findViewById(R.id.editAge);
        genderGroup = view.findViewById(R.id.genderGroup);
        checkboxClean = view.findViewById(R.id.checkboxClean);
        checkboxSmoker = view.findViewById(R.id.checkboxSmoker);
        checkboxNightOwl = view.findViewById(R.id.checkboxNightOwl);
        checkboxQuiet = view.findViewById(R.id.checkboxQuiet);
        checkboxParty = view.findViewById(R.id.checkboxParty);
        checkboxMusic = view.findViewById(R.id.checkboxMusic);
        checkboxSports = view.findViewById(R.id.checkboxSports);
        checkboxTravel = view.findViewById(R.id.checkboxTravel);
        checkboxCooking = view.findViewById(R.id.checkboxCooking);
        checkboxReading = view.findViewById(R.id.checkboxReading);
        userTypeGroup = view.findViewById(R.id.userTypeGroup);
        saveProfileButton = view.findViewById(R.id.buttonSaveProfile);
        lifestyleLabel = view.findViewById(R.id.lifestyleLabel);
        interestsLabel = view.findViewById(R.id.interestsLabel);
        lifestyleCheckboxes = view.findViewById(R.id.lifestyleCheckboxes);
        interestsCheckboxes = view.findViewById(R.id.interestsCheckboxes);

        // Set initial visibility to GONE
        setVisibility(View.GONE);

        saveProfileButton.setOnClickListener(v -> saveProfile());

        userTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (lifestyleLabel == null || interestsLabel == null || lifestyleCheckboxes == null || interestsCheckboxes == null) {
                Log.e("CreateProfileFragment", "One or more views are null");
                return;
            }
            if (checkedId == R.id.radioSeeker) {
                setVisibility(View.VISIBLE);
            } else {
                setVisibility(View.GONE);
            }
        });

        // צפייה בהודעות Toast
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getProfileSaved().observe(getViewLifecycleOwner(), isSaved -> {   // <-- שם חדש
            if (Boolean.TRUE.equals(isSaved)) {
                navigateToMainActivity(getUserType());
            }
        });
    }

    private void setVisibility(int visibility) {
        if (lifestyleLabel != null) lifestyleLabel.setVisibility(visibility);
        if (interestsLabel != null) interestsLabel.setVisibility(visibility);
        if (lifestyleCheckboxes != null) lifestyleCheckboxes.setVisibility(visibility);
        if (interestsCheckboxes != null) interestsCheckboxes.setVisibility(visibility);
    }

    private void saveProfile() {
        String fullName = editFullName.getText().toString().trim();
        String ageStr = editAge.getText().toString().trim();
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        int selectedUserTypeId = userTypeGroup.getCheckedRadioButtonId();

        viewModel.saveProfile(
                fullName,
                ageStr,
                selectedGenderId,
                checkboxClean.isChecked(),
                checkboxSmoker.isChecked(),
                checkboxNightOwl.isChecked(),
                checkboxQuiet.isChecked(),
                checkboxParty.isChecked(),
                checkboxMusic.isChecked(),
                checkboxSports.isChecked(),
                checkboxTravel.isChecked(),
                checkboxCooking.isChecked(),
                checkboxReading.isChecked(),
                selectedUserTypeId
        );
    }

    private String getUserType() {
        int selectedId = userTypeGroup.getCheckedRadioButtonId();
        return selectedId == R.id.radioSeeker ? "seeker" : "owner";
    }

    private void navigateToMainActivity(String userType) {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        if (userType.equals("owner")) {
            intent.putExtra("fragment", "owner_apartments");
        } else {
            intent.putExtra("fragment", "menu_apartments");
        }
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}