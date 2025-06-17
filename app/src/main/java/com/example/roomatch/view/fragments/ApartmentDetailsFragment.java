package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.roomatch.R;
import com.example.roomatch.viewmodel.ViewModelFactoryProvider;
import com.example.roomatch.model.Apartment;
import com.example.roomatch.viewmodel.AppViewModelFactory;
import com.example.roomatch.viewmodel.ApartmentDetailsViewModel;

public class ApartmentDetailsFragment extends Fragment {

    private ApartmentDetailsViewModel viewModel;

    public static ApartmentDetailsFragment newInstance(Bundle apartmentData) {
        ApartmentDetailsFragment fragment = new ApartmentDetailsFragment();
        fragment.setArguments(apartmentData);
        return fragment;
    }

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

        // שימוש ב-AppViewModelFactory ממקום מרכזי
        AppViewModelFactory factory = ViewModelFactoryProvider.createFactory();
        viewModel = new ViewModelProvider(this, factory).get(ApartmentDetailsViewModel.class);

        if (viewModel.getApartmentDetails().getValue() == null && getArguments() != null) {
            Apartment apartment = (Apartment) getArguments().getSerializable("apartment");
            if (apartment != null) {
                viewModel.setApartmentDetails(apartment);
            }
        }

        // קישור רכיבי UI
        TextView cityTV = view.findViewById(R.id.cityTextView);
        TextView streetTV = view.findViewById(R.id.streetTextView);
        TextView houseNumTV = view.findViewById(R.id.houseNumberTextView);
        TextView priceTV = view.findViewById(R.id.priceTextView);
        TextView roommatesTV = view.findViewById(R.id.roommatesTextView);
        TextView descriptionTV = view.findViewById(R.id.descriptionTextView);
        ImageView imageView = view.findViewById(R.id.apartmentImageView);
        Button messageBtn = view.findViewById(R.id.messageButton);

        // תצוגת הדירה
        viewModel.getApartmentDetails().observe(getViewLifecycleOwner(), apartment -> {
            cityTV.setText(apartment.getCity());
            streetTV.setText(apartment.getStreet());
            houseNumTV.setText(String.valueOf(apartment.getHouseNumber()));
            priceTV.setText(apartment.getPrice() + " ₪ / חודש");
            roommatesTV.setText(apartment.getRoommatesNeeded() + " מקומות פנויים ");
            descriptionTV.setText("תיאור: " + apartment.getDescription());

            Glide.with(requireContext())
                    .load(!TextUtils.isEmpty(apartment.getImageUrl()) ? apartment.getImageUrl() : null)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(imageView);
        });

        // מעבר לצ'אט
        messageBtn.setOnClickListener(v -> viewModel.onMessageOwnerClicked());

        viewModel.getNavigateToChatWith().observe(getViewLifecycleOwner(), chatKey -> {
            if (chatKey != null) {
                String[] parts = chatKey.split("::");
                if (parts.length == 2) {
                    String ownerId = parts[0];
                    String apartmentId = parts[1];
                    ChatFragment chatFragment = new ChatFragment(ownerId, apartmentId);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, chatFragment)
                            .addToBackStack(null)
                            .commit();
                } else {
                    Toast.makeText(getContext(), "שגיאה בנתוני הצ'אט", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}