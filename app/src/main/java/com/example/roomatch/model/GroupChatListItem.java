package com.example.roomatch.model;

import com.example.roomatch.model.ChatListItem;
import com.example.roomatch.model.GroupChat;

public class GroupChatListItem implements ChatListItem {
    private final GroupChat groupChat;

    public GroupChatListItem(GroupChat groupChat) {
        this.groupChat = groupChat;
    }

    @Override
    public long getTimestamp() {
        return groupChat.getLastMessageTimestamp();
    }

    @Override
    public String getTitle() {
        return groupChat.getGroupName(); // או groupId אם אין שם
    }

    @Override
    public String getSubText() {
        return "דירה: " + groupChat.getApartmentId();
    }

    @Override
    public String getLastMessage() {
        return groupChat.getLastMessage() != null ? groupChat.getLastMessage() : "אין הודעות";
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    public String getGroupId() {
        return groupChat.getGroupId();
    }

    public String getApartmentId() {
        return groupChat.getApartmentId();
    }
}
