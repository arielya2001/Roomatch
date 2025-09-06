package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.SharedGroupsAdapter;
import com.example.roomatch.viewmodel.SharedGroupsViewModel;

public class SharedGroupsFragment extends Fragment {

    private SharedGroupsViewModel viewModel;
    private RecyclerView recyclerView;
    private SharedGroupsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shared_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("SharedGroups", "onViewCreated: Fragment loaded");

        recyclerView = view.findViewById(R.id.sharedGroupsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        Log.d("SharedGroups", "RecyclerView set");

        viewModel = new ViewModelProvider(this).get(SharedGroupsViewModel.class);
        Log.d("SharedGroups", "ViewModel created");

        adapter = new SharedGroupsAdapter(viewModel.getCurrentUserId());
        recyclerView.setAdapter(adapter);
        Log.d("SharedGroups", "Adapter set with currentUserId: " + viewModel.getCurrentUserId());

        viewModel.getSharedGroups().observe(getViewLifecycleOwner(), groups -> {
            Log.d("SharedGroups", "LiveData<groups> updated. Total groups: " + (groups != null ? groups.size() : 0));
            adapter.setGroups(groups);
            viewModel.loadUserNamesForGroups(groups);
        });

        viewModel.getUserNamesMap().observe(getViewLifecycleOwner(), map -> {
            Log.d("SharedGroups", "User names map updated. Total entries: " + (map != null ? map.size() : 0));
            adapter.setUserIdToNameMap(map);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            Log.d("SharedGroups", "Toast message: " + msg);
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        Log.d("SharedGroups", "Calling viewModel.loadSharedGroups()");
        viewModel.loadSharedGroups();

        ImageButton addGroupButton = view.findViewById(R.id.buttonAddGroup);
        addGroupButton.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new ContactsFragment()) // ודא שזה ה-ID של ה-container
                    .addToBackStack(null)
                    .commit();
        });
    }

}
