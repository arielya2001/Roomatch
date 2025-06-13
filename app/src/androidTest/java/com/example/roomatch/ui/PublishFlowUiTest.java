package com.example.roomatch.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.roomatch.view.activities.MainActivity;
import com.example.roomatch.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PublishFlowUiTest {

    @Test
    public void navigateToOwnerFragment_andShowEmptyToast() {

        try (ActivityScenario<MainActivity> sc = ActivityScenario.launch(MainActivity.class)) {

            // כפתור "+" בטולבר → OwnerFragment
            onView(withId(R.id.buttonChats)).perform(click());
            onView(withId(R.id.buttonPublish)).check(matches(isDisplayed()));

            // לוחצים Publish בלי למלא שדות
            onView(withId(R.id.buttonPublish)).perform(click());

            // מצפים ל‑Toast "נא למלא את כל השדות"
            onView(withText("נא למלא את כל השדות"))
                    .inRoot(new ToastMatcher())
                    .check(matches(isDisplayed()));
        }
    }
}
