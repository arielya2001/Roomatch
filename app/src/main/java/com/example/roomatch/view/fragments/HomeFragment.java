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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.roomatch.R;
import com.example.roomatch.adapters.UserAdapter;
import com.example.roomatch.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView greetingTextView;
    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<UserProfile> userList = new ArrayList<>();
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

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        greetingTextView = view.findViewById(R.id.textViewGreeting);
        recyclerView = view.findViewById(R.id.recyclerViewUsers);
        logoutButton = view.findViewById(R.id.buttonLogout);

        adapter = new UserAdapter(userList, getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        logoutButton.setOnClickListener(v -> {
            if (logoutListener != null) {
                logoutListener.onLogout();
            }
        });

        loadUserData();
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        // המשתמש הנוכחי
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        greetingTextView.setText("שלום, " + name + "!");
                    }
                });

        // כל השאר
        db.collection("users").get()
                .addOnSuccessListener(query -> {
                    userList.clear();
                    for (var doc : query) {
                        if (!doc.getId().equals(uid)) {
                            UserProfile user = new UserProfile(
                                    doc.getString("fullName"),
                                    doc.getLong("age") != null ? doc.getLong("age").intValue() : 0,
                                    doc.getString("lifestyle"),
                                    doc.getString("interests")
                            );
                            userList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
