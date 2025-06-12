package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import com.example.roomatch.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.util.*;

public class OwnerFragment extends Fragment {

    private EditText cityEditText, streetEditText, houseNumberEditText;
    private EditText priceEditText, roommatesEditText, descriptionEditText;
    private Button selectImageButton, publishButton;
    private ImageView imageView;
    private Uri imageUri;
    private String imageUrl = "";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    public OwnerFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

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

        openGallery(); // אם יש הרשאה, פתח את הגלריה
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
            openGallery(); // אם המשתמש אישר — פתח גלריה
        } else {
            Toast.makeText(getContext(), "נדרשת הרשאה לגשת לתמונות", Toast.LENGTH_SHORT).show();
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
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid == null || city.isEmpty() || street.isEmpty() || houseNumStr.isEmpty()
                || priceStr.isEmpty() || roommatesStr.isEmpty() || description.isEmpty()) {
            Toast.makeText(getContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }



        int price, roommatesNeeded, houseNumber;
        try {
            price = Integer.parseInt(priceStr);
            roommatesNeeded = Integer.parseInt(roommatesStr);
            houseNumber = Integer.parseInt(houseNumStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "מספרים לא תקינים בשדות כמות/מחיר/מספר בית", Toast.LENGTH_SHORT).show();
            return;
        }

        if (price < 0 || roommatesNeeded < 0 || houseNumber < 0) {
            Toast.makeText(getContext(), "שדות מספריים חייבים להיות חיוביים", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullAddress = "עיר: " + city + ", רחוב: " + street + ", מספר: " + houseNumber;

        Runnable uploadAndSave = () -> {
            Map<String, Object> apt = new HashMap<>();
            apt.put("ownerId", uid);
            apt.put("city", city);
            apt.put("street", street);
            apt.put("houseNumber", houseNumber);
            apt.put("price", price);
            apt.put("roommatesNeeded", roommatesNeeded);
            apt.put("description", description);
            apt.put("imageUrl", imageUrl);

            db.collection("apartments").add(apt)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(getContext(), "הדירה פורסמה", Toast.LENGTH_SHORT).show();
                        resetForm();

                        // מעבר למסך "הדירות שלי"
                        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                        transaction.replace(R.id.fragmentContainer, new OwnerApartmentsFragment());
                        transaction.addToBackStack(null);
                        transaction.commit();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בפרסום", Toast.LENGTH_SHORT).show());
        };

        if (imageUri != null) {
            String filename = UUID.randomUUID().toString();
            StorageReference ref = storage.getReference().child("images/" + filename);
            ref.putFile(imageUri)
                    .continueWithTask(task -> ref.getDownloadUrl())
                    .addOnSuccessListener(uri -> {
                        imageUrl = uri.toString();
                        uploadAndSave.run();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בהעלאת תמונה", Toast.LENGTH_SHORT).show());
        } else {
            uploadAndSave.run();
        }
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
