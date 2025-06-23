package com.example.roomatch.viewmodel;

import androidx.lifecycle.ViewModel;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.AppViewModelFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ViewModelFactoryProvider {

    public static AppViewModelFactory createFactory() {
        Map<Class<? extends ViewModel>, Supplier<? extends ViewModel>> creators = new HashMap<>();

        // רישום ApartmentRepository משותף
        ApartmentRepository apartmentRepository = new ApartmentRepository();

        // רישום OwnerApartmentsViewModel
        creators.put(OwnerApartmentsViewModel.class, () -> new OwnerApartmentsViewModel(apartmentRepository));

        // רישום CreateProfileViewModel
        creators.put(CreateProfileViewModel.class, CreateProfileViewModel::new);

        // רישום ApartmentDetailsViewModel
        creators.put(ApartmentDetailsViewModel.class, () -> new ApartmentDetailsViewModel(apartmentRepository));

        // הוסף כאן ViewModels נוספים לפי הצורך
        // לדוגמה:
        // creators.put(SeekerApartmentsViewModel.class, () -> new SeekerApartmentsViewModel(apartmentRepository));

        creators.put(AdvancedSearchViewModel.class, () -> new AdvancedSearchViewModel(apartmentRepository));

        return new AppViewModelFactory(creators);
    }
}