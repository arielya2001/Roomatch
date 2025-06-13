package com.example.roomatch.view.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import com.example.roomatch.R;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.AppViewModelFactory;
import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;
import com.google.firebase.storage.FirebaseStorage;

import java.util.*;
import java.util.function.Supplier;

public class OwnerFragment extends Fragment {

    EditText cityEditText, streetEditText, houseNumberEditText;
    EditText priceEditText, roommatesEditText, descriptionEditText;
    Button selectImageButton, publishButton;
    ImageView imageView;
    Uri imageUri;

    FirebaseStorage storage;
    OwnerApartmentsViewModel viewModel;

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

        storage = FirebaseStorage.getInstance();

        // ViewModel מחובר לריפוזיטורי
        ApartmentRepository repository = new ApartmentRepository();
        Map<Class<? extends ViewModel>, Supplier<? extends ViewModel>> creators = new HashMap<>();
        creators.put(OwnerApartmentsViewModel.class, () -> new OwnerApartmentsViewModel(repository));
        AppViewModelFactory factory = new AppViewModelFactory(creators);
        viewModel = new ViewModelProvider(this, factory).get(OwnerApartmentsViewModel.class);

        cityEditText = view.findViewById(R.id.editTextCity);
        streetEditText = view.findViewById(R.id.editTextStreet);
        houseNumberEditText = view.findViewById(R.id.editTextHouseNumber);
        priceEditText = view.findViewById(R.id.editTextPrice);
        roommatesEditText = view.findViewById(R.id.editTextRoommates);
        descriptionEditText = view.findViewById(R.id.editTextDescription);
        selectImageButton = view.findViewById(R.id.buttonSelectImage);
        publishButton = view.findViewById(R.id.buttonPublish);
        imageView = view.findViewById(R.id.imageViewPreview);

        selectImageButton.setOnClickListener(v -> openFileChooser());
        publishButton.setOnClickListener(v -> publishApartment());

        // Observers
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == 102 || requestCode == 103) && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            showToast("נדרשת הרשאה לגשת לתמונות");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }

    private void publishApartment() {
        String city = cityEditText.getText().toString().trim();
        String street = streetEditText.getText().toString().trim();
        String houseNumStr = houseNumberEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String roommatesStr = roommatesEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        viewModel.publishApartment(city, street, houseNumStr, priceStr, roommatesStr, description, imageUri);
    }

    private void resetForm() {
        cityEditText.setText("");
        streetEditText.setText("");
        houseNumberEditText.setText("");
        priceEditText.setText("");
        roommatesEditText.setText("");
        descriptionEditText.setText("");
        imageUri = null;
        imageView.setImageDrawable(null);
    }
}
