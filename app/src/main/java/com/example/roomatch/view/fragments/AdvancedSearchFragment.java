package com.example.roomatch.view.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ApartmentAdapter;
import com.example.roomatch.viewmodel.AdvancedSearchViewModel;
import com.example.roomatch.model.Apartment;
import com.example.roomatch.viewmodel.AppViewModelFactory;
import com.example.roomatch.viewmodel.ViewModelFactoryProvider;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;
import java.util.List;

public class AdvancedSearchFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private RecyclerView recyclerView;
    private ApartmentAdapter adapter;
    private Button buttonUseCurrentLocation;
    private AdvancedSearchViewModel viewModel;

    private LatLng selectedLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_advanced_search, container, false);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Map
        SupportMapFragment mapFragment = new SupportMapFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.mapFragmentContainer, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        recyclerView = view.findViewById(R.id.recyclerViewNearbyApartments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ApartmentAdapter(List.of(), getContext(), this::openApartmentDetails);
        recyclerView.setAdapter(adapter);

        buttonUseCurrentLocation = view.findViewById(R.id.buttonUseCurrentLocation);
        buttonUseCurrentLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            } else {
                useCurrentLocation();
            }
        });

        // ViewModel
        AppViewModelFactory factory = ViewModelFactoryProvider.createFactory();
        viewModel = new ViewModelProvider(this, factory).get(AdvancedSearchViewModel.class);

        // Address autocomplete
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                selectedLocation = place.getLatLng();
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15f));
                viewModel.fetchAndSortApartmentsByDistance(selectedLocation);
            }

            @Override
            public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                Toast.makeText(getContext(), "שגיאה בבחירת כתובת: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getNearbyApartments().observe(getViewLifecycleOwner(), apartments -> {
            adapter.updateApartments(apartments);
            adapter.notifyDataSetChanged();
        });
    }

    private void openApartmentDetails(Apartment apt) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("apartment", apt);
        ApartmentDetailsFragment fragment = ApartmentDetailsFragment.newInstance(bundle);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @SuppressLint("MissingPermission")
    private void useCurrentLocation() {
        if (map != null) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            map.setOnMyLocationChangeListener(location -> {
                selectedLocation = new LatLng(location.getLatitude(), location.getLongitude());
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15f));
                viewModel.fetchAndSortApartmentsByDistance(selectedLocation);
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
    }
}
