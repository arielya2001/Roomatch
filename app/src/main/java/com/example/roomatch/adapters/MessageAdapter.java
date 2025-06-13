package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Map<String, Object>> messageList;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(String fromUserId);
    }

    public MessageAdapter(List<Map<String, Object>> messageList, OnChatClickListener listener) {
        this.messageList = messageList != null ? messageList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_card, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Map<String, Object> msg = messageList.get(position);
        String text = msg.get("text") != null ? msg.get("text").toString() : "לא זמין";
        String fromUser = msg.get("fromUserId") != null ? msg.get("fromUserId").toString() : "לא ידוע";
        String aptId = msg.get("apartmentId") != null ? msg.get("apartmentId").toString() : "לא זמין";

        holder.textViewMessage.setText("הודעה: " + text);
        holder.textViewFromUser.setText("מאת: " + fromUser);
        holder.textViewApartmentId.setText("עבור דירה: " + aptId);

        holder.buttonOpenChat.setOnClickListener(v -> {
            if (listener != null && !fromUser.isEmpty()) {
                listener.onChatClick(fromUser);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public void updateMessages(List<Map<String, Object>> newMessages) {
        if (newMessages != null) {
            this.messageList = newMessages;
            notifyDataSetChanged();
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage, textViewFromUser, textViewApartmentId;
        Button buttonOpenChat;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessageText);
            textViewFromUser = itemView.findViewById(R.id.textViewFromUser);
            textViewApartmentId = itemView.findViewById(R.id.textViewApartmentId);
            buttonOpenChat = itemView.findViewById(R.id.buttonOpenChat);
        }
    }
}