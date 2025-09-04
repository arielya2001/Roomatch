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

    private String groupId;
    private String apartmentId;

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

        viewModel = new ViewModelProvider(this).get(GroupChatViewModel.class);
        String currentUserId = viewModel.getCurrentUserId();

        adapter = new GroupChatAdapter(currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
            apartmentId = getArguments().getString("apartmentId");

            if (groupId == null || apartmentId == null) {
                Toast.makeText(getContext(), "נתונים חסרים לפתיחת צ'אט", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🧠 חיפוש groupChatId אמיתי מה־Firestore
            viewModel.findGroupChatId(groupId, apartmentId).observe(getViewLifecycleOwner(), foundChatId -> {
                if (foundChatId == null) {
                    Toast.makeText(getContext(), "צ'אט קבוצתי לא נמצא", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ✅ טעינת הודעות עם ה־chatId שנמצא בפועל
                viewModel.loadMessages(foundChatId).observe(getViewLifecycleOwner(), messages -> {
                    adapter.updateMessages(messages);
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                });

                // ✅ שליחת הודעה עם ה־chatId האמיתי
                sendButton.setOnClickListener(v -> {
                    String text = messageInput.getText().toString().trim();
                    if (!text.isEmpty()) {
                        viewModel.sendMessage(foundChatId, text);
                        messageInput.setText("");
                    }
                });
            });
        }
    }
}
