package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentCardAdapter;
import com.example.roomatch.utils.ChatUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import androidx.appcompat.widget.Toolbar;
import java.util.*;

public class OwnerApartmentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ApartmentCardAdapter adapter;
    private List<Map<String, Object>> apartmentList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public OwnerApartmentsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_apartments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recyclerViewOwnerApartments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ApartmentCardAdapter(apartmentList, this::showApartmentDetails);
        recyclerView.setAdapter(adapter);

        loadApartments();

        Toolbar toolbar = view.findViewById(R.id.toolbar); // שורה 51
        if (toolbar != null) {
            ImageButton chatsButton = toolbar.findViewById(R.id.buttonChats);
            chatsButton.setOnClickListener(v -> {
                ChatsFragment chatsFragment = new ChatsFragment();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, chatsFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
    }

    private void loadApartments() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("apartments")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    apartmentList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        String apartmentId = doc.getId();
                        data.put("id", apartmentId);
                        data.put("hasMessages", false); // Default: no messages
                        data.put("lastSenderId", null); // Default: no last sender

                        // Check for the most recent message for this apartment
                        db.collectionGroup("chat")
                                .whereEqualTo("apartmentId", apartmentId)
                                .whereEqualTo("toUserId", uid)
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(messageSnapshot -> {
                                    if (!messageSnapshot.isEmpty()) {
                                        data.put("hasMessages", true);
                                        String fromUserId = messageSnapshot.getDocuments().get(0).getString("fromUserId");
                                        data.put("lastSenderId", fromUserId);
                                    }
                                    if (!apartmentList.contains(data)) {
                                        apartmentList.add(data);
                                        adapter.notifyDataSetChanged();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "שגיאה בבדיקת הודעות: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "שגיאה בטעינת דירות: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showApartmentDetails(Map<String, Object> apt) {
        String details = "כתובת: " + apt.get("address") +
                "\nמחיר: " + apt.get("price") +
                "\nשותפים דרושים: " + apt.get("roommatesNeeded") +
                "\nתיאור: " + apt.get("description");

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle("פרטי הדירה")
                .setMessage(details)
                .setPositiveButton("סגור", null);

        // ❌ הסרנו את כפתור "פתח שיחה"

        builder.show();
    }
}