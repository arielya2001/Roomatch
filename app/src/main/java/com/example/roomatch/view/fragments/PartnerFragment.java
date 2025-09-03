package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.PartnerAdapter;
import com.example.roomatch.viewmodel.PartnerViewModel;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PartnerFragment extends Fragment {

    private PartnerViewModel viewModel;
    private RecyclerView partnersRecyclerView;
    private PartnerAdapter adapter;
    private SearchView searchViewName;

    private ImageButton toggleFilterMenuButton;
    private LinearLayout filterContainer;
    private Button resetFiltersButton;

    private Button toggleLifestyleButton;
    private LinearLayout lifestyleCheckboxContainer;

    private Button toggleInterestsButton;
    private LinearLayout interestsCheckboxContainer;
    
    private Button toggleLocationButton;
    private LinearLayout locationCheckboxContainer;

    private final Set<String> selectedLifestyles = new HashSet<>();
    private final Set<String> selectedInterests = new HashSet<>();
    private final Set<String> selectedLocations = new HashSet<>();

    private boolean filterMenuVisible = false;
    private boolean lifestyleVisible = false;
    private boolean interestsVisible = false;
    private boolean locationVisible = false;

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

        viewModel = new ViewModelProvider(this).get(PartnerViewModel.class);

        // Bind views
        partnersRecyclerView = view.findViewById(R.id.recyclerViewPartners);
        searchViewName = view.findViewById(R.id.searchViewPartner);
        toggleFilterMenuButton = view.findViewById(R.id.buttonToggleFilters);
        filterContainer = view.findViewById(R.id.filterCheckboxContainer);
        resetFiltersButton = view.findViewById(R.id.buttonResetFilters);
        toggleLifestyleButton = view.findViewById(R.id.buttonToggleLifestyle);
        lifestyleCheckboxContainer = view.findViewById(R.id.lifestyleCheckboxContainer);
        toggleInterestsButton = view.findViewById(R.id.buttonToggleInterests);
        interestsCheckboxContainer = view.findViewById(R.id.interestsCheckboxContainer);
        toggleLocationButton = view.findViewById(R.id.buttonToggleLocation);
        locationCheckboxContainer = view.findViewById(R.id.locationCheckboxContainer);

        // Setup RecyclerView
        partnersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PartnerAdapter(
                new ArrayList<>(),
                partner -> showProfileDialog(partner),
                partner -> {
                    viewModel.sendMatchRequest(partner);
                    NotificationHelper.sendMatchRequestNotification(getContext(), partner.getFullName());
                },
                partner -> showReportDialog(partner)
        );
        partnersRecyclerView.setAdapter(adapter);

        // Observe ViewModel data
        viewModel.getPartners().observe(getViewLifecycleOwner(), adapter::setData);
        viewModel.getToastMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());

        // Handle search by name
        searchViewName.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                viewModel.applyPartnerSearchByName(query);
                return true;
            }

            @Override public boolean onQueryTextChange(String newText) {
                viewModel.applyPartnerSearchByName(newText);
                return true;
            }
        });

        // Toggle full filter menu
        toggleFilterMenuButton.setOnClickListener(v -> {
            filterMenuVisible = !filterMenuVisible;
            filterContainer.setVisibility(filterMenuVisible ? View.VISIBLE : View.GONE);
        });

        // Toggle Lifestyle section
        toggleLifestyleButton.setOnClickListener(v -> {
            lifestyleVisible = !lifestyleVisible;
            lifestyleCheckboxContainer.setVisibility(lifestyleVisible ? View.VISIBLE : View.GONE);
        });

        // Toggle Interests section
        toggleInterestsButton.setOnClickListener(v -> {
            interestsVisible = !interestsVisible;
            interestsCheckboxContainer.setVisibility(interestsVisible ? View.VISIBLE : View.GONE);
        });
        
        // Toggle Location section
        toggleLocationButton.setOnClickListener(v -> {
            locationVisible = !locationVisible;
            locationCheckboxContainer.setVisibility(locationVisible ? View.VISIBLE : View.GONE);
        });

        // Reset all filters - Amélioration de l'UX
        resetFiltersButton.setOnClickListener(v -> resetAllFilters());
        // Rendre toute la zone cliquable pour une meilleure UX
        View resetContainer = view.findViewById(R.id.resetFilterContainer);
        if (resetContainer != null) {
            resetContainer.setOnClickListener(v -> resetAllFilters());
        }

        // Build checkboxes dynamically
        setupFilterCheckboxes();
    }

    private void setupFilterCheckboxes() {
        List<String> lifestyles = Arrays.asList("דתי", "חילוני", "מסורתי", "נקי", "שקט", "מעשן");
        List<String> interests = Arrays.asList("מוזיקה", "ספורט", "טיולים", "בישול", "קריאה", "סרטים", "טכנולוגיה");
        List<String> locations = Arrays.asList("תל אביב", "ירושלים", "חיפה", "באר שבע", "ראשון לציון", "נתניה", "רמת גן");

        // Style de vie checkboxes
        for (String lifestyle : lifestyles) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(lifestyle);
            cb.setPadding(8, 8, 8, 8);
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedLifestyles.add(lifestyle);
                else selectedLifestyles.remove(lifestyle);
                applyAllFilters();
            });
            lifestyleCheckboxContainer.addView(cb);
        }

        // Intérêts checkboxes
        for (String interest : interests) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(interest);
            cb.setPadding(8, 8, 8, 8);
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedInterests.add(interest);
                else selectedInterests.remove(interest);
                applyAllFilters();
            });
            interestsCheckboxContainer.addView(cb);
        }
        
        // Localisation checkboxes
        for (String location : locations) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(location);
            cb.setPadding(8, 8, 8, 8);
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedLocations.add(location);
                else selectedLocations.remove(location);
                applyAllFilters();
            });
            locationCheckboxContainer.addView(cb);
        }
    }

    private void clearAllCheckboxes(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof CheckBox) {
                ((CheckBox) child).setChecked(false);
            }
        }
    }
    
    private void applyAllFilters() {
        viewModel.applyCompleteFilter(
            new ArrayList<>(selectedLifestyles),
            new ArrayList<>(selectedInterests),
            new ArrayList<>(selectedLocations),
            searchViewName.getQuery().toString()
        );
    }
    
    private void resetAllFilters() {
        selectedLifestyles.clear();
        selectedInterests.clear();
        selectedLocations.clear();
        viewModel.clearPartnerFilter();

        clearAllCheckboxes(lifestyleCheckboxContainer);
        clearAllCheckboxes(interestsCheckboxContainer);
        clearAllCheckboxes(locationCheckboxContainer);

        searchViewName.setQuery("", false);
        searchViewName.clearFocus();
        
        Toast.makeText(getContext(), "הסינונים נוקו", Toast.LENGTH_SHORT).show();
    }
    
    private void showProfileDialog(UserProfile profile) {
        viewModel.showProfileDialog(profile);
    }
    
    private void showReportDialog(UserProfile profile) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("דיווח על " + profile.getFullName());
        
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("תאר את הבעיה...");
        builder.setView(input);
        
        builder.setPositiveButton("שלח", (dialog, which) -> {
            String reportText = input.getText().toString();
            if (!reportText.isEmpty()) {
                viewModel.reportUser(profile, reportText);
                NotificationHelper.sendReportNotification(getContext());
            }
        });
        
        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
}
