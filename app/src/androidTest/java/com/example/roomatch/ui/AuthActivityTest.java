package com.example.roomatch.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static androidx.test.espresso.assertion.ViewAssertions.matches; // ✅ הנכון

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.roomatch.R;
import com.example.roomatch.view.activities.AuthActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthActivityTest {

    @Rule
    public ActivityScenarioRule<AuthActivity> rule =
            new ActivityScenarioRule<>(AuthActivity.class);

    @Before
    public void setUp() {
        // הפעל מצב טסט
        rule.getScenario().onActivity(activity -> AuthActivity.isTestMode = true);
    }

    @Test
    public void login_success_showsTestStatusText() {
        onView(withId(R.id.editEmail)).perform(typeText("iran1@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.editPassword)).perform(typeText("ariel2001"), closeSoftKeyboard());
        onView(withId(R.id.buttonAction)).perform(click());

        onView(withId(R.id.testStatusTextView))
                .check(matches(isDisplayed()))
                .check(matches(withText("התחברת בהצלחה")));
    }


}
