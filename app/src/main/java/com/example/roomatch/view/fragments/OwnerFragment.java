package com.example.roomatch.view.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomatch.R;
import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;
import com.example.roomatch.viewmodel.ViewModelFactoryProvider;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class OwnerFragment extends Fragment {

    EditText priceEditText, roommatesEditText, descriptionEditText;
    Button selectImageButton, publishButton, cameraButton;
    ImageView imageView;
    Uri imageUri;
    OwnerApartmentsViewModel viewModel;

    private String selectedCity;
    private String selectedStreet;
    private LatLng selectedLocation;

    private String selectedHouseNumber;

    public OwnerFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, ViewModelFactoryProvider.createFactory())
                .get(OwnerApartmentsViewModel.class);

        priceEditText = view.findViewById(R.id.editTextPrice);
        roommatesEditText = view.findViewById(R.id.editTextRoommates);
        descriptionEditText = view.findViewById(R.id.editTextDescription);
        selectImageButton = view.findViewById(R.id.buttonSelectImage);
        publishButton = view.findViewById(R.id.buttonPublish);
        cameraButton = view.findViewById(R.id.camera);
        imageView = view.findViewById(R.id.imageViewPreview);

        selectImageButton.setOnClickListener(v -> openFileChooser());
        publishButton.setOnClickListener(v -> publishApartment());
        cameraButton.setOnClickListener(v -> openCamera());

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), this::showToast);
        viewModel.getPublishSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                resetForm();
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new OwnerApartmentsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // הגדרת AutocompleteSupportFragment
// אתחול Places אם עדיין לא אותחל
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(requireContext(), getString(R.string.google_maps_key));
        }

// יצירה דינמית של AutocompleteSupportFragment בתוך FrameLayout
        AutocompleteSupportFragment autocompleteFragment = new AutocompleteSupportFragment();

        getChildFragmentManager().beginTransaction()
                .replace(R.id.autocompleteFragmentContainer, autocompleteFragment)
                .commitNow();

// הגדרת שדות לקבלה
        autocompleteFragment.setPlaceFields(Arrays.asList(
                Place.Field.ID,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS_COMPONENTS
        ));

// מאזין לבחירת מקום
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLatLng();
                String city = extractComponent(place, "locality");
                String street = extractComponent(place, "route");
                String houseNumber = extractComponent(place, "street_number"); // ⬅️ חדש

                if (latLng == null || city == null || street == null) {
                    showToast("יש לבחור כתובת תקינה הכוללת עיר ורחוב");
                    return;
                }

                selectedCity = city;
                selectedStreet = street;
                selectedHouseNumber = houseNumber != null ? houseNumber : ""; // ✅ שמור בנפרד
                selectedLocation = latLng;

                TextView addressView = requireView().findViewById(R.id.textViewSelectedAddress);
                if (addressView != null) {
                    String displayAddress = "כתובת נבחרה: " + street;
                    if (!selectedHouseNumber.isEmpty()) {
                        displayAddress += " " + selectedHouseNumber;
                    }
                    displayAddress += ", " + city;
                    addressView.setText(displayAddress);
                }
            }

            @Override
            public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                showToast("שגיאה בבחירת כתובת: " + status.getStatusMessage());
            }
        });


    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileChooser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 102);
                return;
            }
        } else {
            if (requireContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 103);
                return;
            }
        }
        openGallery();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    private void openCamera() {
        if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 104);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 105);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == 102 || requestCode == 103) && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else if (requestCode == 104 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            showToast("נדרשת הרשאה לגשת לתמונות או למצלמה");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 101 || requestCode == 105) && resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 101 && data.getData() != null) {
                imageUri = data.getData();
            } else if (requestCode == 105 && data.getExtras() != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imageUri = getImageUri(requireContext(), bitmap);
            }
            imageView.setImageURI(imageUri);
        }
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "ApartmentImage", null);
        return Uri.parse(path);
    }

    private void publishApartment() {
        String priceStr = priceEditText.getText().toString().trim();
        String roommatesStr = roommatesEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (selectedCity == null || selectedStreet == null || selectedLocation == null ||
                priceStr.isEmpty() || roommatesStr.isEmpty() || description.isEmpty()) {
            showToast("יש למלא את כל השדות ולבחור כתובת תקינה");
            return;
        }

        viewModel.publishApartment(selectedCity, selectedStreet, selectedHouseNumber, priceStr, roommatesStr, description, imageUri, selectedLocation);
    }

    private void resetForm() {
        selectedCity = null;
        selectedStreet = null;
        selectedLocation = null;
        if (priceEditText != null) priceEditText.setText("");
        if (roommatesEditText != null) roommatesEditText.setText("");
        if (descriptionEditText != null) descriptionEditText.setText("");
        if (imageView != null) imageView.setImageDrawable(null);
        imageUri = null;

        TextView addressView = requireView().findViewById(R.id.textViewSelectedAddress);
        if (addressView != null) addressView.setText("לא נבחרה כתובת");
    }

    private String extractComponent(Place place, String type) {
        if (place.getAddressComponents() == null) return null;
        for (AddressComponent component : place.getAddressComponents().asList()) {
            if (component.getTypes().contains(type)) {
                return component.getName();
            }
        }
        return null;
    }
}
