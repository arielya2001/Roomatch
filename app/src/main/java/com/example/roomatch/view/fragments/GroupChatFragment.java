package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.GroupChatAdapter;
import com.example.roomatch.viewmodel.GroupChatViewModel;

public class GroupChatFragment extends Fragment {

    private GroupChatViewModel viewModel;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private GroupChatAdapter adapter;

    private String groupChatId;

    public static GroupChatFragment newInstance(String groupChatId) {
        GroupChatFragment fragment = new GroupChatFragment();
        Bundle args = new Bundle();
        args.putString("groupChatId", groupChatId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewGroupChat);
        messageInput = view.findViewById(R.id.editTextGroupMessage);
        sendButton = view.findViewById(R.id.buttonSendGroupMessage);

        viewModel = new ViewModelProvider(
                this,
                new GroupChatViewModel.Factory(requireContext())
        ).get(GroupChatViewModel.class);
        String currentUserId = viewModel.getCurrentUserId();

        adapter = new GroupChatAdapter(currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (getArguments() != null) {
            groupChatId = getArguments().getString("groupChatId");

            if (groupChatId == null) {
                Toast.makeText(getContext(), "שגיאה: groupChatId חסר", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ ✅ ✅ הוסף שורה זו בדיוק כאן:
            viewModel.markGroupMessagesAsRead(groupChatId);

            // ✅ טעינת הודעות מה־Firestore לפי groupChatId
            viewModel.loadMessages(groupChatId).observe(getViewLifecycleOwner(), messages -> {
                adapter.updateMessages(messages);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            });

            // ✅ שליחת הודעה חדשה לצ'אט
            sendButton.setOnClickListener(v -> {
                String text = messageInput.getText().toString().trim();
                if (!text.isEmpty()) {
                    viewModel.sendMessage(groupChatId, text);
                    messageInput.setText("");
                }
            });
        }

    }
}
