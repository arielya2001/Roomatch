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

import java.util.List;
import java.util.Map;

public class ApartmentCardAdapter
        extends RecyclerView.Adapter<ApartmentCardAdapter.ApartmentViewHolder> {

    /* ~~~~~~~~~~~~~~~~ interface ~~~~~~~~~~~~~~~~ */
    public interface OnApartmentClickListener {
        void onViewApartmentClick (Map<String,Object> apartment);
        void onEditApartmentClick (Map<String,Object> apartment);
        void onDeleteApartmentClick(Map<String,Object> apartment);   // ← חדש
    }

    /* ~~~~~~~~~~~~~~~~ data + ctor ~~~~~~~~~~~~~~~~ */
    private List<Map<String,Object>> apartments;
    private final OnApartmentClickListener listener;

    public ApartmentCardAdapter(List<Map<String,Object>> apartments,
                                OnApartmentClickListener listener) {
        this.apartments = apartments;
        this.listener   = listener;
    }

    /* ~~~~~~~~~~~~~~~~ ViewHolder creation ~~~~~~~~~~~~~~~~ */
    @NonNull @Override
    public ApartmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_apartment_card,parent,false);
        return new ApartmentViewHolder(v);
    }

    /* ~~~~~~~~~~~~~~~~ binding ~~~~~~~~~~~~~~~~ */
    @Override
    public void onBindViewHolder(@NonNull ApartmentViewHolder h,int pos){
        Map<String,Object> apt = apartments.get(pos);

        String city        = val(apt,"city");
        String street      = val(apt,"street");
        String houseNumber = val(apt,"houseNumber");
        String price       = val(apt,"price") + " ₪";

        h.city.setText(city);
        h.street.setText(street);
        h.houseNumber.setText(houseNumber);
        h.price.setText(" חודש / " + price);

        String img = apt.get("imageUrl")!=null? apt.get("imageUrl").toString() : "";
        if(!img.isEmpty())
            Glide.with(h.itemView.getContext())
                    .load(img)
                    .placeholder(R.drawable.placeholder_image)
                    .into(h.apartmentImageView);

        /*  --- clicks --- */
        h.itemView.setOnClickListener(v -> { if(listener!=null) listener.onViewApartmentClick(apt); });
        h.buttonEditApartment.setOnClickListener (v -> { if(listener!=null) listener.onEditApartmentClick(apt); });
        h.buttonDeleteApartment.setOnClickListener(v -> { if(listener!=null) listener.onDeleteApartmentClick(apt); });
    }

    /* ~~~~~~~~~~~~~~~~ misc ~~~~~~~~~~~~~~~~ */
    @Override public int getItemCount(){ return apartments!=null? apartments.size():0; }

    public void updateApartments(List<Map<String,Object>> newList){
        apartments = newList;
        notifyDataSetChanged();
    }

    private static String val(Map<String,Object> m,String key){
        Object o = m.get(key);
        return o!=null? o.toString() : "לא זמין";
    }

    /* ~~~~~~~~~~~~~~~~ ViewHolder ~~~~~~~~~~~~~~~~ */
    static class ApartmentViewHolder extends RecyclerView.ViewHolder{
        TextView city,street,houseNumber,price;
        ImageButton buttonEditApartment,buttonDeleteApartment;   // ← חדש
        ImageView apartmentImageView;

        ApartmentViewHolder(@NonNull View itemView){
            super(itemView);
            apartmentImageView   = itemView.findViewById(R.id.apartmentImageView);
            city                 = itemView.findViewById(R.id.textViewApartmentCity);
            street               = itemView.findViewById(R.id.textViewApartmentStreet);
            houseNumber          = itemView.findViewById(R.id.textViewApartmentHouseNumber);
            price                = itemView.findViewById(R.id.textViewApartmentPrice);
            buttonEditApartment  = itemView.findViewById(R.id.buttonEditApartment);
            buttonDeleteApartment= itemView.findViewById(R.id.buttonDeleteApartment); // ← חדש
        }
    }
}
