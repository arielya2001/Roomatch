package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.GroupChat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatsAdapter extends RecyclerView.Adapter<GroupChatsAdapter.ChatViewHolder> {

    private List<GroupChat> chats;
    private OnChatClickListener listener;

    private Map<String, String> apartmentIdToAddressMap = new HashMap<>();


    public interface OnChatClickListener {
        void onChatClick(GroupChat chat);
    }

    public GroupChatsAdapter(List<GroupChat> chats) {
        this.chats = chats;
    }

    public void setChats(List<GroupChat> newChats) {
        this.chats = newChats;
        notifyDataSetChanged();
    }
    public void setApartmentIdToAddressMap(Map<String, String> map) {
        this.apartmentIdToAddressMap = map;
        notifyDataSetChanged();
    }


    public void setOnChatClickListener(OnChatClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        GroupChat chat = chats.get(position);

        // שימוש בכתובת מלאה במקום שם עיר בלבד
        String address = apartmentIdToAddressMap.getOrDefault(chat.getApartmentId(), "כתובת לא ידועה");

        holder.chatTitle.setText("צ'אט עם דירה: " + address);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });
    }



    @Override
    public int getItemCount() {
        return chats != null ? chats.size() : 0;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView chatTitle;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatTitle = itemView.findViewById(R.id.textViewChatTitle);
        }
    }
}
