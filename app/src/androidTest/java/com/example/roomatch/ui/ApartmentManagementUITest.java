package com.example.roomatch.ui;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.view.activities.MainActivity;
import com.example.roomatch.view.fragments.OwnerApartmentsFragment;
import com.example.roomatch.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;

import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;

public class ApartmentManagementUITest {

    private ApartmentRepository mockRepo;

    @Before
    public void setup() {
        // הזרקת mock
        mockRepo = Mockito.mock(ApartmentRepository.class);

        // הצלחה מזויפת לפרסום דירה
        Mockito.when(mockRepo.publishApartment(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.isNull()))
                .thenReturn(Tasks.forResult(Mockito.mock(DocumentReference.class)));
    }

    @Test
    public void publishApartment_displaysSuccessToast() {
        // הפעלת מצב טסט
        MainActivity.isTestMode = true;
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra("fragment", "owner_apartments");

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                OwnerApartmentsFragment fragment = new OwnerApartmentsFragment();
                fragment.setTestingConditions(mockRepo);
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commitNow();
            });

            // פתיחת דיאלוג פרסום דירה
            onView(withId(R.id.buttonChats)).perform(click());

            onView(withId(R.id.editCity)).perform(typeText("תל אביב"), closeSoftKeyboard());
            onView(withId(R.id.editStreet)).perform(typeText("דיזנגוף"), closeSoftKeyboard());
            onView(withId(R.id.editHouseNumber)).perform(typeText("12"), closeSoftKeyboard());
            onView(withId(R.id.editPrice)).perform(typeText("4500"), closeSoftKeyboard());
            onView(withId(R.id.editRoommatesNeeded)).perform(typeText("2"), closeSoftKeyboard());
            onView(withId(R.id.editDescription)).perform(typeText("דירה שווה ממש!"), closeSoftKeyboard());

            onView(withId(R.id.btn_save)).perform(click());

            // בדיקה שה-toast מוצג
            onView(withText("הדירה פורסמה"))
                    .inRoot(withDecorView(not(is(getActivity().getWindow().getDecorView()))))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void updateApartment_displaysSuccessToast() {
        // מימוש מזויף של update
        Task<Void> fakeUpdateTask = Tasks.forResult((Void) null);
        Mockito.when(mockRepo.updateApartment(
                        Mockito.anyString(), // apartmentId
                        Mockito.anyString(), // ownerId
                        Mockito.anyString(), // city
                        Mockito.anyString(), // street ✅ חשוב!
                        Mockito.anyInt(),    // houseNumber
                        Mockito.anyInt(),    // price
                        Mockito.anyInt(),    // roommates
                        Mockito.anyString(), // description
                        Mockito.nullable(Uri.class)))
                .thenReturn(fakeUpdateTask);




        MainActivity.isTestMode = true;
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra("fragment", "owner_apartments");

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                OwnerApartmentsFragment fragment = new OwnerApartmentsFragment();
                fragment.setTestingConditions(mockRepo);

                fragment.addDummyApartmentForTesting(
                        "apt123", "תל אביב", "דיזנגוף", "12", "4500", "2", "דירה מהממת");

                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commitNow();
            });

            // לוחץ על כפתור עריכה של הדירה
            onView(withId(R.id.buttonEditApartment)).perform(click());

            // משנה עיר
            onView(withId(R.id.editCity)).perform(clearText(), typeText("רמת גן"), closeSoftKeyboard());

            // לוחץ שמירה
            onView(withId(R.id.btn_save)).perform(click());

            // בודק שה-toast מוצג
            onView(withText("דירה עודכנה בהצלחה"))
                    .inRoot(withDecorView(not(is(getActivity().getWindow().getDecorView()))))
                    .check(matches(isDisplayed()));
        }
    }

    // עוזר להביא את activity
    private MainActivity getActivity() {
        final MainActivity[] holder = new MainActivity[1];
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> holder[0] = activity);
        return holder[0];
    }
}
