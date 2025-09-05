<<<<<<< Updated upstream
package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ContactsAdapter;
import com.example.roomatch.viewmodel.ContactsViewModel;

import java.util.List;

public class ContactsFragment extends Fragment {

    private ContactsViewModel viewModel;
    private RecyclerView recyclerView;
    private Button createGroupButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.contactsRecyclerView);
        createGroupButton = view.findViewById(R.id.createGroupButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ContactsAdapter adapter = new ContactsAdapter(new ContactsAdapter.OnContactSelectionListener() {
            @Override
            public void onCreateGroup(List<String> selectedUserIds) {
                viewModel.createSharedGroup(selectedUserIds);
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);
        viewModel.getContacts().observe(getViewLifecycleOwner(), adapter::setContacts);

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());

        createGroupButton.setOnClickListener(v -> {
            List<String> selectedIds = adapter.getSelectedContacts();
            if (!selectedIds.isEmpty()) {
                viewModel.createSharedGroup(selectedIds);
            } else {
                Toast.makeText(getContext(), "בחר לפחות חבר אחד", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadContacts();
    }
=======
package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ContactsAdapter;
import com.example.roomatch.viewmodel.ContactsViewModel;

import java.util.List;

public class ContactsFragment extends Fragment {

    private ContactsViewModel viewModel;
    private RecyclerView recyclerView;
    private Button createGroupButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.contactsRecyclerView);
        createGroupButton = view.findViewById(R.id.createGroupButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ContactsAdapter adapter = new ContactsAdapter(new ContactsAdapter.OnContactSelectionListener() {
            @Override
            public void onCreateGroup(List<String> selectedUserIds) {
                viewModel.createSharedGroup(selectedUserIds);
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);
        viewModel.getContacts().observe(getViewLifecycleOwner(), contacts -> {
            android.util.Log.d("ContactsFragment", "🚀 Received " + contacts.size() + " contacts");
            adapter.setContacts(contacts);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());

        createGroupButton.setOnClickListener(v -> {
            List<String> selectedIds = adapter.getSelectedContacts();
            if (!selectedIds.isEmpty()) {
                viewModel.createSharedGroup(selectedIds);
            } else {
                Toast.makeText(getContext(), "בחר לפחות חבר אחד", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadContacts();
    }
>>>>>>> Stashed changes
}