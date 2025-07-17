package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.UserAdapter;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.viewmodel.HomeViewModel;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private TextView greetingTextView;
    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private Button logoutButton;

    public interface OnLogoutListener {
        void onLogout();
    }

    private OnLogoutListener logoutListener;

    public HomeFragment(OnLogoutListener listener) {
        this.logoutListener = listener;
    }

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        greetingTextView = view.findViewById(R.id.textViewGreeting);
        recyclerView = view.findViewById(R.id.recyclerViewUsers);
        logoutButton = view.findViewById(R.id.buttonLogout);

        adapter = new UserAdapter(new ArrayList<>(), getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        logoutButton.setOnClickListener(v -> {
            if (logoutListener != null) {
                logoutListener.onLogout();
            }
        });

        viewModel.getGreeting().observe(getViewLifecycleOwner(), greeting -> {
            if (greeting != null) {
                greetingTextView.setText(greeting);
            }
        });

        viewModel.getUserList().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                adapter.updateUsers(users);
            }
        });
    }
}