package com.microsoft.aad.automation.testapp;


import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiSelector;
import android.widget.Button;
import android.widget.EditText;


import com.microsoft.aad.automation.testapp.utility.Scenario;
import com.microsoft.aad.automation.testapp.utility.TestConfigurationQuery;

import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@Ignore
@LargeTest
//@RunWith(AndroidJUnit4.class)
@RunWith(Parameterized.class)
public class MainActivityTest2 {


    @Parameterized.Parameter
    public Scenario mScenario = null;

    @Parameterized.Parameters
    public static Iterable<Scenario> TokenRequestData(){

        TestConfigurationQuery query = new TestConfigurationQuery();
        query.federationProvider = "ADFSv3";
        Scenario scenarioADFSv3 = Scenario.GetScenario(query);
        query.federationProvider = "ADFSv4";
        Scenario scenarioADFSv4 = Scenario.GetScenario(query);
        query.federationProvider = "ADFSv2";
        Scenario scenarioADFSv2 = Scenario.GetScenario(query);


        List<Scenario> requests = new ArrayList<>();
        requests.add(scenarioADFSv2);
        requests.add(scenarioADFSv3);
        requests.add(scenarioADFSv4);

        return requests;
    }



    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);


    @Before
    public void before() {
        //IdlingRegistry.getInstance().register(SignInActivity.IDLING);
    }

    @After
    public void after() {
        //IdlingRegistry.getInstance().unregister(SignInActivity.IDLING);
    }

    @Ignore
    @Test
    public void mainActivityTest2() throws InterruptedException, UiObjectNotFoundException {
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
        appCompatEditText.perform(ViewActions.typeText(mScenario.getTokenRequestAsJson()));

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.requestGo), withText("Go"),
                        isDisplayed()));

        appCompatButton2.perform(ViewActions.closeSoftKeyboard());
        appCompatButton2.perform(ViewActions.click());

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Thread.sleep(4000);
        UiObject userNameBox = device.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class)
        );

        userNameBox.setText(mScenario.getCredential().userName);
        UiObject2 nextButton = device.findObject(By.text("Next"));
        nextButton.click();

        Thread.sleep(8000);
        UiObject passwordBox = device.findObject(new UiSelector()
        .instance(1)
        .className(EditText.class));
        passwordBox.setText(mScenario.getCredential().password);

        UiObject signInButton = device.findObject(new UiSelector()
                .instance(0)
                .className(Button.class)
        );
        signInButton.click();

        /*
        Thread.sleep(20000);


        ViewInteraction resultsView = onView(
                allOf(withId(R.id.resultInfo), isDisplayed())
        );
        */




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
