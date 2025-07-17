//// Package declaration for organizing the test class within the project structure
//package com.example.roomatch.ui;
//
//// Import Android Intent for launching activities
//import android.content.Intent;
//// Import Android utilities for logging
//import android.util.Log;
//import android.view.View;
//// Import Espresso and related classes for UI testing
//import androidx.test.core.app.ActivityScenario;
//import androidx.test.core.app.ApplicationProvider;
//import androidx.test.espresso.Espresso;
//import androidx.test.espresso.PerformException;
//import androidx.test.espresso.Root;
//import androidx.test.espresso.UiController;
//import androidx.test.espresso.ViewAction;
//import androidx.test.espresso.contrib.RecyclerViewActions;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//import androidx.test.filters.LargeTest;
//// Import application resources and model/repository classes
//import com.example.roomatch.R;
//import com.example.roomatch.model.Apartment;
//import com.example.roomatch.view.activities.MainActivity;
//import com.example.roomatch.view.fragments.OwnerApartmentsFragment;
//import com.example.roomatch.viewmodel.OwnerApartmentsViewModel;
//// Import Hamcrest matchers for assertions
//import org.hamcrest.Description;
//import org.hamcrest.Matcher;
//import org.hamcrest.TypeSafeMatcher;
//// Import JUnit annotations for test setup and execution
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//// Import Java utilities for maps and UUID generation
//import java.util.UUID;
//
//// Import static Espresso and Hamcrest methods for concise usage
//import static androidx.test.espresso.Espresso.onView;
//import static androidx.test.espresso.action.ViewActions.click;
//import static androidx.test.espresso.assertion.ViewAssertions.matches;
//import static androidx.test.espresso.matcher.ViewMatchers.*;
//import static org.hamcrest.CoreMatchers.containsString;
//import static org.hamcrest.CoreMatchers.not;
//
///**
// * Test class for managing apartment operations in the UI using Espresso.
// * This class focuses on UI interactions with the OwnerApartmentsFragment within MainActivity,
// * verifying functionalities like filtering and deleting apartments.
// */
//@LargeTest
//@RunWith(AndroidJUnit4.class)
//public class ApartmentManagementUITest {
//
//    private String dummyId; // Unique identifier for the first dummy apartment.
//    private String new_dummyId; // Unique identifier for the second dummy apartment.
//    private String mockUserId = "testUserId"; // Mock user ID for testing
//
//    /**
//     * Sets up the test environment before each test by generating unique IDs.
//     */
//    @Before
//    public void setUp() {
//        dummyId = UUID.randomUUID().toString(); // Generate a unique ID for the first dummy apartment.
//        new_dummyId = UUID.randomUUID().toString(); // Generate a unique ID for the second dummy apartment.
//    }
//
//    /**
//     * Tests the apply filter functionality to order the apartment list ascending by city.
//     * Verifies that the RecyclerView displays apartments in the correct ascending order (e.g., Ashdod, Rishon).
//     */
//    @Test
//    public void applyFilter_ordersListAscendingByCity() {
//        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class); // Create intent to launch MainActivity.
//        intent.putExtra("fragment", "owner_apartments"); // Set the fragment to load OwnerApartmentsFragment.
//
//        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) { // Launch the activity and manage its lifecycle.
//            scenario.onActivity(activity -> { // Perform actions on the activity.
//                OwnerApartmentsFragment fragment = (OwnerApartmentsFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer); // Find the fragment by ID.
//                OwnerApartmentsViewModel viewModel = fragment.getViewModel();
//                viewModel.loadApartments(mockUserId); // Load apartments for mock user
//
//                // Add test apartments
//                Apartment apt1 = new Apartment(UUID.randomUUID().toString(), mockUserId, "Rishon", "Main", 1, 2000, 1, "Nice", null);
//                Apartment apt2 = new Apartment(UUID.randomUUID().toString(), mockUserId, "Ashdod", "Sea", 2, 2500, 2, "Great", null);
//                viewModel.addTestApartment(apt1);
//                viewModel.addTestApartment(apt2);
//            });
//
//            // Wait for UI update
//            Espresso.onIdle();
//
//            // Select the "city" field for filtering
//            onView(withId(R.id.spinnerOwnerFilterField)).perform(click()); // Click the filter field spinner.
//            onView(withText("עיר")).perform(click()); // Select "city" option.
//
//            // Set ascending order
//            onView(withId(R.id.spinnerOwnerOrder)).perform(click()); // Click the order spinner.
//            onView(withText("עולה")).perform(click()); // Select "ascending" option.
//
//            // Apply the filter
//            onView(withId(R.id.buttonOwnerFilter)).perform(click()); // Click the filter button.
//
//            // Verify the first item is "Ashdod" (ascending order)
//            onView(withId(R.id.recyclerViewOwnerApartments)) // Target the RecyclerView.
//                    .perform(RecyclerViewActions.scrollToPosition(0)) // Scroll to the first position.
//                    .check(matches(hasDescendant(withText(containsString("Ashdod"))))); // Check that "Ashdod" is displayed.
//
//            // Verify the second item is "Rishon" (ascending order)
//            onView(withId(R.id.recyclerViewOwnerApartments)) // Target the RecyclerView.
//                    .perform(RecyclerViewActions.scrollToPosition(1)) // Scroll to the second position.
//                    .check(matches(hasDescendant(withText(containsString("Rishon"))))); // Check that "Rishon" is displayed.
//        }
//    }
//
//    /**
//     * Tests the delete apartment functionality and verifies the apartment is removed from the list.
//     * Adds two dummy apartments, deletes the first one, and checks that its city ("haifa") is no longer displayed.
//     */
//    @Test
//    public void deleteApartment_removesApartmentFromList() {
//        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class); // Create intent to launch MainActivity.
//        intent.putExtra("fragment", "owner_apartments"); // Set the fragment to load OwnerApartmentsFragment.
//
//        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) { // Launch the activity and manage its lifecycle.
//            scenario.onActivity(activity -> { // Perform actions on the activity.
//                OwnerApartmentsFragment fragment = (OwnerApartmentsFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer); // Find the fragment by ID.
//                OwnerApartmentsViewModel viewModel = fragment.getViewModel();
//                viewModel.loadApartments(mockUserId); // Load apartments for mock user
//
//                // Add test apartments
//                Apartment dummy = new Apartment(dummyId, mockUserId, "Tel Aviv", "Dizzengof", 12, 3000, 2, "pretty Apartment", null);
//                Apartment new_dummy = new Apartment(new_dummyId, mockUserId, "haifa", "Dizzengof", 12, 3000, 2, "pretty Apartment", null);
//                viewModel.addTestApartment(new_dummy); // First apartment
//                viewModel.addTestApartment(dummy); // Second apartment
//            });
//
//            // Wait for UI update
//            Espresso.onIdle();
//
//            // Click the delete button on the first item (index 0, "haifa" apartment).
//            onView(withId(R.id.recyclerViewOwnerApartments)) // Target the RecyclerView.
//                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.buttonDeleteApartment))); // Click the delete button.
//
//            // Confirm the deletion dialog.
//            onView(withText("מחק")).perform(click()); // Click the delete confirmation button.
//
//            // Wait for UI update
//            Espresso.onIdle();
//
//            // Check that the deleted apartment's city ("haifa") is no longer in the list.
//            onView(withId(R.id.recyclerViewOwnerApartments)) // Target the RecyclerView.
//                    .check(matches(not(hasDescendant(withText(containsString("haifa")))))); // Verify "haifa" is not present.
//
//            // Verify the toast message appears with the success message.
//            onView(withText("הדירה נמחקה")) // Target the toast message.
//                    .inRoot(isToast()) // Ensure it's a toast root.
//                    .check(matches(isDisplayed())); // Verify the toast is displayed.
//        } catch (Exception e) {
//            Log.e("TestDebug", "🚨 Exception thrown during test execution", e); // Log any exceptions for debugging.
//            e.printStackTrace(); // Print stack trace for detailed error information.
//        }
//    }
//
//    /**
//     * Custom ViewAction to click a child view with the specified ID within a parent view.
//     * This action is designed to be used with Espresso to simulate a click on a specific child view
//     * identified by its resource ID. It checks if the child view exists before performing the click,
//     * throwing a PerformException if the view is not found.
//     *
//     * @param id The resource ID of the child view to click.
//     * @return A ViewAction that performs the click on the specified child view.
//     */
//    public static ViewAction clickChildViewWithId(final int id) {
//        return new ViewAction() { // Return a new anonymous ViewAction implementation.
//            @Override
//            public Matcher<View> getConstraints() {
//                return isAssignableFrom(View.class); // Constraint to apply on any view to ensure it can be processed.
//            }
//
//            @Override
//            public String getDescription() {
//                return "Click on a child view with specified id."; // Description for the action to be used in test reports.
//            }
//
//            @Override
//            public void perform(UiController uiController, View view) {
//                View childView = view.findViewById(id); // Find the child view within the parent view using the provided ID.
//                if (childView != null) { // Check if the child view was found.
//                    childView.performClick(); // Perform the click action on the child view.
//                } else { // Handle the case where the child view is not found.
//                    throw new PerformException.Builder() // Create a new PerformException builder.
//                            .withCause(new Throwable("Child view with ID " + id + " not found.")) // Set the cause of the exception with a descriptive message.
//                            .build(); // Build and throw the exception.
//                }
//            }
//        };
//    }
//
//    /**
//     * Custom matcher to identify a toast root for Espresso assertions.
//     * This matcher checks if the root view is a toast by verifying its window type.
//     *
//     * @return A Matcher<Root> that identifies a toast root.
//     */
//    public static Matcher<Root> isToast() {
//        return new TypeSafeMatcher<Root>() {
//            @Override
//            public void describeTo(Description description) {
//                description.appendText("is a toast root"); // Description for the matcher.
//            }
//
//            @Override
//            public boolean matchesSafely(Root root) {
//                int type = root.getWindowLayoutParams().get().type;
//                return type == android.view.WindowManager.LayoutParams.TYPE_TOAST; // Check if the window type is a toast.
//            }
//        };
//    }
//}
