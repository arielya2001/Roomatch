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

import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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


// רץ עם Robolectric לטסטים UI/אסינכרוניים, מתעלם מקובץ manifest
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ApartmentManagementTests {

    // כלל להרצת משימות ב-Thread הראשי לטסטים אסינכרוניים
    @Rule public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();
    // כלל לניהול מוקים (Mocks) עם Mockito
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    // מוק של מחסנית הדירות לטובת הטסטים
    @Mock ApartmentRepository mockRepository;
    // מופע של ViewModel לטסטים, תלוי ב-mockRepository
    private OwnerApartmentsViewModel viewModel;

    // מתודה שמריצה את ההכנה לפני כל טסט
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this); // מאתחל את המוקים
        viewModel = new OwnerApartmentsViewModel(mockRepository); // יוצר מופע ViewModel

        // Stubbing ברירת מחדל ל-getApartmentsByOwnerId, מחזיר רשימה ריקה
        QuerySnapshot defaultQuerySnapshot = mock(QuerySnapshot.class); // יוצר מוק של QuerySnapshot
        when(defaultQuerySnapshot.getDocuments()).thenReturn(Collections.emptyList()); // מגדיר תוצאה ריקה
        when(mockRepository.getApartmentsByOwnerId(anyString())).thenReturn(Tasks.forResult(defaultQuerySnapshot)); // Stubbing עם תוצאה מוצלחת

        // Stubbing ל-getCurrentUserId, מחזיר מזהה משתמש קבוע
        when(mockRepository.getCurrentUserId()).thenReturn("testUserId");

        // Stubbing ל-publishApartment, מחזיר תוצאה מוצלחת עם DocumentReference
        when(mockRepository.publishApartment(anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()))
                .thenReturn(Tasks.forResult(mock(DocumentReference.class)));

        // Stubbing ל-updateApartment, מחזיר תוצאה מוצלחת עם null (לפי ההגדרה הנוכחית)
        when(mockRepository.updateApartment(anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()))
                .thenReturn(Tasks.forResult(null));

        // Stubbing ל-deleteApartment, מחזיר תוצאה מוצלחת (אם כי לא משתמשים בו בטסטים אלה)
        when(mockRepository.deleteApartment(anyString()))
                .thenReturn(Tasks.forResult(null));

        // איפוס מצב ה-LiveData של publishSuccess לפני כל טסט
        viewModel.setPublishSuccess(false);
    }

    // טסט לבדיקת מקרים שונים של פרסום דירה
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
        Shadows.shadowOf(Looper.getMainLooper()).idle(); // ממתין לעדכון אסינכרוני
        assertTrue(viewModel.getPublishSuccess().getValue()); // בודק שהפרסום הצליח
        assertEquals("הדירה פורסמה", viewModel.getToastMessage().getValue()); // בודק הודעת הצלחה
        verify(mockRepository, times(1)).publishApartment(anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()); // מאמת שהמתודה נקראה פעם אחת

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
        assertFalse(viewModel.getPublishSuccess().getValue()); // בודק שהפרסום נכשל
        assertEquals("כל השדות חייבים להיות מלאים", viewModel.getToastMessage().getValue()); // בודק הודעת שגיאה
        verify(mockRepository, times(1)).publishApartment(anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()); // מאמת שלא נקרא שוב

        // מקרה 3: כשל רשת
        when(mockRepository.publishApartment(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyString(), any()))
                .thenReturn(Tasks.forException(new Exception("Network error"))); // משנה Stubbing לכשל
        viewModel.publishApartment(city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שגיאה בפרסום: Network error", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(2)).publishApartment(anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()); // מאמת שני קריאות

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
        assertEquals("שדות מספריים חייבים להיות חיוביים", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(2)).publishApartment(anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()); // מאמת שלא נקרא שוב
    }

    // טסט לבדיקת מקרים שונים של עדכון דירה
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
        verify(mockRepository, times(1)).updateApartment(anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()); // מאמת קריאה עם 9 ארגומנטים

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
        verify(mockRepository, times(1)).updateApartment(anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()); // מאמת שלא נקרא שוב

        // מקרה 3: דירה לא נמצאה
        String apartmentId3 = "nonExistentId";
        when(mockRepository.updateApartment(anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()))
                .thenReturn(Tasks.forException(new IllegalArgumentException("Apartment not found")));
        viewModel.updateApartment(apartmentId3, city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שגיאה: הדירה לא נמצאה", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(2)).updateApartment(anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()); // מאמת שתי קריאות

        // מקרה 4: כשל רשת
        when(mockRepository.updateApartment(anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()))
                .thenReturn(Tasks.forException(new Exception("Network error")));
        viewModel.updateApartment(apartmentId1, city1, street1, houseNumStr1, priceStr1, roommatesStr1, description1, imageUri1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(viewModel.getPublishSuccess().getValue());
        assertEquals("שגיאה בעדכון: Network error", viewModel.getToastMessage().getValue());
        verify(mockRepository, times(3)).updateApartment(anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), any()); // מאמת שלוש קריאות
    }

    // טסט לבדיקת מקרים שונים של טעינת דירות
    @Test
    public void testLoadApartments_VariousCases() {
        // מקרה 1: טעינה מוצלחת עם נתונים
        String ownerId1 = "testUserId";
        QuerySnapshot mockQuerySnapshot1 = mock(QuerySnapshot.class);
        when(mockQuerySnapshot1.getDocuments()).thenReturn(Collections.singletonList(mock(DocumentSnapshot.class)));
        when(mockRepository.getApartmentsByOwnerId(ownerId1)).thenReturn(Tasks.forResult(mockQuerySnapshot1));

        viewModel.loadApartments(ownerId1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(viewModel.getFilteredApartments().getValue()); // בודק שהרשימה לא ריקה
        assertEquals(1, viewModel.getFilteredApartments().getValue().size()); // בודק גודל הרשימה
        assertNull(viewModel.getToastMessage().getValue()); // בודק שאין הודעת שגיאה

        // מקרה 2: טעינה מוצלחת ללא נתונים
        String ownerId2 = "testUserId";
        QuerySnapshot mockQuerySnapshot2 = mock(QuerySnapshot.class);
        when(mockQuerySnapshot2.getDocuments()).thenReturn(Collections.emptyList());
        when(mockRepository.getApartmentsByOwnerId(ownerId2)).thenReturn(Tasks.forResult(mockQuerySnapshot2));

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