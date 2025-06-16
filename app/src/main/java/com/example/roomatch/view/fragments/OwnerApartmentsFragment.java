package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import androidx.appcompat.widget.SearchView;
import com.bumptech.glide.Glide;
import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentCardAdapter;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.view.activities.MainActivity;
import com.example.roomatch.viewmodel.AppViewModelFactory;
import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;
import androidx.appcompat.widget.Toolbar; // החלף ייבוא זה
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class OwnerApartmentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ApartmentCardAdapter adapter;
    private OwnerApartmentsViewModel viewModel;
    private ApartmentRepository testRepository; // For testing
    private boolean isTestingMode = false;

    private List<Map<String, Object>> apartmentList = new ArrayList<>(); // For testing dummy data

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

        // Initialize ViewModel
        Map<Class<? extends ViewModel>, Supplier<? extends ViewModel>> creators = new HashMap<>();
        ApartmentRepository repository = isTestingMode && testRepository != null
                ? testRepository
                : new ApartmentRepository(MainActivity.isTestMode);

        creators.put(OwnerApartmentsViewModel.class, () -> new OwnerApartmentsViewModel(repository));
        AppViewModelFactory factory = new AppViewModelFactory(creators);
        viewModel = new ViewModelProvider(this, factory).get(OwnerApartmentsViewModel.class);

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                TextView testTextView = view.findViewById(R.id.textViewTestMessage);
                testTextView.setText(message);
            }
        });

        recyclerView = view.findViewById(R.id.recyclerViewOwnerApartments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        spinnerFilterField = view.findViewById(R.id.spinnerOwnerFilterField);
        spinnerOrder = view.findViewById(R.id.spinnerOwnerOrder);
        buttonFilter = view.findViewById(R.id.buttonOwnerFilter);
        buttonClear = view.findViewById(R.id.buttonOwnerClear);
        searchView = view.findViewById(R.id.searchViewOwner);

        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"עיר", "רחוב", "מספר בית", "מחיר", "מספר שותפים"});
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterField.setAdapter(fieldAdapter);

        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new String[]{"עולה", "יורד"});
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrder.setAdapter(orderAdapter);

        adapter = new ApartmentCardAdapter(apartmentList, new ApartmentCardAdapter.OnApartmentClickListener() {
            @Override
            public void onViewApartmentClick(Map<String, Object> apartment) {
                showApartmentDetails(apartment);
            }

            @Override
            public void onEditApartmentClick(Map<String, Object> apartment) {
                showEditApartmentDialog(apartment);
            }

            @Override
            public void onDeleteApartmentClick(Map<String, Object> apartment) {
                confirmAndDelete(apartment);
            }
        });
        recyclerView.setAdapter(adapter);

        buttonFilter.setOnClickListener(v -> applyFilter());
        buttonClear.setOnClickListener(v -> resetFilter());

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

        Toolbar toolbar = view.findViewById(R.id.toolbar); // השתמש ב-androidx.appcompat.widget.Toolbar
        if (toolbar != null) {
            ImageButton publishButton = toolbar.findViewById(R.id.buttonChats);
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

        // Load apartments via ViewModel
        viewModel.getFilteredApartments().observe(getViewLifecycleOwner(), apartments -> {
            apartmentList.clear();
            if (apartments != null) {
                apartmentList.addAll(apartments);
            }
            adapter.updateApartments(apartmentList);
        });

        // Load apartments when fragment is created
        viewModel.loadApartments(viewModel.getCurrentUserId());
        if (isTestingMode) {
            addDummyApartmentForTesting("dummy1", "תל אביב", "דיזנגוף", "10", "4000", "2", "נחמדה מאוד");
        }

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

    private void searchApartments(String query) {
        viewModel.searchApartments(query);
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

        String currentUid = viewModel.getCurrentUserId();
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
        Log.d("DialogDebug", "Starting showEditApartmentDialog with apartment: " + apt);

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
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // וידוא שכל ה-View-ים נמצאו
        if (editCity == null || editStreet == null || editHouseNumber == null ||
                editPrice == null || editDescription == null || editRoommatesNeeded == null ||
                btnSave == null || btnCancel == null) {
            Log.e("DialogDebug", "One or more views in dialog_edit_apartment layout are null!");
        } else {
            Log.d("DialogDebug", "All dialog views initialized successfully.");
        }

        // מילוי שדות עם ערכים קיימים או ברירת מחדל
        editCity.setText((String) apt.get("city"));
        editStreet.setText((String) apt.get("street"));
        editHouseNumber.setText(apt.get("houseNumber") != null ? String.valueOf(apt.get("houseNumber")) : "");
        editPrice.setText(apt.get("price") != null ? String.valueOf(apt.get("price")) : "");
        editDescription.setText((String) apt.get("description"));
        editRoommatesNeeded.setText(apt.get("roommatesNeeded") != null ? String.valueOf(apt.get("roommatesNeeded")) : "");

        Log.d("DialogDebug", "Fields populated: city=" + editCity.getText() +
                ", street=" + editStreet.getText() +
                ", houseNum=" + editHouseNumber.getText() +
                ", price=" + editPrice.getText() +
                ", description=" + editDescription.getText() +
                ", roommates=" + editRoommatesNeeded.getText());

        AlertDialog dialog = builder.setTitle("עריכת דירה").create();
        Log.d("DialogDebug", "Dialog created with title: עריכת דירה");

        btnSave.setOnClickListener(v -> {
            Log.d("DialogDebug", "Save button clicked.");
            String newCity = editCity.getText().toString().trim();
            String newStreet = editStreet.getText().toString().trim();
            String houseNumStr = editHouseNumber.getText().toString().trim();
            String priceStr = editPrice.getText().toString().trim();
            String newDescription = editDescription.getText().toString().trim();
            String roommatesStr = editRoommatesNeeded.getText().toString().trim();

            Log.d("DialogDebug", "Collected data: city=" + newCity +
                    ", street=" + newStreet +
                    ", houseNum=" + houseNumStr +
                    ", price=" + priceStr +
                    ", description=" + newDescription +
                    ", roommates=" + roommatesStr);

            // קריאה אחת - ה־ViewModel כבר מטפל ב־isTestMode ובעדכון הרשימה
            viewModel.updateApartment(
                    (String) apt.get("id"),
                    newCity,
                    newStreet,
                    houseNumStr,
                    priceStr,
                    roommatesStr,
                    newDescription,
                    null  // אם אין שינוי בתמונה
            );

            Log.d("DialogDebug", "Calling viewModel.updateApartment with ID: " + apt.get("id"));
            dialog.dismiss();
            Log.d("DialogDebug", "Dialog dismissed after save.");
        });

        btnCancel.setOnClickListener(v -> {
            Log.d("DialogDebug", "Cancel button clicked.");
            dialog.dismiss();
            Log.d("DialogDebug", "Dialog dismissed after cancel.");
        });

        dialog.show();
        Log.d("DialogDebug", "Dialog shown.");
    }


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

    /**
     * Method to set testing conditions - similar to your friend's approach
     * This allows us to inject a mock repository for testing
     */
    public void setTestingConditions(ApartmentRepository fakeRepo) {
        this.testRepository = fakeRepo;
        this.isTestingMode = true;

        // עדכון ה-ViewModel עם ה-repository המזויף וסימון מצב בדיקה
        if (viewModel != null) {
            viewModel.setTestRepository(fakeRepo);
            viewModel.setTestingConditions(fakeRepo); // העברת מצב הבדיקה ל-ViewModel
        }
    }

    /**
     * Helper method to add dummy apartment data for testing
     */
    public void addDummyApartmentForTesting(String id, String city, String street,
                                            String houseNumber, String price,
                                            String roommates, String description) {
        if (isTestingMode) {
            // Create a dummy apartment object
            Map<String, Object> dummyApartment = new HashMap<>();
            dummyApartment.put("id", id);
            dummyApartment.put("city", city);
            dummyApartment.put("street", street);
            dummyApartment.put("houseNumber", houseNumber.isEmpty() ? 0 : Integer.parseInt(houseNumber));
            dummyApartment.put("price", price.isEmpty() ? 0 : Integer.parseInt(price));
            dummyApartment.put("roommatesNeeded", roommates.isEmpty() ? 0 : Integer.parseInt(roommates));
            dummyApartment.put("description", description);

            // Add to your apartment list
            apartmentList.add(dummyApartment);

            // עדכון ה-ViewModel עם הנתונים הדמה
            if (viewModel != null) {
                viewModel.setFilteredApartments(apartmentList); // עדכון ישיר של הרשימה המסוננת
            }

            // Notify adapter if it exists
            if (adapter != null) {
                adapter.updateApartments(apartmentList); // עדכון ישיר של רשימת הדירות
            }
        }
    }
    public OwnerApartmentsViewModel getViewModel() {
        return this.viewModel;
    }
    // מחיקת כל הדירות (שימושי בטסטים)




}