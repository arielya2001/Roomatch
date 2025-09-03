package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentAdapter;
import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.ApartmentSearchViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApartmentSearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private Spinner spinnerFilterField, spinnerOrder;
    private Button buttonFilter, buttonClearFilter;
    private SearchView searchView;
    private ApartmentAdapter adapter;
    private List<Apartment> apartments = new ArrayList<>();
    private List<Apartment> originalApartments = new ArrayList<>();

    private ApartmentSearchViewModel viewModel;

    private final Map<String, String> fieldMap = new HashMap<String, String>() {{
        put("עיר", "city");
        put("רחוב", "street");
        put("מספר בית", "houseNumber");
        put("מחיר", "price");
        put("מספר שותפים", "roommatesNeeded");
    }};

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

        // חיבור רכיבי UI רגילים
        recyclerView = view.findViewById(R.id.apartmentRecyclerView);
        spinnerFilterField = view.findViewById(R.id.spinnerFilterField);
        spinnerOrder = view.findViewById(R.id.spinnerOrder);
        buttonFilter = view.findViewById(R.id.buttonFilter);
        buttonClearFilter = view.findViewById(R.id.buttonClearFilter);
        searchView = view.findViewById(R.id.searchView);

        // כפתור לחיפוש מתקדם
        FloatingActionButton buttonAdvancedSearch = view.findViewById(R.id.buttonAdvancedSearch);

        // RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ApartmentAdapter(apartments, getContext(), this::openApartmentDetails);
        recyclerView.setAdapter(adapter);

        // ViewModel
        ApartmentRepository repository = new ApartmentRepository();
        viewModel = new ApartmentSearchViewModel(repository);

        viewModel.getApartments().observe(getViewLifecycleOwner(), list -> {
            apartments.clear();
            apartments.addAll(list);
            originalApartments.clear();
            originalApartments.addAll(list);
            adapter.notifyDataSetChanged();
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show()
        );

        // Spinners
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"עיר", "רחוב", "מספר בית", "מחיר", "מספר שותפים"});
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterField.setAdapter(fieldAdapter);

        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"עולה", "יורד"});
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrder.setAdapter(orderAdapter);

        // כפתורי סינון
        buttonFilter.setOnClickListener(v -> applyFilter());
        buttonClearFilter.setOnClickListener(v -> resetFilter());

        // חיפוש חופשי
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                viewModel.searchApartments(query);
                return true;
            }

            @Override public boolean onQueryTextChange(String newText) {
                viewModel.searchApartments(newText);
                return true;
            }
        });

        // טען את כל הדירות
        viewModel.loadApartments();

        // מעבר לחיפוש המתקדם
        buttonAdvancedSearch.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new AdvancedSearchFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }



    private void applyFilter() {
        String selectedLabel = spinnerFilterField.getSelectedItem().toString();
        String selectedField = fieldMap.get(selectedLabel);
        boolean ascending = spinnerOrder.getSelectedItem().toString().equals("עולה");

        if (selectedField != null) {
            viewModel.applyFilter(selectedField, ascending);
        } else {
            Toast.makeText(getContext(), "שגיאה בסינון", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetFilter() {
        viewModel.resetFilter();
        searchView.setQuery("", false);
        searchView.clearFocus();
    }

    private void openApartmentDetails(Apartment apt) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("apartment", apt);

        ApartmentDetailsFragment fragment = ApartmentDetailsFragment.newInstance(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}