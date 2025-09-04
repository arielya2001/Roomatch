package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
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
    //private LinearLayout lifestyleCheckboxContainer;

    private Button toggleInterestsButton;
    //private LinearLayout interestsCheckboxContainer;

    private FragmentContainerView autoComplete, lifeStyles, interests;

    private LifeStylesFragment lifeStylesFragment;
    private InterestsFragment interestsFragment;
    
    private Button toggleLocationButton;
    //private LinearLayout locationCheckboxContainer;
    private Spinner radiusSpinner;

    private int selectedRadius = Integer.MAX_VALUE;

    //private final Set<String> selectedLifestyles = new HashSet<>();
    //private final Set<String> selectedInterests = new HashSet<>();
    private  List<String> selectedLifestyles=new ArrayList<>();
    private  List<String> selectedInterests=new ArrayList<>();
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
        //lifestyleCheckboxContainer = view.findViewById(R.id.lifestyleCheckboxContainer);
        toggleInterestsButton = view.findViewById(R.id.buttonToggleInterests);
        //interestsCheckboxContainer = view.findViewById(R.id.interestsCheckboxContainer);
        toggleLocationButton = view.findViewById(R.id.buttonToggleLocation);
        radiusSpinner = view.findViewById(R.id.radiusSpinner);
        setRadiusSpinner();

        //locationCheckboxContainer = view.findViewById(R.id.locationCheckboxContainer);
        lifeStyles=view.findViewById(R.id.seekerLifeStyles);
        interests=view.findViewById(R.id.seekerInterests);
        lifeStylesFragment=(LifeStylesFragment) getChildFragmentManager().findFragmentById(R.id.seekerLifeStyles);
        lifeStylesFragment.setOnLifestyleChangedListener(updatedList -> updateLifeStyles(updatedList));
        interestsFragment=(InterestsFragment)getChildFragmentManager().findFragmentById(R.id.seekerInterests);
        interestsFragment.setOnInterestsChangedListener(updatedInterests -> updateInterests(updatedInterests));

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
            lifeStyles.setVisibility(lifestyleVisible ? View.VISIBLE : View.GONE);
        });

        // Toggle Interests section
        toggleInterestsButton.setOnClickListener(v -> {
            interestsVisible = !interestsVisible;
            interests.setVisibility(interestsVisible ? View.VISIBLE : View.GONE);
        });
        
        // Toggle Location section
        toggleLocationButton.setOnClickListener(v -> {
            locationVisible = !locationVisible;
            radiusSpinner.setVisibility(locationVisible ? View.VISIBLE : View.GONE);
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

    private void setRadiusSpinner() {
        List<String> items = Arrays.asList("כולם", "10 KM", "50 KM", "100 KM","150 KM");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                items
        );
        adapter.setDropDownViewResource(
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item
        );
        radiusSpinner.setAdapter(adapter);

        radiusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                switch (selected)
                {
                    case "10 KM":
                        selectedRadius=10;
                        break;

                    case  "50 KM":
                        selectedRadius=50;
                        break;

                    case "100 KM":
                        selectedRadius =100;
                        break;

                    case "150 KM":
                        selectedRadius=150;
                        break;

                    case "כולם":
                        selectedRadius=Integer.MAX_VALUE;
                        break;


                }
                applyAllFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { /* אופציונלי */ }
        });
    }

    private void updateInterests(List<String> updatedInterests) {
        selectedInterests=interestsFragment.getInterests();
        applyAllFilters();
    }

    private void updateLifeStyles(List<String> updatedList) {
        selectedLifestyles=lifeStylesFragment.getLifeStyles();
        applyAllFilters();
    }

    private void setupFilterCheckboxes() {
        //List<String> lifestyles = LifeStylesFragment.getAlllifeStyles();
        //List<String> interests = Arrays.asList("מוזיקה", "ספורט", "טיולים", "בישול", "קריאה", "סרטים", "טכנולוגיה");
        List<String> locations = Arrays.asList("תל אביב", "ירושלים", "חיפה", "באר שבע", "ראשון לציון", "נתניה", "רמת גן");

        // Style de vie checkboxes
//        for (String lifestyle : lifestyles) {
//            CheckBox cb = new CheckBox(getContext());
//            cb.setText(lifestyle);
//            cb.setPadding(8, 8, 8, 8);
//            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
//                if (isChecked) selectedLifestyles.add(lifestyle);
//                else selectedLifestyles.remove(lifestyle);
//                applyAllFilters();
//            });
//            lifestyleCheckboxContainer.addView(cb);
//        }

        // Intérêts checkboxes
//        for (String interest : interests) {
//            CheckBox cb = new CheckBox(getContext());
//            cb.setText(interest);
//            cb.setPadding(8, 8, 8, 8);
//            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
//                if (isChecked) selectedInterests.add(interest);
//                else selectedInterests.remove(interest);
//                applyAllFilters();
//            });
//            interestsCheckboxContainer.addView(cb);
//        }
        
        // Localisation checkboxes
//        for (String location : locations) {
//            CheckBox cb = new CheckBox(getContext());
//            cb.setText(location);
//            cb.setPadding(8, 8, 8, 8);
//            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
//                if (isChecked) selectedLocations.add(location);
//                else selectedLocations.remove(location);
//                applyAllFilters();
//            });
//            locationCheckboxContainer.addView(cb);
//        }
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
            selectedLifestyles,
            selectedInterests,
            selectedRadius,
            searchViewName.getQuery().toString()
        );
    }
    
    private void resetAllFilters() {
        selectedLifestyles.clear();
        lifeStylesFragment.setBoxes("");
        selectedInterests.clear();
        interestsFragment.setBoxes("");
        selectedLocations.clear();
        viewModel.clearPartnerFilter();

        //clearAllCheckboxes(lifestyleCheckboxContainer);
        //clearAllCheckboxes(interestsCheckboxContainer);
        //clearAllCheckboxes(locationCheckboxContainer);

        radiusSpinner.setSelection(0);
        selectedRadius=Integer.MAX_VALUE;
        searchViewName.setQuery("", false);
        searchViewName.clearFocus();
        
        Toast.makeText(getContext(), "הסינונים נוקו", Toast.LENGTH_SHORT).show();
    }
    
    private void showProfileDialog(UserProfile profile) {
        viewModel.showProfileDialog(profile);
        showShowProfileDialog(profile);
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
    private void showShowProfileDialog(@NonNull UserProfile profile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_show_profile, null);
        builder.setView(dialogView);

        TextView name       = dialogView.findViewById(R.id.textShowProfileName);
        TextView age        = dialogView.findViewById(R.id.textShowProfileAge);
        TextView gender     = dialogView.findViewById(R.id.textShowProfileGender);
        TextView lifestyles = dialogView.findViewById(R.id.textShowProfileLifestyles);
        TextView interests  = dialogView.findViewById(R.id.textShowProfileInterests);
        TextView description= dialogView.findViewById(R.id.textShowProfileDescription);
        Button exit         = dialogView.findViewById(R.id.buttonShowProfileExit);

        String safeName   = profile.getFullName()   != null ? profile.getFullName()   : "—";
        String safeAge    = (profile.getAge() != null && profile.getAge() > 0) ? String.valueOf(profile.getAge()) : "—";
        String safeGender = profile.getGender()     != null ? profile.getGender()     : "—";
        String safeLife   = profile.getLifestyle()  != null ? profile.getLifestyle()  : "—";
        String safeInter  = profile.getInterests()  != null ? profile.getInterests()  : "—";
        String safeDesc   = profile.getDescription()!= null ? profile.getDescription(): "—";

        name.setText(safeName);
        age.setText(safeAge);
        gender.setText(safeGender);
        lifestyles.setText(safeLife);
        interests.setText(safeInter);
        description.setText(safeDesc);

        AlertDialog dialog = builder.create();
        exit.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    
}
