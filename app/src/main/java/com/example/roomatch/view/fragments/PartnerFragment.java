package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.PartnerAdapter;
import com.example.roomatch.viewmodel.PartnerViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PartnerFragment extends Fragment {

    private PartnerViewModel viewModel;
    private RecyclerView partnersRecyclerView;
    private PartnerAdapter adapter;

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

        partnersRecyclerView = view.findViewById(R.id.recyclerViewPartners);

        adapter = new PartnerAdapter(new ArrayList<>(),
                new PartnerAdapter.OnProfileClickListener() {
                    @Override
                    public void onProfileClick(Map<String, Object> partner) {
                        viewModel.showProfileDialog(partner); // נשלח אירוע ל-ViewModel
                    }
                },
                new PartnerAdapter.OnReportClickListener() {
                    @Override
                    public void onReportClick(String fullName) {
                        viewModel.showReportDialog(fullName); // נשלח אירוע ל-ViewModel
                    }
                });

        partnersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        partnersRecyclerView.setAdapter(adapter);

        // צפייה ברשימת שותפים
        viewModel.getPartners().observe(getViewLifecycleOwner(), partners -> {
            if (partners != null) {
                adapter.updatePartners(partners); // תלוי בשיטה ב-PartnerAdapter
            }
        });

        // צפייה בהודעות Toast
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        // טיפול בהצגת דיאלוגי פרופיל
        viewModel.getPartners().observe(getViewLifecycleOwner(), partners -> {
            // כאן ניתן להוסיף לוגיקה להצגת דיאלוג אם יש שינוי, אבל נעביר את זה ל-OnProfileClick
        });
    }

    // שיטות להצגת דיאלוגים (מועברות מה-ViewModel ל-Fragment)
    public void showProfileDialog(Map<String, Object> partner) {
        String profile = "גיל: " + partner.getOrDefault("age", "לא צוין") +
                "\nמגדר: " + partner.getOrDefault("gender", "לא צוין") +
                "\nתחומי עניין: " + partner.getOrDefault("interests", "לא צוין") +
                "\nסגנון חיים: " + partner.getOrDefault("lifestyle", "לא צוין");

        new AlertDialog.Builder(getContext())
                .setTitle("פרופיל: " + partner.getOrDefault("fullName", "לא ידוע"))
                .setMessage(profile)
                .setPositiveButton("סגור", null)
                .show();
    }

    public void showReportDialog(String fullName) {
        final EditText input = new EditText(getContext());
        new AlertDialog.Builder(getContext())
                .setTitle("דווח על " + fullName)
                .setView(input)
                .setPositiveButton("שלח", (dialog, which) -> {
                    String reason = input.getText().toString();
                    Toast.makeText(getContext(), "דיווח נשלח על " + fullName + ": " + reason, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}