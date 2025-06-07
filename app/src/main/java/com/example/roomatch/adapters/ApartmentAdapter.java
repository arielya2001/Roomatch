package com.example.roomatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.Apartment;

import java.util.List;

public class ApartmentAdapter extends RecyclerView.Adapter<ApartmentAdapter.ApartmentViewHolder> {

    public interface OnApartmentClickListener {
        void onApartmentClick(Apartment apartment);
    }

    private List<Apartment> apartmentList;
    private Context context;
    private OnApartmentClickListener listener;

    public ApartmentAdapter(List<Apartment> apartmentList, Context context, OnApartmentClickListener listener) {
        this.apartmentList = apartmentList;
        this.context = context;
        this.listener = listener;
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
        holder.locationTextView.setText("כתובת: " + apt.getAddress());
        holder.priceTextView.setText("מחיר: " + apt.getPrice() + " ש\"ח");
        holder.roommatesTextView.setText("שותפים דרושים: " + apt.getRoommatesNeeded());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApartmentClick(apt);
            }
        });
    }

    @Override
    public int getItemCount() {
        return apartmentList.size();
    }

    static class ApartmentViewHolder extends RecyclerView.ViewHolder {
        TextView locationTextView, priceTextView, roommatesTextView;

        public ApartmentViewHolder(@NonNull View itemView) {
            super(itemView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            roommatesTextView = itemView.findViewById(R.id.roommatesTextView);
        }
    }
}
