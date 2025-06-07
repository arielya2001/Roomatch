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

    public static final String ARG_APARTMENT = "apartment";
    private String address, description, imageUrl, ownerId, apartmentId; // הוספנו apartmentId
    private int price, roommatesNeeded;

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

        // קבלת נתונים
        if (getArguments() != null) {
            address = getArguments().getString("address");
            description = getArguments().getString("description");
            imageUrl = getArguments().getString("imageUrl");
            ownerId = getArguments().getString("ownerId");
            apartmentId = getArguments().getString("apartmentId"); // חדש
            price = getArguments().getInt("price");
            roommatesNeeded = getArguments().getInt("roommatesNeeded");
        }

        // הצגת נתונים
        TextView addressTV = view.findViewById(R.id.addressTextView);
        TextView priceTV = view.findViewById(R.id.priceTextView);
        TextView roommatesTV = view.findViewById(R.id.roommatesTextView);
        TextView descriptionTV = view.findViewById(R.id.descriptionTextView);
        ImageView imageView = view.findViewById(R.id.apartmentImageView);
        Button messageBtn = view.findViewById(R.id.messageButton);

        addressTV.setText("כתובת: " + address);
        priceTV.setText("מחיר: " + price + " ₪");
        roommatesTV.setText("שותפים דרושים: " + roommatesNeeded);
        descriptionTV.setText("תיאור: " + description);

        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(requireContext()).load(imageUrl).into(imageView);
        }

        messageBtn.setOnClickListener(v -> {
            if (ownerId == null || ownerId.isEmpty()) {
                Toast.makeText(getContext(), "שגיאה: לא ניתן ליצור קשר עם בעל הדירה", Toast.LENGTH_SHORT).show();
                return;
            }

            // פתיחת הצ'אט עם בעל הדירה, כולל מזהה הדירה
            ChatFragment chatFragment = new ChatFragment(ownerId, apartmentId); // ← נשלח גם את מזהה הדירה

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, chatFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
}
