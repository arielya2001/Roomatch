package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ChatListAdapter;
import com.example.roomatch.utils.ChatUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SearchView searchView;
    private ChatListAdapter adapter;

    private List<Map<String, Object>> chats = new ArrayList<>();
    private List<Map<String, Object>> allChats = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public ChatsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recyclerViewChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        searchView = view.findViewById(R.id.searchViewChats);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterChats(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterChats(newText);
                return true;
            }
        });

        adapter = new ChatListAdapter(chats, this::openChat);
        recyclerView.setAdapter(adapter);

        loadChats();
    }

    private void loadChats() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collectionGroup("chat")
                .whereEqualTo("toUserId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Map<String, Object>> uniqueChats = new HashMap<>();
                    List<Map<String, Object>> tempList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Map<String, Object> data = doc.getData();
                        String fromUserId = (String) data.get("fromUserId");
                        String apartmentId = (String) data.get("apartmentId");
                        String text = (String) data.get("text");
                        Long timestamp = doc.getLong("timestamp");
                        Boolean isRead = doc.getBoolean("read");

                        if (fromUserId != null && apartmentId != null && text != null && timestamp != null) {
                            String chatKey = fromUserId + "_" + apartmentId;
                            if (!uniqueChats.containsKey(chatKey)) {
                                Map<String, Object> chat = new HashMap<>();
                                chat.put("fromUserId", fromUserId);
                                chat.put("apartmentId", apartmentId);
                                chat.put("lastMessage", text);
                                chat.put("timestamp", timestamp);
                                chat.put("hasUnread", isRead != null ? !isRead : true);

                                // TODO: בעתיד אפשר למשוך שם משתמש מלא מ־Users או Apartments
                                chat.put("fromUserName", fromUserId); // זמני
                                chat.put("apartmentName", apartmentId); // זמני

                                uniqueChats.put(chatKey, chat);
                                tempList.add(chat);
                            }
                        }
                    }

                    allChats.clear();
                    allChats.addAll(tempList);

                    chats.clear();
                    chats.addAll(tempList);
                    adapter.notifyDataSetChanged();

                    if (chats.isEmpty()) {
                        Toast.makeText(getContext(), "אין צ'אטים זמינים", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatsFragment", "Error loading chats: " + e.getMessage());
                    Toast.makeText(getContext(), "שגיאה בטעינת צ'אטים: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void filterChats(String query) {
        String lowerQuery = query.toLowerCase();
        chats.clear();

        for (Map<String, Object> chat : allChats) {
            String sender = ((String) chat.get("fromUserName")).toLowerCase();
            String apartment = ((String) chat.get("apartmentName")).toLowerCase();
            if (sender.contains(lowerQuery) || apartment.contains(lowerQuery)) {
                chats.add(chat);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void openChat(String fromUserId, String apartmentId) {
        ChatFragment chatFragment = new ChatFragment(fromUserId, apartmentId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, chatFragment)
                .addToBackStack(null)
                .commit();
    }
}
