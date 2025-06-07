package com.example.roomatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.UserProfile;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<UserProfile> userList;
    private Context context;

    public UserAdapter(List<UserProfile> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_profile, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserProfile user = userList.get(position);
        holder.name.setText("שם: " + user.getFullName());
        holder.age.setText("גיל: " + user.getAge());
        holder.lifestyle.setText("סגנון חיים: " + user.getLifestyle());
        holder.interests.setText("תחומי עניין: " + user.getInterests());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView name, age, lifestyle, interests;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textViewName);
            age = itemView.findViewById(R.id.textViewAge);
            lifestyle = itemView.findViewById(R.id.textViewLifestyle);
            interests = itemView.findViewById(R.id.textViewInterests);
        }
    }
}
