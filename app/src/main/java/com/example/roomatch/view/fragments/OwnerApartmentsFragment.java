package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.bumptech.glide.Glide;
import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentCardAdapter;
import com.example.roomatch.utils.ChatUtil;
import androidx.appcompat.widget.SearchView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import androidx.appcompat.widget.Toolbar;
import java.util.*;

public class OwnerApartmentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ApartmentCardAdapter adapter;
    private List<Map<String, Object>> apartmentList = new ArrayList<>();
    private List<Map<String, Object>> allApartments = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

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

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

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


        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            ImageButton chatsButton = toolbar.findViewById(R.id.buttonChats);
            chatsButton.setOnClickListener(v -> {
                ChatsFragment chatsFragment = new ChatsFragment();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, chatsFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        loadApartments();
    }

    private void loadApartments() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("apartments")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    apartmentList.clear();
                    allApartments.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        data.put("hasMessages", false);
                        data.put("lastSenderId", null);

                        allApartments.add(data);
                        apartmentList.add(data);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בטעינת דירות", Toast.LENGTH_SHORT).show());
    }

    private void applyFilter() {
        String selectedLabel = spinnerFilterField.getSelectedItem().toString();
        String selectedField = fieldMap.get(selectedLabel);
        String order = spinnerOrder.getSelectedItem().toString();

        if (selectedField == null) {
            Toast.makeText(getContext(), "שדה לא תקין", Toast.LENGTH_SHORT).show();
            return;
        }

        apartmentList.sort((a, b) -> {
            Comparable valueA = (Comparable) a.get(selectedField);
            Comparable valueB = (Comparable) b.get(selectedField);
            if (valueA == null || valueB == null) return 0;
            return order.equals("עולה") ? valueA.compareTo(valueB) : valueB.compareTo(valueA);
        });

        adapter.notifyDataSetChanged();
    }

    private void resetFilter() {
        apartmentList.clear();
        apartmentList.addAll(allApartments);
        adapter.notifyDataSetChanged();
        searchView.setQuery("", false);
        searchView.clearFocus();
    }

    private void searchApartments(String query) {
        String lowerQuery = query.toLowerCase();
        apartmentList.clear();
        for (Map<String, Object> apt : allApartments) {
            if (apt.values().stream().anyMatch(val -> val != null && val.toString().toLowerCase().contains(lowerQuery))) {
                apartmentList.add(apt);
            }
        }
        adapter.notifyDataSetChanged();
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

        // נתונים מהדירה
        String city = (String) apt.get("city");
        String street = (String) apt.get("street");
        String description = (String) apt.get("description");
        int houseNumber = apt.get("houseNumber") != null ? ((Number) apt.get("houseNumber")).intValue() : 0;
        int price = apt.get("price") != null ? ((Number) apt.get("price")).intValue() : 0;
        int roommates = apt.get("roommatesNeeded") != null ? ((Number) apt.get("roommatesNeeded")).intValue() : 0;
        String imageUrl = (String) apt.get("imageUrl");

        // הצגת הנתונים
        cityTextView.setText("עיר: " + city);
        streetTextView.setText("רחוב: " + street);
        houseNumberTextView.setText("מספר בית: " + houseNumber);
        priceTextView.setText("מחיר: " + price + " ₪");
        roommatesTextView.setText("שותפים דרושים: " + roommates);
        descriptionTextView.setText("תיאור: " + description);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(apartmentImageView);
        } else {
            apartmentImageView.setImageResource(R.drawable.placeholder_image); // תמונה ברירת מחדל אם אין
        }

        // כפתור שליחת הודעה (אפשר לשים כאן מעבר לצ'אט בעתיד)
        messageButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "כאן יהיה מעבר לצ'אט עם בעל הדירה 😊", Toast.LENGTH_SHORT).show();
        });

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

        // מילוי ערכים קיימים
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

                    if (!newCity.isEmpty() && !newStreet.isEmpty() && !houseNumStr.isEmpty() &&
                            !priceStr.isEmpty() && !newDescription.isEmpty() && !roommatesStr.isEmpty()) {
                        try {
                            int newHouseNum = Integer.parseInt(houseNumStr);
                            int newPrice = Integer.parseInt(priceStr);
                            int newRoommates = Integer.parseInt(roommatesStr);
                            if (newPrice >= 0 && newRoommates >= 0 && newHouseNum >= 0) {
                                String apartmentId = (String) apt.get("id");
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("city", newCity);
                                updates.put("street", newStreet);
                                updates.put("houseNumber", newHouseNum);
                                updates.put("price", newPrice);
                                updates.put("description", newDescription);
                                updates.put("roommatesNeeded", newRoommates);

                                db.collection("apartments").document(apartmentId)
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "דירה עודכנה בהצלחה", Toast.LENGTH_SHORT).show();
                                            loadApartments(); // רענון הרשימה
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בעדכון הדירה", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(getContext(), "ערכים חייבים להיות מספרים חיוביים", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "שדות מספריים חייבים להיות מספרים תקינים", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "כל השדות חייבים להיות מלאים", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null);

        builder.create().show();
    }

}