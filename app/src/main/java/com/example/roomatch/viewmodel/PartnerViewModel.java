package com.example.roomatch.viewmodel;

import android.location.Location;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.UserSession;
import com.example.roomatch.model.repository.UserRepository;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PartnerViewModel extends ViewModel {

    private final MutableLiveData<List<UserProfile>> partners = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final List<UserProfile> allPartners = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final UserRepository repository = new UserRepository();

    private final LiveData<UserProfile> profile = UserSession.getInstance().getProfileLiveData();

    public LiveData<UserProfile> getProfile() { return profile; }

    private DocumentSnapshot lastDoc = null;
    private boolean isLoading = false;
    private boolean hasMore = true;
    public boolean isLoading() { return isLoading; }
    public boolean hasMore() { return hasMore; }
    private List<String> selectedLifestyles = new ArrayList<>();
    private List<String> selectedInterests = new ArrayList<>();
    private int selectedRadius = Integer.MAX_VALUE;
    private String searchQuery = "";
    private static final int PAGE_SIZE = 10;

    public PartnerViewModel() {
        //loadPartnersFromRepository();
        loadProfile();
    }

    public void loadFirstPage() {
        if (isLoading) return;
        isLoading = true;
        hasMore = true;
        lastDoc = null;
        partners.setValue(new ArrayList<>());
        // ננסה למלא לפחות PAGE_SIZE תוצאות אחרי סינון
        fetchAccumulated(PAGE_SIZE, /*replace=*/true, new ArrayList<>());
    }

    public void loadNextPage() {
        if (isLoading || !hasMore) return;
        isLoading = true;
        // מוסיפים עוד PAGE_SIZE תוצאות אחרי סינון
        fetchAccumulated(PAGE_SIZE, /*replace=*/false, new ArrayList<>());
    }


    private Query buildQuery() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Query q = db.collection("users")
                .whereEqualTo("userType", "seeker")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        // חיפוש בשם – prefix search
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            q = db.collection("users")
                    .whereEqualTo("userType", "seeker")
                    .orderBy("fullName")
                    .startAt(searchQuery)
                    .endAt(searchQuery + "\uf8ff");
            // ייתכן ותידרש אינדקס משולב (הקונסול ייתן קישור אם חסר)
        }
        return q;
    }
    private boolean docMatches(UserProfile p, String myUid) {
        if (p == null) return false;
        if (p.getUserId() == null) return false;

        // לא להציג את עצמי
        if (myUid != null && myUid.equals(p.getUserId())) return false;

        if (p.getCreatedAt() == null) {
            p.setCreatedAt(new java.util.Date(0));
        }

        boolean okLifestyle = containsAllTokensAsWords(p.getLifestyle(), selectedLifestyles);
        boolean okInterests = containsAllTokensAsWords(p.getInterests(), selectedInterests);
        boolean okRadius    = isInRadius(p);
        boolean okText      = (searchQuery == null || searchQuery.trim().isEmpty()) || matchFreeText(p, searchQuery);

        return okLifestyle && okInterests && okRadius && okText;
    }
    private java.util.List<UserProfile> filterBatch(com.google.firebase.firestore.QuerySnapshot snap, String myUid) {
        java.util.List<UserProfile> out = new java.util.ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            UserProfile p = doc.toObject(UserProfile.class);
            if (p == null) continue;
            p.setUserId(doc.getId());
            if (docMatches(p, myUid)) {
                out.add(p);
            }
        }
        return out;
    }
    private void fetchAccumulated(final int wanted, final boolean replace, final java.util.List<UserProfile> acc) {
        Query q = buildQuery().limit(PAGE_SIZE);
        if (lastDoc != null) q = q.startAfter(lastDoc);

        final String myUid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        q.get()
                .addOnSuccessListener(snap -> {
                    if (snap.size() > 0) {
                        lastDoc = snap.getDocuments().get(snap.size() - 1);
                    }

                    // מסננים את האצווה שהגיעה
                    acc.addAll(filterBatch(snap, myUid));

                    boolean noMoreServerPages = (snap.size() < PAGE_SIZE);
                    boolean filledEnough = (acc.size() >= wanted);

                    if (!filledEnough && !noMoreServerPages) {
                        // ממשיכים להביא עוד עמודים עד שממלאים
                        fetchAccumulated(wanted, replace, acc);
                        return;
                    }

                    // פרסום לתצוגה
                    if (replace) {
                        partners.setValue(new java.util.ArrayList<>(acc));
                    } else {
                        java.util.List<UserProfile> cur = partners.getValue();
                        if (cur == null) cur = new java.util.ArrayList<>();
                        cur.addAll(acc);
                        partners.setValue(cur);
                    }

                    hasMore = !noMoreServerPages;
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    toastMessage.setValue("שגיאה בטעינה: " + e.getMessage());
                });
    }






    private void loadPartnersFromRepository() {

//        FirebaseFirestore.getInstance()
//                .collection("users")
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    allPartners.clear();
//                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
//                        UserProfile profile = doc.toObject(UserProfile.class);
//                        profile.setUserId(doc.getId());
//                        if(profile.getCreatedAt()==null)
//                        {
//                            profile.setCreatedAt(new Date());
//                        }
//                        try
//                        {
//                            Map<String,Double> loc = (Map<String, Double>) doc.get("selectedLocation");
//                            profile.setLat(loc.get("latitude"));
//                            profile.setLng(loc.get("longitude"));
//
//                        }
//                        catch (Exception ex)
//                        {
//                            profile.setLat(0);
//                            profile.setLng(0);
//                        }
//                        if (profile != null) {
//                            allPartners.add(profile);
//                        }
//                    }
//                    partners.setValue(new ArrayList<>(allPartners));
//
//                })
//                .addOnFailureListener(e -> {
//                    toastMessage.setValue("שגיאה בטעינת שותפים: " + e.getMessage());
//                });
    }

    public void loadProfile() {

        UserSession.getInstance().ensureStarted();
//        repository.getMyProfile()
//                .addOnSuccessListener(doc -> {
//                    if (doc.exists()) {
//                        UserProfile userProfile = doc.toObject(UserProfile.class);
//                        try
//                        {
//                            Map<String,Double> loc = (Map<String, Double>) doc.get("selectedLocation");
//                            userProfile.setLat(loc.get("latitude"));
//                            userProfile.setLng(loc.get("longitude"));
//                        }
//                        catch (Exception ex)
//                        {
//                            userProfile.setLat(0);
//                            userProfile.setLng(0);
//                        }
//
//                        profile.setValue(userProfile);
//                    } else {
//                        toastMessage.setValue("פרופיל לא נמצא");
//                    }
//                })
//                .addOnFailureListener(e ->
//                        toastMessage.setValue("שגיאה בטעינת פרופיל: " + e.getMessage()));
    }

    public LiveData<List<UserProfile>> getPartners() {
        return partners;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void showProfileDialog(UserProfile profile) {
        toastMessage.setValue("צפייה בפרופיל של " + profile.getFullName());
    }

    public void showReportDialog(UserProfile profile) {
        toastMessage.setValue("דיווח על " + profile.getFullName());
    }

    public void sendMatchRequest(UserProfile profile) {
        String currentUserId = repository.getCurrentUserId();
        String targetUserId = profile.getUserId();

        if (currentUserId == null || targetUserId == null) {
            toastMessage.setValue("שגיאה: משתמש לא תקין");
            return;
        }

        repository.sendMatchRequest(currentUserId, targetUserId)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("נשלחה בקשת התאמה ל־" + profile.getFullName());
                    sendNotificationToUser(targetUserId, "בקשת התאמה חדשה");
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בשליחת הבקשה: " + e.getMessage())
                );
    }

    public void applyPartnerSearchByName(String query) {
        List<UserProfile> result = new ArrayList<>();
        for (UserProfile profile : allPartners) {
            if (profile.getFullName() != null &&
                    profile.getFullName().toLowerCase().contains(query.toLowerCase())) {
                result.add(profile);
            }
        }
        partners.setValue(result);
    }

    public void applyPartnerMultiFilter(List<String> selectedLifestyles, List<String> selectedInterests) {
        List<UserProfile> result = new ArrayList<>();
        for (UserProfile profile : allPartners) {
            boolean matchLifestyle = selectedLifestyles.isEmpty()
                    || selectedLifestyles.stream().anyMatch(l ->
                    profile.getLifestyle() != null && profile.getLifestyle().contains(l));

            boolean matchInterest = selectedInterests.isEmpty()
                    || selectedInterests.stream().anyMatch(i ->
                    profile.getInterests() != null && profile.getInterests().contains(i));

            if (matchLifestyle && matchInterest) {
                result.add(profile);
            }
        }
        partners.setValue(result);
    }

    public void clearPartnerFilter() {
        this.selectedLifestyles.clear();
        this.selectedInterests.clear();
        this.selectedRadius = Integer.MAX_VALUE;
        this.searchQuery = "";
        loadFirstPage();
//        partners.setValue(new ArrayList<>(allPartners));
    }

    public void applyCompleteFilter(List<String> lifestyles, List<String> interests,
                                    int radius, String query) {
        this.selectedLifestyles = (lifestyles != null) ? lifestyles : new ArrayList<>();
        this.selectedInterests  = (interests  != null) ? interests  : new ArrayList<>();
        this.selectedRadius     = radius;
        this.searchQuery        = (query != null) ? query.trim() : "";

        // איפוס פגינציה
        lastDoc = null;
        hasMore = true;
        isLoading = false;
        partners.setValue(new ArrayList<>());

        // טעינה לפי הפילטרים הנוכחיים
        loadFirstPage();
    }

    private boolean isInRadius(UserProfile u) {
        if (selectedRadius == Integer.MAX_VALUE) return true;
        if (u.getSelectedLocation() == null) return false;
        if (UserSession.getInstance().getCachedProfile() == null) return true;

        LatLng myLoc = UserSession.getInstance().getCachedProfile().getSelectedLocation();
        LatLng otherLoc = u.getSelectedLocation();
        if (myLoc == null || otherLoc == null) return false;

        float[] results = new float[1];
        Location.distanceBetween(myLoc.latitude, myLoc.longitude,
                otherLoc.latitude, otherLoc.longitude,
                results);
        double distKm = results[0] / 1000.0;
        return distKm <= selectedRadius;
    }
    private boolean containsText(UserProfile profile, String query) {
        query = query.toLowerCase();

        return (profile.getFullName() != null && profile.getFullName().toLowerCase().contains(query)) ||
                (profile.getSelectedCity() != null && profile.getSelectedCity().toLowerCase().contains(query)) ||
                (profile.getSelectedStreet() != null && profile.getSelectedStreet().toLowerCase().contains(query)) ||
                (profile.getDescription() != null && profile.getDescription().toLowerCase().contains(query)) ||
                (profile.getInterests() != null && profile.getInterests().toLowerCase().contains(query)) ||
                (profile.getLifestyle() != null && profile.getLifestyle().toLowerCase().contains(query)) ||
                (String.valueOf(profile.getAge()) != null && String.valueOf(profile.getAge()).contains(query));
    }



    public void reportUser(UserProfile profile, String reason) {
        Map<String, Object> report = new HashMap<>();
        report.put("reportedUserId", profile.getUserId());
        report.put("reportedBy", auth.getCurrentUser().getUid());
        report.put("reason", reason);
        report.put("timestamp", System.currentTimeMillis());
        
        db.collection("reports").add(report)
            .addOnSuccessListener(ref -> 
                toastMessage.setValue("הדיווח נשלח בהצלחה")
            )
            .addOnFailureListener(e -> 
                toastMessage.setValue("שגיאה בשליחת הדיווח")
            );
    }
    
    private void sendNotificationToUser(String userId, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        
        db.collection("notifications").add(notification);
    }

    // מפרק למילים/תגים לפי רווחים + מפרידי תגים נפוצים
    private java.util.Set<String> tokenizeToSet(@Nullable String s) {
        java.util.Set<String> out = new java.util.HashSet<>();
        if (s == null) return out;
        // מפרקים גם לפי , ; / | • וגם רווחים
        for (String part : s.toLowerCase(java.util.Locale.ROOT).split(", ")) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    // AND: כל ה־needles חייבים להופיע כמילה שלמה ב־haystack
    private boolean containsAllTokensAsWords(@Nullable String haystack, java.util.List<String> needles) {
        if (needles == null || needles.isEmpty()) return true;
        java.util.Set<String> bag = tokenizeToSet(haystack);
        if (bag.isEmpty()) return false;
        for (String n : needles) {
            if (n == null) return false;
            String token = n.trim().toLowerCase(java.util.Locale.ROOT);
            if (token.isEmpty() || !bag.contains(token)) return false;
        }
        return true;
    }


    private boolean matchFreeText(UserProfile p, String q) {
        if (q == null || q.trim().isEmpty()) return true;
        String query = q.toLowerCase(java.util.Locale.ROOT);
        return (p.getFullName() != null && p.getFullName().toLowerCase().contains(query)) ||
                (p.getSelectedCity() != null && p.getSelectedCity().toLowerCase().contains(query)) ||
                (p.getSelectedStreet() != null && p.getSelectedStreet().toLowerCase().contains(query)) ||
                (p.getDescription() != null && p.getDescription().toLowerCase().contains(query)) ||
                (p.getInterests() != null && p.getInterests().toLowerCase().contains(query)) ||
                (p.getLifestyle() != null && p.getLifestyle().toLowerCase().contains(query)) ||
                (p.getAge() != null && String.valueOf(p.getAge()).contains(query));
    }


}
