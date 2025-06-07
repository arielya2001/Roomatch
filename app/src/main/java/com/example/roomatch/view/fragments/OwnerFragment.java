package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.roomatch.adapters.ApartmentCardAdapter;
import com.example.roomatch.adapters.MessageAdapter;

import com.example.roomatch.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.util.*;

public class OwnerFragment extends Fragment {

    private EditText addressEditText, priceEditText, roommatesEditText, descriptionEditText;
    private Button selectImageButton, publishButton, logoutButton;
    private ImageView imageView;
    private RecyclerView apartmentsRecyclerView, messagesRecyclerView;
    private Uri imageUri;
    private String imageUrl = "";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    private List<Map<String, Object>> apartments = new ArrayList<>();
    private List<Map<String, Object>> messages = new ArrayList<>();

    private ApartmentCardAdapter apartmentAdapter;
    private MessageAdapter messageAdapter;

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

        addressEditText = view.findViewById(R.id.editTextAddress);
        priceEditText = view.findViewById(R.id.editTextPrice);
        roommatesEditText = view.findViewById(R.id.editTextRoommates);
        descriptionEditText = view.findViewById(R.id.editTextDescription);
        selectImageButton = view.findViewById(R.id.buttonSelectImage);
        publishButton = view.findViewById(R.id.buttonPublish);
        logoutButton = view.findViewById(R.id.buttonLogout);
        imageView = view.findViewById(R.id.imageViewPreview);

        apartmentsRecyclerView = view.findViewById(R.id.recyclerViewApartments);
        apartmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        apartmentAdapter = new ApartmentCardAdapter(apartments, this::showApartmentDetails);
        apartmentsRecyclerView.setAdapter(apartmentAdapter);

        messagesRecyclerView = view.findViewById(R.id.recyclerViewMessages);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messageAdapter = new MessageAdapter(messages, this::openChatWithUser);
        messagesRecyclerView.setAdapter(messageAdapter);

        selectImageButton.setOnClickListener(v -> openFileChooser());

        publishButton.setOnClickListener(v -> publishApartment());

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(getContext(), "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();
        });

        loadApartments();
        loadMessages();
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
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
        String address = addressEditText.getText().toString();
        String priceStr = priceEditText.getText().toString();
        String roommatesStr = roommatesEditText.getText().toString();
        String description = descriptionEditText.getText().toString();
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid == null || address.isEmpty() || priceStr.isEmpty() || roommatesStr.isEmpty()) {
            Toast.makeText(getContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        int price, roommatesNeeded;
        try {
            price = Integer.parseInt(priceStr);
            roommatesNeeded = Integer.parseInt(roommatesStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "מחיר ושותפים חייבים להיות מספרים", Toast.LENGTH_SHORT).show();
            return;
        }

        Runnable uploadAndSave = () -> {
            Map<String, Object> apt = new HashMap<>();
            apt.put("ownerId", uid);
            apt.put("address", address);
            apt.put("price", price);
            apt.put("roommatesNeeded", roommatesNeeded);
            apt.put("description", description);
            apt.put("imageUrl", imageUrl);

            db.collection("apartments").add(apt)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(getContext(), "הדירה פורסמה", Toast.LENGTH_SHORT).show();
                        resetForm();
                        loadApartments();
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
        addressEditText.setText("");
        priceEditText.setText("");
        roommatesEditText.setText("");
        descriptionEditText.setText("");
        imageUri = null;
        imageView.setImageDrawable(null);
    }

    private void loadApartments() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("apartments").whereEqualTo("ownerId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    apartments.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        apartments.add(data);
                    }
                    apartmentAdapter.notifyDataSetChanged();
                });
    }

    private void loadMessages() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("messages").whereEqualTo("toUserId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    messages.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        messages.add(doc.getData());
                    }
                    messageAdapter.notifyDataSetChanged();
                });
    }

    private void showApartmentDetails(Map<String, Object> apt) {
        String details = "כתובת: " + apt.get("address") +
                "\nמחיר: " + apt.get("price") +
                "\nשותפים דרושים: " + apt.get("roommatesNeeded") +
                "\nתיאור: " + apt.get("description");

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("פרטי הדירה");
        builder.setMessage(details);
        builder.setPositiveButton("סגור", null);
        builder.show();
    }

    private void openChatWithUser(String userId) {
        Toast.makeText(getContext(), "פתיחת צ'אט עם: " + userId, Toast.LENGTH_SHORT).show();
        // כאן תוכל להחליף לנווט לצ'אט בפועל
    }
}
