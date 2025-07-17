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
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.roomatch.R;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.view.activities.MainActivity;
import com.example.roomatch.viewmodel.ApartmentDetailsViewModel;
import com.example.roomatch.viewmodel.AppViewModelFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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

        // הגדרת Factory עם ApartmentRepository
        Map<Class<? extends ViewModel>, Supplier<? extends ViewModel>> creators = new HashMap<>();
        creators.put(ApartmentDetailsViewModel.class, () -> new ApartmentDetailsViewModel(new ApartmentRepository(MainActivity.isTestMode)));
        AppViewModelFactory factory = new AppViewModelFactory(creators);

        // יצירת ViewModel עם ה-Factory
        viewModel = new ViewModelProvider(this, factory).get(ApartmentDetailsViewModel.class);

        // אם הדאטה עדיין לא ב-ViewModel, נטען אותה
        if (viewModel.getApartmentDetails().getValue() == null && getArguments() != null) {
            Map<String, Object> apt = new HashMap<>();
            Bundle args = getArguments();
            apt.put("city", args.getString("city"));
            apt.put("street", args.getString("street"));
            apt.put("houseNumber", args.getInt("houseNumber"));
            apt.put("description", args.getString("description"));
            apt.put("imageUrl", args.getString("imageUrl"));
            apt.put("ownerId", args.getString("ownerId"));
            apt.put("apartmentId", args.getString("apartmentId"));
            apt.put("price", args.getInt("price"));
            apt.put("roommatesNeeded", args.getInt("roommatesNeeded"));
            viewModel.setApartmentDetails(apt);
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
        viewModel.getApartmentDetails().observe(getViewLifecycleOwner(), data -> {
            cityTV.setText((String) data.get("city"));
            streetTV.setText((String) data.get("street"));
            houseNumTV.setText(data.get("houseNumber") + "");
            priceTV.setText(data.get("price") + " ₪ / חודש");
            roommatesTV.setText(data.get("roommatesNeeded") + " מקומות פנויים ");
            descriptionTV.setText("תיאור: " + data.get("description"));

            Glide.with(requireContext())
                    .load(!TextUtils.isEmpty((String) data.get("imageUrl")) ? data.get("imageUrl") : null)
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