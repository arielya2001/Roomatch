package com.example.roomatch.model;

public interface ChatListItem {
    long getTimestamp();
    String getTitle(); // שם משתמש או שם קבוצה
    String getSubText(); // למשל: "דירה: X" או כתובת
    String getLastMessage(); // תוכן ההודעה האחרונה
    String getLastMessageSenderName(); // <<< חדש
    String getParticipantsString();
    void setParticipantsString(String names);
    boolean isGroup();
}
