package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ChatAdapter;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private ChatViewModel viewModel;
    private RecyclerView recyclerView;
    private EditText messageEditText;
    private Button sendButton, backButton;
    private ChatAdapter adapter;
    private String otherUserId;
    private String apartmentId;
    private String chatId;

    public ChatFragment(String otherUserId, String apartmentId) {
        this.otherUserId = otherUserId;
        this.apartmentId = apartmentId;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> c) {
                return (T) new ChatViewModel(new ChatRepository(requireContext()), new UserRepository());
            }
        }).get(ChatViewModel.class);


        String currentUid = viewModel.getCurrentUserId();
        if (currentUid == null) {
            Toast.makeText(getContext(), "שגיאה: משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת chatId עקבי על ידי מיון
        chatId = generateConsistentChatId(currentUid, otherUserId, apartmentId);

        recyclerView = view.findViewById(R.id.recyclerViewMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter(new ArrayList<>(), currentUid);
        recyclerView.setAdapter(adapter);

        messageEditText = view.findViewById(R.id.editTextMessage);
        sendButton = view.findViewById(R.id.buttonSend);
        backButton = view.findViewById(R.id.buttonBack);

        sendButton.setOnClickListener(v -> {
            String text = messageEditText.getText().toString().trim();
            if (!text.isEmpty()) {
                viewModel.sendMessage(chatId, otherUserId, apartmentId, text);
                messageEditText.setText("");
            }
        });
        backButton.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack());

        // האזנה להודעות עם הגבלה של 20 הודעות
        viewModel.getChatMessagesQuery(chatId, 20).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listener error: " + e.getMessage());
                return;
            }
            if (snapshot != null) {
                Log.d(TAG, "Received snapshot with " + snapshot.getDocuments().size() + " messages");
                List<Message> newMessages = new ArrayList<>();
                for (var doc : snapshot.getDocuments()) {
                    Message message = doc.toObject(Message.class);
                    if (message != null) {
                        message.setId(doc.getId());
                        newMessages.add(message);
                    }
                }
                adapter.updateMessages(newMessages);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
        });

        // סימון הודעות כנקראות
        viewModel.markMessagesAsRead(chatId);

        // צפייה בהודעות Toast
        viewModel.getToast().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * יוצר chatId עקבי על ידי מיון של userIds ואז להוסיף את apartmentId.
     */
    private String generateConsistentChatId(String userId1, String userId2, String apartmentId) {
        if (userId1 == null || userId2 == null || apartmentId == null) {
            throw new IllegalArgumentException("User IDs and apartment ID must not be null");
        }

        List<String> ids = Arrays.asList(userId1, userId2);
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1) + "_" + apartmentId;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // המאזין יוסר אוטומטית עם החיים של ה-Fragment
    }
}