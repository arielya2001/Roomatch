package com.example.roomatch.view.fragments;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.roomatch.viewmodel.ViewModelFactoryProvider;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.view.activities.MainActivity;
import com.example.roomatch.viewmodel.AppViewModelFactory;
import com.example.roomatch.viewmodel.CreateProfileViewModel;

import java.util.ArrayList;
import java.util.List;

public class CreateProfileFragment extends Fragment {

    private EditText editFullName, editAge,editDescription;
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

        // שימוש ב-AppViewModelFactory ממקום מרכזי
        AppViewModelFactory factory = ViewModelFactoryProvider.createFactory();
        viewModel = new ViewModelProvider(this, factory).get(CreateProfileViewModel.class);

        // Initialize UI elements
        editFullName = view.findViewById(R.id.editFullName);
        editAge = view.findViewById(R.id.editprofileAge);
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
        editDescription=view.findViewById(R.id.editCreateDescription);


        // Set initial visibility to GONE
        setVisibility(View.GONE);

        saveProfileButton.setOnClickListener(v -> saveProfile());

        userTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (lifestyleLabel == null || interestsLabel == null || lifestyleCheckboxes == null || interestsCheckboxes == null) {
                Log.e("CreateProfileFragment", "One or more views are null");
                return;
            }
            setVisibility(checkedId == R.id.radioSeeker ? View.VISIBLE : View.GONE);
        });

        // Observe Toast messages
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe profile saved status
        viewModel.getProfileSaved().observe(getViewLifecycleOwner(), isSaved -> {
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
        if(editDescription!=null) editDescription.setVisibility(visibility);
    }

    private void saveProfile() {
        String fullName = editFullName.getText().toString().trim();
        String ageStr = editAge.getText().toString().trim();
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        int selectedUserTypeId = userTypeGroup.getCheckedRadioButtonId();

        // Collect lifestyle
        List<String> lifestyleList = new ArrayList<>();
        if (checkboxClean.isChecked()) lifestyleList.add("נקי");
        if (checkboxSmoker.isChecked()) lifestyleList.add("מעשן");
        if (checkboxNightOwl.isChecked()) lifestyleList.add("חיית לילה");
        if (checkboxQuiet.isChecked()) lifestyleList.add("שקט");
        if (checkboxParty.isChecked()) lifestyleList.add("אוהב מסיבות");
        String lifestyle = String.join(", ", lifestyleList);

        // Collect interests
        List<String> interestsList = new ArrayList<>();
        if (checkboxMusic.isChecked()) interestsList.add("מוזיקה");
        if (checkboxSports.isChecked()) interestsList.add("ספורט");
        if (checkboxTravel.isChecked()) interestsList.add("טיולים");
        if (checkboxCooking.isChecked()) interestsList.add("בישול");
        if (checkboxReading.isChecked()) interestsList.add("קריאה");
        String interests = String.join(", ", interestsList);

        String description = editDescription.getText().toString();

        // Create UserProfile
        UserProfile profile = new UserProfile();
        profile.setFullName(fullName);
        profile.setAge(tryParseInt(ageStr) != null ? tryParseInt(ageStr) : 0);
        profile.setGender(getGender(selectedGenderId));
        profile.setLifestyle(lifestyle);
        profile.setInterests(interests);
        profile.setUserType(getUserType(selectedUserTypeId));
        profile.setDescription(description);

        viewModel.saveProfile(profile);
    }

    private Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getGender(int selectedGenderId) {
        if (selectedGenderId == R.id.radioMaleProfile) return "זכר";
        if (selectedGenderId == R.id.radioFemale) return "נקבה";
        if (selectedGenderId == R.id.radioOther) return "אחר";
        return "";
    }

    private String getUserType(int selectedUserTypeId) {
        if (selectedUserTypeId == R.id.radioSeeker) return "seeker";
        if (selectedUserTypeId == R.id.radioOwner) return "owner";
        return "";
    }

    private String getUserType() {
        return getUserType(userTypeGroup.getCheckedRadioButtonId());
    }

    private void navigateToMainActivity(String userType) {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("fragment", userType.equals("owner") ? "owner_apartments" : "menu_apartments");
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}