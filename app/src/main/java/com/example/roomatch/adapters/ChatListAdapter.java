package com.example.roomatch.adapters;

import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private final String currentUserId;

    public interface OnChatClickListener {
        void onPrivateChatClick(String chatId, String otherUserId, String apartmentId);
        void onGroupChatClick(String groupChatId, String apartmentId, boolean isGroup);
    }

    public ChatListAdapter(List<ChatListItem> items, String currentUserId, OnChatClickListener listener) {
        this.items = new ArrayList<>(items != null ? items : new ArrayList<>());
        this.listener = listener;
        this.currentUserId = currentUserId;
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
        String date = formatDate(item.getTimestamp());
        long nowMs = System.currentTimeMillis();
        String currentDate=formatDate(nowMs);
        if(currentDate.equals(date))
        {
            holder.textViewTime.setText(time);
        }
        else
        {
            holder.textViewTime.setText(date);
        }

        holder.textViewSender.setText("הודעה אחרונה מאת: " + item.getLastMessageSenderName());
        holder.textViewApartment.setText("כתובת: " + item.getSubText());
        holder.textViewMessage.setText("הודעה אחרונה: " + item.getLastMessage());
        if(item.isGroup())
        {
            holder.imageMessageType.setImageResource(R.mipmap.ic_group_foreground);
        }
        else
        {
            holder.imageMessageType.setImageResource(R.mipmap.ic_person_foreground);
        }


        String participants = item.getParticipantsString();
        if (participants != null && !participants.isEmpty()) {
            holder.textViewParticipants.setText("משתתפים בצ'אט: " + participants);
            holder.textViewParticipants.setVisibility(View.VISIBLE);
        } else {
            holder.textViewParticipants.setVisibility(View.GONE);
        }


        boolean hasUnread = false;

        if (!item.isGroup()) {
            hasUnread = ((Chat) item).isHasUnread();
        } else {
            hasUnread = ((GroupChatListItem) item).isHasUnread();
        }

        if (hasUnread) {
            holder.textViewUnreadBadge.setText("📨");
            holder.textViewUnreadBadge.setVisibility(View.VISIBLE);
        } else {
            holder.textViewUnreadBadge.setVisibility(View.GONE);
        }



        holder.itemView.setOnClickListener(v -> {
            if (item.isGroup()) {
                GroupChatListItem g = (GroupChatListItem) item;
                listener.onGroupChatClick(g.getId(), g.getApartmentId(), true);
            } else {
                Chat chat = (Chat) item;

                String other = null;

                // 1) אם יש שני מזהים מפורשים במודל
                if (chat.getFromUserId() != null && chat.getToUserId() != null) {
                    other = currentUserId != null && currentUserId.equals(chat.getFromUserId())
                            ? chat.getToUserId() : chat.getFromUserId();
                }

                // 2) אחרת, נסה מתוך from/to אם קיימים
                if (other == null && chat.getFromUserId() != null && chat.getToUserId() != null) {
                    other = currentUserId != null && currentUserId.equals(chat.getFromUserId())
                            ? chat.getToUserId() : chat.getFromUserId();
                }

                // 3) אחרת, נסה לפענח מתוך chatId בפורמט userA_userB_apartmentId
                if (other == null && chat.getId() != null && chat.getId().contains("_") && currentUserId != null) {
                    String[] parts = chat.getId().split("_");
                    if (parts.length >= 3) {
                        String u1 = parts[0], u2 = parts[1];
                        other = currentUserId.equals(u1) ? u2 : u1;
                    }
                }

                listener.onPrivateChatClick(
                        chat.getId(),                      // יכול להיות null — זה בסדר
                        other,                             // חשוב שיצא לא-null בסוף!
                        chat.getApartmentId()
                );
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

    private String formatDate(long timestamp)
    {
        try {
            Date date = new Date(timestamp);
            return new SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "לא זמין";
        }
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSender, textViewApartment, textViewMessage, textViewTime, textViewUnreadBadge, textViewParticipants;
        ImageView imageMessageType;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSender = itemView.findViewById(R.id.textViewSender);
            textViewApartment = itemView.findViewById(R.id.textViewApartment);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewUnreadBadge = itemView.findViewById(R.id.textViewUnreadBadge);
            textViewParticipants = itemView.findViewById(R.id.textViewParticipants); // 🆕
            imageMessageType=itemView.findViewById(R.id.imageMessageType);
        }
    }
}
