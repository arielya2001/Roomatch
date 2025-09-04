package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.roomatch.R;
import com.example.roomatch.viewmodel.ProfileViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LifeStylesFragment extends Fragment {

    private CheckBox checkBoxClean, checkBoxSmoker, checkBoxNightOwl, checkBoxQuiet, checkBoxParty;

    private ProfileViewModel viewModel;

    // interface for checkbox change
    public interface OnLifestyleChangedListener {
        void onLifestyleChanged(List<String> updatedList);
    }

    private OnLifestyleChangedListener lifestyleChangedListener;

    public void setOnLifestyleChangedListener(OnLifestyleChangedListener listener) {
        this.lifestyleChangedListener = listener;
    }

    public LifeStylesFragment() {
        // Required empty public constructor
    }

    public static LifeStylesFragment newInstance() {
        return new LifeStylesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_life_styles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        checkBoxClean = view.findViewById(R.id.checkboxClean);
        checkBoxParty = view.findViewById(R.id.checkboxParty);
        checkBoxQuiet = view.findViewById(R.id.checkboxQuiet);
        checkBoxSmoker = view.findViewById(R.id.checkboxSmoker);
        checkBoxNightOwl = view.findViewById(R.id.checkboxNightOwl);

        setupListeners();
    }

    private void setupListeners() {
        View.OnClickListener listener = v -> {
            if (lifestyleChangedListener != null) {
                lifestyleChangedListener.onLifestyleChanged(getLifeStyles());
            }
        };

        checkBoxClean.setOnClickListener(listener);
        checkBoxParty.setOnClickListener(listener);
        checkBoxQuiet.setOnClickListener(listener);
        checkBoxSmoker.setOnClickListener(listener);
        checkBoxNightOwl.setOnClickListener(listener);
    }

    public void setViewModel(ProfileViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public List<String> getLifeStyles() {
        List<String> lifeStyles = new ArrayList<>();
        if (checkBoxClean.isChecked()) lifeStyles.add("נקי");
        if (checkBoxSmoker.isChecked()) lifeStyles.add("מעשן");
        if (checkBoxNightOwl.isChecked()) lifeStyles.add("חיית לילה");
        if (checkBoxQuiet.isChecked()) lifeStyles.add("שקט");
        if (checkBoxParty.isChecked()) lifeStyles.add("אוהב מסיבות");
        return lifeStyles;
    }

    public void setBoxes(String lifeStyles) {
        List<String> lifeStylesList = Arrays.asList(lifeStyles.split(", "));
        checkBoxClean.setChecked(false);
        checkBoxSmoker.setChecked(false);
        checkBoxNightOwl.setChecked(false);
        checkBoxQuiet.setChecked(false);
        checkBoxParty.setChecked(false);

        for (String s : lifeStylesList) {
            switch (s) {
                case "נקי":
                    checkBoxClean.setChecked(true);
                    break;
                case "מעשן":
                    checkBoxSmoker.setChecked(true);
                    break;
                case "חיית לילה":
                    checkBoxNightOwl.setChecked(true);
                    break;
                case "שקט":
                    checkBoxQuiet.setChecked(true);
                    break;
                case "אוהב מסיבות":
                    checkBoxParty.setChecked(true);
                    break;
            }
        }
    }
    public static List<String> getAlllifeStyles()
    {
        List<String> lifeStyles =  new ArrayList<String>();
        lifeStyles.add("נקי");
        lifeStyles.add("מעשן");
        lifeStyles.add("חיית לילה");
        lifeStyles.add("שקט");
        lifeStyles.add("אוהב מסיבות");
        return lifeStyles;
    }
}
