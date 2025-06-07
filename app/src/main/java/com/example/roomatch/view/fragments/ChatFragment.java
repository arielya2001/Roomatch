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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class ChatFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String otherUserId;
    private String chatId;

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private Button sendButton, backButton;

    private List<Map<String, Object>> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private ListenerRegistration listener;

    public ChatFragment(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        chatId = getSortedChatId(currentUid, otherUserId);

        recyclerView = view.findViewById(R.id.recyclerViewMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter(messages, currentUid);
        recyclerView.setAdapter(adapter);

        messageEditText = view.findViewById(R.id.editTextMessage);
        sendButton = view.findViewById(R.id.buttonSend);
        backButton = view.findViewById(R.id.buttonBack);

        sendButton.setOnClickListener(v -> sendMessage(currentUid));
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        startMessageListener();
    }

    private void sendMessage(String currentUid) {
        String text = messageEditText.getText().toString().trim();
        if (text.isEmpty()) return;

        Map<String, Object> message = new HashMap<>();
        message.put("fromUserId", currentUid);
        message.put("toUserId", otherUserId);
        message.put("text", text);
        message.put("timestamp", System.currentTimeMillis());

        db.collection("messages")
                .document(chatId)
                .collection("chat")
                .add(message)
                .addOnSuccessListener(docRef -> {
                    messageEditText.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatFragment", "Error sending message", e);
                    Toast.makeText(getContext(), "שגיאה בשליחת הודעה", Toast.LENGTH_SHORT).show();
                });
    }

    private void startMessageListener() {
        listener = db.collection("messages")
                .document(chatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("ChatFragment", "Listener error", error);
                        return;
                    }

                    if (snapshot != null) {
                        messages.clear();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            messages.add(doc.getData());
                        }
                        adapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private String getSortedChatId(String user1, String user2) {
        List<String> sorted = Arrays.asList(user1, user2);
        Collections.sort(sorted);
        return sorted.get(0) + "_" + sorted.get(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) {
            listener.remove(); // מנקה את המאזין
        }
    }
}
