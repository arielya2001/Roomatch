package com.example.roomatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;

import java.util.List;
import java.util.Map;

public class ApartmentCardAdapter extends RecyclerView.Adapter<ApartmentCardAdapter.ApartmentViewHolder> {

    public interface OnApartmentClickListener {
        void onClick(Map<String, Object> apartment);
    }

    private List<Map<String, Object>> apartments;
    private OnApartmentClickListener listener;

    public ApartmentCardAdapter(List<Map<String, Object>> apartments, OnApartmentClickListener listener) {
        this.apartments = apartments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ApartmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_apartment_card, parent, false);
        return new ApartmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApartmentViewHolder holder, int position) {
        Map<String, Object> apartment = apartments.get(position);
        holder.address.setText("כתובת: " + apartment.get("address"));
        holder.price.setText("מחיר: " + apartment.get("price") + " ₪");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(apartment);
        });
    }

    @Override
    public int getItemCount() {
        return apartments.size();
    }

    static class ApartmentViewHolder extends RecyclerView.ViewHolder {
        TextView address, price;

        public ApartmentViewHolder(@NonNull View itemView) {
            super(itemView);
            address = itemView.findViewById(R.id.textViewApartmentAddress);
            price = itemView.findViewById(R.id.textViewApartmentPrice);
        }
    }
}
