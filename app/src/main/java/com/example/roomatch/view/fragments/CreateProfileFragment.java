package com.example.roomatch.view.fragments;

//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.*;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentContainerView;
//import androidx.lifecycle.ViewModelProvider;
//
//import com.example.roomatch.R;
//import com.example.roomatch.viewmodel.ViewModelFactoryProvider;
//import com.example.roomatch.model.UserProfile;
//import com.example.roomatch.view.activities.MainActivity;
//import com.example.roomatch.viewmodel.AppViewModelFactory;
//import com.example.roomatch.viewmodel.CreateProfileViewModel;
//import com.google.firebase.firestore.FieldValue;
//
//import java.util.ArrayList;
//import java.util.List;

import android.content.Intent;
import android.net.Uri; // For image URI
import android.os.Bundle;
import android.provider.MediaStore; // For image picker
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView; // << ADD THIS
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // << ADD THIS
import androidx.activity.result.contract.ActivityResultContracts; // << ADD THIS
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide; // << ADD THIS
import com.example.roomatch.R;
import com.example.roomatch.viewmodel.ViewModelFactoryProvider;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.view.activities.MainActivity; // Keep if you navigate this way
import com.example.roomatch.viewmodel.CreateProfileViewModel;
import com.google.firebase.firestore.FieldValue; // Keep if used for other things

import java.util.ArrayList; // Keep if used by your other fragments
import java.util.List;    // Keep if used by your other fragments


public class CreateProfileFragment extends Fragment {

    private EditText editFullName, editAge,editDescription;
    private RadioGroup userTypeGroup, genderGroup;
    private Button saveProfileButton;
    //private CheckBox checkboxClean, checkboxSmoker, checkboxNightOwl, checkboxQuiet, checkboxParty;
    //private CheckBox checkboxMusic, checkboxSports, checkboxTravel, checkboxCooking, checkboxReading;
    private TextView lifestyleLabel, interestsLabel;
    //private LinearLayout lifestyleCheckboxes, interestsCheckboxes;

    private FragmentContainerView lifeStyles, interests;
    private LifeStylesFragment lifeStylesFragment;
    private InterestsFragment interestsFragment;

    private CreateProfileViewModel viewModel;

    private ImageView imageCreateProfilePic;       // To display the selected image
    private Button buttonSelectProfileImage;    // Button to trigger image selection
    private Uri selectedImageUri;                // To store the URI of the selected image

    // ActivityResultLauncher for picking an image
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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

        // ViewModel
        ViewModelProvider.Factory factory = ViewModelFactoryProvider.factory;
        viewModel = new ViewModelProvider(this, factory).get(CreateProfileViewModel.class);

        // ---- Find views ----
        editFullName = view.findViewById(R.id.editFullName);
        editAge = view.findViewById(R.id.editprofileAge);
        editDescription = view.findViewById(R.id.editCreateDescription);

        genderGroup = view.findViewById(R.id.genderGroup);
        userTypeGroup = view.findViewById(R.id.userTypeGroup);
        saveProfileButton = view.findViewById(R.id.buttonSaveProfile);

        lifestyleLabel = view.findViewById(R.id.lifestyleLabel);
        interestsLabel = view.findViewById(R.id.interestsLabel);

        lifeStyles = view.findViewById(R.id.createProfileLifeStyles);
        interests = view.findViewById(R.id.createProfileInterests);

        // New picture views
        imageCreateProfilePic = view.findViewById(R.id.imageCreateProfilePic);
        buttonSelectProfileImage = view.findViewById(R.id.buttonSelectProfileImage);

        // ---- Child fragments (null-safe) ----
        lifeStylesFragment = (LifeStylesFragment) getChildFragmentManager()
                .findFragmentById(R.id.createProfileLifeStyles);
        if (lifeStylesFragment != null) {
            lifeStylesFragment.setOnLifestyleChangedListener(this::updateLifeStyles);
        }

        interestsFragment = (InterestsFragment) getChildFragmentManager()
                .findFragmentById(R.id.createProfileInterests);
        if (interestsFragment != null) {
            interestsFragment.setOnInterestsChangedListener(this::updateInterests);
        }

        // ---- Initial visibility (same as your app logic) ----
        setVisibility(View.GONE);

        // ---- Image select button ----
        if (buttonSelectProfileImage != null) {
            buttonSelectProfileImage.setOnClickListener(v -> openImageChooser());
        }
        // Optional: also let users tap the avatar to pick:
        // if (imageCreateProfilePic != null) imageCreateProfilePic.setOnClickListener(v -> openImageChooser());

        // ---- Save button ----
        saveProfileButton.setOnClickListener(v -> saveProfile());

        // ---- Role changes ----
        userTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioSeeker) {
                setVisibility(View.VISIBLE);
            } else {
                setVisibility(View.GONE);
                if (editDescription != null) editDescription.setVisibility(View.VISIBLE); // leave description visible for owners
            }
        });

        // ---- Toasts ----
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                // If you have a "clearToastMessage()" method, call it to avoid repeats
                // viewModel.clearToastMessage();
            }
        });

        // ---- Save complete ----
        viewModel.getProfileSaved().observe(getViewLifecycleOwner(), isSaved -> {
            if (Boolean.TRUE.equals(isSaved)) {
                navigateToMainActivity(getUserType());
            }
        });

    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // intent.setType("image/*"); // optional
        if (imagePickerLauncher != null) {
            try {
                imagePickerLauncher.launch(intent);
            } catch (Exception e) {
                Log.e("CreateProfileFragment", "Failed to launch image picker", e);
                Toast.makeText(getContext(), "Cannot open image picker.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e("CreateProfileFragment", "imagePickerLauncher not initialized");
            Toast.makeText(getContext(), "Image picker not ready.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ActivityResultLauncher for the image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri imageData = result.getData().getData();
                        if (imageData != null) {
                            selectedImageUri = imageData;

                            // If the view is already created, show it now
                            if (isAdded() && imageCreateProfilePic != null) {
                                Glide.with(requireContext())
                                        .load(selectedImageUri)
                                        .placeholder(R.drawable.ic_default_profile_placeholder)
                                        .error(R.drawable.ic_default_profile_placeholder)
                                        .circleCrop()
                                        .into(imageCreateProfilePic);
                            } else {
                                Log.d("CreateProfileFragment",
                                        "View not ready yet; image will be shown after view is available.");
                            }
                        }
                    }
                }
        );
    }


    private void updateInterests(Object updatedInterests) {

    }

    private void updateLifeStyles(Object updatedList) {
        //String lifeStyleStr=String.join(",",updatedList);
        //textProfileLifeStyles.setText(safe(lifeStyleStr));

    }

    private void setVisibility(int visibility) {
        if (lifestyleLabel != null) lifestyleLabel.setVisibility(visibility);
        if (interestsLabel != null) interestsLabel.setVisibility(visibility);
        if (lifeStyles != null) lifeStyles.setVisibility(visibility);
        if (interests != null) interests.setVisibility(visibility);
        if(editDescription!=null) editDescription.setVisibility(visibility);
    }

    private void saveProfile() {
        String fullName = editFullName.getText().toString().trim();
        String ageStr = editAge.getText().toString().trim();

        if (fullName.isEmpty()) {
            editFullName.setError("יש להזין שם מלא");
            editFullName.requestFocus();
            return;
        }

        Integer ageNum = tryParseInt(ageStr);
        if (ageNum == null || ageNum <= 0 || ageNum > 120) {
            editAge.setError("גיל לא תקין");
            editAge.requestFocus();
            return;
        }

        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        String gender = getGender(selectedGenderId);

        int selectedUserTypeId = userTypeGroup.getCheckedRadioButtonId();
        String userType = getUserType(selectedUserTypeId);

        // Lifestyles
        String lifestyle = "";
        if (lifeStylesFragment != null && lifeStyles != null && lifeStyles.getVisibility() == View.VISIBLE) {
            List<String> lifestyleList = lifeStylesFragment.getLifeStyles();
            lifestyle = (lifestyleList != null && !lifestyleList.isEmpty()) ? String.join(", ", lifestyleList) : "";
        }

        // Interests
        String interestsStr = "";
        if (interestsFragment != null && interests != null && interests.getVisibility() == View.VISIBLE) {
            List<String> interestsList = interestsFragment.getInterests();
            interestsStr = (interestsList != null && !interestsList.isEmpty()) ? String.join(", ", interestsList) : "";
        }

        String description = editDescription != null ? editDescription.getText().toString().trim() : "";

        UserProfile profile = new UserProfile();
        profile.setFullName(fullName);
        profile.setAge(ageNum);
        profile.setGender(gender);
        profile.setUserType(userType);
        profile.setLifestyle(lifestyle);
        profile.setInterests(interestsStr);
        profile.setDescription(description);
        // profile.setCreatedAt(FieldValue.serverTimestamp()); // if you use Firebase server time

        // Image url
        if (selectedImageUri != null) {
            profile.setProfileImageUrl(selectedImageUri.toString());
        } else {
            profile.setProfileImageUrl(null);
        }

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