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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Map<String, Object>> messages;
    private String currentUserId;

    public ChatAdapter(List<Map<String, Object>> messages, String currentUserId) {
        this.messages = new ArrayList<>(messages != null ? messages : new ArrayList<>());
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Map<String, Object> message = messages.get(position);

        String fromUserId = (String) message.get("fromUserId");
        String text = (String) message.get("text");

        holder.messageText.setText(text);

        // יישור לפי השולח
        if (fromUserId != null && fromUserId.equals(currentUserId)) {
            holder.container.setGravity(Gravity.END);
        } else {
            holder.container.setGravity(Gravity.START);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * מעדכן את רשימת ההודעות ומתריע על שינוי.
     */
    public void updateMessages(List<Map<String, Object>> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        LinearLayout container;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textMessage);
            container = itemView.findViewById(R.id.messageContainer);
        }
    }
}