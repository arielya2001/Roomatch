package com.example.roomatch.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.AppViewModelFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ViewModelFactoryProvider {
    // 👇 נוסיף שדה שניתן להחלפה בטסטים
    public static ViewModelProvider.Factory factory = createFactory();

    public static AppViewModelFactory createFactory() {
        Map<Class<? extends ViewModel>, Supplier<? extends ViewModel>> creators = new HashMap<>();

        ApartmentRepository apartmentRepository = new ApartmentRepository();

        creators.put(OwnerApartmentsViewModel.class, () -> new OwnerApartmentsViewModel(apartmentRepository));
        creators.put(CreateProfileViewModel.class, CreateProfileViewModel::new);
        creators.put(ApartmentDetailsViewModel.class, () -> new ApartmentDetailsViewModel(apartmentRepository));
        creators.put(AdvancedSearchViewModel.class, () -> new AdvancedSearchViewModel(apartmentRepository));

        return new AppViewModelFactory(creators);
    }
}
