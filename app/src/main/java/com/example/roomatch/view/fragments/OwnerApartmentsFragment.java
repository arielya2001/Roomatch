package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentCardAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

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
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recyclerViewOwnerApartments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ApartmentCardAdapter(apartmentList, this::showApartmentDetails);
        recyclerView.setAdapter(adapter);

        loadApartments();
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
                        data.put("id", doc.getId());  // לצרכים עתידיים
                        apartmentList.add(data);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "שגיאה בטעינת דירות", Toast.LENGTH_SHORT).show());
    }

    private void showApartmentDetails(Map<String, Object> apt) {
        String details = "כתובת: " + apt.get("address") +
                "\nמחיר: " + apt.get("price") +
                "\nשותפים דרושים: " + apt.get("roommatesNeeded") +
                "\nתיאור: " + apt.get("description");

        new AlertDialog.Builder(getContext())
                .setTitle("פרטי הדירה")
                .setMessage(details)
                .setPositiveButton("סגור", null)
                .show();
    }
}
