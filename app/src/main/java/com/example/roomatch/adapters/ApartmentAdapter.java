package com.example.roomatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

        holder.cityTextView.setText(apt.getCity());
        holder.streetTextView.setText(apt.getStreet());
        holder.houseNumberTextView.setText(apt.getHouseNumber()+"");
        holder.priceTextView.setText(" חודש/ "+"₪"+apt.getPrice() );
        holder.roommatesTextView.setText("שותפים דרושים: " + apt.getRoommatesNeeded());

        if (apt.getImageUrl() != null && !apt.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(apt.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.apartmentImageView);
        }

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
        ImageView apartmentImageView;
        TextView cityTextView, streetTextView, houseNumberTextView, priceTextView, roommatesTextView;

        public ApartmentViewHolder(@NonNull View itemView) {
            super(itemView);
            apartmentImageView = itemView.findViewById(R.id.apartmentImageView);
            cityTextView = itemView.findViewById(R.id.cityTextView);
            streetTextView = itemView.findViewById(R.id.streetTextView);
            houseNumberTextView = itemView.findViewById(R.id.houseNumberTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            roommatesTextView = itemView.findViewById(R.id.roommatesTextView);
        }
    }

}

