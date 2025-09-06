package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.FriendsAdapter;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    private RecyclerView friendsRecyclerView;
    private FriendsAdapter adapter;
    private UserRepository userRepository = new UserRepository();

    public FriendsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // הטענת Fragment של בקשות לקשר (כמו קיים)
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.matchRequestsContainer, new MatchRequestsFragment());
        transaction.commit();

        // RecyclerView של חברים
        friendsRecyclerView = view.findViewById(R.id.recyclerViewFriends);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendsAdapter(new ArrayList<>(), this::openProfileDialog);
        friendsRecyclerView.setAdapter(adapter);

        // טען חברים מ-UserRepository
        userRepository.loadFriends(friends -> {
            adapter.setData(friends);
            toggleNoFriendsMessage(friends.isEmpty());
        });
    }

    private void openProfileDialog(UserProfile profile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_show_profile, null);
        builder.setView(dialogView);

        TextView name = dialogView.findViewById(R.id.textShowProfileName);
        TextView age = dialogView.findViewById(R.id.textShowProfileAge);
        TextView gender = dialogView.findViewById(R.id.textShowProfileGender);
        TextView lifestyles = dialogView.findViewById(R.id.textShowProfileLifestyles);
        TextView interests = dialogView.findViewById(R.id.textShowProfileInterests);
        TextView description = dialogView.findViewById(R.id.textShowProfileDescription);

        name.setText(profile.getFullName());
        age.setText(profile.getAge() != null ? String.valueOf(profile.getAge()) : "—");
        gender.setText(profile.getGender());
        lifestyles.setText(profile.getLifestyle());
        interests.setText(profile.getInterests());
        description.setText(profile.getDescription());

        // צור את הדיאלוג ואז הוסף listener
        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.buttonShowProfileExit).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void toggleNoFriendsMessage(boolean show) {
        View noFriendsMsg = requireView().findViewById(R.id.textNoFriends);
        noFriendsMsg.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
