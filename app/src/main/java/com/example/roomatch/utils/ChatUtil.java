package com.example.roomatch.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatUtil {

    /**
     * מחזיר מזהה ייחודי לצ'אט בין שני משתמשים עבור דירה מסוימת.
     * המזהה בלתי תלוי בסדר של המשתמשים.
     */
    public static String generateChatId(String user1, String user2, String apartmentId) {
        List<String> sortedUsers = Arrays.asList(user1, user2);
        Collections.sort(sortedUsers);
        return sortedUsers.get(0) + "_" + sortedUsers.get(1) + "_" + apartmentId;
    }
    public static String getChatId(String userA, String userB, String apartmentId) {
        // סדר משתמשים לפי מיון אלפביתי כדי שיהיה תמיד עקבי
        String sorted = userA.compareTo(userB) < 0
                ? userA + "_" + userB
                : userB + "_" + userA;

        return sorted + "_" + apartmentId;
    }
}
