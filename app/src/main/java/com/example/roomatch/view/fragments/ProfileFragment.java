package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomatch.R;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.viewmodel.ProfileViewModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private EditText editname, editAge,editDescription;
    private RadioGroup chooseGender;

    private RadioButton radioMale, radioFemale, radioOther;

    private TextView  textGender,textWhere,textProfileLifeStyles,textProfileInterests,labelLifeStyles,labelInterests,location;

    private String selectedCity="";
    private String selectedStreet="";
    private LatLng selectedLocation; // ממפות

    private Button updateProfileButton, saveProfileButton,cancelEditButton;

    private boolean isEdit=false;

    private FragmentContainerView autoComplete, lifeStyles, interests;

    private LifeStylesFragment lifeStylesFragment;
    private InterestsFragment interestsFragment;

    private String currentName,currentGender,currentCity,currentStreet,currentDescription,currentLifeStyle,currentInterests;
    private Integer currentAge;
    private LatLng currentSelectedLocation;



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

        editname=view.findViewById(R.id.editProfileName);
        editAge=view.findViewById(R.id.editprofileAge);
        editDescription=view.findViewById(R.id.editProfileDescription);
        chooseGender=view.findViewById(R.id.ChooseGender);
        radioMale=view.findViewById(R.id.radioMaleProfile);
        radioFemale=view.findViewById(R.id.radioFemaleProfile);
        radioOther=view.findViewById(R.id.radioOtherProfile);
        textWhere = view.findViewById(R.id.whereToSearch);
        textGender = view.findViewById(R.id.textgenderProfile);
        chooseGender.setVisibility(View.GONE);
        lifeStyles=view.findViewById(R.id.profileLifeStyles);
        interests=view.findViewById(R.id.profileInterests);
        lifeStylesFragment=(LifeStylesFragment) getChildFragmentManager().findFragmentById(R.id.profileLifeStyles);
        lifeStylesFragment.setOnLifestyleChangedListener(updatedList -> updateLifeStyles(updatedList));
        interestsFragment=(InterestsFragment)getChildFragmentManager().findFragmentById(R.id.profileInterests);
        interestsFragment.setOnInterestsChangedListener(updatedInterests -> updateInterests(updatedInterests));
        chooseGender.setOnCheckedChangeListener((group, checkedId)->genderChanged(checkedId));
        textProfileLifeStyles=view.findViewById(R.id.textprofilelifeStyles);
        textProfileInterests=view.findViewById(R.id.textProfileInterests);
        LinearLayout layoutLifestyleAndInterests = view.findViewById(R.id.layoutLifestyleAndInterests);
        updateProfileButton = view.findViewById(R.id.buttonUpdateProfile);
        saveProfileButton = view.findViewById(R.id.saveButton);
        cancelEditButton = view.findViewById(R.id.cancelButton);
        labelLifeStyles=view.findViewById(R.id.labelProfileLifeStyles);
        labelInterests=view.findViewById(R.id.labelProfileInterests);
        location = view.findViewById(R.id.locationTextView);
        labelInterests.setVisibility(View.GONE);
        labelLifeStyles.setVisibility(View.GONE);
        updateProfileButton.setOnClickListener(v ->editClicked() );
        saveProfileButton.setOnClickListener(v->saveClicked());
        cancelEditButton.setOnClickListener(v->cancelClicked());
        saveProfileButton.setVisibility(View.GONE);
        cancelEditButton.setVisibility(View.GONE);
        editDescription.setEnabled(false);




        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                editname.setText(safe(profile.getFullName()));
                editAge.setText(profile.getAge()+"");
                textGender.setText(safe(profile.getGender()));
                selectedCity=profile.getSelectedCity();
                selectedStreet=profile.getSelectedStreet();
                selectedLocation=profile.getSelectedLocation();
                textWhere.setText(safe(selectedCity)+", "+safe(selectedStreet));
                textProfileLifeStyles.setText(safe(profile.getLifestyle()));
                textProfileInterests.setText(safe(profile.getInterests()));
                if(profile.getUserType().equals("seeker"))
                {
                    lifeStylesFragment.setBoxes(profile.getLifestyle());
                    interestsFragment.setBoxes(profile.getInterests());
                }

                location.setText(profile.getLat()+", "+profile.getLng());
                if ("owner".equals(profile.getUserType())) {
                    LinearLayout seekerDetails = view.findViewById(R.id.seekerProfileDetails);
                    seekerDetails.setVisibility(View.GONE);
                    //layoutLifestyleAndInterests.setVisibility(View.GONE);
                } else {
                    //layoutLifestyleAndInterests.setVisibility(View.VISIBLE);
                    //textLifestyle.setText("סגנון חיים: " + safe(profile.getLifestyle()));
                    //textInterests.setText("תחומי עניין: " + safe(profile.getInterests()));
                }
                if(profile.getSelectedLocation()!=null)
                {
                    selectedLocation=profile.getSelectedLocation();
                }
                else
                {
                    selectedLocation=new LatLng(0.0,0.0);
                }
                if(profile.getDescription()!=null)
                {
                    editDescription.setText(profile.getDescription());
                }
                else
                {
                    editDescription.setText("אין תיאור");
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
                //showEditProfileDialog();
                viewModel.resetEditRequest();
            }
        });

        viewModel.loadProfile();

        autoComplete=view.findViewById(R.id.autocompleteFragmentContainer);
        autoComplete.setVisibility(View.GONE);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getChildFragmentManager().findFragmentById(R.id.autocompleteFragmentContainer);


        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS_COMPONENTS
            ));

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    LatLng latLng = place.getLatLng();
                    String city = extractComponent(place, "locality");
                    String street = extractComponent(place, "route");

                    if (latLng == null || city == null || street == null) {
                        showToast("יש לבחור כתובת תקינה הכוללת עיר ורחוב");
                        return;
                    }
                    selectedCity=city;
                    selectedStreet=street;
                    selectedLocation=latLng;
                    viewModel.setSelectedAddress(city, street, latLng);
                    showToast("כתובת נבחרה: " + street + ", " + city);
                    textWhere.setText(city+", "+street);
                    location.setText(latLng.latitude+", "+latLng.longitude);
                }

                @Override
                public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                    showToast("שגיאה בבחירת כתובת: " + status.getStatusMessage());
                }
            });
        }


    }

    private void updateLifeStyles(List<String> updatedList)
    {
        String lifeStyleStr=String.join(", ",updatedList);
        textProfileLifeStyles.setText(safe(lifeStyleStr));

    }

    private void updateInterests(List<String> updatedList)
    {
        String interestsStr=String.join(", ",updatedList);
        textProfileInterests.setText(safe(interestsStr));

    }

    private void genderChanged(int checked)
    {
        RadioButton selected = chooseGender.findViewById(checked);
        textGender.setText(selected.getText());
    }

    private void editClicked()
    {
        viewModel.requestEditProfile();
        updateProfileButton.setVisibility(View.GONE);
        saveProfileButton.setVisibility(View.VISIBLE);
        cancelEditButton.setVisibility(View.VISIBLE);
        editname.setEnabled(true);
        editAge.setEnabled(true);
        editDescription.setEnabled(true);
        chooseGender.setVisibility(View.VISIBLE);
        autoComplete.setVisibility(View.VISIBLE);
        labelInterests.setVisibility(View.VISIBLE);
        labelLifeStyles.setVisibility(View.VISIBLE);
        if(textGender.getText().toString().equals("זכר"))
        {
            radioMale.setChecked(true);
        }
        if(textGender.getText().toString().equals("נקבה"))
        {
            radioFemale.setChecked(true);
        }
        if(textGender.getText().toString().equals("אחר / לא רוצה לשתף"))
        {
            radioOther.setChecked(true);
        }
        lifeStyles.setVisibility(View.VISIBLE);
        interests.setVisibility(View.VISIBLE);
        currentName=editname.getText().toString();
        currentAge=Integer.parseInt(editAge.getText().toString());
        currentGender=textGender.getText().toString();
        currentCity=selectedCity;
        currentStreet=selectedStreet;
        currentSelectedLocation=selectedLocation;
        currentDescription=editDescription.getText().toString();
        currentLifeStyle=textProfileLifeStyles.getText().toString();
        currentInterests=textProfileInterests.getText().toString();
    }
    private void saveClicked()
    {
        boolean isOwner = viewModel.isCurrentUserOwner(); // ← ודא שיש מתודה כזו ב־ViewModel
        String ageStr = editAge.getText().toString().trim();
        

        //String lifestyle = isOwner ? null : editLifestyle.getText().toString().trim();
        //String interests = isOwner ? null : editInterests.getText().toString().trim();

        viewModel.updateProfile(
                editname.getText().toString().trim(),
                ageStr.isEmpty() ? "0" : ageStr,
                textGender.getText().toString().trim(),
                textProfileLifeStyles.getText().toString(),
                textProfileInterests.getText().toString(),
                selectedCity,
                selectedStreet,
                selectedLocation,
                editDescription.getText().toString()
        );
        updateProfileButton.setVisibility(View.VISIBLE);
        saveProfileButton.setVisibility(View.GONE);
        cancelEditButton.setVisibility(View.GONE);
        editname.setEnabled(false);
        editAge.setEnabled(false);
        editDescription.setEnabled(false);
        chooseGender.setVisibility(View.GONE);
        autoComplete.setVisibility(View.GONE);
        lifeStyles.setVisibility(View.GONE);
        interests.setVisibility(View.GONE);
        labelInterests.setVisibility(View.GONE);
        labelLifeStyles.setVisibility(View.GONE);
    }

    private void cancelClicked()
    {
        editname.setText(currentName);
        editAge.setText(String.valueOf(currentAge));
        textGender.setText(currentGender);
        selectedCity=currentCity;
        selectedStreet=currentStreet;
        selectedLocation=currentSelectedLocation;
        editDescription.setText(currentDescription);
        textProfileLifeStyles.setText(currentLifeStyle);
        textProfileInterests.setText(currentInterests);
        updateProfileButton.setVisibility(View.VISIBLE);
        saveProfileButton.setVisibility(View.GONE);
        cancelEditButton.setVisibility(View.GONE);
        editname.setEnabled(false);
        editAge.setEnabled(false);
        editDescription.setEnabled(false);
        chooseGender.setVisibility(View.GONE);
        autoComplete.setVisibility(View.GONE);
        lifeStyles.setVisibility(View.GONE);
        interests.setVisibility(View.GONE);
        labelInterests.setVisibility(View.GONE);
        labelLifeStyles.setVisibility(View.GONE);

    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    public void setSelectedAddress(String city, String street, LatLng location) {
        this.selectedCity = city;
        this.selectedStreet = street;
        this.selectedLocation = location;
    }

    private String extractComponent(Place place, String type) {
        if (place.getAddressComponents() == null) return "";
        for (AddressComponent component : place.getAddressComponents().asList()) {
            if (component.getTypes().contains(type)) {
                return component.getName();
            }
        }
        return "";
    }


    private String safe(String value) {
        return value != null ? value : "לא זמין";
    }

//    private void showEditProfileDialog() {
//        UserProfile current = viewModel.getProfile().getValue();
//        if (current == null) {
//            Toast.makeText(getContext(), "לא ניתן לערוך פרופיל ריק", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
//        builder.setView(dialogView);
//
//        EditText editFullName = dialogView.findViewById(R.id.editFullName);
//        EditText editAge = dialogView.findViewById(R.id.editprofileAge);
//        EditText editGender = dialogView.findViewById(R.id.editGender);
//        EditText editLifestyle = dialogView.findViewById(R.id.editLifestyle);
//        EditText editInterests = dialogView.findViewById(R.id.editInterests);
//        TextView editWhere = dialogView.findViewById(R.id.editWhereToSearch);
//        LinearLayout layoutLifestyleAndInterests = dialogView.findViewById(R.id.layoutLifestyleAndInterests);
//
//
//        // הזנת ערכים
//        editFullName.setText(safe(current.getFullName()));
//        editAge.setText(String.valueOf(current.getAge()));
//        editGender.setText(safe(current.getGender()));
//        editLifestyle.setText(safe(current.getLifestyle()));
//        editInterests.setText(safe(current.getInterests()));
//        editWhere.setText(safe(current.getSelectedCity())+", "+safe(current.getSelectedStreet()));
//
//        boolean isOwner = viewModel.isCurrentUserOwner(); // ← ודא שיש מתודה כזו ב־ViewModel
//        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
//                getChildFragmentManager().findFragmentById(R.id.autocompleteFragmentContainer);
//
//        if (autocompleteFragment != null) {
//            autocompleteFragment.setPlaceFields(Arrays.asList(
//                    Place.Field.ID,
//                    Place.Field.LAT_LNG,
//                    Place.Field.ADDRESS_COMPONENTS
//            ));
//
//            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//                @Override
//                public void onPlaceSelected(@NonNull Place place) {
//                    LatLng latLng = place.getLatLng();
//                    String city = extractComponent(place, "locality");
//                    String street = extractComponent(place, "route");
//
//                    if (latLng == null || city == null || street == null) {
//                        showToast("יש לבחור כתובת תקינה הכוללת עיר ורחוב");
//                        return;
//                    }
//
//                    viewModel.setSelectedAddress(city, street, latLng);
//                    showToast("כתובת נבחרה: " + street + ", " + city);
//                    textWhere.setText(city+", "+street);
//                }
//
//                @Override
//                public void onError(@NonNull com.google.android.gms.common.api.Status status) {
//                    showToast("שגיאה בבחירת כתובת: " + status.getStatusMessage());
//                }
//            });
//        }
//        // הסתרת שדות אם המשתמש בעל דירה
//        if (isOwner) {
//            editLifestyle.setVisibility(View.GONE);
//            editInterests.setVisibility(View.GONE);
//        }
//
//        if (isOwner) {
//            layoutLifestyleAndInterests.setVisibility(View.GONE);
//        } else {
//            layoutLifestyleAndInterests.setVisibility(View.VISIBLE);
//        }
//
//
//        builder.setTitle("עדכון פרטים אישיים")
//                .setPositiveButton("שמור", (dialog, which) -> {
//                    String ageStr = editAge.getText().toString().trim();
//
//                    String lifestyle = isOwner ? null : editLifestyle.getText().toString().trim();
//                    String interests = isOwner ? null : editInterests.getText().toString().trim();
//
//                    viewModel.updateProfile(
//                            editFullName.getText().toString().trim(),
//                            ageStr.isEmpty() ? "0" : ageStr,
//                            editGender.getText().toString().trim(),
//                            lifestyle,
//                            interests,
//                            selectedCity,
//                            selectedStreet,
//                            selectedLocation,
//                            editDescription.getText().toString()
//                    );
//                })
//                .setNegativeButton("ביטול", null)
//                .create()
//                .show();
//    }


}