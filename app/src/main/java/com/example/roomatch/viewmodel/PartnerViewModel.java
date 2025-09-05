package com.example.roomatch.viewmodel;

import android.location.Location;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PartnerViewModel extends ViewModel {

    private final MutableLiveData<List<UserProfile>> partners = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final List<UserProfile> allPartners = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final UserRepository repository = new UserRepository();

    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();

    public LiveData<UserProfile> getProfile() { return profile; }

    public PartnerViewModel() {
        loadPartnersFromRepository();
        loadProfile();
    }

    private void loadPartnersFromRepository() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allPartners.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        UserProfile profile = doc.toObject(UserProfile.class);
                        profile.setUserId(doc.getId());
                        try
                        {
                            Map<String,Double> loc = (Map<String, Double>) doc.get("selectedLocation");
                            profile.setLat(loc.get("latitude"));
                            profile.setLng(loc.get("longitude"));

                        }
                        catch (Exception ex)
                        {
                            profile.setLat(0);
                            profile.setLng(0);
                        }
                        if (profile != null) {
                            allPartners.add(profile);
                        }
                    }
                    partners.setValue(new ArrayList<>(allPartners));

                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בטעינת שותפים: " + e.getMessage());
                });
    }

    public void loadProfile() {
        repository.getMyProfile()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        UserProfile userProfile = doc.toObject(UserProfile.class);
                        try
                        {
                            Map<String,Double> loc = (Map<String, Double>) doc.get("selectedLocation");
                            userProfile.setLat(loc.get("latitude"));
                            userProfile.setLat(loc.get("longitude"));
                        }
                        catch (Exception ex)
                        {
                            userProfile.setLat(0);
                            userProfile.setLng(0);
                        }

                        profile.setValue(userProfile);
                    } else {
                        toastMessage.setValue("פרופיל לא נמצא");
                    }
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת פרופיל: " + e.getMessage()));
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
        partners.setValue(new ArrayList<>(allPartners));
    }
    
    public void applyCompleteFilter(List<String> lifestyles, List<String> interests, 
                                   int radius, String nameQuery) {
        List<UserProfile> result = new ArrayList<>();
        
        for (UserProfile profile : allPartners) {
            boolean matchName = nameQuery.isEmpty() || 
                (profile.getFullName() != null && 
                 profile.getFullName().toLowerCase().contains(nameQuery.toLowerCase()));
            
            boolean matchLifestyle = lifestyles.isEmpty() ||
                lifestyles.stream().allMatch(l ->
                    profile.getLifestyle() != null && profile.getLifestyle().contains(l));

            boolean matchInterest = interests.isEmpty() ||
                interests.stream().allMatch(i ->
                    profile.getInterests() != null && profile.getInterests().contains(i));
            
            // Pour la localisation, on vérifie si l'utilisateur cherche dans les mêmes villes
            boolean matchLocation=false;
            UserProfile current = getProfile().getValue();
            LatLng loc = current.getSelectedLocation();
            LatLng otherLoc = profile.getSelectedLocation();
            //מחשב מרחק בין שני מיקומים
            float[] results = new float[1]; // meters
            Location.distanceBetween(loc.latitude, loc.longitude, otherLoc.latitude, otherLoc.longitude, results);
            double dist =  results[0] / 1000.0; // km
            //בודק האם המרחק קטן מרדיוס
            if(dist<=radius)
            {
                matchLocation=true;
            }
//            if (!locations.isEmpty() && profile.getUserId() != null) {
//                // On pourrait récupérer la préférence de localisation depuis Firestore
//                // Pour l'instant, on fait un match basique
//                matchLocation = true; // Simplifié pour l'exemple
//            }

            if (matchName && matchLifestyle && matchInterest && matchLocation) {
                result.add(profile);
            }
        }
        
        // Trier par distance si des localisations sont spécifiées
//        if (!locations.isEmpty()) {
//            result.sort((p1, p2) -> {
//                return 0;
//            });
//        }
        
        partners.setValue(result);
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


}
