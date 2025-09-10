package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.utils.NotificationHelper;
import com.example.roomatch.viewmodel.PartnerViewModel;

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

    private Button toggleFilterMenuButton;
    private LinearLayout filterContainer;
    private Button toggleLifestyleButton;
    private Button toggleInterestsButton;
    private Button clearFiltersButton;
    private Button toggleLocationButton;
    private Spinner radiusSpinner;

    private FragmentContainerView lifeStyles, interests;
    private LifeStylesFragment lifeStylesFragment;
    private InterestsFragment interestsFragment;

    private int selectedRadius = Integer.MAX_VALUE;
    private List<String> selectedLifestyles = new ArrayList<>();
    private List<String> selectedInterests = new ArrayList<>();
    private final Set<String> selectedLocations = new HashSet<>();

    private LinearLayoutManager layoutManager;
    private RecyclerView.OnScrollListener pagingScrollListener;

    // דיבאונס לחיפוש
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    // 🔽 מצב תצוגת מקטעי הסינון
    private enum Section { NONE, LIFESTYLE, INTERESTS, LOCATION }
    private Section openSection = Section.NONE;

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
        toggleLifestyleButton = view.findViewById(R.id.buttonToggleLifestyle);
        toggleInterestsButton = view.findViewById(R.id.buttonToggleInterests);
        toggleLocationButton = view.findViewById(R.id.buttonToggleLocation);
        radiusSpinner = view.findViewById(R.id.radiusSpinner);
        clearFiltersButton = view.findViewById(R.id.buttonResetFilters);

        lifeStyles = view.findViewById(R.id.seekerLifeStyles);
        interests = view.findViewById(R.id.seekerInterests);

        lifeStylesFragment = (LifeStylesFragment) getChildFragmentManager().findFragmentById(R.id.seekerLifeStyles);
        if (lifeStylesFragment != null) {
            lifeStylesFragment.setOnLifestyleChangedListener(updatedList -> updateLifeStyles(updatedList));
        }
        interestsFragment = (InterestsFragment) getChildFragmentManager().findFragmentById(R.id.seekerInterests);
        if (interestsFragment != null) {
            interestsFragment.setOnInterestsChangedListener(updatedInterests -> updateInterests(updatedInterests));
        }

        // RecyclerView + Adapter
        layoutManager = new LinearLayoutManager(getContext());
        partnersRecyclerView.setLayoutManager(layoutManager);

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

        // Observe
        viewModel.getPartners().observe(getViewLifecycleOwner(), adapter::setData);
        viewModel.getToastMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());

        // פגינציה בגלילה
        pagingScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0) return;

                int visible = layoutManager.getChildCount();
                int total = layoutManager.getItemCount();
                int first = layoutManager.findFirstVisibleItemPosition();

                if (!viewModel.isLoading() && viewModel.hasMore()
                        && (first + visible) >= (total - 5)) {
                    viewModel.loadNextPage();
                }
            }
        };
        partnersRecyclerView.addOnScrollListener(pagingScrollListener);

        // איפוס סינונים
        clearFiltersButton.setOnClickListener(v -> resetAllFilters());

        // חיפוש עם דיבאונס
        searchRunnable = this::applyAllFilters;
        searchViewName.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                applyAllFilters();
                return true;
            }

            @Override public boolean onQueryTextChange(String newText) {
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 300);
                return true;
            }
        });

        // תפריט סינונים (פתיחה/סגירה של הקונטיינר הגדול)
        toggleFilterMenuButton.setOnClickListener(v -> {
            if (filterContainer.getVisibility() == View.VISIBLE) {
                filterContainer.setVisibility(View.GONE);
                setOpenSection(Section.NONE);
            } else {
                filterContainer.setVisibility(View.VISIBLE);
                // לא לפתוח תת־מקטע אוטומטית
            }
        });

        // 🔽 לחיצה על כל מקטע — פותחת אותו וסוגרת את האחרים; לחיצה שנייה עליו — סוגרת הכל
        toggleLifestyleButton.setOnClickListener(v -> toggleSection(Section.LIFESTYLE));
        toggleInterestsButton.setOnClickListener(v -> toggleSection(Section.INTERESTS));
        toggleLocationButton.setOnClickListener(v -> toggleSection(Section.LOCATION));

        setRadiusSpinner();
        setupFilterCheckboxes();

        // טעינה ראשונה לפי הפילטרים
        applyAllFilters();

        // בהתחלה — כל המקטעים סגורים
        setOpenSection(Section.NONE);
        filterContainer.setVisibility(View.GONE); // אם תרצה שהקופסה הראשית תתחיל סגורה
    }

    @Override
    public void onResume() {
        super.onResume();
        applyAllFilters(); // ריענון אוטומטי בכל חזרה למסך
    }

    /* ---------- ניהול תצוגת המקטעים ---------- */

    private void toggleSection(Section section) {
        // אם לוחצים על מה שכבר פתוח — נסגור הכל
        if (openSection == section) {
            setOpenSection(Section.NONE);
        } else {
            setOpenSection(section);
        }
    }

    private void setOpenSection(Section section) {
        openSection = section;

        // ברירת מחדל: הכול מוסתר
        lifeStyles.setVisibility(View.GONE);
        interests.setVisibility(View.GONE);
        radiusSpinner.setVisibility(View.GONE);

        // מציגים רק את המקטע שביקשו
        switch (openSection) {
            case LIFESTYLE:
                lifeStyles.setVisibility(View.VISIBLE);
                break;
            case INTERESTS:
                interests.setVisibility(View.VISIBLE);
                break;
            case LOCATION:
                radiusSpinner.setVisibility(View.VISIBLE);
                break;
            case NONE:
            default:
                // הכול סגור
                break;
        }
    }

    /* ---------- שאר הלוגיקה הקיימת ---------- */

    private void setRadiusSpinner() {
        List<String> items = Arrays.asList("כולם", "10 KM", "50 KM", "100 KM", "150 KM");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                items
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        radiusSpinner.setAdapter(adapter);

        radiusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                switch (selected) {
                    case "10 KM": selectedRadius = 10; break;
                    case "50 KM": selectedRadius = 50; break;
                    case "100 KM": selectedRadius = 100; break;
                    case "150 KM": selectedRadius = 150; break;
                    case "כולם": default: selectedRadius = Integer.MAX_VALUE; break;
                }
                applyAllFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateInterests(List<String> updatedInterests) {
        if (interestsFragment != null) {
            selectedInterests = interestsFragment.getInterests();
            applyAllFilters();
        }
    }

    private void updateLifeStyles(List<String> updatedList) {
        if (lifeStylesFragment != null) {
            selectedLifestyles = lifeStylesFragment.getLifeStyles();
            applyAllFilters();
        }
    }

    private void setupFilterCheckboxes() {
        // דוגמה אם תרצה להוסיף צ׳קבוקסים דינמיים בעתיד
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
        // סוגרים את כל המקטעים + את הקונטיינר הראשי
        setOpenSection(Section.NONE);
        filterContainer.setVisibility(View.GONE);

        selectedLifestyles.clear();
        if (lifeStylesFragment != null) lifeStylesFragment.setBoxes("");
        selectedInterests.clear();
        if (interestsFragment != null) interestsFragment.setBoxes("");
        selectedLocations.clear();
        selectedRadius = Integer.MAX_VALUE;
        radiusSpinner.setSelection(0);
        searchViewName.setQuery("", false);
        searchViewName.clearFocus();

        viewModel.clearPartnerFilter();
        Toast.makeText(getContext(), "הסינונים נוקו", Toast.LENGTH_SHORT).show();
        applyAllFilters();
    }

    private void showProfileDialog(UserProfile profile) {
        showShowProfileDialog(profile);
    }

    private void showReportDialog(UserProfile profile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pagingScrollListener != null) {
            partnersRecyclerView.removeOnScrollListener(pagingScrollListener);
            pagingScrollListener = null;
        }
    }
}
