package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentAdapter;
import com.example.roomatch.model.Apartment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.*;

public class ApartmentSearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private Spinner spinnerFilterField, spinnerOrder;
    private Button buttonFilter, buttonClearFilter;
    private SearchView searchView;
    private ApartmentAdapter adapter;
    private List<Apartment> apartments = new ArrayList<>();
    private List<Apartment> originalApartments = new ArrayList<>(); // שמירת המקור
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final Map<String, String> fieldMap = new HashMap<String, String>() {{
        put("עיר", "city");
        put("רחוב", "street");
        put("מספר בית", "houseNumber");
        put("מחיר", "price");
        put("מספר שותפים", "roommatesNeeded");
    }};

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
        spinnerFilterField = view.findViewById(R.id.spinnerFilterField);
        spinnerOrder = view.findViewById(R.id.spinnerOrder);
        buttonFilter = view.findViewById(R.id.buttonFilter);
        buttonClearFilter = view.findViewById(R.id.buttonClearFilter); // חדש
        searchView = view.findViewById(R.id.searchView); // חדש

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new ApartmentAdapter(apartments, getContext(), this::openApartmentDetails);
        recyclerView.setAdapter(adapter);

        // מילוי הספינרים
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"עיר", "רחוב", "מספר בית", "מחיר", "מספר שותפים"});
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterField.setAdapter(fieldAdapter);

        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"עולה", "יורד"});
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrder.setAdapter(orderAdapter);

        buttonFilter.setOnClickListener(v -> applyFilter());
        buttonClearFilter.setOnClickListener(v -> resetList());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterLocally(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterLocally(newText);
                return true;
            }
        });

        loadApartments(); // טען את כל הדירות
    }

    private void loadApartments() {
        db.collection("apartments")
                .get()
                .addOnSuccessListener(result -> {
                    apartments.clear();
                    originalApartments.clear();
                    for (var doc : result) {
                        Apartment apt = doc.toObject(Apartment.class);
                        apt.setId(doc.getId());
                        apartments.add(apt);
                        originalApartments.add(apt);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "שגיאה בטעינת דירות", Toast.LENGTH_SHORT).show()
                );
    }

    private void applyFilter() {
        String selectedLabel = spinnerFilterField.getSelectedItem().toString();
        String selectedField = fieldMap.get(selectedLabel);
        String order = spinnerOrder.getSelectedItem().toString();

        if (selectedField == null) {
            Toast.makeText(getContext(), "שגיאה בסינון", Toast.LENGTH_SHORT).show();
            return;
        }

        Query.Direction direction = order.equals("עולה") ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;

        db.collection("apartments")
                .orderBy(selectedField, direction)
                .get()
                .addOnSuccessListener(result -> {
                    apartments.clear();
                    for (var doc : result) {
                        Apartment apt = doc.toObject(Apartment.class);
                        apt.setId(doc.getId());
                        apartments.add(apt);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "שגיאה בסינון", Toast.LENGTH_SHORT).show()
                );
    }

    private void resetList() {
        apartments.clear();
        apartments.addAll(originalApartments);
        adapter.notifyDataSetChanged();
        searchView.setQuery("", false);
        searchView.clearFocus();
    }

    private void filterLocally(String text) {
        List<Apartment> filtered = new ArrayList<>();
        for (Apartment apt : originalApartments) {
            if ((apt.getCity() != null && apt.getCity().toLowerCase().contains(text.toLowerCase())) ||
                    (apt.getStreet() != null && apt.getStreet().toLowerCase().contains(text.toLowerCase())) ||
                    (apt.getDescription() != null && apt.getDescription().toLowerCase().contains(text.toLowerCase()))) {
                filtered.add(apt);
            }
        }
        apartments.clear();
        apartments.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void openApartmentDetails(Apartment apt) {
        Bundle bundle = new Bundle();
        bundle.putString("city", apt.getCity());
        bundle.putString("street", apt.getStreet());
        bundle.putInt("houseNumber", apt.getHouseNumber());
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
