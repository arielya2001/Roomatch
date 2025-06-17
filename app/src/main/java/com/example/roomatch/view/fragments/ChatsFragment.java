package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.viewmodel.ChatViewModel;

import java.util.ArrayList;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SearchView searchView;
    private ChatListAdapter adapter;
    private ChatViewModel viewModel;

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
            @NonNull @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> cls) {
                return (T) new ChatViewModel(new ChatRepository(), new UserRepository());
            }
        }).get(ChatViewModel.class);

        // RecyclerView
        recyclerView = v.findViewById(R.id.recyclerViewChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatListAdapter(new ArrayList<>(), this::openChat);
        recyclerView.setAdapter(adapter);

        // SearchView
        searchView = v.findViewById(R.id.searchViewChats);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) {
                viewModel.filterChats(q);
                return true;
            }
            @Override public boolean onQueryTextChange(String q) {
                viewModel.filterChats(q);
                return true;
            }
        });

        // LiveData observers
        viewModel.getChats().observe(getViewLifecycleOwner(), chats -> {
            if (chats != null) {
                adapter.updateChats(chats);
                if (chats.isEmpty() && viewModel.getChats().getValue() == null) {
                    Toast.makeText(getContext(), "אין צ'אטים זמינים", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        // טען צ'אטים
        viewModel.loadChats();
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
}