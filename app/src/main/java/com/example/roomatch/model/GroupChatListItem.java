package com.example.roomatch.model;

public class GroupChatListItem implements ChatListItem {
    private final GroupChat groupChat;
    private String lastMessageSenderName;

    private String addressStreet;
    private String addressHouseNumber;
    private String addressCity;

    public String getAddressStreet() { return addressStreet; }
    public void setAddressStreet(String addressStreet) { this.addressStreet = addressStreet; }

    public String getAddressHouseNumber() { return addressHouseNumber; }
    public void setAddressHouseNumber(String addressHouseNumber) { this.addressHouseNumber = addressHouseNumber; }

    public String getAddressCity() { return addressCity; }
    public void setAddressCity(String addressCity) { this.addressCity = addressCity; }


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
        if (addressStreet != null && addressHouseNumber != null && addressCity != null) {
            return addressStreet + " " + addressHouseNumber + ", " + addressCity;
        } else {
            return "דירה: " + groupChat.getApartmentId();
        }
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
