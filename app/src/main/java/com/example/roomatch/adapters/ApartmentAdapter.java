package com.example.roomatch.adapters;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.roomatch.R;
import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.UserSession;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class ApartmentAdapter extends RecyclerView.Adapter<ApartmentAdapter.ApartmentViewHolder> {

    public interface OnApartmentClickListener {
        void onApartmentClick(Apartment apartment);
    }

    public interface OnReportClickListener {
        void onReportClick(Apartment apartment);
    }

    private List<Apartment> apartmentList;
    private Context context;
    private OnApartmentClickListener listener;
    private OnReportClickListener reportListener;
    private final LiveData<UserProfile> profile = UserSession.getInstance().getProfileLiveData();
    public LiveData<UserProfile> getProfile() { return profile; }
    public void loadProfile() {
        UserSession.getInstance().ensureStarted();
    }

    public ApartmentAdapter(List<Apartment> apartmentList, Context context,
                            OnApartmentClickListener listener,
                            OnReportClickListener reportListener) {
        this.apartmentList = apartmentList;
        this.context = context;
        this.listener = listener;
        this.reportListener = reportListener;
        loadProfile();
    }

    @NonNull
    @Override
    public ApartmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_apartment, parent, false);
        return new ApartmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApartmentViewHolder holder, int position) {
        Apartment apt = apartmentList.get(position);

        // Log הקואורדינטות של הדירה ושל המיקום שנבחר
        Log.d("DistanceDebug", "Apt LatLng: " + apt.getLatitude() + ", " + apt.getLongitude());

        holder.cityTextView.setText(apt.getCity());
        holder.streetTextView.setText(apt.getStreet());
        holder.houseNumberTextView.setText(String.valueOf(apt.getHouseNumber()));
        holder.priceTextView.setText(" חודש/ " + "₪" + apt.getPrice());

        // הצגת המרחק אם קיים
        LatLng aptLoc = apt.getSelectedLocation();
        LatLng userloc = getProfile().getValue().getSelectedLocation();
        if(userloc==null)
        {
            userloc=new LatLng(0,0);
        }
        float[] results = new float[1];
        Location.distanceBetween(userloc.latitude, userloc.longitude,
                aptLoc.latitude, aptLoc.longitude,
                results);
        double distKm = results[0] / 1000.0;
        if (distKm > 0 && distKm < Double.MAX_VALUE) {
            holder.distanceTextView.setText(String.format("מרחק: %.1f ק\"מ", distKm));
            holder.distanceTextView.setVisibility(View.VISIBLE);
        } else {
            holder.distanceTextView.setVisibility(View.GONE);
        }

        // תמונה
        if (apt.getImageUrl() == null || apt.getImageUrl().isEmpty()) {
            Glide.with(context).clear(holder.apartmentImageView);
            holder.apartmentImageView.setImageResource(R.drawable.placeholder_image);
        } else {
            Glide.with(context)
                    .load(apt.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.apartmentImageView);
        }

        // לחיצה על כל הפריט – פתיחת פרטים
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApartmentClick(apt);
            }
        });

    }

    @Override
    public int getItemCount() {
        return apartmentList != null ? apartmentList.size() : 0;
    }

    public void updateApartments(List<Apartment> newApartments) {
        this.apartmentList = newApartments;
        notifyDataSetChanged();
    }

    static class ApartmentViewHolder extends RecyclerView.ViewHolder {
        ImageView apartmentImageView;
        TextView cityTextView, streetTextView, houseNumberTextView, priceTextView;
        TextView distanceTextView;
        //ImageButton reportButton;

        public ApartmentViewHolder(@NonNull View itemView) {
            super(itemView);
            apartmentImageView = itemView.findViewById(R.id.apartmentImageView);
            cityTextView = itemView.findViewById(R.id.cityTextView);
            streetTextView = itemView.findViewById(R.id.streetTextView);
            houseNumberTextView = itemView.findViewById(R.id.houseNumberTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            distanceTextView = itemView.findViewById(R.id.textViewApartmentDistance);
            //reportButton = itemView.findViewById(R.id.buttonReport);
        }
    }
}

