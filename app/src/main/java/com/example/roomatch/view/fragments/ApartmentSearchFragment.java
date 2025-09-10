package com.example.roomatch.view.fragments;

import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentAdapter;
import com.example.roomatch.adapters.PartnerAdapter;
import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.UserSession;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.viewmodel.ApartmentSearchViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApartmentSearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private Spinner spinnerFilterField, spinnerOrder;
    private Button buttonFilter, buttonClearFilter, buttonSort;
    private SearchView searchView;
    private ApartmentAdapter adapter;
    private List<Apartment> apartments = new ArrayList<>();
    private List<Apartment> originalApartments = new ArrayList<>();
    private boolean sortAscending=false, showFilters=false;
    private View filtersView;

    private ApartmentSearchViewModel viewModel;

    private Spinner spinnerRadius;
    private int selectedRadiusInMeters = Integer.MAX_VALUE;

    // מיקום חיפוש קבוע זמנית (ת״א) – נשפר בהמשך

    private UserRepository userRepo = new UserRepository(); // למעלה



    private final Map<String, String> fieldMap = new HashMap<String, String>() {{
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
        //spinnerOrder = view.findViewById(R.id.spinnerOrder);
        buttonClearFilter = view.findViewById(R.id.buttonClearFilter);
        searchView = view.findViewById(R.id.searchView);
        buttonSort = view.findViewById(R.id.buttonSort);
        buttonFilter = view.findViewById(R.id.buttonOpenFilters);
        filtersView = view.findViewById(R.id.filtersView);

        buttonFilter.setOnClickListener(v->
        {
            onButtonFilterClick();
        });

        buttonSort.setOnClickListener(v->
        {
            onButtonSortClick();
        });



        // כפתור לחיפוש מתקדם
        //FloatingActionButton buttonAdvancedSearch = view.findViewById(R.id.buttonAdvancedSearch);

        // RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ApartmentAdapter(
                List.of(),
                getContext(),
                this::openApartmentDetails,
                this::showReportDialog // נכניס גם callback לדיווח
        );
        recyclerView.setAdapter(adapter);

        // ViewModel
        ApartmentRepository repository = new ApartmentRepository();
        viewModel = new ApartmentSearchViewModel(repository);

        viewModel.getApartments().observe(getViewLifecycleOwner(), list -> {
            apartments.clear();
            apartments.addAll(list);
            originalApartments.clear();
            originalApartments.addAll(list);
            adapter.updateApartments(list);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show()
        );

        // Spinners
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>(fieldMap.keySet())
        );
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterField.setAdapter(fieldAdapter);
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterField.setAdapter(fieldAdapter);

//        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
//                new String[]{"עולה", "יורד"});
//        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerOrder.setAdapter(orderAdapter);

        spinnerFilterField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter(); // כל פעם שבוחרים שדה לסינון
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

//        spinnerOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                applyFilter(); // כל פעם שמשנים סדר
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {}
//        });


        // כפתורי סינון
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

        UserProfile profile = UserSession.getInstance().getCachedProfile();
        try {
            double lat = profile.getLat();
            double lng = profile.getLng();
            Apartment.setSearchLocation(lat,lng);
        }
        catch (Exception ex)
        {
            Apartment.setSearchLocation(0,0);
        }
//        userRepo.getMyProfile().addOnSuccessListener(doc -> {
//            UserProfile profile = doc.toObject(UserProfile.class);
//            if (profile != null) {
//                double lat = profile.getLat();
//                double lng = profile.getLng();
//                Apartment.setSearchLocation(lat, lng); // נקודת ההשוואה למרחקים
//
//                // חישוב מחודש של מרחקים מול מיקום זה
//                for (Apartment apt : originalApartments) {
//                    apt.calculateDistanceFromSearchLocation();
//                }
//
//                // ואז תפעיל את הסינון הרגיל
//                applyFilter();
//            }
//        });



        spinnerRadius = view.findViewById(R.id.spinnerRadius);

        List<String> radiusOptions = Arrays.asList("כולם", "10 ק\"מ", "50 ק\"מ", "100 ק\"מ", "150 ק\"מ");
        ArrayAdapter<String> radiusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, radiusOptions);
        radiusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRadius.setAdapter(radiusAdapter);

        spinnerRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                switch (position) {
                    case 1: selectedRadiusInMeters = 10_000; break;
                    case 2: selectedRadiusInMeters = 50_000; break;
                    case 3: selectedRadiusInMeters = 100_000; break;
                    case 4: selectedRadiusInMeters = 150_000; break;
                    default: selectedRadiusInMeters = Integer.MAX_VALUE;
                }

                applyFilter(); // נפעיל סינון מחדש
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });


        // מעבר לחיפוש המתקדם
//        buttonAdvancedSearch.setOnClickListener(v -> {
//            requireActivity().getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragmentContainer, new AdvancedSearchFragment())
//                    .addToBackStack(null)
//                    .commit();
//        });
    }

    private void onButtonFilterClick()
    {
        if(showFilters)
        {
            filtersView.setVisibility(View.GONE);
        }
        else
        {
            filtersView.setVisibility(View.VISIBLE);
        }
        showFilters=!showFilters;
    }
    private void onButtonSortClick()
    {
        if(sortAscending)
        {
            buttonSort.setBackgroundResource(R.mipmap.ic_ascending_foreground);

        }
        else
        {
            buttonSort.setBackgroundResource(R.mipmap.ic_descending_foreground);
        }
        sortAscending=!sortAscending;
        applyFilter();
    }


    private void applyFilter() {
        String selectedLabel = spinnerFilterField.getSelectedItem() != null ? spinnerFilterField.getSelectedItem().toString() : null;
        String selectedField = fieldMap.get(selectedLabel);
        boolean ascending = sortAscending;

        List<Apartment> filtered = new ArrayList<>();

        // סינון לפי טווח
        for (Apartment apt : originalApartments) {
            if (apt.getLatitude() != 0 && apt.getLongitude() != 0) {
                apt.calculateDistanceFromSearchLocation();
                if (apt.getDistance() <= selectedRadiusInMeters) {
                    filtered.add(apt);
                }
            } else if (selectedRadiusInMeters == Integer.MAX_VALUE) {
                filtered.add(apt);
            }
        }

        // מיון: לפי השדה הנבחר (מחיר/שותפים) או לפי מרחק כברירת מחדל
        if (selectedField != null) {
            filtered.sort((a1, a2) -> {
                int result = 0;
                switch (selectedField) {
                    case "price":
                        result = Integer.compare(a1.getPrice(), a2.getPrice());
                        break;
                    case "roommatesNeeded":
                        result = Integer.compare(a1.getRoommatesNeeded(), a2.getRoommatesNeeded());
                        break;
                }
                return ascending ? result : -result;
            });
        } else {
            // אם לא נבחר שדה מיון – מיון לפי מרחק
            filtered.sort((a1, a2) -> {
                int result = Double.compare(a1.getDistance(), a2.getDistance());
                return ascending ? result : -result;
            });
        }

        adapter.updateApartments(filtered);
    }





    private void resetFilter() {
        // איפוס חיפוש
        searchView.setQuery("", false);
        searchView.clearFocus();
        viewModel.resetFilter();

        // איפוס טווח מרחק
        spinnerRadius.setSelection(0); // "כולם"
        selectedRadiusInMeters = Integer.MAX_VALUE;

        // איפוס שדה מיון וסדר
        spinnerFilterField.setSelection(0);
        //spinnerOrder.setSelection(0);
        sortAscending=false;
        buttonSort.setBackgroundResource(R.mipmap.ic_ascending_foreground);


        // חישוב מרחקים מחדש
        for (Apartment apt : originalApartments) {
            apt.calculateDistanceFromSearchLocation();
        }

        // הצגת כל הדירות
        adapter.updateApartments(originalApartments);
    }


    private void openApartmentDetails(Apartment apt) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("apartment", apt);

        ApartmentDetailsFragment fragment = ApartmentDetailsFragment.newInstance(bundle,this::showReportDialog );

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

            // נשתמש ב-ApartmentRepository ישירות
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