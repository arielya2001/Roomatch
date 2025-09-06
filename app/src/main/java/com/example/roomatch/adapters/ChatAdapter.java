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
import com.example.roomatch.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Message> messages;
    private String currentUserId;

    public ChatAdapter(List<Message> messages, String currentUserId) {
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
        Message message = messages.get(position);

        String text = message.getText() != null ? message.getText() : "הודעה ריקה";
        String fromUserId = message.getFromUserId() != null ? message.getFromUserId() : "";
        long timestamp = message.getTimestamp();
        String time= formatTime(timestamp);
        String date= formatDate(timestamp);
        long nowMs = System.currentTimeMillis();
        String currentDate=formatDate(nowMs);
        if(currentDate.equals(date))
        {
            holder.messageText.setText(text+"\n"+time);
        }
        else {
            holder.messageText.setText(text+"\n"+time+" "+ date);
        }

        // יישור לפי השולח
        if (fromUserId.equals(currentUserId)) {
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
    public void updateMessages(List<Message> newMessages) {
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

    private String formatTime(long timestamp) {
        try {
            Date date = new Date(timestamp);
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "לא זמין";
        }
    }

    private String formatDate(long timestamp)
    {
        try {
            Date date = new Date(timestamp);
            return new SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "לא זמין";
        }
    }
}