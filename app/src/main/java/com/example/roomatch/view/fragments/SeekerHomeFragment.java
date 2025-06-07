package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.roomatch.R;

public class SeekerHomeFragment extends Fragment {

    private Button btnFindApartments, btnFindPartners;

    public SeekerHomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seeker_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnFindApartments = view.findViewById(R.id.btnFindApartments);
        btnFindPartners = view.findViewById(R.id.btnFindPartners);

        btnFindApartments.setOnClickListener(v -> {
            FragmentTransaction ft = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();
            ft.replace(R.id.fragmentContainer, new ApartmentSearchFragment());
            ft.addToBackStack(null);
            ft.commit();
        });

        btnFindPartners.setOnClickListener(v -> {
            FragmentTransaction ft = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();
            ft.replace(R.id.fragmentContainer, new PartnerFragment());
            ft.addToBackStack(null);
            ft.commit();
        });
    }
}
