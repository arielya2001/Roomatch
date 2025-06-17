package com.example.roomatch.view.fragments;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import android.net.Uri;
import android.os.Looper;

import java.util.Collections;

/**
 * This file contains unit tests for the OwnerApartmentsViewModel class using the Robolectric test runner
 * and Mockito for mocking dependencies. The tests focus on verifying the behavior of methods such as
 * publishApartment, updateApartment, and loadApartments under various scenarios, including valid input,
 * invalid input, and network failures.
 *
 * Each test uses a mock ApartmentRepository to simulate interactions with the data layer, and checks
 * the ViewModel's LiveData outputs, such as success flags and toast messages.
 *
 * The InstantTaskExecutorRule ensures that asynchronous LiveData updates are executed synchronously on the main thread.
 * MockitoRule manages mock object lifecycle and simplifies setup.
 * The @Config(manifest = Config.NONE) annotation disables Android manifest loading, making the tests
 * faster and focused solely on business logic.
 *
 * The following test groups are included:
 *
 * - testPublishApartment_VariousCases:
 *   Tests the publishApartment method under different conditions including:
 *   valid input, empty fields (invalid), network failure, and negative numeric input (invalid).
 *
 * - testUpdateApartment_VariousCases:
 *   Tests updateApartment with valid data, invalid empty fields, non-existent apartment IDs (causing exceptions),
 *   and simulated network failures.
 *
 * - testLoadApartments_VariousCases:
 *   Tests the loading of apartments for a user, covering successful data loads (with and without results),
 *   as well as network failure cases.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ApartmentManagementTests {

    @Rule public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock ApartmentRepository mockRepository;
    private OwnerApartmentsViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new OwnerApartmentsViewModel(mockRepository);

        // Stubbing ל-getCurrentUserId
        when(mockRepository.getCurrentUserId()).thenReturn("testUserId");

        // Stubbing ל-getApartmentsByOwnerId עם רשימה ריקה (החזר חדש בהתאם למודל)
        when(mockRepository.getApartmentsByOwnerId(anyString()))
                .thenReturn(Tasks.forResult(Collections.emptyList()));

        // --------------------------
        // Stubbing ל-publishApartment
        // --------------------------
        DocumentReference mockDocRef = mock(DocumentReference.class);
        Task<DocumentReference> publishTask = mock(Task.class);
        when(publishTask.isSuccessful()).thenReturn(true);
        when(publishTask.getResult()).thenReturn(mockDocRef);
        doAnswer(invocation -> {
            OnSuccessListener<DocumentReference> listener = invocation.getArgument(0);
            listener.onSuccess(mockDocRef);
            return publishTask;
        }).when(publishTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockRepository.publishApartment(any(Apartment.class), any(Uri.class)))
                .thenReturn(publishTask);

        // --------------------------
        // Stubbing ל-updateApartment
        // --------------------------
        Task<Void> updateTask = mock(Task.class);
        when(updateTask.isSuccessful()).thenReturn(true);
        when(updateTask.getResult()).thenReturn(null); // מתאים ל־Task<Void>
        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return updateTask;
        }).when(updateTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockRepository.updateApartment(anyString(), any(Apartment.class), any(Uri.class)))
                .thenReturn(updateTask);

        // --------------------------
        // Stubbing ל-deleteApartment
        // --------------------------
        Task<Void> deleteTask = mock(Task.class);
        when(deleteTask.isSuccessful()).thenReturn(true);
        when(deleteTask.getResult()).thenReturn(null);
        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return deleteTask;
        }).when(deleteTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockRepository.deleteApartment(anyString())).thenReturn(deleteTask);

    }


    @Test
    public void testPublishApartment_VariousCases() {
        // מקרה 1: קלט תקין
        String city1 = "ariel";
        String street1 = "neve shaanan";
        String houseNumStr1 = "12";
        String priceStr1 = "2500";
        String roommatesStr1 = "4";
        String description1 = "very pretty apartment";
        Uri imageUri1 = null;

        viewModel.publishApartment(city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue(viewModel.getPublishSuccess().getValue());
        assertEquals("הדירה פורסמה", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(1)).publishApartment(any(Apartment.class), any(Uri.class));

        // מקרה 2: שדה ריק (קלט לא תקין)
        String city2 = "";
        String street2 = "neve shaanan";
        String houseNumStr2 = "12";
        String priceStr2 = "2500";
        String roommatesStr2 = "4";
        String description2 = "very pretty apartment";
        Uri imageUri2 = null;

        viewModel.publishApartment(city2, street2, houseNumStr2, priceStr2, roommatesStr2, description2, imageUri2);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("כל השדות חייבים להיות מלאים", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(1)).publishApartment(any(Apartment.class), any(Uri.class));

// מקרה 3: כשל רשת
        Task<DocumentReference> failPublishTask = Tasks.forException(new Exception("Network error"));
        when(mockRepository.publishApartment(any(Apartment.class), any(Uri.class)))
                .thenReturn(failPublishTask);

        viewModel.publishApartment(city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שגיאה בפרסום: Network error", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(2)).publishApartment(any(Apartment.class), any(Uri.class));


        // מקרה 4: מספר שלילי (קלט לא תקין)
        String city4 = "ariel";
        String street4 = "neve shaanan";
        String houseNumStr4 = "-12";
        String priceStr4 = "2500";
        String roommatesStr4 = "4";
        String description4 = "very pretty apartment";
        Uri imageUri4 = null;

        viewModel.publishApartment(city4, street4, houseNumStr4, priceStr4, roommatesStr4, description4, imageUri4);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שדות מספריים חיוביים", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(2)).publishApartment(any(Apartment.class), any(Uri.class));
    }

    @Test
    public void testUpdateApartment_VariousCases() {
        // מקרה 1: קלט תקין
        String apartmentId1 = "testApartmentId";
        String city1 = "Tel Aviv";
        String street1 = "Rothschild";
        String houseNumStr1 = "20";
        String priceStr1 = "3500";
        String roommatesStr1 = "4";
        String description1 = "Modern apartment";
        Uri imageUri1 = null;

        viewModel.updateApartment(apartmentId1, city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue(viewModel.getPublishSuccess().getValue());
        assertEquals("דירה עודכנה בהצלחה", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(1)).updateApartment(anyString(), any(Apartment.class), any(Uri.class));

        // מקרה 2: שדה ריק (קלט לא תקין)
        String apartmentId2 = "testApartmentId";
        String city2 = "";
        String street2 = "Rothschild";
        String houseNumStr2 = "20";
        String priceStr2 = "3500";
        String roommatesStr2 = "4";
        String description2 = "Modern apartment";
        Uri imageUri2 = null;

        viewModel.updateApartment(apartmentId2, city2, street2, houseNumStr2, priceStr2, roommatesStr2, description2, imageUri2);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("כל השדות חייבים להיות מלאים", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(1)).updateApartment(anyString(), any(Apartment.class), any(Uri.class));

        // מקרה 3: דירה לא נמצאה
        String apartmentId3 = "nonExistentId";
        Task<Void> failUpdateTask = mock(Task.class);
        when(failUpdateTask.isSuccessful()).thenReturn(false);
        when(failUpdateTask.getException()).thenReturn(new IllegalArgumentException("Apartment not found"));
        when(mockRepository.updateApartment(anyString(), any(Apartment.class), any(Uri.class))).thenReturn(failUpdateTask);
        viewModel.updateApartment(apartmentId3, city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שגיאה בעדכון: Apartment not found", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(2)).updateApartment(anyString(), any(Apartment.class), any(Uri.class));

        // מקרה 4: כשל רשת
        Task<Void> networkFailTask = Tasks.forException(new Exception("Network error"));
        when(mockRepository.updateApartment(anyString(), any(Apartment.class), any(Uri.class)))
                .thenReturn(networkFailTask);

        viewModel.updateApartment(apartmentId1, city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שגיאה בעדכון: Network error", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(3)).updateApartment(anyString(), any(Apartment.class), any(Uri.class));

    }

    @Test
    public void testLoadApartments_VariousCases() {
        // מקרה 1: טעינה מוצלחת עם נתונים
        String ownerId1 = "testUserId";
        QuerySnapshot mockQuerySnapshot1 = mock(QuerySnapshot.class);
        when(mockQuerySnapshot1.getDocuments()).thenReturn(Collections.singletonList(mock(DocumentSnapshot.class)));
        when(mockRepository.getApartmentsByOwnerId(ownerId1)).thenReturn(Tasks.forResult(Collections.singletonList(mock(Apartment.class))));

        viewModel.loadApartments(ownerId1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(viewModel.getFilteredApartments().getValue());
        assertEquals(1, viewModel.getFilteredApartments().getValue().size());
        assertNull(viewModel.getToastMessage().getValue());

        // מקרה 2: טעינה מוצלחת ללא נתונים
        String ownerId2 = "testUserId";
        QuerySnapshot mockQuerySnapshot2 = mock(QuerySnapshot.class);
        when(mockQuerySnapshot2.getDocuments()).thenReturn(Collections.emptyList());
        when(mockRepository.getApartmentsByOwnerId(ownerId2)).thenReturn(Tasks.forResult(Collections.emptyList()));

        viewModel.loadApartments(ownerId2);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(viewModel.getFilteredApartments().getValue());
        assertEquals(0, viewModel.getFilteredApartments().getValue().size());
        assertNull(viewModel.getToastMessage().getValue());

        // מקרה 3: כשל (שגיאת רשת)
        String ownerId3 = "testUserId";
        when(mockRepository.getApartmentsByOwnerId(ownerId3))
                .thenReturn(Tasks.forException(new Exception("Network error")));

        viewModel.loadApartments(ownerId3);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(viewModel.getFilteredApartments().getValue());
        assertEquals(0, viewModel.getFilteredApartments().getValue().size());
        assertTrue(viewModel.getToastMessage().getValue().contains("שגיאה בטעינת הדירות"));
    }
}