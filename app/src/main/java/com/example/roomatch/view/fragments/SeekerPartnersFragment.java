package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.PartnerAdapter;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.viewmodel.PartnerViewModel;

import java.util.ArrayList;

public class SeekerPartnersFragment extends Fragment {

    private PartnerViewModel viewModel;
    private RecyclerView recyclerView;
    private PartnerAdapter adapter;

    public SeekerPartnersFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seeker_partners, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PartnerViewModel.class);
        recyclerView = view.findViewById(R.id.recyclerViewSeekerPartners);

        adapter = new PartnerAdapter(new ArrayList<>(),
                partner -> viewModel.showProfileDialog(partner),
                partner -> viewModel.showReportDialog(partner.getFullName()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getPartners().observe(getViewLifecycleOwner(), partners -> {
            if (partners != null) {
                adapter.updatePartners(partners);
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getShowProfileDialog().observe(getViewLifecycleOwner(), partner -> {
            if (partner != null) {
                showProfileDialog(partner);
            }
        });

        viewModel.getShowReportDialog().observe(getViewLifecycleOwner(), fullName -> {
            if (fullName != null) {
                showReportDialog(fullName);
            }
        });
    }

    private void showProfileDialog(UserProfile partner) {
        String profile = "גיל: " + (partner.getAge() > 0 ? partner.getAge() : "לא צוין") +
                "\nמגדר: " + (partner.getGender() != null ? partner.getGender() : "לא צוין") +
                "\nתחומי עניין: " + (partner.getInterests() != null ? partner.getInterests() : "לא צוין") +
                "\nסגנון חיים: " + (partner.getLifestyle() != null ? partner.getLifestyle() : "לא צוין");

        new AlertDialog.Builder(getContext())
                .setTitle("פרופיל: " + (partner.getFullName() != null ? partner.getFullName() : "לא ידוע"))
                .setMessage(profile)
                .setPositiveButton("סגור", null)
                .show();
    }

    private void showReportDialog(String fullName) {
        final EditText input = new EditText(getContext());
        new AlertDialog.Builder(getContext())
                .setTitle("דווח על " + fullName)
                .setView(input)
                .setPositiveButton("שלח", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (!reason.isEmpty()) {
                        Toast.makeText(getContext(), "דיווח נשלח על " + fullName + ": " + reason, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "נא להזין סיבה לדיווח", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}