package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ChatListAdapter;
import com.example.roomatch.model.ChatListItem;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SearchView searchView;
    private ChatListAdapter adapter;
    private ChatViewModel viewModel;

    Spinner spinnerChatType;


    public ChatsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        // ViewModel
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ChatViewModel(
                        new UserRepository(),
                        new ChatRepository(requireContext()),
                        new ApartmentRepository()
                );
            }
        }).get(ChatViewModel.class);

        // UI רכיבים
        recyclerView = v.findViewById(R.id.recyclerViewChats);
        searchView = v.findViewById(R.id.searchViewChats);
        Spinner spinnerChatType = v.findViewById(R.id.spinnerChatType);

        // Adapter + RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatListAdapter(new ArrayList<>(), new ChatListAdapter.OnChatClickListener() {
            @Override
            public void onPrivateChatClick(String fromUserId, String apartmentId) {
                openPrivateChat(fromUserId, apartmentId);
            }

            @Override
            public void onGroupChatClick(String groupChatId, String apartmentId) {
                openGroupChat(groupChatId);
            }
        });
        recyclerView.setAdapter(adapter);

        // Spinner - סוג צ'אט
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"הכל", "צ'אט פרטי", "צ'אט קבוצתי"}
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChatType.setAdapter(typeAdapter);

        // חיפוש + סינון
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) {
                filterBySearchAndType(q, spinnerChatType.getSelectedItemPosition());
                return true;
            }

            @Override public boolean onQueryTextChange(String q) {
                filterBySearchAndType(q, spinnerChatType.getSelectedItemPosition());
                return true;
            }
        });

        spinnerChatType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String query = searchView.getQuery().toString();
                filterBySearchAndType(query, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // LiveData
        viewModel.getChats().observe(getViewLifecycleOwner(), chats -> {
            if (chats != null) {
                Log.d("ChatsFragment", "📥 התקבלו " + chats.size() + " צ'אטים מ־ViewModel");

                for (ChatListItem item : chats) {
                    Log.d("ChatsFragment", "🧾 פריט: " +
                            (item.isGroup() ? "קבוצתי" : "פרטי") +
                            " | כותרת: " + item.getTitle() +
                            " | מאת: " + item.getLastMessageSenderName() +
                            " | הודעה: " + item.getLastMessage());
                }

                filterBySearchAndType(searchView.getQuery().toString(), spinnerChatType.getSelectedItemPosition());
            }
        });


        viewModel.getToast().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        // טען צ'אטים
        viewModel.loadChats();
    }

    private void filterBySearchAndType(String query, int typePosition) {
        if (viewModel.getChats().getValue() == null) return;

        List<ChatListItem> all = viewModel.getChats().getValue();
        List<ChatListItem> filtered = new ArrayList<>();

        String lowerQuery = query.toLowerCase();

        for (ChatListItem item : all) {
            boolean matchesQuery =
                    (item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerQuery)) ||
                            (item.getSubText() != null && item.getSubText().toLowerCase().contains(lowerQuery)) ||
                            (item.getLastMessage() != null && item.getLastMessage().toLowerCase().contains(lowerQuery)) ||
                            (item.getLastMessageSenderName() != null && item.getLastMessageSenderName().toLowerCase().contains(lowerQuery));

            boolean matchesType = switch (typePosition) {
                case 0 -> true; // הכל
                case 1 -> !item.isGroup(); // פרטי
                case 2 -> item.isGroup(); // קבוצתי
                default -> true;
            };

            if (matchesQuery && matchesType) {
                Log.d("ChatsFragment", "✅ עבר סינון: " + item.getTitle() +
                        " | מאת: " + item.getLastMessageSenderName() +
                        " | הודעה: " + item.getLastMessage());

                filtered.add(item);
            }
        }

        Log.d("ChatsFragment", "📊 כמות אחרי סינון: " + filtered.size());
        adapter.updateChats(filtered);
    }





    private void openChat(String fromUserId, String apartmentId) {
        String me = viewModel.getCurrentUserId();
        if (me == null) {
            Toast.makeText(getContext(), "שגיאה: משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }
        ChatFragment cf = new ChatFragment(fromUserId, apartmentId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, cf)
                .addToBackStack(null)
                .commit();
    }

    private void openPrivateChat(String fromUserId, String apartmentId) {
        ChatFragment cf = new ChatFragment(fromUserId, apartmentId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, cf)
                .addToBackStack(null)
                .commit();
    }

    private void openGroupChat(String groupChatId) {
        Log.d("ChatsFragment", "פותח צ'אט קבוצתי עם groupChatId: " + groupChatId);
        GroupChatFragment fragment = GroupChatFragment.newInstance(groupChatId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

}