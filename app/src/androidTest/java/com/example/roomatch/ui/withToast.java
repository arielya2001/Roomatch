package com.example.roomatch.ui;

import android.view.WindowManager;
import androidx.test.espresso.Root;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class withToast {
    public static TypeSafeMatcher<Root> withToast() {
        return new ToastMatcher();
    }

    private static class ToastMatcher extends TypeSafeMatcher<Root> {

        @Override
        public void describeTo(Description description) {
            description.appendText("is toast");
        }

        @Override
        public boolean matchesSafely(Root root) {
            int type = root.getWindowLayoutParams().get().type;
            return (type == WindowManager.LayoutParams.TYPE_TOAST
                    || type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }
    }
}
