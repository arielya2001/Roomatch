package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentCardAdapter;
import com.example.roomatch.model.Apartment;
import com.example.roomatch.viewmodel.SeekerApartmentsViewModel;

import java.util.ArrayList;

public class SeekerApartmentsFragment extends Fragment {

    private SeekerApartmentsViewModel viewModel;
    private RecyclerView recyclerView;
    private ApartmentCardAdapter adapter;

    public SeekerApartmentsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seeker_apartments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SeekerApartmentsViewModel.class);
        recyclerView = view.findViewById(R.id.recyclerViewSeekerApartments);

        adapter = new ApartmentCardAdapter(new ArrayList<>(), new ApartmentCardAdapter.OnApartmentClickListener() {
            @Override
            public void onViewApartmentClick(Apartment apartment) {
                // הצג פרטי דירה (למשל, דיאלוג או פרגמנט)
                showApartmentDetails(apartment);
            }

            @Override
            public void onEditApartmentClick(Apartment apartment) {
                // לא רלוונטי לחיפוש (משתמש seeker לא עורך דירות)
            }

            @Override
            public void onDeleteApartmentClick(Apartment apartment) {
                // לא רלוונטי לחיפוש
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getApartments().observe(getViewLifecycleOwner(), apartments -> {
            if (apartments != null) {
                adapter.updateApartments(apartments);
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadApartments();
    }

    private void showApartmentDetails(Apartment apartment) {
        // דוגמה להצגת פרטים (צריך ליישם בהתאם לעיצוב)
        Toast.makeText(getContext(), "הצגת פרטי דירה: " + apartment.getCity(), Toast.LENGTH_SHORT).show();
        // ניתן להוסיף דיאלוג או מעבר לפרגמנט פרטים
    }
}