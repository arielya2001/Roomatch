//package com.example.roomatch.ui;
//
//import static androidx.test.espresso.Espresso.onView;
//import static androidx.test.espresso.action.ViewActions.*;
//import static androidx.test.espresso.assertion.ViewAssertions.matches;
//import static androidx.test.espresso.matcher.ViewMatchers.*;
//import static androidx.test.espresso.contrib.RecyclerViewActions.scrollTo;
//import static org.hamcrest.Matchers.allOf;
//
//import androidx.fragment.app.testing.FragmentScenario;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//import androidx.test.filters.MediumTest;
//
//import com.example.roomatch.R;
//import com.example.roomatch.view.fragments.OwnerApartmentsFragment;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.util.*;
//
//@RunWith(AndroidJUnit4.class)
//@MediumTest
//public class OwnerApartmentsUiTest {
//
//    @Test
//    public void searchFiltersListToSingleItem() {
//
//        try (FragmentScenario<OwnerApartmentsFragment> fs =
//                     FragmentScenario.launchInContainer(
//                             OwnerApartmentsFragment.class,
//                             null,
//                             R.style.Theme_Roomatch)) {
//
//            // Arrange
//            fs.onFragment(f -> {
//                f.getAllApartments().add(Map.of("city", "Tel‑Aviv", "street", "Dizengoff"));
//                f.getAllApartments().add(Map.of("city", "Tel‑Aviv", "street", "Ibn Gvirol"));
//                f.getAllApartments().add(Map.of("city", "Haifa", "street", "Herzl"));
//                f.callResetFilter();
//
//                // פתיחת SearchView
//                androidx.appcompat.widget.SearchView searchView =
//                        f.requireView().findViewById(R.id.searchViewOwner);
//                searchView.setIconified(false);
//            });
//
//            // Act – חיפוש לפי טקסט
//            onView(allOf(isAssignableFrom(android.widget.EditText.class),
//                    isDescendantOfA(withId(R.id.searchViewOwner))))
//                    .perform(typeText("Haifa"), pressImeActionButton());
//
//            // Assert
//            onView(withId(R.id.recyclerViewOwnerApartments))
//                    .check(matches(hasChildCount(1)));
//
//            onView(withId(R.id.recyclerViewOwnerApartments))
//                    .perform(scrollTo(hasDescendant(withText("Haifa"))));
//        }
//    }
//}
