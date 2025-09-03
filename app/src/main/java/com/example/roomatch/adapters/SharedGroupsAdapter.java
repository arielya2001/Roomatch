package com.example.roomatch.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.SharedGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedGroupsAdapter extends RecyclerView.Adapter<SharedGroupsAdapter.GroupViewHolder> {

    private List<SharedGroup> groups = new ArrayList<>();
    private Map<String, String> userIdToNameMap = new HashMap<>();
    private final String currentUserId;

    public SharedGroupsAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setGroups(List<SharedGroup> groups) {
        this.groups = groups;
        notifyDataSetChanged();
    }

    public void setUserIdToNameMap(Map<String, String> map) {
        this.userIdToNameMap = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shared_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        SharedGroup group = groups.get(position);
        holder.groupNameTextView.setText(group.getName());

        // הצגת כל שמות החברים
        List<String> memberNames = new ArrayList<>();
        for (String memberId : group.getMemberIds()) {
            String name = userIdToNameMap.get(memberId);
            if (name != null) {
                memberNames.add(name);
            }
        }
        holder.membersTextView.setText("חברים: " + TextUtils.join(", ", memberNames));

        // הצגת המנהל
        String adminId = getAdminId(group);
        if (adminId != null) {
            String adminName = userIdToNameMap.get(adminId);
            if (adminName != null) {
                holder.managerNameTextView.setText("מנהל: " + adminName);
                holder.managerCrown.setVisibility(View.VISIBLE);
            } else {
                holder.managerNameTextView.setText("מנהל: לא ידוע");
                holder.managerCrown.setVisibility(View.GONE);
            }
        } else {
            holder.managerNameTextView.setText("אין מנהל");
            holder.managerCrown.setVisibility(View.GONE);
        }
    }

    private String getAdminId(SharedGroup group) {
        for (Map.Entry<String, String> entry : group.getRoles().entrySet()) {
            if ("admin".equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return groups != null ? groups.size() : 0;
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupNameTextView;
        TextView membersTextView;
        TextView managerNameTextView;
        TextView managerCrown;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            membersTextView = itemView.findViewById(R.id.membersTextView);
            managerNameTextView = itemView.findViewById(R.id.managerNameTextView);
            managerCrown = itemView.findViewById(R.id.managerCrown);
        }
    }
}
