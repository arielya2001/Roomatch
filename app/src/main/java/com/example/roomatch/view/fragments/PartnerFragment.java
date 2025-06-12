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
import com.example.roomatch.adapters.PartnerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class PartnerFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView partnersRecyclerView;
    private PartnerAdapter adapter;

    private final List<Map<String, Object>> partners = new ArrayList<>();

    public PartnerFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        partnersRecyclerView = view.findViewById(R.id.recyclerViewPartners);

        adapter = new PartnerAdapter(partners,
                new PartnerAdapter.OnProfileClickListener() {
                    @Override
                    public void onProfileClick(Map<String, Object> partner) {
                        showProfileDialog(partner);
                    }
                },
                new PartnerAdapter.OnReportClickListener() {
                    @Override
                    public void onReportClick(String fullName) {
                        showReportDialog(fullName);
                    }
                });

        partnersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        partnersRecyclerView.setAdapter(adapter);

        loadPartners();
    }

    private void loadPartners() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("users")
                .whereEqualTo("userType", "seeker")
                .whereEqualTo("seekerType", "partner")
                .get()
                .addOnSuccessListener(docs -> {
                    partners.clear();
                    for (DocumentSnapshot doc : docs) {
                        if (!doc.getId().equals(uid)) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("id", doc.getId());
                                partners.add(data);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בטעינה", Toast.LENGTH_SHORT).show());
    }

    private void showProfileDialog(Map<String, Object> partner) {
        String profile = "גיל: " + partner.getOrDefault("age", "לא צוין") +
                "\nמגדר: " + partner.getOrDefault("gender", "לא צוין") +
                "\nתחומי עניין: " + partner.getOrDefault("interests", "לא צוין") +
                "\nסגנון חיים: " + partner.getOrDefault("lifestyle", "לא צוין");

        new AlertDialog.Builder(getContext())
                .setTitle("פרופיל: " + partner.getOrDefault("fullName", "לא ידוע"))
                .setMessage(profile)
                .setPositiveButton("סגור", null)
                .show();
    }

    private void showReportDialog(String fullName) {
        final EditText input = new EditText(getContext());
        new AlertDialog.Builder(getContext())
                .setTitle("דווח על " + fullName)
                .setView(input)
                .setPositiveButton("שלח", (dialog, which) -> {
                    String reason = input.getText().toString();
                    Toast.makeText(getContext(), "דיווח נשלח על " + fullName + ": " + reason, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}
