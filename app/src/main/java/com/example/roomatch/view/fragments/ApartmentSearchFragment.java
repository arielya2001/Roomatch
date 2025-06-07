package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new ApartmentAdapter(apartments, getContext(), this::openApartmentDetails);
        recyclerView.setAdapter(adapter);

        loadApartments();
    }

    private void loadApartments() {
        db.collection("apartments")
                .get()
                .addOnSuccessListener(result -> {
                    apartments.clear();
                    for (var doc : result) {
                        Apartment apt = doc.toObject(Apartment.class);
                        apt.setId(doc.getId());  // הוסף setId למודל Apartment
                        apartments.add(apt);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "שגיאה בטעינת דירות", Toast.LENGTH_SHORT).show()
                );
    }

    private void openApartmentDetails(Apartment apt) {
        Bundle bundle = new Bundle();
        bundle.putString("address", apt.getAddress());
        bundle.putString("description", apt.getDescription());
        bundle.putString("imageUrl", apt.getImageUrl());
        bundle.putString("ownerId", apt.getOwnerId());
        bundle.putInt("price", apt.getPrice());
        bundle.putInt("roommatesNeeded", apt.getRoommatesNeeded());
        bundle.putString("apartmentId", apt.getId());


        ApartmentDetailsFragment fragment = ApartmentDetailsFragment.newInstance(bundle);

        FragmentTransaction ft = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();
        ft.replace(R.id.fragmentContainer, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }
}
