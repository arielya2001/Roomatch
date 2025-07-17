package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.PartnerAdapter;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.viewmodel.PartnerViewModel;
import java.util.ArrayList;
import java.util.Arrays;

public class PartnerFragment extends Fragment {

    private PartnerViewModel viewModel;
    private RecyclerView partnersRecyclerView;
    private PartnerAdapter adapter;

    private Spinner spinnerFilterField, spinnerOrder;
    private SearchView searchViewName;
    private View buttonFilter, buttonClear;



    public PartnerFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PartnerViewModel.class);

        partnersRecyclerView = view.findViewById(R.id.recyclerViewPartners);
        spinnerFilterField = view.findViewById(R.id.spinnerPartnerFilterField);
        spinnerOrder = view.findViewById(R.id.spinnerPartnerOrder);
        searchViewName = view.findViewById(R.id.searchViewPartner);
        buttonFilter = view.findViewById(R.id.buttonPartnerFilter);
        buttonClear = view.findViewById(R.id.buttonPartnerClear);

        // אתחול ספינר שדה סינון
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item,
                Arrays.asList("שם", "גיל", "מגדר", "סגנון חיים", "תחומי עניין")
        );
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterField.setAdapter(fieldAdapter);

        // אתחול ספינר סדר סינון
        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item,
                Arrays.asList("עולה", "יורד")
        );
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrder.setAdapter(orderAdapter);

        // RecyclerView
        adapter = new PartnerAdapter(new ArrayList<>(),
                partner -> viewModel.showProfileDialog(partner),
                partner -> viewModel.sendMatchRequest(partner),
                partner -> viewModel.showReportDialog(partner.getFullName()));
        partnersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        partnersRecyclerView.setAdapter(adapter);

        // תצפיות
        viewModel.getPartners().observe(getViewLifecycleOwner(), partners -> {
            if (partners != null) adapter.updatePartners(partners);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });

        viewModel.getShowProfileDialog().observe(getViewLifecycleOwner(), partner -> {
            if (partner != null) showProfileDialog(partner);
        });

        viewModel.getShowReportDialog().observe(getViewLifecycleOwner(), fullName -> {
            if (fullName != null) showReportDialog(fullName);
        });

        // לחצן סינון
        buttonFilter.setOnClickListener(v -> {
            String field = spinnerFilterField.getSelectedItem().toString();
            String order = spinnerOrder.getSelectedItem().toString();
            viewModel.applyPartnerSort(field, order);
        });

        // לחצן איפוס
        buttonClear.setOnClickListener(v -> viewModel.loadPartners());

        // חיפוש לפי שם
        searchViewName.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.applyPartnerSearchByName(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    viewModel.loadPartners();
                }
                return true;
            }
        });
    }


    private void showProfileDialog(UserProfile partner) {
        String profile = "גיל: " + (partner.getAge() > 0 ? partner.getAge() : "לא צוין") +
                "\nמגדר: " + (partner.getGender() != null ? partner.getGender() : "לא צוין") +
                "\nתחומי עניין: " + (partner.getInterests() != null ? partner.getInterests() : "לא צוין") +
                "\nסגנון חיים: " + (partner.getLifestyle() != null ? partner.getLifestyle() : "לא צוין");

        new AlertDialog.Builder(getContext())
                .setTitle("פרופיל: " + (partner.getFullName() != null ? partner.getFullName() : "לא ידוע"))
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
                    String reason = input.getText().toString().trim();
                    if (!reason.isEmpty()) {
                        Toast.makeText(getContext(), "דיווח נשלח על " + fullName + ": " + reason, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "נא להזין סיבה לדיווח", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}