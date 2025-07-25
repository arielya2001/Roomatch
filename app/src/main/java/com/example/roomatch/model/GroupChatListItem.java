package com.example.roomatch.model;

public class GroupChatListItem implements ChatListItem {
    private final GroupChat groupChat;
    private String lastMessageSenderName;

    public GroupChatListItem(GroupChat groupChat) {
        this.groupChat = groupChat;
    }

    public void setLastMessageSenderName(String lastMessageSenderName) {
        this.lastMessageSenderName = lastMessageSenderName;
    }

    public String getLastMessageSenderName() {
        return lastMessageSenderName != null ? lastMessageSenderName : "אנונימי";
    }

    @Override
    public long getTimestamp() {
        return groupChat.getLastMessageTimestamp();
    }

    @Override
    public String getTitle() {
        return groupChat.getGroupName();
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

    private String participantsString;

    @Override
    public String getParticipantsString() {
        return participantsString;
    }

    @Override
    public void setParticipantsString(String names) {
        this.participantsString = names;
    }

}
