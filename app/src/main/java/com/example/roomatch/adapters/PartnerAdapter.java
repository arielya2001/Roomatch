package com.example.roomatch.adapters;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import com.example.roomatch.R;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Map;

public class PartnerAdapter extends
        RecyclerView.Adapter<PartnerAdapter.PartnerViewHolder> {
    private final List<UserProfile> partnerList;
    private final OnProfileClickListener profileClickListener;
    private final OnMatchRequestClickListener matchClickListener;
    private final OnReportClickListener reportClickListener;
    private final UserRepository repository = new UserRepository();

    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();

    public LiveData<UserProfile> getProfile() { return profile; }
    public PartnerAdapter(List<UserProfile> partnerList,
                          OnProfileClickListener profileClickListener,
                          OnMatchRequestClickListener matchClickListener,
                          OnReportClickListener reportClickListener) {this.partnerList = partnerList;
        this.profileClickListener = profileClickListener;
        this.matchClickListener = matchClickListener;
        this.reportClickListener = reportClickListener;
        loadProfile();
    }

    public void loadProfile() {
        repository.getMyProfile()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        UserProfile userProfile = doc.toObject(UserProfile.class);
                        try {
                            Map<String,Double> loc = (Map<String, Double>) doc.get("selectedLocation");
                            userProfile.setLat(loc.get("latitude"));
                            userProfile.setLng(loc.get("longitude"));
                        }
                        catch (Exception ex)
                        {
                            userProfile.setLat(0);
                            userProfile.setLng(0);
                        }

                        profile.setValue(userProfile);
                    } else {
                        //toastMessage.setValue("פרופיל לא נמצא");
                    }
                });
                //.addOnFailureListener(e ->
                        //toastMessage.setValue("שגיאה בטעינת פרופיל: " + e.getMessage()));
    }
    public interface OnProfileClickListener {
        void onProfileClick(UserProfile profile);
    }
    public interface OnMatchRequestClickListener {
        void onMatchRequest(UserProfile profile);
    }
    public interface OnReportClickListener {
        void onReport(UserProfile profile);
    }
    @NonNull
    @Override
    public PartnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int
            viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_partner_card, parent, false);
        return new PartnerViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(@NonNull PartnerViewHolder holder, int position) {
        UserProfile partner = partnerList.get(position);
        holder.bind(partner);
    }
    @Override
    public int getItemCount() {
        return partnerList.size();
    }
    public void setData(List<UserProfile> newList) {
        partnerList.clear();
        partnerList.addAll(newList);
        notifyDataSetChanged();
    }
    class PartnerViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView lifestyleText;
        private final TextView distance;
        private final Button btnView;
        private final Button btnMatch;
        private final Button btnReport;
        public PartnerViewHolder(@NonNull View itemView) {super(itemView);
            nameText = itemView.findViewById(R.id.partnerNameTextView);
            lifestyleText = itemView.findViewById(R.id.partnerLifestyleTextView);

            distance = itemView.findViewById(R.id.distanceTextView);
            btnView = itemView.findViewById(R.id.buttonViewProfile);
            btnMatch = itemView.findViewById(R.id.buttonRequestMatch);
            btnReport = itemView.findViewById(R.id.buttonReport);
        }
        public void bind(UserProfile profile) {
            nameText.setText(profile.getFullName());
            lifestyleText.setText(profile.getLifestyle());
            double dist;
            if(getProfile().getValue()!=null)
            {
                UserProfile current = getProfile().getValue();
                LatLng loc = current.getSelectedLocation();
                LatLng otherLoc = profile.getSelectedLocation();
                //מחשב מרחק בין שני מיקומים
                float[] results = new float[1]; // meters
                Location.distanceBetween(loc.latitude, loc.longitude, otherLoc.latitude, otherLoc.longitude, results);
                dist =  results[0] / 1000.0; // km

            }
            else {
                dist=Double.MAX_VALUE;
            }
            if(dist!=Double.MAX_VALUE)
                distance.setText(String.format(java.util.Locale.US, "%.2f", dist)+"KM");
            else
                distance.setText("unavailable");
            itemView.setOnClickListener(v -> profileClickListener.onProfileClick(profile));
            btnMatch.setOnClickListener(v -> matchClickListener.onMatchRequest(profile));
            btnReport.setOnClickListener(v -> reportClickListener.onReport(profile));
            btnView.setOnClickListener(c->profileClickListener.onProfileClick(profile));
        }
    }
}