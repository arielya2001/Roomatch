package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
                partner -> viewModel.showProfileDialog(partner),
                partner -> viewModel.sendMatchRequest(partner), // ✔️ CORRECT ici
                partner -> viewModel.showReportDialog(partner)
        );
        recyclerView.setAdapter(adapter);

        viewModel.getPartners().observe(getViewLifecycleOwner(), adapter::setData);
        viewModel.getToastMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
    }
}
