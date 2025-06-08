package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.MessageAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class ProfileFragment extends Fragment {

    private TextView textName, textAge, textGender, textLifestyle, textInterests;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private List<Map<String, Object>> messages = new ArrayList<>();
    private Map<String, Object> userProfile = new HashMap<>(); // לשמור את הפרופיל

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textName = view.findViewById(R.id.textProfileName);
        textAge = view.findViewById(R.id.textProfileAge);
        textGender = view.findViewById(R.id.textProfileGender);
        textLifestyle = view.findViewById(R.id.textProfileLifestyle);
        textInterests = view.findViewById(R.id.textProfileInterests);

        recyclerViewMessages = view.findViewById(R.id.recyclerViewUserMessages);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        messageAdapter = new MessageAdapter(messages, this::onChatClick);
        recyclerViewMessages.setAdapter(messageAdapter);

        Button updateProfileButton = view.findViewById(R.id.buttonUpdateProfile);
        updateProfileButton.setOnClickListener(v -> {
            if (userProfile.isEmpty()) {
                Toast.makeText(getContext(), "טוען פרופיל, נסה שוב בעוד רגע", Toast.LENGTH_SHORT).show();
            } else {
                showEditProfileDialog();
            }
        });

        loadProfile();
        loadMessages();
    }

    private void loadProfile() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(getContext(), "משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    userProfile.clear();
                    userProfile.put("fullName", doc.getString("fullName"));
                    userProfile.put("age", doc.getLong("age") != null ? doc.getLong("age").intValue() : null); // טיפול ב-null
                    userProfile.put("gender", doc.getString("gender"));
                    userProfile.put("lifestyle", doc.getString("lifestyle"));
                    userProfile.put("interests", doc.getString("interests"));

                    // עדכון המסך ב-thread ה-UI
                    requireActivity().runOnUiThread(() -> {
                        textName.setText("שם: " + (userProfile.get("fullName") != null ? userProfile.get("fullName") : ""));
                        textAge.setText("גיל: " + (userProfile.get("age") != null ? userProfile.get("age").toString() : "לא זמין"));
                        textGender.setText("מגדר: " + (userProfile.get("gender") != null ? userProfile.get("gender") : ""));
                        textLifestyle.setText("סגנון חיים: " + (userProfile.get("lifestyle") != null ? userProfile.get("lifestyle") : ""));
                        textInterests.setText("תחומי עניין: " + (userProfile.get("interests") != null ? userProfile.get("interests") : ""));
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "שגיאה בטעינת הפרופיל: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    userProfile.clear(); // ניקוי במקרה של כשל
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

    private void onChatClick(String userId) {
        Toast.makeText(getContext(), "מעבר לצ'אט עם " + userId, Toast.LENGTH_SHORT).show();
        // בעתיד: נווט לצ'אט אמיתי
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        // מציאת הרכיבים מה-XML
        EditText editFullName = dialogView.findViewById(R.id.editFullName);
        EditText editAge = dialogView.findViewById(R.id.editAge);
        EditText editGender = dialogView.findViewById(R.id.editGender);
        EditText editLifestyle = dialogView.findViewById(R.id.editLifestyle);
        EditText editInterests = dialogView.findViewById(R.id.editInterests);
        TextView labelFullName = dialogView.findViewById(R.id.labelFullName); // הוספת ID לתווית
        TextView labelAge = dialogView.findViewById(R.id.labelAge);          // הוספת ID לתווית
        TextView labelGender = dialogView.findViewById(R.id.labelGender);    // הוספת ID לתווית
        TextView labelLifestyle = dialogView.findViewById(R.id.labelLifestyle); // הוספת ID לתווית
        TextView labelInterests = dialogView.findViewById(R.id.labelInterests); // הוספת ID לתווית

        // הגדרת טקסט לתוויות אם הן קיימות
        if (labelFullName != null) labelFullName.setText("שם מלא:");
        if (labelAge != null) labelAge.setText("גיל:");
        if (labelGender != null) labelGender.setText("מגדר:");
        if (labelLifestyle != null) labelLifestyle.setText("סגנון חיים:");
        if (labelInterests != null) labelInterests.setText("תחומי עניין:");

        // טעינת הערכים מהפרופיל המקומי
        editFullName.setText((String) userProfile.get("fullName"));
        editAge.setText(userProfile.get("age") != null ? userProfile.get("age").toString() : "");
        editGender.setText((String) userProfile.get("gender"));
        editLifestyle.setText((String) userProfile.get("lifestyle"));
        editInterests.setText((String) userProfile.get("interests"));

        builder.setTitle("עדכון פרטים אישיים")
                .setPositiveButton("שמור", (dialog, which) -> {
                    String newFullName = editFullName.getText().toString().trim();
                    String ageStr = editAge.getText().toString().trim();
                    String newGender = editGender.getText().toString().trim();
                    String newLifestyle = editLifestyle.getText().toString().trim();
                    String newInterests = editInterests.getText().toString().trim();

                    if (!newFullName.isEmpty() && !ageStr.isEmpty() && !newGender.isEmpty() && !newLifestyle.isEmpty() && !newInterests.isEmpty()) {
                        try {
                            int newAge = Integer.parseInt(ageStr);
                            if (newAge >= 0) {
                                String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                                if (userId != null) {
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("fullName", newFullName);
                                    updates.put("age", newAge); // שמירה כ-int
                                    updates.put("gender", newGender);
                                    updates.put("lifestyle", newLifestyle);
                                    updates.put("interests", newInterests);

                                    db.collection("users").document(userId)
                                            .update(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getContext(), "פרטים עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                                                loadProfile(); // רענון הפרופיל
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בעדכון הפרופיל: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                }
                            } else {
                                Toast.makeText(getContext(), "הגיל חייב להיות מספר חיובי", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "הגיל חייב להיות מספר תקין", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "כל השדות חייבים להיות מלאים", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null);

        builder.create().show();
    }
}