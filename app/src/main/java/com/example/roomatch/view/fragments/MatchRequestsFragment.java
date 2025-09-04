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
import com.example.roomatch.adapters.MatchRequestsAdapter;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.viewmodel.MatchRequestsViewModel;

import java.util.ArrayList;

public class MatchRequestsFragment extends Fragment {

    private MatchRequestsViewModel viewModel;
    private RecyclerView recyclerView;
    private MatchRequestsAdapter adapter;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // יצירת ViewModel מוקדם יותר
        viewModel = new ViewModelProvider(this).get(MatchRequestsViewModel.class);

        recyclerView = view.findViewById(R.id.matchRequestsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // יצירת Adapter עם Listener
        adapter = new MatchRequestsAdapter(new MatchRequestsAdapter.OnRequestActionListener() {
            @Override
            public void onApprove(String requestId) {
                if (requestId != null) {
                    viewModel.approveMatchRequest(requestId);
                } else {
                    Toast.makeText(getContext(), "שגיאה: מזהה בקשה חסר", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onReject(String requestId) {
                if (requestId != null) {
                    viewModel.rejectMatchRequest(requestId);
                } else {
                    Toast.makeText(getContext(), "שגיאה: מזהה בקשה חסר", Toast.LENGTH_SHORT).show();
                }
            }
        });
        adapter.setOnItemClickListener((contact, position) -> {
            String uid =  contact.getUserId();

        });
        recyclerView.setAdapter(adapter);

        // תצפית על רשימת הבקשות
        viewModel.getMatchRequests().observe(getViewLifecycleOwner(), requests -> {
            if (requests != null) {
                adapter.setRequests(requests);
            } else {
                adapter.setRequests(new ArrayList<>()); // טיפול בנתונים ריקים
                Toast.makeText(getContext(), "אין בקשות זמינות", Toast.LENGTH_SHORT).show();
            }
        });

        // תצפית על הודעות טוסט
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        // טעינת הבקשות
        viewModel.loadMatchRequests();
    }

    private void showShowProfileDialog(UserProfile profile)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_show_profile, null);
        builder.setView(dialogView);

        TextView name=dialogView.findViewById(R.id.textShowProfileName);
        TextView age=dialogView.findViewById(R.id.textShowProfileAge);
        TextView gender=dialogView.findViewById(R.id.textShowProfileGender);
        TextView lifestyles=dialogView.findViewById(R.id.textShowProfileLifestyles);
        TextView interests=dialogView.findViewById(R.id.textShowProfileInterests);
        TextView description=dialogView.findViewById(R.id.textShowProfileDescription);
        Button exit = dialogView.findViewById(R.id.buttonShowProfileExit);

        name.setText(profile.getFullName());
        age.setText(profile.getAge()+"");
        gender.setText(profile.getGender());
        lifestyles.setText(profile.getLifestyle());
        interests.setText(profile.getInterests());
        description.setText(profile.getDescription());

        AlertDialog dialog = builder.create();

        exit.setOnClickListener(v->{dialog.dismiss();});

        dialog.show();
    }
}