<<<<<<< Updated upstream
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
=======
package com.example.roomatch.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.SharedGroup;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedGroupsAdapter extends RecyclerView.Adapter<SharedGroupsAdapter.GroupViewHolder> {

    private List<SharedGroup> groups = new ArrayList<>();
    private Map<String, String> userIdToNameMap = new HashMap<>();
    private final String currentUserId;

    ImageView deleteGroupButton;


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

        if (adminId != null && adminId.equals(currentUserId)) {
            holder.editGroupNameButton.setVisibility(View.VISIBLE);
            holder.deleteGroupButton.setVisibility(View.VISIBLE);

            holder.editGroupNameButton.setOnClickListener(v -> {
                showEditGroupNameDialog(v.getContext(), group);
            });

            holder.deleteGroupButton.setOnClickListener(v -> {
                showDeleteGroupDialog(v.getContext(), group.getId(), position);
            });

        } else {
            holder.editGroupNameButton.setVisibility(View.GONE);
            holder.deleteGroupButton.setVisibility(View.GONE);
        }

    }

    private void showDeleteGroupDialog(Context context, String groupId, int position) {
        new AlertDialog.Builder(context)
                .setTitle("מחיקת קבוצה")
                .setMessage("האם אתה בטוח שברצונך למחוק את הקבוצה?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    FirebaseFirestore.getInstance()
                            .collection("shared_groups")
                            .document(groupId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                groups.remove(position);
                                notifyItemRemoved(position);
                                Toast.makeText(context, "הקבוצה נמחקה", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "שגיאה במחיקת הקבוצה", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("ביטול", null)
                .show();
    }


    private void showEditGroupNameDialog(Context context, SharedGroup group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("עריכת שם הקבוצה");

        final EditText input = new EditText(context);
        input.setText(group.getName());
        builder.setView(input);

        builder.setPositiveButton("שמור", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                FirebaseFirestore.getInstance()
                        .collection("shared_groups")
                        .document(group.getId())
                        .update("name", newName)
                        .addOnSuccessListener(aVoid -> {
                            group.setName(newName);
                            notifyDataSetChanged();
                            Toast.makeText(context, "שם הקבוצה עודכן", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "שגיאה בעדכון השם", Toast.LENGTH_SHORT).show());
            }
        });

        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.cancel());
        builder.show();
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
        ImageView editGroupNameButton;
        ImageView deleteGroupButton;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            membersTextView = itemView.findViewById(R.id.membersTextView);
            managerNameTextView = itemView.findViewById(R.id.managerNameTextView);
            managerCrown = itemView.findViewById(R.id.managerCrown);
            editGroupNameButton = itemView.findViewById(R.id.editGroupNameButton);
            deleteGroupButton = itemView.findViewById(R.id.deleteGroupButton);
        }
    }

}
>>>>>>> Stashed changes
