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

    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Comparator;
    import java.util.Date;
    import java.util.List;
    import java.util.Locale;

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
            holder.textViewSenderName.setText(msg.getSenderName());
            long timestamp = msg.getTimestamp();
            String time= formatTime(timestamp);
            String date= formatDate(timestamp);
            long nowMs = System.currentTimeMillis();
            String currentDate=formatDate(nowMs);
            if(currentDate.equals(date))
            {
                holder.textTime.setText(time);
            }
            else
            {
                holder.textTime.setText(date+" "+time);
            }

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
            TextView textViewMessage, textViewSenderName,textTime;
            LinearLayout messageContainer;

            public GroupChatViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewMessage = itemView.findViewById(R.id.textMessage);
                textViewSenderName = itemView.findViewById(R.id.textSenderName);
                messageContainer = itemView.findViewById(R.id.messageContainer);
                textTime = itemView.findViewById(R.id.textTime);
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
