package com.microsoft.aad.automation.testapp;


import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;


import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/*
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertTrue;
*/

@RunWith(AndroidJUnit4.class)
public class AutomationAppTest1 {
    UiDevice mDevice;


    @Test
    public void testContactsSync() throws Exception {

        // Espresso code: login the user, navigate to contact sync and enable clicking on toggle
        //logInUser();
        //onView(withText(R.string.sync_button)).perform(click());
        //onView(withId(R.id.contacts_sync_enable_toggle)).perform(click());

        // UIAutomator code: navigate to People app
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Thread.sleep(4000);
        mDevice.pressHome();
        //UiObject conutactsApp = mDevice.findObject(new UiSelector().text(PEOPLE_APP));
        //conutactsApp.clickAndWaitForNewWindow();

        // UIAutomator code: check that contact is present in it after sync was triggered
        //UiObject contactName = mDevice.findObject(new UiSelector().text(userContactName));
        //assertTrue(contactName.exists());

        // UIAutomator code: navigate back to my app under testm
        //Device.pressHome();
        //UiObject myApp = mDevice.findObject(new UiSelector().text(MY_APP));
        //myApp.clickAndWaitForNewWindow();

        // Espresso code: turn off contact sync and logout
        //onView(withId(R.id.contacts_sync_enable_toggle)).perform(click());
        //onView(withId(R.id.logout)).perform(click());
    }

 }
