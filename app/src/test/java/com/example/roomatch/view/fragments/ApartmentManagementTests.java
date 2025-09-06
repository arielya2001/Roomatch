package com.example.roomatch.view.fragments;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
// Import Robolectric annotations and classes for running Android tests in a JVM
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
// Import InstantTaskExecutorRule to ensure LiveData updates are synchronous in tests
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
// Import model, repository, and viewmodel classes for testing the OwnerApartmentsViewModel
import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;
// Import Google Maps LatLng for simulating geographic coordinates in tests
import com.google.android.gms.maps.model.LatLng;
// Import Firestore and Tasks classes for mocking asynchronous repository operations
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
// Import Android Uri class for simulating image URIs in tests
import android.net.Uri;
// Import Android Looper for handling asynchronous operations in tests
import android.os.Looper;
// Import Java Collections for creating empty lists in mock responses
import java.util.Collections;
// Import JUnit assertions for verifying test outcomes
import static org.junit.Assert.*;
// Import Mockito methods for defining mock behavior and verifying interactions
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * This file contains unit tests for the OwnerApartmentsViewModel class using the Robolectric test runner
 * and Mockito for mocking dependencies. The tests verify the behavior of methods such as publishApartment,
 * updateApartment, and loadApartments under various scenarios, including valid input, invalid input, and
 * simulated network failures. The tests interact with a mocked ApartmentRepository to simulate data layer
 * operations, including Firestore Task-based asynchronous calls, and check the ViewModel's LiveData outputs
 * (publishSuccess, toastMessage, and filteredApartments) for expected values.
 *
 * The InstantTaskExecutorRule ensures that asynchronous LiveData updates are executed synchronously on the
 * main thread for reliable testing. MockitoRule manages the lifecycle of mock objects, simplifying setup.
 * The @Config(manifest = Config.NONE) annotation disables Android manifest loading, making the tests faster
 * and focused solely on business logic.
 *
 * The tests handle Hebrew error messages, reflecting the application's localized user feedback. Asynchronous
 * operations are synchronized using Shadows.shadowOf(Looper.getMainLooper()).idle() to ensure LiveData
 * updates are complete before assertions.
 *
 * The following test groups are included:
 *
 * - testPublishApartment_VariousCases:
 *   Tests the publishApartment method under different conditions, including valid input (with a pre-set address
 *   via setSelectedAddress), empty fields (invalid input preventing repository calls), network failures
 *   (simulated via Task exceptions), and negative numeric input (invalid input preventing repository calls).
 *
 * - testUpdateApartment_VariousCases:
 *   Tests the updateApartment method with valid data, invalid empty fields (preventing repository calls),
 *   non-existent apartment IDs (causing Task exceptions), and simulated network failures.
 *
 * - testLoadApartments_VariousCases:
 *   Tests the loadApartments method for a user, covering successful data loads (with and without results,
 *   updating filteredApartments LiveData), as well as network failure cases (triggering error toast messages).
 */

// Annotate the test class to run with RobolectricTestRunner for Android-specific testing in a JVM
@RunWith(RobolectricTestRunner.class)
// Configure Robolectric to disable manifest loading for faster, logic-focused tests
@Config(manifest = Config.NONE)
public class ApartmentManagementTests {

    // Define a rule to ensure LiveData updates are executed synchronously in tests
    @Rule public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();
    // Define a rule to initialize and manage Mockito mocks automatically
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    // Declare a mock ApartmentRepository to simulate data layer interactions
    @Mock ApartmentRepository mockRepository;
    // Declare a variable to hold the OwnerApartmentsViewModel instance under test
    private OwnerApartmentsViewModel viewModel;

    // Annotate the setup method to run before each test case
    @Before
    public void setUp() {
        viewModel = new OwnerApartmentsViewModel(mockRepository);
        when(mockRepository.getCurrentUserId()).thenReturn("testUserId");
        when(mockRepository.getApartmentsByOwnerId(anyString()))
                .thenReturn(Tasks.forResult(Collections.emptyList()));
    }

    @Test
    public void testPublishApartment_VariousCases() {
        // הגדרות בסיסיות
        String city = "תל אביב";
        String street = "דיזנגוף";
        String houseNumStr = "10";
        String priceStr = "3000";
        String roommatesStr = "2";
        String description = "דירה מהממת";
        Uri imageUri = null;
        LatLng location = new LatLng(32.0853, 34.7818);

        // Case 1: פרסום תקין
        when(mockRepository.publishApartment(any(Apartment.class), any(Uri.class)))
                .thenReturn(Tasks.forResult(mock(DocumentReference.class)));

        viewModel.publishApartment(city, street, houseNumStr, priceStr, roommatesStr, description, imageUri, location);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(viewModel.getPublishSuccess().getValue());
        assertEquals("הדירה פורסמה", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(1)).publishApartment(any(Apartment.class), any(Uri.class));

        // Case 2: שדה ריק
        viewModel.publishApartment("", street, houseNumStr, priceStr, roommatesStr, description, imageUri, location);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("כל השדות חייבים להיות מלאים", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(1)).publishApartment(any(Apartment.class), any(Uri.class)); // לא מתווסף

        // Case 3: ערך מספרי שלילי
        viewModel.publishApartment(city, street, "-5", priceStr, roommatesStr, description, imageUri, location);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שדות מספריים חייבים להיות חיוביים", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(1)).publishApartment(any(Apartment.class), any(Uri.class)); // עדיין לא מתווסף

        // Case 4: ערך מספרי לא חוקי
        viewModel.publishApartment(city, street, "abc", priceStr, roommatesStr, description, imageUri, location);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שדות מספריים חייבים להיות מספרים תקינים", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(1)).publishApartment(any(Apartment.class), any(Uri.class)); // עדיין רק 1

        // Case 5: שגיאה מה־Repository (כשל ברשת למשל)
        when(mockRepository.publishApartment(any(Apartment.class), any(Uri.class)))
                .thenReturn(Tasks.forException(new Exception("Network error")));

        viewModel.publishApartment(city, street, houseNumStr, priceStr, roommatesStr, description, imageUri, location);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שגיאה בפרסום: Network error", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(2)).publishApartment(any(Apartment.class), any(Uri.class)); // מתווסף רק כאן
    }


    // Annotate the test method to test the updateApartment method under various conditions
    @Test
    public void testUpdateApartment_VariousCases() {
        // Case 1: Valid input
        // Define valid input parameters for updating an apartment
        String apartmentId1 = "testApartmentId";
        String city1 = "Tel Aviv";
        String street1 = "Rothschild";
        String houseNumStr1 = "20";
        String priceStr1 = "3500";
        String roommatesStr1 = "4";
        String description1 = "Modern apartment";
        Uri imageUri1 = null;

        // Create a Task that simulates a successful update operation
        Task<Void> successTask = Tasks.forResult(null);
        // Configure the mock repository to return the successTask when updateApartment is called
        when(mockRepository.updateApartment(anyString(), any(Apartment.class), any(Uri.class)))
                .thenReturn(successTask);

        // Call the updateApartment method with valid input
        viewModel.updateApartment(apartmentId1, city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        // Process any pending asynchronous operations to update LiveData
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        // Verify that the publishSuccess LiveData is set to true
        assertTrue(viewModel.getPublishSuccess().getValue());
        // Verify that the toastMessage LiveData contains the success message in Hebrew
        assertEquals("דירה עודכנה בהצלחה", viewModel.getToastMessage().getValue());
        // Verify that the updateApartment method was called exactly once on the repository
        verify(mockRepository, times(1)).updateApartment(anyString(), any(Apartment.class), any(Uri.class));

        // Case 2: Empty field (invalid input)
        // Define input parameters with an empty city field to test validation failure
        String apartmentId2 = "testApartmentId";
        String city2 = "";
        String street2 = "Rothschild";
        String houseNumStr2 = "20";
        String priceStr2 = "3500";
        String roommatesStr2 = "4";
        String description2 = "Modern apartment";
        Uri imageUri2 = null;

        // Call the updateApartment method with invalid input
        viewModel.updateApartment(apartmentId2, city2, street2, houseNumStr2, priceStr2, roommatesStr2, description2, imageUri2);
        // Process any pending asynchronous operations to update LiveData
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        // Verify that the publishSuccess LiveData is set to false due to validation failure
        assertFalse(viewModel.getPublishSuccess().getValue());
        // Verify that the toastMessage LiveData contains the validation error message in Hebrew
        assertEquals("כל השדות חייבים להיות מלאים", viewModel.getToastMessage().getValue());
        // Verify that no additional calls were made to updateApartment due to validation failure
        verify(mockRepository, times(1)).updateApartment(anyString(), any(Apartment.class), any(Uri.class));

        // Case 3: Apartment not found
        // Define an invalid apartment ID to simulate a non-existent apartment
        String apartmentId3 = "nonExistentId";
        // Create a Task that simulates an update failure with an exception
        Task<Void> failUpdateTask = Tasks.forException(new IllegalArgumentException("Apartment not found"));
        // Configure the mock repository to return the failUpdateTask when updateApartment is called
        when(mockRepository.updateApartment(anyString(), any(Apartment.class), any(Uri.class)))
                .thenReturn(failUpdateTask);

        // Call the updateApartment method with a non-existent apartment ID
        viewModel.updateApartment(apartmentId3, city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        // Process any pending asynchronous operations to update LiveData
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        // Verify that the publishSuccess LiveData is set to false due to the failure
        assertFalse(viewModel.getPublishSuccess().getValue());
        // Verify that the toastMessage LiveData contains the error message for a non-existent apartment
        assertEquals("שגיאה בעדכון: Apartment not found", viewModel.getToastMessage().getValue());
        // Verify that updateApartment was called twice (once in Case 1, once here)
        verify(mockRepository, times(2)).updateApartment(anyString(), any(Apartment.class), any(Uri.class));

        // Case 4: Network failure
        // Create a Task that simulates a network failure with an exception
        Task<Void> networkFailTask = Tasks.forException(new Exception("Network error"));
        // Configure the mock repository to return the networkFailTask when updateApartment is called
        when(mockRepository.updateApartment(anyString(), any(Apartment.class), any(Uri.class)))
                .thenReturn(networkFailTask);

        // Call the updateApartment method with valid input to test network failure
        viewModel.updateApartment(apartmentId1, city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        // Process any pending asynchronous operations to update LiveData
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        // Verify that the publishSuccess LiveData is set to false due to network failure
        assertFalse(viewModel.getPublishSuccess().getValue());
        // Verify that the toastMessage LiveData contains the network error message in Hebrew
        assertEquals("שגיאה בעדכון: Network error", viewModel.getToastMessage().getValue());
        // Verify that updateApartment was called three times (once in Case 1, once in Case 3, once here)
        verify(mockRepository, times(3)).updateApartment(anyString(), any(Apartment.class), any(Uri.class));
    }

    // Annotate the test method to test the loadApartments method under various conditions
    @Test
    public void testLoadApartments_VariousCases() {
        // Case 1: Successful load with data
        // Define a user ID for loading apartments
        String ownerId1 = "testUserId";
        // Create a sample Apartment object to simulate a Firestore query result
        Apartment apartment = new Apartment("1", ownerId1, "Tel Aviv", "Rothschild", 20, 3500, 4, "Modern apartment", null);
        // Configure the mock repository to return a list with the sample apartment
        when(mockRepository.getApartmentsByOwnerId(ownerId1))
                .thenReturn(Tasks.forResult(Collections.singletonList(apartment)));

        // Call the loadApartments method with the user ID
        viewModel.loadApartments(ownerId1);
        // Process any pending asynchronous operations to update LiveData
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        // Verify that the filteredApartments LiveData is not null
        assertNotNull(viewModel.getFilteredApartments().getValue());
        // Verify that the filteredApartments LiveData contains exactly one apartment
        assertEquals(1, viewModel.getFilteredApartments().getValue().size());
        // Verify that no error message is set in the toastMessage LiveData
        assertNull(viewModel.getToastMessage().getValue());

        // Case 2: Successful load with no data
        // Define a user ID for loading apartments with no results
        String ownerId2 = "testUserId";
        // Configure the mock repository to return an empty list
        when(mockRepository.getApartmentsByOwnerId(ownerId2))
                .thenReturn(Tasks.forResult(Collections.emptyList()));

        // Call the loadApartments method with the user ID
        viewModel.loadApartments(ownerId2);
        // Process any pending asynchronous operations to update LiveData
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        // Verify that the filteredApartments LiveData is not null
        assertNotNull(viewModel.getFilteredApartments().getValue());
        // Verify that the filteredApartments LiveData contains no apartments
        assertEquals(0, viewModel.getFilteredApartments().getValue().size());
        // Verify that no error message is set in the toastMessage LiveData
        assertNull(viewModel.getToastMessage().getValue());

        // Case 3: Network failure
        // Define a user ID for testing network failure
        String ownerId3 = "testUserId";
        // Configure the mock repository to return a Task with a network error
        when(mockRepository.getApartmentsByOwnerId(ownerId3))
                .thenReturn(Tasks.forException(new Exception("Network error")));

        // Call the loadApartments method with the user ID
        viewModel.loadApartments(ownerId3);
        // Process any pending asynchronous operations to update LiveData
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        // Verify that the filteredApartments LiveData is not null
        assertNotNull(viewModel.getFilteredApartments().getValue());
        // Verify that the filteredApartments LiveData contains no apartments due to the error
        assertEquals(0, viewModel.getFilteredApartments().getValue().size());
        // Verify that the toastMessage LiveData contains an error message in Hebrew
        assertTrue(viewModel.getToastMessage().getValue().contains("שגיאה בטעינת הדירות"));
    }
}
