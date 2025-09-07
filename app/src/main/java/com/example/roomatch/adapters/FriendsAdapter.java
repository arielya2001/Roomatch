package com.example.roomatch.adapters;

import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.UserProfile;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    public interface OnFriendClickListener {
        void onFriendClick(UserProfile profile);
    }

    private List<UserProfile> friends;
    private final OnFriendClickListener listener;

    public FriendsAdapter(List<UserProfile> friends, OnFriendClickListener listener) {
        this.friends = friends;
        this.listener = listener;
    }

    public void setData(List<UserProfile> newFriends) {
        this.friends = newFriends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        UserProfile friend = friends.get(position);
        holder.bind(friend);
    }

    @Override
    public int getItemCount() {
        return friends != null ? friends.size() : 0;
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        //private final TextView lifestyleText;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textFriendName);
            //lifestyleText = itemView.findViewById(R.id.textFriendLifestyle);
        }

        public void bind(UserProfile profile) {
            nameText.setText(profile.getFullName() != null ? profile.getFullName() : "אנונימי");
            //lifestyleText.setText(profile.getLifestyle() != null ? profile.getLifestyle() : "לא צויין");

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFriendClick(profile);
            });
        }
    }
}
