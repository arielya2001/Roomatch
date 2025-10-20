package com.example.roomatch.model;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.roomatch.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * מחזיק את פרופיל המשתמש המחובר כ-Singleton בזיכרון,
 * מסנכרן מול Firestore באמצעות snapshot listener,
 * ומספק גישה מהירה לנתונים + LiveData לעדכונים.
 */
public class UserSession {

    private static volatile UserSession INSTANCE;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<UserProfile> profileLiveData = new MutableLiveData<>();
    private volatile UserProfile cachedProfile; // גישה מהירה מהזיכרון
    private ListenerRegistration registration;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private UserSession() {
    }

    public static UserSession getInstance() {
        if (INSTANCE == null) {
            synchronized (UserSession.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserSession();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * החזרה מהירה של הפרופיל אם כבר בזיכרון (עשוי להיות null אם טרם נטען).
     */
    @Nullable
    public UserProfile getCachedProfile() {
        return cachedProfile;
    }

    /**
     * חשיפה כ-LiveData כדי ש-UI יתעדכן אוטומטית.
     */
    public LiveData<UserProfile> getProfileLiveData() {
        return profileLiveData;
    }

    /**
     * ודא שהסשן הופעל והאזנה למסמך המשתמש הותקנה.
     * בטוח לקרוא מספר פעמים; יופעל פעם אחת בלבד.
     */
    public Task<UserProfile> ensureStarted() {
        if (started.compareAndSet(false, true)) {
            String uid = getUidOrNull();
            if (uid == null) {
                started.set(false);
                return Tasks.forException(new IllegalStateException("No authenticated user"));
            }
            DocumentReference docRef = db.collection("users").document(uid);

            // טעינה ראשונית חד-פעמית (לתת תשובה ל-Task)
            Task<UserProfile> firstLoad = docRef.get().continueWith(task -> {
                if (!task.isSuccessful()) throw task.getException();
                DocumentSnapshot snap = task.getResult();
                if (snap != null && snap.exists()) {
                    UserProfile p = snap.toObject(UserProfile.class);
                    if (p != null) {
                        try {
                            Map<String, Double> loc = (Map<String, Double>) snap.get("selectedLocation");
                            p.setLat(loc.get("latitude"));
                            p.setLng(loc.get("longitude"));
                        } catch (Exception ex) {
                            p.setLat(0);
                            p.setLng(0);
                        }
                        if(p.getCreatedAt()==null)
                        {
                            p.setCreatedAt(new Date());
                        }
                        p.setUserId(snap.getId());
                        updateCache(p);
                        return p;
                    }
                }
                return null;
            });

            // מאזין חיי למסמך — ישמור את הקאש עדכני כל שינוי
            registration = docRef.addSnapshotListener((snapshot, e) -> {
                if (e != null) return;
                if (snapshot != null && snapshot.exists()) {
                    UserProfile p = snapshot.toObject(UserProfile.class);
                    if (p != null) {
                        p.setUserId(snapshot.getId());
                        updateCache(p);
                    }
                }
            });

            return firstLoad;
        } else {
            // כבר התחיל — נחזיר Task מיידי עם המצב הנוכחי
            return Tasks.forResult(cachedProfile);
        }
    }

    /**
     * עדכון כתיבה דרך הסשן — write-through: מעדכן גם Firestore וגם את הקאש.
     */
    public Task<Void> updateMyProfile(UserProfile newProfile) {
        String uid = getUidOrNull();
        if (uid == null)
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        // כתיבה ל-Firestore
        return db.collection("users").document(uid).set(newProfile)
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    newProfile.setUserId(uid);
                    updateCache(newProfile); // עדכן קאש מיידית (UI מגיב מהר)
                    return null;
                });
    }

    /**
     * ניקוי בעת התנתקות. חשוב כדי לא לדלוף מאזינים!
     */
    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        started.set(false);
        cachedProfile = null;
        profileLiveData.postValue(null);
    }

    // ---------- Helpers ----------

    private void updateCache(UserProfile p) {
        cachedProfile = p;
        profileLiveData.postValue(p);
    }

    @Nullable
    private String getUidOrNull() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
