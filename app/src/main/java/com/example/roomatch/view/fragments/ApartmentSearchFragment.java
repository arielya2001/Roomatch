package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentAdapter;
import com.example.roomatch.model.Apartment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ApartmentSearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private ApartmentAdapter adapter;
    private List<Apartment> apartments = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public ApartmentSearchFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_apartment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.apartmentRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ApartmentAdapter(apartments, getContext());
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadApartments();
    }

    private void loadApartments() {
        db.collection("apartments")
                .get()
                .addOnSuccessListener(result -> {
                    apartments.clear();
                    for (var doc : result) {
                        Apartment apt = doc.toObject(Apartment.class);
                        apartments.add(apt);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "שגיאה בטעינת דירות", Toast.LENGTH_SHORT).show()
                );
    }
}
