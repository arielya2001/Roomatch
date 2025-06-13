package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PartnerViewModel extends ViewModel {

    /* -------- Repository אחד שאחראי על כל פעולות‑המשתמש -------- */
    private final UserRepository repository = new UserRepository();

    /* -------- LiveData -------- */
    private final MutableLiveData<List<Map<String, Object>>> partners     = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String>                    toastMessage = new MutableLiveData<>();

    /* -------- Getters -------- */
    public LiveData<List<Map<String, Object>>> getPartners()   { return partners; }
    public LiveData<String>                    getToastMessage(){ return toastMessage; }

    /* -------- ctor -------- */
    public PartnerViewModel() { loadPartners(); }

    /* ------------------------------------------------------------------ */
    private void loadPartners() {

        String uid = repository.getCurrentUserId();
        if (uid == null) { toastMessage.setValue("שגיאה: משתמש לא מחובר"); return; }

        /* “שותפים” = כל משתמשי seeker‑partner חוץ ממני */
        repository.getPartners()
                .addOnSuccessListener(query -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    query.forEach(doc -> {
                        if (!doc.getId().equals(uid)) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("id", doc.getId());
                                list.add(data);
                            }
                        }
                    });
                    partners.setValue(list);
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת שותפים: " + e.getMessage()));
    }

    /* ------------------------------------------------------------------ */
    /*  הטיפול בדיאלוגים נשאר ב‑Fragment; כאן רק נוכל לזרוק אירועים */
    public void showProfileDialog(Map<String,Object> partner) { /* נשלח אירוע – יטופל ב‑UI */ }
    public void showReportDialog (String fullName)            { /* idem */ }
}
