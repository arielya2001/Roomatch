package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.GroupChatsAdapter;
import com.example.roomatch.viewmodel.GroupChatsViewModel;

import java.util.ArrayList;

public class GroupChatsListFragment extends Fragment {

    private GroupChatsViewModel viewModel;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_chats_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewGroupChats);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        GroupChatsAdapter adapter = new GroupChatsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(GroupChatsViewModel.class);
        viewModel.loadGroupChats().observe(getViewLifecycleOwner(), adapter::setChats);
        viewModel.getApartmentIdToAddressMap().observe(getViewLifecycleOwner(), adapter::setApartmentIdToAddressMap);

        // 💥 הוספת ה־click listener:
        adapter.setOnChatClickListener(chat -> {
            GroupChatFragment fragment = new GroupChatFragment();
            Bundle args = new Bundle();
            args.putString("groupId", chat.getGroupId());
            args.putString("apartmentId", chat.getApartmentId());
            fragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment) // וודא שיש לך container כזה ב־activity
                    .addToBackStack(null)
                    .commit();
        });
    }

}