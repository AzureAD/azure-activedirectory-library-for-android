package com.microsoft.aad.automation.testapp;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.core.AllOf.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    /*
    public static final String AUTHORITY = "authority";
    public static final String RESOURCE = "resource";
    public static final String CLIENT_ID = "client_id";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String USE_BROKER = "use_broker";
    public static final String PROMPT_BEHAVIOR = "prompt_behavior";
    public static final String EXTRA_QUERY_PARAM = "extra_qp";
    public static final String VALIDATE_AUTHORITY = "validate_authority";
    public static final String USER_IDENTIFIER = "user_identifier";
    public static final String USER_IDENTIFIER_TYPE = "user_identifier_type";
    public static final String CORRELATION_ID = "correlation_id";

    */

    String mBasicAuth = "{" +
            "authority='https://login.microsoftonline.com/common" +
            "client_id='4b0db8c2-9f26-4417-8bde-3f0e3656f8e0'" +
            "resource='00000002-0000-0000-c000-000000000000'" +
            "redirect_uri='urn:ietf:wg:oauth:2.0:oob'" +
            "validate_authority=true" +
            "}";

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Ignore
    @Test
    public void mainActivityTest() {
        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.acquireToken), withText("Acquire Token"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()));
        appCompatButton.perform(click());

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.requestInfo),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));


    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
