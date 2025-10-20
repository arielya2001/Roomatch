package com.example.roomatch.viewmodel;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.UserSession;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ApartmentSearchViewModel extends ViewModel {
    private final ApartmentRepository repository;  // מאותחל בקונסטרקטור ברירת מחדל

    private final MutableLiveData<List<Apartment>> apartments = new MutableLiveData<>(new ArrayList<>());
    private final List<Apartment> allApartments = new ArrayList<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    private String searchQuery="";
    private boolean ascending = false;
    private String sortBy="price";

    public ApartmentSearchViewModel() {
        // בלי Factory – מאתחלים כאן
        this.repository = new ApartmentRepository();
    }

    public LiveData<List<Apartment>> getApartments() { return apartments; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    private DocumentSnapshot lastDoc = null;
    private boolean isLoading = false;
    private boolean hasMore = true;
    public boolean isLoading() { return isLoading; }
    public boolean hasMore() { return hasMore; }
    private int selectedRadius = Integer.MAX_VALUE;
    private static final int PAGE_SIZE = 10;

    public void loadFirstPage() {
        if (isLoading) return;
        isLoading = true;
        hasMore = true;
        lastDoc = null;
        apartments.setValue(new ArrayList<>());
        fetchAccumulated(PAGE_SIZE, /*replace=*/true, new ArrayList<>());
    }

    public void loadNextPage() {
        if (isLoading || !hasMore) return;
        isLoading = true;
        fetchAccumulated(PAGE_SIZE, /*replace=*/false, new ArrayList<>());
    }

    private Query buildQuery() {
        String field = (sortBy == null || sortBy.trim().isEmpty())
                ? "price"
                : sortBy.trim();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query q =  db.collection("apartments")
                .orderBy(field, this.ascending ? Query.Direction.ASCENDING
                        : Query.Direction.DESCENDING);

        return q;
    }

    private void fetchAccumulated(final int wanted, final boolean replace, final List<Apartment> acc) {
        Query q = buildQuery().limit(PAGE_SIZE);
        if (lastDoc != null) q = q.startAfter(lastDoc);

        q.get()
                .addOnSuccessListener(snap -> {
                    if (snap.size() > 0) lastDoc = snap.getDocuments().get(snap.size() - 1);
                    acc.addAll(filterBatch(snap));

                    boolean noMoreServerPages = (snap.size() < PAGE_SIZE);
                    boolean filledEnough = (acc.size() >= wanted);

                    if (!filledEnough && !noMoreServerPages) {
                        fetchAccumulated(wanted, replace, acc);
                        return;
                    }

                    if (replace) {
                        apartments.setValue(new ArrayList<>(acc));
                    } else {
                        List<Apartment> cur = apartments.getValue();
                        if (cur == null) cur = new ArrayList<>();
                        cur.addAll(acc);
                        apartments.setValue(cur);
                    }

                    hasMore = !noMoreServerPages;
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    toastMessage.setValue("שגיאה בטעינה: " + e.getMessage());
                });
    }

    private List<Apartment> filterBatch(com.google.firebase.firestore.QuerySnapshot snap) {
        List<Apartment> out = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Apartment a = doc.toObject(Apartment.class);
            if (a == null) continue;
            a.setId(doc.getId());
            if (isInRadius(a)&& matchesQuery(a,searchQuery)) out.add(a);
        }
        return out;
    }

    public void clearFilter() {
        this.selectedRadius = Integer.MAX_VALUE;
        this.searchQuery = "";

        loadFirstPage();
    }

    public void applyCompleteFilter(boolean ascending, int radius,String sortBy,String searchQuery) {
        this.selectedRadius = radius;
        this.ascending = ascending;
        this.searchQuery = (searchQuery != null) ? searchQuery : "";

        this.sortBy = (sortBy == null || sortBy.trim().isEmpty())
                ? "price"
                : sortBy.trim();

        lastDoc = null;
        hasMore = true;
        isLoading = false;
        apartments.setValue(new ArrayList<>());

        loadFirstPage();
    }
    private boolean matchesQuery(Apartment a, String q) {
        if (q == null || q.trim().isEmpty()) return true;

        String query = q.trim().toLowerCase();
        String city  = a.getCity() != null ? a.getCity().toLowerCase() : "";
        String street= a.getStreet() != null ? a.getStreet().toLowerCase() : "";
        String desc  = a.getDescription() != null ? a.getDescription().toLowerCase() : "";
        String priceStr = String.valueOf(a.getPrice()); // נשתמש גם כמחרוזת

        // התאמה “contains” לשדות טקסטואליים
        boolean textHit = city.contains(query) || street.contains(query) || desc.contains(query);

        // התאמת מחיר פשוטה:
        // - אם המשתמש הזין מספר טהור (למשל "4500") — נבדוק מחיר בדיוק
        // - אחרת, נאפשר גם התאמה טקסטואלית (למשל "45" יופיע במחיר "4500")
        boolean priceHit = false;
        try {
            int asInt = Integer.parseInt(query);
            priceHit = (a.getPrice() == asInt);
        } catch (NumberFormatException ignore) {
            priceHit = priceStr.contains(query);
        }

        return textHit || priceHit;
    }

    private boolean isInRadius(Apartment a) {
        if (selectedRadius == Integer.MAX_VALUE) return true;
        if (a.getSelectedLocation() == null) return false;
        if (UserSession.getInstance().getCachedProfile() == null) return true;

        LatLng myLoc = UserSession.getInstance().getCachedProfile().getSelectedLocation();
        LatLng otherLoc = a.getSelectedLocation();
        if (myLoc == null || otherLoc == null) return false;

        float[] results = new float[1];
        Location.distanceBetween(myLoc.latitude, myLoc.longitude,
                otherLoc.latitude, otherLoc.longitude,
                results);
        double distKm = results[0] / 1000.0;
        return distKm <= selectedRadius;
    }

    public void loadApartments() {
        repository.getApartments()
                .addOnSuccessListener(list -> {
                    allApartments.clear();
                    allApartments.addAll(list);
                    apartments.setValue(new ArrayList<>(list));
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בטעינת הדירות"));
    }

    public void applyFilter(String field, boolean ascending) {
        repository.getApartmentsOrderedBy(field, ascending ? Query.Direction.ASCENDING : Query.Direction.DESCENDING)
                .addOnSuccessListener(apartments::setValue)
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בסינון"));
    }

    public void searchApartments(String query) {
        if (query == null || query.trim().isEmpty()) {
            resetFilter();
            return;
        }

        String lower = query.toLowerCase();
        List<Apartment> filtered = new ArrayList<>();
        for (Apartment apt : allApartments) {
            String searchable =
                    (apt.getCity() != null ? apt.getCity() : "") + " " +
                            (apt.getStreet() != null ? apt.getStreet() : "") + " " +
                            (apt.getDescription() != null ? apt.getDescription() : "") + " " +
                            apt.getPrice() + " " + apt.getRoommatesNeeded();

            if (searchable.toLowerCase().contains(lower)) filtered.add(apt);
        }
        apartments.setValue(filtered);
    }

    public void reportApartment(Apartment apartment, String reason, String details) {
        repository.reportApartment(apartment.getId(), apartment.getOwnerId(), reason, details)
                .addOnSuccessListener(unused -> toastMessage.setValue("הדיווח נשלח בהצלחה"))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בשליחת הדיווח"));
    }

    public void resetFilter() {
        apartments.setValue(new ArrayList<>(allApartments));
    }
}
