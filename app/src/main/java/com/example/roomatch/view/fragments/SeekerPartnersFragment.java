package com.example.roomatch.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
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
import com.example.roomatch.model.UserProfile;

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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new PartnerAdapter(
                new ArrayList<>(),
                partner -> showShowProfileDialog(partner),
                partner -> viewModel.sendMatchRequest(partner),
                partner -> viewModel.showReportDialog(partner)
        );
        recyclerView.setAdapter(adapter);

        viewModel.getPartners().observe(getViewLifecycleOwner(), adapter::setData);
        viewModel.getToastMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
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
}
