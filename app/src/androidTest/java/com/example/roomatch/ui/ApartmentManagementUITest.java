package com.example.roomatch.ui;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is; //
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.Root;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.roomatch.R;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.view.activities.MainActivity;
import com.example.roomatch.view.fragments.OwnerApartmentsFragment;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;

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


/**
 * Test class for managing apartment operations in the UI.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ApartmentManagementUITest {

    private String dummyId; // Unique identifier for the dummy apartment.
    private String new_dummyId; // Unique identifier for the dummy apartment.


    /**
     * Sets up the test environment before each test.
     */
    @Before
    public void setUp() {
        MainActivity.isTestMode = true; // Enable test mode for the main activity.
        dummyId = UUID.randomUUID().toString(); // Generate a unique ID for the dummy apartment.
        new_dummyId = UUID.randomUUID().toString(); // Generate a unique ID for the dummy apartment.


    }

    /**
     * Tests the edit apartment functionality and verifies the updated data is displayed.
     */
//    @Test
//    public void editApartment_displaysUpdatedData() {
//        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
//        intent.putExtra("fragment", "owner_apartments");
//
//        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
//            Log.d("TestDebug", "📱 Launching MainActivity with OwnerApartmentsFragment...");
//
//            final MainActivity[] activityHolder = new MainActivity[1];
//
//            scenario.onActivity(activity -> {
//                Log.d("TestDebug", "✅ Activity launched and accessible.");
//                activityHolder[0] = activity;
//
//                OwnerApartmentsFragment fragment = (OwnerApartmentsFragment)
//                        activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
//                Log.d("TestDebug", "📦 Retrieved OwnerApartmentsFragment: " + (fragment != null));
//
//                ApartmentRepository fakeRepo = new ApartmentRepository(true);
//                fragment.setTestingConditions(fakeRepo);
//                Log.d("TestDebug", "🧪 Injected fake repository and enabled test mode.");
//
//                fragment.getViewModel().clearApartmentsForTest();
//                Log.d("TestDebug", "🧹 Cleared apartment list in ViewModel.");
//
//                Map<String, Object> dummy = new HashMap<>();
//                dummy.put("id", dummyId);
//                dummy.put("city", "Tel Aviv");
//                dummy.put("street", "Dizzengof");
//                dummy.put("houseNumber", 12);
//                dummy.put("price", 3000);
//                dummy.put("roommatesNeeded", 2);
//                dummy.put("description", "pretty Apartment");
//
//                Log.d("TestDebug", "🏠 Adding dummy apartment: " + dummy);
//                fragment.getViewModel().addTestApartment(dummy);
//
//                printApartmentsInViewModel(fragment);
//            });
//
//            Log.d("TestDebug", "🖱 Clicking edit button on first apartment...");
//            onView(withId(R.id.recyclerViewOwnerApartments))
//                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.buttonEditApartment)));
//
//            Log.d("TestDebug", "📝 Editing city to 'Haifa' in dialog...");
//            onView(withId(R.id.editCity)).perform(clearText(), typeText("Haifa"), closeSoftKeyboard());
//            onView(withId(R.id.btn_save)).perform(click());
//
//            Log.d("TestDebug", "✅ Checking success message...");
//            onView(isRoot()).perform(printAllViews());
//            onView(withText("דירה עודכנה בהצלחה"))
//                    .inRoot(isSnackbarRoot())
//                    .check(matches(isDisplayed()));
//            onView(withText("דירה עודכנה בהצלחה"))
//                    .check(matches(isDisplayed()));
//
//            Log.d("TestDebug", "🎉 Success message was shown successfully!");
//
//            Log.d("TestDebug", "🔍 Checking apartment list still contains at least 1 child...");
//            onView(withId(R.id.recyclerViewOwnerApartments))
//                    .check(matches(hasMinimumChildCount(1)));
//            Log.d("TestDebug", "📋 Apartment list contains at least one item.");
//
//            Log.d("TestDebug", "❌ Checking that 'Haifa' does not appear (intentionally failing this to debug)...");
//            onView(allOf(withId(R.id.textViewApartmentCity), withText(containsString("Haifa"))))
//                    .check(doesNotExist());
//        } catch (Exception e) {
//            Log.e("TestDebug", "🚨 Exception thrown during test execution", e);
//            e.printStackTrace();
//        }
//    }

    // Custom Snackbar root matcher
    public static Matcher<Root> isSnackbarRoot() {
        return new TypeSafeMatcher<Root>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is a Snackbar root with text 'דירה עודכנה בהצלחה'");
            }

            @Override
            public boolean matchesSafely(Root root) {
                int type = root.getWindowLayoutParams().get().type;
                if (type == WindowManager.LayoutParams.TYPE_APPLICATION ||
                        type == WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG) {
                    View decorView = root.getDecorView();
                    if (decorView != null) {
                        // Look for a TextView with the target text
                        return findTextViewWithText(decorView, "דירה עודכנה בהצלחה");
                    }
                }
                return false;
            }

            private boolean findTextViewWithText(View view, String text) {
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    if (textView.getText().toString().equals(text)) {
                        return true;
                    }
                }
                if (view instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) view;
                    for (int i = 0; i < group.getChildCount(); i++) {
                        if (findTextViewWithText(group.getChildAt(i), text)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }
//    @Test
//    public void searchApartments_filtersListByQuery() {
//        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
//        intent.putExtra("fragment", "owner_apartments");
//
//        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
//            scenario.onActivity(activity -> {
//                OwnerApartmentsFragment fragment = (OwnerApartmentsFragment) activity.getSupportFragmentManager()
//                        .findFragmentById(R.id.fragmentContainer);
//                ApartmentRepository fakeRepo = new ApartmentRepository(true);
//                fragment.setTestingConditions(fakeRepo);
//
//                Map<String, Object> apt1 = new HashMap<>();
//                apt1.put("id", UUID.randomUUID().toString());
//                apt1.put("city", "Tel Aviv");
//                apt1.put("description", "Big apartment");
//
//                Map<String, Object> apt2 = new HashMap<>();
//                apt2.put("id", UUID.randomUUID().toString());
//                apt2.put("city", "Haifa");
//                apt2.put("description", "Near beach");
//
//                fragment.getViewModel().addTestApartment(apt1);
//                fragment.getViewModel().addTestApartment(apt2);
//            });
//
//            // Verify SearchView is displayed
//            onView(withId(R.id.searchViewOwner))
//                    .check(matches(isDisplayed()));
//
//            // Type into the SearchView's EditText
//            onView(withId(androidx.appcompat.R.id.search_src_text))
//                    .perform(typeText("Tel"), closeSoftKeyboard());
//
//            // Verify Tel Aviv is displayed
//            onView(withText(containsString("Tel Aviv"))).check(matches(isDisplayed()));
//
//            // Verify Haifa is not displayed
//            onView(withText(containsString("Haifa"))).check(doesNotExist());
//        }
//    }




    /**
     * Tests the delete apartment functionality and verifies the apartment is removed from the list.
     */
    @Test
    public void deleteApartment_removesApartmentFromList() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class); // Create intent to launch MainActivity.
        intent.putExtra("fragment", "owner_apartments"); // Set the fragment to load.

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) { // Launch the activity and manage its lifecycle.

            scenario.onActivity(activity -> { // Perform actions on the activity.
                OwnerApartmentsFragment fragment = (OwnerApartmentsFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer); // Find the fragment by ID.
                ApartmentRepository fakeRepo = new ApartmentRepository(true); // Create a mock repository for testing.
                fragment.setTestingConditions(fakeRepo); // Set testing conditions for the fragment.

                // Create a dummy apartment map.
                Map<String, Object> dummy = new HashMap<>();
                dummy.put("id", dummyId); // Add unique ID to the dummy apartment.
                dummy.put("city", "Tel Aviv"); // Add city to the dummy apartment.
                dummy.put("street", "Dizzengof"); // Add street to the dummy apartment.
                dummy.put("houseNumber", 12); // Add house number to the dummy apartment.
                dummy.put("price", 3000); // Add price to the dummy apartment.
                dummy.put("roommatesNeeded", 2); // Add number of roommates needed to the dummy apartment.
                dummy.put("description", "pretty Apartment"); // Add description to the dummy apartment.

                Map<String, Object> new_dummy = new HashMap<>();
                new_dummy.put("id", new_dummyId); // Add unique ID to the dummy apartment.
                new_dummy.put("city", "haifa"); // Add city to the dummy apartment.
                new_dummy.put("street", "Dizzengof"); // Add street to the dummy apartment.
                new_dummy.put("houseNumber", 12); // Add house number to the dummy apartment.
                new_dummy.put("price", 3000); // Add price to the dummy apartment.
                new_dummy.put("roommatesNeeded", 2); // Add number of roommates needed to the dummy apartment.
                new_dummy.put("description", "pretty Apartment"); // Add description to the dummy apartment.

                // Add the dummy apartment to the ViewModel directly.
                fragment.getViewModel().addTestApartment(new_dummy);

                // Add the dummy apartment to the ViewModel directly.
                fragment.getViewModel().addTestApartment(dummy);
                printApartmentsInViewModel(fragment);
            });

            // Click the delete button on the first item.
            onView(withId(R.id.recyclerViewOwnerApartments)).perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.buttonDeleteApartment)));

            // Confirm the deletion dialog.
            onView(withText("מחק")).perform(click()); // Click the delete confirmation button.
            onView(withId(R.id.textViewTestMessage))
                    .check(matches(withText("הדירה נמחקה")));
            // Check that the apartment is removed (list is empty or does not contain the text).
            onView(withId(R.id.recyclerViewOwnerApartments)).check(matches(not(hasDescendant(withText(containsString("haifa"))))));
        } catch (Exception e) {
            e.printStackTrace(); // Handle any exceptions that occur during the test.
        }
    }

    @Test
    public void applyFilter_ordersListAscendingByCity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra("fragment", "owner_apartments");

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {

            scenario.onActivity(activity -> {
                OwnerApartmentsFragment fragment = (OwnerApartmentsFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                ApartmentRepository fakeRepo = new ApartmentRepository(true);
                fragment.setTestingConditions(fakeRepo);

                Map<String, Object> apt1 = new HashMap<>();
                apt1.put("id", UUID.randomUUID().toString());
                apt1.put("city", "Rishon");

                Map<String, Object> apt2 = new HashMap<>();
                apt2.put("id", UUID.randomUUID().toString());
                apt2.put("city", "Ashdod");

                fragment.getViewModel().addTestApartment(apt1);
                fragment.getViewModel().addTestApartment(apt2);
            });

            // בחר שדה סינון "עיר"
            onView(withId(R.id.spinnerOwnerFilterField)).perform(click());
            onView(withText("עיר")).perform(click());

            // סדר עולה
            onView(withId(R.id.spinnerOwnerOrder)).perform(click());
            onView(withText("עולה")).perform(click());

            // לחץ על כפתור "סנן"
            onView(withId(R.id.buttonOwnerFilter)).perform(click());

            // בדוק שהפריט הראשון ברשימה הוא Ashdod (סינון עולה לפי עיר)
            onView(withId(R.id.recyclerViewOwnerApartments))
                    .perform(RecyclerViewActions.scrollToPosition(0))
                    .check(matches(hasDescendant(withText(containsString("Ashdod")))));

            // הפריט השני הוא Rishon
            onView(withId(R.id.recyclerViewOwnerApartments))
                    .perform(RecyclerViewActions.scrollToPosition(1))
                    .check(matches(hasDescendant(withText(containsString("Rishon")))));
        }
    }


    /**
     * Custom ViewAction to click a child view with the specified ID within a parent view.
     * This action is designed to be used with Espresso to simulate a click on a specific child view
     * identified by its resource ID. It checks if the child view exists before performing the click,
     * throwing a PerformException if the view is not found.
     *
     * @param id The resource ID of the child view to click.
     * @return A ViewAction that performs the click on the specified child view.
     */
    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() { // Return a new anonymous ViewAction implementation.
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class); // Constraint to apply on any view to ensure it can be processed.
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id."; // Description for the action to be used in test reports.
            }

            @Override
            public void perform(UiController uiController, View view) {
                View childView = view.findViewById(id); // Find the child view within the parent view using the provided ID.
                if (childView != null) { // Check if the child view was found.
                    childView.performClick(); // Perform the click action on the child view.
                } else { // Handle the case where the child view is not found.
                    throw new PerformException.Builder() // Create a new PerformException builder.
                            .withCause(new Throwable("Child view with ID " + id + " not found.")) // Set the cause of the exception with a descriptive message.
                            .build(); // Build and throw the exception.
                }
            }
        };
    }
    private void printApartmentsInViewModel(OwnerApartmentsFragment fragment) {
        List<Map<String, Object>> list = fragment.getViewModel().getFilteredApartments().getValue();
        System.out.println("📋 Apartments: ");
        if (list != null) {
            for (Map<String, Object> apt : list) {
                System.out.println(" - " + apt);
            }
        } else {
            System.out.println(" - [Empty]");
        }
    }

    public static ViewAction printTextViews() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isRoot(); }
            @Override public String getDescription() { return "Print all TextView texts"; }
            @Override public void perform(UiController uiController, View view) {
                if (view instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) view;
                    for (int i = 0; i < group.getChildCount(); i++) {
                        View child = group.getChildAt(i);
                        if (child instanceof TextView) {
                            Log.d("TestDebug", "TextView text: " + ((TextView) child).getText());
                        }
                    }
                }
            }
        };
    }
    public static ViewAction printAllViews() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isRoot(); }
            @Override public String getDescription() { return "Print all views recursively"; }
            @Override public void perform(UiController uiController, View view) {
                printViewHierarchy(view, "");
            }
            private void printViewHierarchy(View view, String prefix) {
                Log.d("TestDebug", prefix + "View: " + view + ", Text: " + (view instanceof TextView ? ((TextView) view).getText() : "N/A"));
                if (view instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) view;
                    for (int i = 0; i < group.getChildCount(); i++) {
                        printViewHierarchy(group.getChildAt(i), prefix + "  ");
                    }
                }
            }
        };
    }

}