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
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomatch.R;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;
import com.example.roomatch.viewmodel.ViewModelFactoryProvider;

import java.io.ByteArrayOutputStream;

public class OwnerFragment extends Fragment {

    EditText cityEditText, streetEditText, houseNumberEditText;
    EditText priceEditText, roommatesEditText, descriptionEditText;
    Button selectImageButton, publishButton, cancelButton, cameraButton;
    ImageView imageView;
    Uri imageUri;
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

        ApartmentRepository repo = new ApartmentRepository();
        viewModel = new ViewModelProvider(this, ViewModelFactoryProvider.createFactory())
                .get(OwnerApartmentsViewModel.class);

        cityEditText = view.findViewById(R.id.editTextCity);
        streetEditText = view.findViewById(R.id.editTextStreet);
        houseNumberEditText = view.findViewById(R.id.editTextHouseNumber);
        priceEditText = view.findViewById(R.id.editTextPrice);
        roommatesEditText = view.findViewById(R.id.editTextRoommates);
        descriptionEditText = view.findViewById(R.id.editTextDescription);
        selectImageButton = view.findViewById(R.id.buttonSelectImage);
        publishButton = view.findViewById(R.id.buttonPublish);
        cancelButton = view.findViewById(R.id.cancel);
        cameraButton = view.findViewById(R.id.camera);
        imageView = view.findViewById(R.id.imageViewPreview);

        selectImageButton.setOnClickListener(v -> openFileChooser());
        publishButton.setOnClickListener(v -> publishApartment());
        cancelButton.setOnClickListener(v -> resetForm());
        cameraButton.setOnClickListener(v -> openCamera());

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), this::showToast);
        viewModel.getPublishSuccess().observe(getViewLifecycleOwner(), success -> {
            Log.d("DEBUG", "Publish success observed: " + success);
            if (Boolean.TRUE.equals(success)) {
                resetForm();
                Log.d("DEBUG", "Calling OwnerApartmentsFragment after publish");
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

    private void openCamera() {
        if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 104);
            return;
        }
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
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
        String city = cityEditText.getText().toString().trim();
        String street = streetEditText.getText().toString().trim();
        String houseNumStr = houseNumberEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String roommatesStr = roommatesEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        Log.d("DEBUG", "Publishing apartment: " + city + ", " + street + ", " + houseNumStr + ", " + priceStr);


        if (city.isEmpty() || street.isEmpty() || houseNumStr.isEmpty() || priceStr.isEmpty() || roommatesStr.isEmpty() || description.isEmpty()) {
            showToast("כל השדות חייבים להיות מלאים");
            return;
        }

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