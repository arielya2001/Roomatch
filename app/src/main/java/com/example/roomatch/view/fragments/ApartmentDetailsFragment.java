package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.roomatch.R;

public class ApartmentDetailsFragment extends Fragment {

    private String city, street, imageUrl, ownerId, apartmentId, description;
    private int houseNumber, price, roommatesNeeded;

    public static ApartmentDetailsFragment newInstance(Bundle apartmentData) {
        ApartmentDetailsFragment fragment = new ApartmentDetailsFragment();
        fragment.setArguments(apartmentData);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_apartment_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // שליפת נתונים מה־Bundle
        if (getArguments() != null) {
            city = getArguments().getString("city");
            street = getArguments().getString("street");
            houseNumber = getArguments().getInt("houseNumber");
            description = getArguments().getString("description");
            imageUrl = getArguments().getString("imageUrl");
            ownerId = getArguments().getString("ownerId");
            apartmentId = getArguments().getString("apartmentId");
            price = getArguments().getInt("price");
            roommatesNeeded = getArguments().getInt("roommatesNeeded");
        }

        // חיבור אל רכיבי ה־XML
        TextView cityTV = view.findViewById(R.id.cityTextView);
        TextView streetTV = view.findViewById(R.id.streetTextView);
        TextView houseNumTV = view.findViewById(R.id.houseNumberTextView);
        TextView priceTV = view.findViewById(R.id.priceTextView);
        TextView roommatesTV = view.findViewById(R.id.roommatesTextView);
        TextView descriptionTV = view.findViewById(R.id.descriptionTextView);
        ImageView imageView = view.findViewById(R.id.apartmentImageView);
        Button messageBtn = view.findViewById(R.id.messageButton);

        // הצגת הנתונים
        cityTV.setText("עיר: " + city);
        streetTV.setText("רחוב: " + street);
        houseNumTV.setText("מספר בית: " + houseNumber);
        priceTV.setText("מחיר: " + price + " ₪");
        roommatesTV.setText("שותפים דרושים: " + roommatesNeeded);
        descriptionTV.setText("תיאור: " + description);

        Glide.with(requireContext())
                .load(!TextUtils.isEmpty(imageUrl) ? imageUrl : null)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(imageView);


        messageBtn.setOnClickListener(v -> {
            if (ownerId == null || ownerId.isEmpty()) {
                Toast.makeText(getContext(), "שגיאה: לא ניתן ליצור קשר עם בעל הדירה", Toast.LENGTH_SHORT).show();
                return;
            }

            ChatFragment chatFragment = new ChatFragment(ownerId, apartmentId);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, chatFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
}
