package com.example.roomatch.view.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.*;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;

import com.bumptech.glide.Glide;
import com.example.roomatch.R;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.ViewModelFactoryProvider;
import com.example.roomatch.adapters.ApartmentCardAdapter;
import com.example.roomatch.model.Apartment;
import com.example.roomatch.view.activities.AuthActivity;
import com.example.roomatch.viewmodel.AppViewModelFactory;
import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OwnerApartmentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ApartmentCardAdapter adapter;
    private OwnerApartmentsViewModel viewModel;

    private final ApartmentRepository repository = new ApartmentRepository();
    private List<Apartment> apartmentList = new ArrayList<>();
    private Spinner spinnerFilterField, spinnerOrder;
    private SearchView searchView;
    private Button buttonFilter, buttonClear;

    private ImageButton addApartmentButton;

    private String selectedCity = "", selectedStreet = "", selectedHouseNumber = "";
    private EditText editCityGlobal, editStreetGlobal, editHouseNumberGlobal;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1010;


    private final Map<String, String> fieldMap = new HashMap<>() {{
        put("מחיר", "price");
        put("מספר שותפים", "roommatesNeeded");
    }};


    public OwnerApartmentsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_apartments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // שימוש ב-AppViewModelFactory ממקום מרכזי
        ViewModelProvider.Factory factory = ViewModelFactoryProvider.factory;
        viewModel = new ViewModelProvider(this, factory).get(OwnerApartmentsViewModel.class);

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView = view.findViewById(R.id.recyclerViewOwnerApartments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        spinnerFilterField = view.findViewById(R.id.spinnerOwnerFilterField);
        spinnerOrder = view.findViewById(R.id.spinnerOwnerOrder);
//        buttonFilter = view.findViewById(R.id.buttonOwnerFilter);
//        buttonClear = view.findViewById(R.id.buttonOwnerClear);
        searchView = view.findViewById(R.id.searchViewOwner);
        addApartmentButton = view.findViewById(R.id.buttonAddApartments);
        addApartmentButton.setOnClickListener(v->
                {
                    OwnerFragment ownerFragment = new OwnerFragment();
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, ownerFragment)
                            .addToBackStack(null)
                            .commit();
                }
                );

        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"מחיר", "מספר שותפים"});
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterField.setAdapter(fieldAdapter);

        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"עולה", "יורד"});
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrder.setAdapter(orderAdapter);

        adapter = new ApartmentCardAdapter(apartmentList, new ApartmentCardAdapter.OnApartmentClickListener() {
            @Override
            public void onViewApartmentClick(Apartment apartment) {
                showApartmentDetails(apartment);
            }

            @Override
            public void onEditApartmentClick(Apartment apartment) {
                showEditApartmentDialog(apartment);
            }

            @Override
            public void onDeleteApartmentClick(Apartment apartment) {
                confirmAndDelete(apartment);
            }
//            @Override
//            public void onReportApartmentClick(Apartment apartment) {
//                showReportDialog(apartment);
//            }

        });
        recyclerView.setAdapter(adapter);

//        buttonFilter.setOnClickListener(v -> applyFilter());
//        buttonClear.setOnClickListener(v -> resetFilter());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchApartments(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchApartments(newText);
                return true;
            }
        });
        //לחצן פרסום דירה
        Toolbar toolbar = view.findViewById(R.id.toolbar);
//        if (toolbar != null) {
//            ImageButton publishButton = toolbar.findViewById(R.id.buttonAddApartments);
//            publishButton.setImageResource(android.R.drawable.ic_menu_add);
//            publishButton.setOnClickListener(v -> {
//                OwnerFragment ownerFragment = new OwnerFragment();
//                requireActivity().getSupportFragmentManager()
//                        .beginTransaction()
//                        .replace(R.id.fragmentContainer, ownerFragment)
//                        .addToBackStack(null)
//                        .commit();
//            });
//        }
        //תקרא לפונקציה שמחזירה לך את הדירות המעודכנות לפי סינון ואז תקרא לupdate כלומר תנקה את הרשימה הישנה תוסיף חדשה ותעדכן את UI
        viewModel.getFilteredApartments().observe(getViewLifecycleOwner(), apartments -> {
            adapter.updateApartments(apartments);
        });

        // בדיקת משתמש מחובר לפני טעינת דירות
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            viewModel.loadApartments(currentUser.getUid());
        } else {
            Log.e("OwnerApartmentsFragment", "No user logged in");
            Toast.makeText(getContext(), "שגיאה: משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            // ניווט למסך התחברות
            Intent intent = new Intent(getActivity(), AuthActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
        spinnerFilterField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                triggerAutoFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                triggerAutoFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinnerOrder.setAdapter(orderAdapter);

    }

    private void applyFilter() {
        String selectedLabel = spinnerFilterField.getSelectedItem().toString();
        String selectedField = fieldMap.get(selectedLabel);
        boolean ascending = spinnerOrder.getSelectedItem().toString().equals("עולה");
        if (selectedField != null) {
            viewModel.applyFilter(selectedField, ascending);
        } else {
            showToast("שדה לא תקין");
        }
    }

    private void resetFilter() {
        viewModel.resetFilter();
        // איפוס ספינרים
        spinnerFilterField.setSelection(0);
        spinnerOrder.setSelection(0);
        // איפוס חיפוש
        searchView.setQuery("", false);
        searchView.clearFocus();
    }

    private void triggerAutoFilter() {
        if (spinnerFilterField.getSelectedItem() == null || spinnerOrder.getSelectedItem() == null) return;

        String selectedLabel = spinnerFilterField.getSelectedItem().toString();
        String selectedField = fieldMap.get(selectedLabel);
        boolean ascending = spinnerOrder.getSelectedItem().toString().equals("עולה");

        if (selectedField != null) {
            viewModel.applyFilter(selectedField, ascending);
        }
    }
    private void updateAddressFields(Place place, EditText editCity, EditText editStreet, EditText editHouseNumber) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    place.getLatLng().latitude,
                    place.getLatLng().longitude,
                    1
            );
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                editCity.setText(address.getLocality()); // עיר
                editStreet.setText(address.getThoroughfare()); // רחוב
                editHouseNumber.setText(address.getSubThoroughfare()); // מספר בית
            }
        } catch (IOException e) {
            Log.e("GeocoderError", "Failed to parse address", e);
        }
    }



    private void searchApartments(String query) {
        viewModel.searchApartments(query);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showReportDialog(Apartment apt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_report_apartment, null);
        builder.setView(dialogView);

        Spinner reasonSpinner = dialogView.findViewById(R.id.spinnerReportReason);
        EditText additionalDetails = dialogView.findViewById(R.id.editTextAdditionalDetails);
        Button sendButton = dialogView.findViewById(R.id.buttonSendReport);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancelReport);

        String[] reasons = {"פרסום כוזב", "תוכן פוגעני", "תמונה לא הולמת", "מידע שגוי", "אחר"};
        reasonSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, reasons));

        AlertDialog dialog = builder.create();

        sendButton.setOnClickListener(v -> {
            String reason = reasonSpinner.getSelectedItem().toString();
            String details = additionalDetails.getText().toString();

            Map<String, Object> report = new HashMap<>();
            report.put("apartmentId", apt.getId());
            report.put("ownerId", apt.getOwnerId());
            report.put("reason", reason);
            report.put("details", details);
            report.put("timestamp", System.currentTimeMillis());

            repository.reportApartment(apt.getId(), apt.getOwnerId(), reason, details)
                    .addOnSuccessListener(d -> Toast.makeText(getContext(), "הדיווח נשלח בהצלחה", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בשליחת הדיווח", Toast.LENGTH_SHORT).show());


            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void showApartmentDetails(Apartment apt) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_apartment_details, null);

        ImageView apartmentImageView = dialogView.findViewById(R.id.apartmentImageView);
        TextView cityTextView = dialogView.findViewById(R.id.cityTextView);
        TextView streetTextView = dialogView.findViewById(R.id.streetTextView);
        TextView houseNumberTextView = dialogView.findViewById(R.id.houseNumberTextView);
        TextView priceTextView = dialogView.findViewById(R.id.priceTextView);
        TextView roommatesTextView = dialogView.findViewById(R.id.roommatesTextView);
        TextView descriptionTextView = dialogView.findViewById(R.id.descriptionTextView);
        Button messageButton = dialogView.findViewById(R.id.messageButton);

        cityTextView.setText("עיר: " + (apt.getCity() != null ? apt.getCity() : "לא זמין"));
        streetTextView.setText("רחוב: " + (apt.getStreet() != null ? apt.getStreet() : "לא זמין"));
        houseNumberTextView.setText(apt.getHouseNumber()+"");
        priceTextView.setText("מחיר: " + apt.getPrice() + " ₪");
        roommatesTextView.setText("שותפים דרושים: " + apt.getRoommatesNeeded());
        descriptionTextView.setText("תיאור: " + (apt.getDescription() != null ? apt.getDescription() : "לא זמין"));

        if (apt.getImageUrl() != null && !apt.getImageUrl().isEmpty()) {
            Glide.with(this).load(apt.getImageUrl()).into(apartmentImageView);
        } else {
            apartmentImageView.setImageResource(R.drawable.placeholder_image);
        }

        String currentUid = viewModel.getCurrentUserId();
        if (currentUid != null && currentUid.equals(apt.getOwnerId())) {
            messageButton.setVisibility(View.GONE);
        } else {
            messageButton.setOnClickListener(v -> {
                showToast("כאן יהיה מעבר לצ'אט עם בעל הדירה 😊");
            });
        }

        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("סגור", null)
                .show();
    }

    private void showEditApartmentDialog(Apartment apt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_apartment, null);
        builder.setView(dialogView);

        // קישור שדות
        editCityGlobal = dialogView.findViewById(R.id.editCity);
        editStreetGlobal = dialogView.findViewById(R.id.editStreet);
        editHouseNumberGlobal = dialogView.findViewById(R.id.editHouseNumber);
        EditText editPrice = dialogView.findViewById(R.id.editPrice);
        EditText editDescription = dialogView.findViewById(R.id.editDescription);
        EditText editRoommatesNeeded = dialogView.findViewById(R.id.editRoommatesNeeded);
//        TextView selectedAddressText = dialogView.findViewById(R.id.selectedAddressText);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSearchAddress = dialogView.findViewById(R.id.btn_search_address); // כפתור חיפוש

        editCityGlobal.setFocusable(false);
        editCityGlobal.setClickable(false);

        editStreetGlobal.setFocusable(false);
        editStreetGlobal.setClickable(false);

        editHouseNumberGlobal.setFocusable(false);
        editHouseNumberGlobal.setClickable(false);




        // מילוי נתונים
        editCityGlobal.setText(apt.getCity());
        editStreetGlobal.setText(apt.getStreet());
        editHouseNumberGlobal.setText(String.valueOf(apt.getHouseNumber()));
        editPrice.setText(String.valueOf(apt.getPrice()));
        editDescription.setText(apt.getDescription());
        editRoommatesNeeded.setText(String.valueOf(apt.getRoommatesNeeded()));
//        selectedAddressText.setVisibility(View.VISIBLE);
//        selectedAddressText.setText(apt.getStreet() + " " +  apt.getHouseNumber() +", " + apt.getCity());

        AlertDialog dialog = builder.setTitle("עריכת דירה").create();

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key));
        }

        btnSearchAddress.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(
                    Place.Field.ADDRESS_COMPONENTS,
                    Place.Field.LAT_LNG
            );
            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY, fields)
                    .build(requireContext());
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });

        btnSave.setOnClickListener(v -> {
            String newCity = editCityGlobal.getText().toString().trim();
            String newStreet = editStreetGlobal.getText().toString().trim();
            String houseNumStr = editHouseNumberGlobal.getText().toString().trim();
            String priceStr = editPrice.getText().toString().trim();
            String newDescription = editDescription.getText().toString().trim();
            String roommatesStr = editRoommatesNeeded.getText().toString().trim();

            viewModel.updateApartment(
                    apt.getId(),
                    newCity,
                    newStreet,
                    houseNumStr,
                    priceStr,
                    roommatesStr,
                    newDescription,
                    null
            );

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);

            for (AddressComponent component : place.getAddressComponents().asList()) {
                if (component.getTypes().contains("locality")) {
                    selectedCity = component.getName();
                } else if (component.getTypes().contains("route")) {
                    selectedStreet = component.getName();
                } else if (component.getTypes().contains("street_number")) {
                    selectedHouseNumber = component.getName();
                }
            }

            // עדכון UI לשדות הקלט בדיאלוג
            if (editCityGlobal != null) editCityGlobal.setText(selectedCity);
            if (editStreetGlobal != null) editStreetGlobal.setText(selectedStreet);
            if (editHouseNumberGlobal != null) editHouseNumberGlobal.setText(selectedHouseNumber);
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Log.e("AutocompleteError", status.getStatusMessage());
        }
    }



    private String extractComponent(Place place, String type) {
        if (place.getAddressComponents() == null) return "";
        for (AddressComponent component : place.getAddressComponents().asList()) {
            if (component.getTypes().contains(type)) {
                return component.getName();
            }
        }
        return "";
    }


    private void confirmAndDelete(Apartment apt) {
        if (apt.getId() == null) {
            showToast("שגיאת מחיקה: מזהה דירה חסר");
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("מחיקת דירה")
                .setMessage("האם למחוק את הדירה לצמיתות?")
                .setPositiveButton("מחק", (d, i) -> viewModel.deleteApartment(apt.getId()))
                .setNegativeButton("בטל", null)
                .show();
    }
    @Override
    public void onResume() {
        super.onResume();
        String uid = viewModel.getCurrentUserId();
        Log.d("DEBUG", "onResume called. UID = " + uid);
        if (uid != null) {
            viewModel.loadApartments(uid);
            Log.d("DEBUG", "Called loadApartments from onResume");
        }
    }


}