package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.roomatch.model.UserProfile;
import com.google.firebase.Timestamp;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<Chat> chats;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(String fromUserId, String apartmentId);
    }

    public ChatListAdapter(List<Chat> chats, OnChatClickListener listener) {
        this.chats = new ArrayList<>(chats != null ? chats : new ArrayList<>());
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
        Chat chat = chats.get(position);

        String fromUserId = chat.getFromUserId() != null ? chat.getFromUserId() : "משתמש לא ידוע";
        String apartmentId = chat.getApartmentId() != null ? chat.getApartmentId() : "דירה לא זמינה";
        String lastMessage = chat.getLastMessage() != null && chat.getLastMessage().getText() != null
                ? chat.getLastMessage().getText() : "אין הודעה";
        String formattedTime = "לא זמין";

        if (chat.getTimestamp() != null) {
            Date date = chat.getTimestamp().toDate();
            formattedTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        }

        boolean hasUnread = chat.isHasUnread();
        holder.textViewTime.setText(formattedTime+"");
        holder.textViewSender.setText("מאת: " + fromUserId);
        holder.textViewApartment.setText("דירה: " + apartmentId);
        holder.textViewMessage.setText("הודעה אחרונה: " + lastMessage);
        holder.textViewUnreadBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onChatClick(fromUserId, apartmentId));
        //holder.buttonOpenChat.setOnClickListener(v -> listener.onChatClick(fromUserId, apartmentId));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    /**
     * מעדכן את רשימת הצ'אטים ומתריע על שינוי.
     */
    public void updateChats(List<Chat> newChats) {
        chats.clear();
        if (newChats != null) {
            chats.addAll(newChats);
        }
        notifyDataSetChanged();
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
            //buttonOpenChat = itemView.findViewById(R.id.buttonOpenChat);
            textViewUnreadBadge = itemView.findViewById(R.id.textViewUnreadBadge);
        }
    }
}