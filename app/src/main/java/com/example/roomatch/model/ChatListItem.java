package com.example.roomatch.model;

public interface ChatListItem {
    long getTimestamp();
    String getTitle(); // שם משתמש או שם קבוצה
    String getSubText(); // למשל: "דירה: X" או מזהה קבוצתי
    String getLastMessage();
    boolean isGroup();
}
