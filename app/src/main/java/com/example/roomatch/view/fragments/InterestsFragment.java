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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InterestsFragment extends Fragment {

    private CheckBox checkboxMusic, checkboxSports, checkboxTravel, checkboxCooking, checkboxReading;

    private OnInterestsChangedListener listener;

    public interface OnInterestsChangedListener {
        void onInterestsChanged(List<String> updatedInterests);
    }

    public void setOnInterestsChangedListener(OnInterestsChangedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_interests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        checkboxMusic = view.findViewById(R.id.checkboxMusic);
        checkboxSports = view.findViewById(R.id.checkboxSports);
        checkboxTravel = view.findViewById(R.id.checkboxTravel);
        checkboxCooking = view.findViewById(R.id.checkboxCooking);
        checkboxReading = view.findViewById(R.id.checkboxReading);

        View.OnClickListener boxChangedListener = v -> {
            if (listener != null) {
                listener.onInterestsChanged(getInterests());
            }
        };

        checkboxMusic.setOnClickListener(boxChangedListener);
        checkboxSports.setOnClickListener(boxChangedListener);
        checkboxTravel.setOnClickListener(boxChangedListener);
        checkboxCooking.setOnClickListener(boxChangedListener);
        checkboxReading.setOnClickListener(boxChangedListener);
    }

    public List<String> getInterests() {
        List<String> interests = new ArrayList<>();
        if (checkboxMusic.isChecked()) interests.add("מוזיקה");
        if (checkboxSports.isChecked()) interests.add("ספורט");
        if (checkboxTravel.isChecked()) interests.add("טיולים");
        if (checkboxCooking.isChecked()) interests.add("בישול");
        if (checkboxReading.isChecked()) interests.add("קריאה");
        return interests;
    }

    public void setBoxes(String interests) {
        List<String> interestsList = Arrays.asList(interests.split(", "));
        checkboxMusic.setChecked(false);
        checkboxSports.setChecked(false);
        checkboxTravel.setChecked(false);
        checkboxCooking.setChecked(false);
        checkboxReading.setChecked(false);

        for (String interest : interestsList) {
            switch (interest) {
                case "מוזיקה":
                    checkboxMusic.setChecked(true);
                    break;
                case "ספורט":
                    checkboxSports.setChecked(true);
                    break;
                case "טיולים":
                    checkboxTravel.setChecked(true);
                    break;
                case "בישול":
                    checkboxCooking.setChecked(true);
                    break;
                case "קריאה":
                    checkboxReading.setChecked(true);
                    break;
            }
        }
    }
    public static List<String> getAllInterests()
    {
        List<String> interests =  new ArrayList<String>();
        interests.add("מוזיקה");
        interests.add("ספורט");
        interests.add("טיולים");
        interests.add("בישול");
        interests.add("קריאה");
        return interests;
    }
}
