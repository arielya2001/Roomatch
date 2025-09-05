package com.example.roomatch.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.GroupChatMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GroupChatViewHolder> {

    private List<GroupChatMessage> messages = new ArrayList<>();
    private final String currentUserId;

    public GroupChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public GroupChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_chat_message, parent, false);
        return new GroupChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupChatViewHolder holder, int position) {
        GroupChatMessage msg = messages.get(position);

        holder.textViewMessage.setText(msg.getContent());
        holder.textViewSenderName.setText("מאת: " + msg.getSenderName());

        if (msg.getSenderId().equals(currentUserId)) {
            holder.messageContainer.setGravity(Gravity.END);
        } else {
            holder.messageContainer.setGravity(Gravity.START);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }


    public void updateMessages(List<GroupChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            // סינון הודעות עם timestamp לא חוקי (0 או שלילי)
            newMessages.removeIf(m -> m.getTimestamp() <= 0);

            // מיון לפי timestamp
            newMessages.sort(Comparator.comparingLong(GroupChatMessage::getTimestamp));

            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }



    static class GroupChatViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage, textViewSenderName;
        LinearLayout messageContainer;

        public GroupChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textMessage);
            textViewSenderName = itemView.findViewById(R.id.textSenderName);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }
    }
}
