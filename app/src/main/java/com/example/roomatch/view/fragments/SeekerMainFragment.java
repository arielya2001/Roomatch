package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.roomatch.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SeekerMainFragment extends Fragment {

    public SeekerMainFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seeker_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNavSeeker);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            if (item.getItemId() == R.id.menu_apartments) {
                selected = new SeekerApartmentsFragment();
            } else if (item.getItemId() == R.id.menu_partners) {
                selected = new SeekerPartnersFragment();
            }

            if (selected != null) {
                getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.seekerContentFrame, selected)
                        .commit();
            }
            return true;
        });

        // טען את ברירת המחדל: דירות
        bottomNav.setSelectedItemId(R.id.menu_apartments);
    }
}
