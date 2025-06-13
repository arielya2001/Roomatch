package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.*;

import com.bumptech.glide.Glide;
import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentCardAdapter;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.AppViewModelFactory;
import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class OwnerApartmentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ApartmentCardAdapter adapter;
    private OwnerApartmentsViewModel viewModel;

    private Spinner spinnerFilterField, spinnerOrder;
    private SearchView searchView;
    private Button buttonFilter, buttonClear;

    private final Map<String, String> fieldMap = new HashMap<>() {{
        put("עיר", "city");
        put("רחוב", "street");
        put("מספר בית", "houseNumber");
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

        ApartmentRepository repository = new ApartmentRepository();
        Map<Class<? extends ViewModel>, Supplier<? extends ViewModel>> creators = new HashMap<>();
        creators.put(OwnerApartmentsViewModel.class, () -> new OwnerApartmentsViewModel(repository));
        AppViewModelFactory factory = new AppViewModelFactory(creators);
        viewModel = new ViewModelProvider(this, factory).get(OwnerApartmentsViewModel.class);

        recyclerView = view.findViewById(R.id.recyclerViewOwnerApartments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        spinnerFilterField = view.findViewById(R.id.spinnerOwnerFilterField);
        spinnerOrder = view.findViewById(R.id.spinnerOwnerOrder);
        buttonFilter = view.findViewById(R.id.buttonOwnerFilter);
        buttonClear = view.findViewById(R.id.buttonOwnerClear);
        searchView = view.findViewById(R.id.searchViewOwner);

        // Set up Spinners
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"עיר", "רחוב", "מספר בית", "מחיר", "מספר שותפים"});
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterField.setAdapter(fieldAdapter);

        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"עולה", "יורד"});
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrder.setAdapter(orderAdapter);

        // Set up Adapter after initial data load
        viewModel.getFilteredApartments().observe(getViewLifecycleOwner(), apartments -> {
            if (adapter == null) {
                adapter = new ApartmentCardAdapter(apartments, new ApartmentCardAdapter.OnApartmentClickListener() {
                    @Override
                    public void onViewApartmentClick(Map<String, Object> apartment) {
                        showApartmentDetails(apartment);
                    }

                    @Override
                    public void onEditApartmentClick(Map<String, Object> apartment) {
                        showEditApartmentDialog(apartment);
                    }
                    @Override
                    public void onDeleteApartmentClick(Map<String, Object> apartment) {   // ← חדש
                        confirmAndDelete(apartment);
                    }
                });
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateApartments(apartments);
            }
        });

        // Set up Button Listeners
        buttonFilter.setOnClickListener(v -> applyFilter());
        buttonClear.setOnClickListener(v -> resetFilter());

        // Set up SearchView Listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.searchApartments(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.searchApartments(newText);
                return true;
            }
        });

        // Set up Toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            ImageButton publishButton = toolbar.findViewById(R.id.buttonChats); // Reuse same ID for now
            publishButton.setImageResource(android.R.drawable.ic_menu_add); // Set "+" icon
            publishButton.setOnClickListener(v -> {
                OwnerFragment ownerFragment = new OwnerFragment();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, ownerFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Observe Toast Messages
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                showToast(message);
            }
        });

        // Load apartments on fragment start
        viewModel.loadApartments(getCurrentUserId());
    }

    private String getCurrentUserId() {
        return viewModel.getCurrentUserId() != null ? viewModel.getCurrentUserId() : "";
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
        searchView.setQuery("", false);
        searchView.clearFocus();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showApartmentDetails(Map<String, Object> apt) {
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

        String city = (String) apt.get("city");
        String street = (String) apt.get("street");
        String description = (String) apt.get("description");
        int houseNumber = apt.get("houseNumber") != null ? ((Number) apt.get("houseNumber")).intValue() : 0;
        int price = apt.get("price") != null ? ((Number) apt.get("price")).intValue() : 0;
        int roommates = apt.get("roommatesNeeded") != null ? ((Number) apt.get("roommatesNeeded")).intValue() : 0;
        String imageUrl = (String) apt.get("imageUrl");
        String ownerId = (String) apt.get("ownerId");

        cityTextView.setText("עיר: " + (city != null ? city : "לא זמין"));
        streetTextView.setText("רחוב: " + (street != null ? street : "לא זמין"));
        houseNumberTextView.setText("מספר בית: " + houseNumber);
        priceTextView.setText("מחיר: " + price + " ₪");
        roommatesTextView.setText("שותפים דרושים: " + roommates);
        descriptionTextView.setText("תיאור: " + (description != null ? description : "לא זמין"));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(apartmentImageView);
        } else {
            apartmentImageView.setImageResource(R.drawable.placeholder_image);
        }

        String currentUid = getCurrentUserId();
        if (currentUid != null && currentUid.equals(ownerId)) {
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

    private void showEditApartmentDialog(Map<String, Object> apt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_apartment, null);
        builder.setView(dialogView);

        EditText editCity = dialogView.findViewById(R.id.editCity);
        EditText editStreet = dialogView.findViewById(R.id.editStreet);
        EditText editHouseNumber = dialogView.findViewById(R.id.editHouseNumber);
        EditText editPrice = dialogView.findViewById(R.id.editPrice);
        EditText editDescription = dialogView.findViewById(R.id.editDescription);
        EditText editRoommatesNeeded = dialogView.findViewById(R.id.editRoommatesNeeded);

        editCity.setText((String) apt.get("city"));
        editStreet.setText((String) apt.get("street"));
        editHouseNumber.setText(String.valueOf(apt.get("houseNumber")));
        editPrice.setText(String.valueOf(apt.get("price")));
        editDescription.setText((String) apt.get("description"));
        editRoommatesNeeded.setText(String.valueOf(apt.get("roommatesNeeded")));

        builder.setTitle("עריכת דירה")
                .setPositiveButton("שמור", (dialog, which) -> {
                    String newCity = editCity.getText().toString().trim();
                    String newStreet = editStreet.getText().toString().trim();
                    String houseNumStr = editHouseNumber.getText().toString().trim();
                    String priceStr = editPrice.getText().toString().trim();
                    String newDescription = editDescription.getText().toString().trim();
                    String roommatesStr = editRoommatesNeeded.getText().toString().trim();

                    viewModel.updateApartment((String) apt.get("id"), newCity, newStreet, houseNumStr,
                            priceStr, roommatesStr, newDescription, null); // null for imageUri if not updated
                    dialog.dismiss(); // Close dialog after save attempt
                })
                .setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    /** תיבת אישור לפני מחיקה */
    private void confirmAndDelete(Map<String, Object> apt) {
        String aptId = (String) apt.get("id");
        if (aptId == null) {
            showToast("שגיאת מחיקה: מזהה דירה חסר");
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("מחיקת דירה")
                .setMessage("האם למחוק את הדירה לצמיתות?")
                .setPositiveButton("מחק", (d, i) -> viewModel.deleteApartment(aptId))
                .setNegativeButton("בטל", null)
                .show();
    }

}