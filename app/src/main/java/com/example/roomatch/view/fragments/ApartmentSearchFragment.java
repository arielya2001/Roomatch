package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentAdapter;
import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.UserSession;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.ApartmentSearchViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApartmentSearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private Spinner spinnerFilterField, spinnerRadius;
    private Button buttonFilter, buttonClearFilter, buttonSort;
    private SearchView searchView;
    private ApartmentAdapter adapter;
    private boolean sortAscending = true, showFilters = false;
    private View filtersView;

    private ApartmentSearchViewModel viewModel;

    private int selectedRadiusInKiloMeters = Integer.MAX_VALUE;
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private final java.util.Map<String, String> sortFieldMap = new java.util.HashMap<String, String>() {{
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

        // ViewModel
        viewModel = new ViewModelProvider(this).get(ApartmentSearchViewModel.class);

        // Bind views
        recyclerView = view.findViewById(R.id.apartmentRecyclerView);
        spinnerFilterField = view.findViewById(R.id.spinnerFilterField);
        buttonClearFilter = view.findViewById(R.id.buttonClearFilter);
        searchView = view.findViewById(R.id.searchView);
        buttonSort = view.findViewById(R.id.buttonSort);
        buttonFilter = view.findViewById(R.id.buttonOpenFilters);
        filtersView = view.findViewById(R.id.filtersView);
        spinnerRadius = view.findViewById(R.id.spinnerRadius);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new ApartmentAdapter(
                new ArrayList<>(),
                getContext(),
                this::openApartmentDetails,
                this::showReportDialog
        );
        recyclerView.setAdapter(adapter);

        // Observe
        viewModel.getApartments().observe(getViewLifecycleOwner(),
                list -> adapter.updateApartments(list));
        viewModel.getToastMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());

        // מאזין גלילה – נוצר אינליין, לא null
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
        });
        // ודא שהוא לא אייקון סגור כברירת מחדל
        searchView.setIconifiedByDefault(false);
// בהתחלה שיהיה סגור (לא מקלדת פתוחה)
        searchView.setIconified(false);
        searchView.clearFocus();

// לפתוח ולתת פוקוס כשנוגעים בכל הקופסה
        searchView.setOnClickListener(v -> {
            searchView.setIconified(false);
            searchView.onActionViewExpanded();
            searchView.requestFocus();

            // פתיחת המקלדת
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            View text = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (imm != null && text != null) {
                text.requestFocus();
                imm.showSoftInput(text, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });

// אם מקבל פוקוס – וודא שהוא פתוח
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                searchView.setIconified(false);
                searchView.onActionViewExpanded();
            }
        });
        // חיפוש
        searchRunnable = this::applyFilter;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                applyFilter();
                return true;
            }

            @Override public boolean onQueryTextChange(String newText) {
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 300);
                return true;
            }
        });
        ArrayAdapter<String> sortFieldAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new java.util.ArrayList<>(sortFieldMap.keySet())
        );
        sortFieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterField.setAdapter(sortFieldAdapter);

// כשהמשתמש משנה את שדה המיון — נפעיל סינון מחדש
        spinnerFilterField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // טווח מרחק
        List<String> radiusOptions = Arrays.asList("כולם", "10 ק\"מ", "50 ק\"מ", "100 ק\"מ", "150 ק\"מ");
        ArrayAdapter<String> radiusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, radiusOptions);
        radiusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRadius.setAdapter(radiusAdapter);

        spinnerRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                switch (position) {
                    case 1: selectedRadiusInKiloMeters = 10; break;
                    case 2: selectedRadiusInKiloMeters = 50; break;
                    case 3: selectedRadiusInKiloMeters = 100; break;
                    case 4: selectedRadiusInKiloMeters = 150; break;
                    default: selectedRadiusInKiloMeters = Integer.MAX_VALUE;
                }
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // כפתורי פעולה
        buttonFilter.setOnClickListener(v -> onButtonFilterClick());
        buttonClearFilter.setOnClickListener(v -> resetFilter());
        buttonSort.setOnClickListener(v -> onButtonSortClick());

        // טען נתונים ראשוניים
        viewModel.loadFirstPage();

        // מיקום חיפוש (פרופיל משתמש)
        UserProfile profile = UserSession.getInstance().getCachedProfile();
        if (profile != null) {
            Apartment.setSearchLocation(profile.getLat(), profile.getLng());
        }
    }

    private void onButtonFilterClick() { if(showFilters) { filtersView.setVisibility(View.GONE); } else { filtersView.setVisibility(View.VISIBLE); } showFilters=!showFilters; }

    private void onButtonSortClick() {
        sortAscending = !sortAscending;
        buttonSort.setBackgroundResource(sortAscending
                ? R.mipmap.ic_ascending_foreground
                : R.mipmap.ic_descending_foreground);
        applyFilter();
    }

    private void applyFilter() {
        String uiLabel = (String) spinnerFilterField.getSelectedItem();         // "מחיר" / "מספר שותפים"
        String orderByField = sortFieldMap.getOrDefault(uiLabel, "price");
        viewModel.applyCompleteFilter(sortAscending, selectedRadiusInKiloMeters,orderByField,searchView.getQuery().toString());
    }

    private void resetFilter() {
        searchView.setQuery("", false);
        searchView.clearFocus();
        spinnerRadius.setSelection(0);
        spinnerFilterField.setSelection(0);
        selectedRadiusInKiloMeters = Integer.MAX_VALUE;
        sortAscending = false;
        buttonSort.setBackgroundResource(R.mipmap.ic_ascending_foreground);
        viewModel.clearFilter();
        applyFilter();
    }

    private void openApartmentDetails(Apartment apt) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("apartment", apt);

        ApartmentDetailsFragment fragment = ApartmentDetailsFragment.newInstance(bundle, this::showReportDialog);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showReportDialog(Apartment apt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_apartment, null);
        builder.setView(dialogView);

        Spinner reasonSpinner = dialogView.findViewById(R.id.spinnerReportReason);
        EditText additionalDetails = dialogView.findViewById(R.id.editTextAdditionalDetails);
        Button sendButton = dialogView.findViewById(R.id.buttonSendReport);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancelReport);

        String[] reasons = {"פרסום כוזב", "תוכן פוגעני", "תמונה לא הולמת", "מידע שגוי", "אחר"};
        reasonSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, reasons));

        AlertDialog dialog = builder.create();

        sendButton.setOnClickListener(v -> {
            String reason = reasonSpinner.getSelectedItem().toString();
            String details = additionalDetails.getText().toString();
            new ApartmentRepository()
                    .reportApartment(apt.getId(), apt.getOwnerId(), reason, details)
                    .addOnSuccessListener(d -> Toast.makeText(requireContext(), "הדיווח נשלח בהצלחה", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "שגיאה בשליחת הדיווח", Toast.LENGTH_SHORT).show());
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


}






