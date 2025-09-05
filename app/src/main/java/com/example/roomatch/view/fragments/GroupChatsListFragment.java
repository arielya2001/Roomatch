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
import com.example.roomatch.adapters.GroupChatsAdapter;
import com.example.roomatch.viewmodel.GroupChatViewModel;
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

        adapter.setOnChatClickListener(chat -> {
            // שלב 1: קבלת ViewModel
            GroupChatViewModel tempViewModel = new ViewModelProvider(this).get(GroupChatViewModel.class);

            // שלב 2: חיפוש groupChatId לפי groupId + apartmentId
            tempViewModel.findGroupChatId(chat.getGroupId(), chat.getApartmentId())
                    .observe(getViewLifecycleOwner(), groupChatId -> {
                        if (groupChatId != null) {
                            // שלב 3: פתיחת GroupChatFragment עם groupChatId
                            GroupChatFragment fragment = GroupChatFragment.newInstance(groupChatId);
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragmentContainer, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        } else {
                            Toast.makeText(getContext(), "לא נמצא groupChatId לצ'אט זה", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}