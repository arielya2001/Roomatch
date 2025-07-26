package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.roomatch.R;
import com.example.roomatch.model.Apartment;

import java.util.ArrayList;
import java.util.List;

public class ApartmentCardAdapter extends RecyclerView.Adapter<ApartmentCardAdapter.ApartmentViewHolder> {

    /* ~~~~~~~~~~~~~~~~ interface ~~~~~~~~~~~~~~~~ */
    public interface OnApartmentClickListener {
        void onViewApartmentClick(Apartment apartment);
        void onEditApartmentClick(Apartment apartment);
        void onDeleteApartmentClick(Apartment apartment);

//        void onReportApartmentClick(Apartment apartment);

    }

    /* ~~~~~~~~~~~~~~~~ data + ctor ~~~~~~~~~~~~~~~~ */
    private List<Apartment> apartments;
    private final OnApartmentClickListener listener;

    public ApartmentCardAdapter(List<Apartment> apartments, OnApartmentClickListener listener) {
        this.apartments = new ArrayList<>(apartments);
        this.listener = listener;
    }

    /* ~~~~~~~~~~~~~~~~ ViewHolder creation ~~~~~~~~~~~~~~~~ */
    @NonNull
    @Override
    public ApartmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_apartment_card, parent, false);
        return new ApartmentViewHolder(v);
    }

    /* ~~~~~~~~~~~~~~~~ binding ~~~~~~~~~~~~~~~~ */
    @Override
    public void onBindViewHolder(@NonNull ApartmentViewHolder h, int pos) {
        Apartment apt = apartments.get(pos);

        h.city.setText(apt.getCity() != null ? apt.getCity() : "לא זמין");
        h.street.setText(apt.getStreet() != null ? apt.getStreet() : "לא זמין");
        h.houseNumber.setText(String.valueOf(apt.getHouseNumber()));
        h.price.setText(" חודש / " + apt.getPrice() + " ₪");

// מנקה את התמונה הקודמת כדי למנוע בלבול של תמונות ממוחזרות
        Glide.with(h.itemView.getContext()).clear(h.apartmentImageView);

        String img = apt.getImageUrl() != null ? apt.getImageUrl() : "";
        if (!img.isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(img)
                    .placeholder(R.drawable.placeholder_image)
                    .into(h.apartmentImageView);
        } else {
            h.apartmentImageView.setImageResource(R.drawable.placeholder_image);
        }


        /*  --- clicks --- */
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onViewApartmentClick(apt);
        });
        h.buttonEditApartment.setOnClickListener(v -> {
            if (listener != null) listener.onEditApartmentClick(apt);
        });
        h.buttonDeleteApartment.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteApartmentClick(apt);
        });
//        h.buttonReportApartment.setOnClickListener(v -> {
//            if (listener != null) listener.onReportApartmentClick(apt);
//        });

    }

    /* ~~~~~~~~~~~~~~~~ misc ~~~~~~~~~~~~~~~~ */
    @Override
    public int getItemCount() {
        return apartments != null ? apartments.size() : 0;
    }

    public void updateApartments(List<Apartment> newApartments) {
        this.apartments.clear();
        this.apartments.addAll(newApartments);
        notifyDataSetChanged();
    }

    /* ~~~~~~~~~~~~~~~~ ViewHolder ~~~~~~~~~~~~~~~~ */
    static class ApartmentViewHolder extends RecyclerView.ViewHolder {
        TextView city, street, houseNumber, price;
        ImageButton buttonEditApartment, buttonDeleteApartment;
        ImageView apartmentImageView;

        ImageButton buttonReportApartment;


        ApartmentViewHolder(@NonNull View itemView) {
            super(itemView);
            apartmentImageView = itemView.findViewById(R.id.apartmentImageView);
            city = itemView.findViewById(R.id.textViewApartmentCity);
            street = itemView.findViewById(R.id.textViewApartmentStreet);
            houseNumber = itemView.findViewById(R.id.textViewApartmentHouseNumber);
            price = itemView.findViewById(R.id.textViewApartmentPrice);
            buttonEditApartment = itemView.findViewById(R.id.buttonEditApartment);
            buttonDeleteApartment = itemView.findViewById(R.id.buttonDeleteApartment);
//            buttonReportApartment = itemView.findViewById(R.id.buttonReportApartment);

        }
    }
}