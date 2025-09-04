const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// 📩 התראה על הודעה פרטית חדשה
exports.sendNewMessageNotification = onDocumentCreated("messages/{chatId}/chat/{messageId}", async (event) => {
  const message = event.data.data();

  const toUserId = message.toUserId;
  const fromUserId = message.fromUserId;

  if (!toUserId || toUserId === fromUserId) return;

  const userDoc = await getFirestore().collection("users").doc(toUserId).get();
  const token = userDoc.get("fcmToken");

  if (!token) return;

  const payload = {
    notification: {
      title: "Roomatch",
      body: "הודעה חדשה לגבי דירה שלך",
    },
    token: token,
  };

  try {
    await getMessaging().send(payload);
    console.log("Notification sent to:", toUserId);
  } catch (error) {
    console.error("Error sending notification:", error);
  }
});

// 👥 התראה על הודעה חדשה בקבוצה
exports.sendGroupMessageNotification = onDocumentCreated("group_messages/{groupId}/chat/{messageId}", async (event) => {
  const message = event.data.data();
  const groupId = event.params.groupId;
  const fromUserId = message.fromUserId;
  const toUserId = message.toUserId; // ⬅️ הוספת שדה חשוב

  if (!fromUserId) return;

  try {
    const firestore = getFirestore();

    // שליפת חברי הקבוצה מתוך group_chats לפי groupId
    const groupDoc = await firestore.collection("group_chats").doc(groupId).get();
    if (!groupDoc.exists) return;

    const memberIds = groupDoc.get("memberIds") || [];

    console.log("👥 חברי קבוצה:", memberIds);

    // שליחת התראה לבעל הדירה (היעד)
    if (toUserId && toUserId !== fromUserId) {
      const userDoc = await firestore.collection("users").doc(toUserId).get();
      const token = userDoc.get("fcmToken");
      console.log("📤 שולח לבעל הדירה:", toUserId, "עם token:", token);

      if (token) {
        const payload = {
          notification: {
            title: "Roomatch",
            body: "קיבלת הודעה חדשה מקבוצה לגבי הדירה שלך",
          },
          token,
        };
        await getMessaging().send(payload);
        console.log("✅ התראה נשלחה לבעל הדירה:", toUserId);
      }
    }

    // שליחת התראה לשאר חברי הקבוצה (למעט השולח)
    const notifications = memberIds
      .filter((uid) => uid !== fromUserId)
      .map(async (userId) => {
        const userDoc = await firestore.collection("users").doc(userId).get();
        const token = userDoc.get("fcmToken");
        console.log("🔔 שליחה לחבר קבוצה:", userId, "token:", token);
        if (!token) return;

        const payload = {
          notification: {
            title: "Roomatch",
            body: "קיבלת הודעה חדשה בקבוצה",
          },
          token,
        };

        await getMessaging().send(payload);
        console.log("📬 התראה נשלחה ל:", userId);
      });

    await Promise.all(notifications);

  } catch (error) {
    console.error("❌ שגיאה בשליחת התראות קבוצתיות:", error);
  }
});