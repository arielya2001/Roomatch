package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import java.util.Date;
import java.util.Locale;
import com.google.firebase.Timestamp;


public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<Map<String, Object>> chats;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(String fromUserId, String apartmentId);
    }

    public ChatListAdapter(List<Map<String, Object>> chats, OnChatClickListener listener) {
        this.chats = chats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Map<String, Object> chat = chats.get(position);
        String fromUserId = (String) chat.get("fromUserId");
        String apartmentId = (String) chat.get("apartmentId");
        String lastMessage = (String) chat.get("lastMessage");
        Object tsObject = chat.get("timestamp");
        String formattedTime = "";

        if (tsObject instanceof Long) {
            // אם זה Long – נניח שזה מילישניות ונמיר לתאריך
            Date date = new Date((Long) tsObject);
            formattedTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } else if (tsObject instanceof Timestamp) {
            // אם זה Firebase Timestamp (נדיר במקרה הזה)
            Timestamp ts = (Timestamp) tsObject;
            formattedTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts.toDate());
        }

        holder.textViewTime.setText("שעה: " + formattedTime);




        boolean hasUnread = (boolean) chat.get("hasUnread");

        if (hasUnread) {
            holder.textViewUnreadBadge.setVisibility(View.VISIBLE);
            holder.textViewUnreadBadge.setText("🔔 הודעה חדשה"); // ← כאן
        } else {
            holder.textViewUnreadBadge.setVisibility(View.GONE);
        }

        holder.textViewSender.setText("מאת: " + fromUserId);
        holder.textViewApartment.setText("דירה: " + apartmentId);
        holder.textViewMessage.setText("הודעה אחרונה: " + lastMessage);
        holder.textViewTime.setText("שעה: " + formattedTime);
        holder.buttonOpenChat.setOnClickListener(v -> listener.onChatClick(fromUserId, apartmentId));
        holder.textViewUnreadBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSender, textViewApartment, textViewMessage, textViewTime, textViewUnreadBadge;
        Button buttonOpenChat;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSender = itemView.findViewById(R.id.textViewSender);
            textViewApartment = itemView.findViewById(R.id.textViewApartment);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            buttonOpenChat = itemView.findViewById(R.id.buttonOpenChat);
            textViewUnreadBadge = itemView.findViewById(R.id.textViewUnreadBadge);
        }
    }
}