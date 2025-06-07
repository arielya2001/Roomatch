package com.example.roomatch.view.fragments;

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

        loadProfile();
        loadMessages();
    }

    private void loadProfile() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    textName.setText("שם: " + doc.getString("fullName"));
                    textAge.setText("גיל: " + String.valueOf(doc.get("age")));
                    textGender.setText("מגדר: " + doc.getString("gender"));
                    textLifestyle.setText("סגנון חיים: " + doc.getString("lifestyle"));
                    textInterests.setText("תחומי עניין: " + doc.getString("interests"));
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בטעינת הפרופיל", Toast.LENGTH_SHORT).show());
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
}
