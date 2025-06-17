package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Message message);
    }

    public MessageAdapter(List<Message> messageList, OnChatClickListener listener) {
        this.messageList = messageList != null ? new ArrayList<>(messageList) : new ArrayList<>();
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
        Message msg = messageList.get(position);

        String text = msg.getText() != null ? msg.getText() : "לא זמין";
        String fromUser = msg.getFromUserId() != null ? msg.getFromUserId() : "לא ידוע";
        String aptId = msg.getApartmentId() != null ? msg.getApartmentId() : "לא זמין";

        holder.textViewMessage.setText("הודעה: " + text);
        holder.textViewFromUser.setText("מאת: " + fromUser);
        holder.textViewApartmentId.setText("עבור דירה: " + aptId);

        holder.buttonOpenChat.setOnClickListener(v -> {
            if (listener != null && !fromUser.isEmpty()) {
                listener.onChatClick(msg);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public void updateMessages(List<Message> newMessages) {
        if (newMessages != null) {
            this.messageList = new ArrayList<>(newMessages);
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