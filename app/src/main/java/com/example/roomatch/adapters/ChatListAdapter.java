package com.example.roomatch.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.Chat;
import com.example.roomatch.model.ChatListItem;
import com.example.roomatch.model.GroupChatListItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<ChatListItem> items;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onPrivateChatClick(String fromUserId, String apartmentId);
        void onGroupChatClick(String groupId, String apartmentId);
    }

    public ChatListAdapter(List<ChatListItem> items, OnChatClickListener listener) {
        this.items = new ArrayList<>(items != null ? items : new ArrayList<>());
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
        ChatListItem item = items.get(position);

        Log.d("ChatAdapter", "binding item: title=" + item.getTitle()
                + " | sub=" + item.getSubText()
                + " | message=" + item.getLastMessage()
                + " | sender=" + item.getLastMessageSenderName()
                + " | participants=" + item.getParticipantsString());

        String time = formatTime(item.getTimestamp());
        holder.textViewTime.setText(time);
        holder.textViewSender.setText("הודעה אחרונה מאת: " + item.getLastMessageSenderName());
        holder.textViewApartment.setText(item.getSubText());
        holder.textViewMessage.setText("הודעה אחרונה: " + item.getLastMessage());

        String participants = item.getParticipantsString();
        if (participants != null && !participants.isEmpty()) {
            holder.textViewParticipants.setText("משתתפים בצ'אט: " + participants);
            holder.textViewParticipants.setVisibility(View.VISIBLE);
        } else {
            holder.textViewParticipants.setVisibility(View.GONE);
        }


        holder.textViewUnreadBadge.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (item.isGroup()) {
                GroupChatListItem g = (GroupChatListItem) item;
                listener.onGroupChatClick(g.getGroupId(), g.getApartmentId());
            } else {
                Chat chat = (Chat) item;
                listener.onPrivateChatClick(chat.getFromUserId(), chat.getApartmentId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateChats(List<ChatListItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    private String formatTime(long timestamp) {
        try {
            Date date = new Date(timestamp);
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "לא זמין";
        }
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSender, textViewApartment, textViewMessage, textViewTime, textViewUnreadBadge, textViewParticipants;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSender = itemView.findViewById(R.id.textViewSender);
            textViewApartment = itemView.findViewById(R.id.textViewApartment);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewUnreadBadge = itemView.findViewById(R.id.textViewUnreadBadge);
            textViewParticipants = itemView.findViewById(R.id.textViewParticipants); // 🆕
        }
    }
}
