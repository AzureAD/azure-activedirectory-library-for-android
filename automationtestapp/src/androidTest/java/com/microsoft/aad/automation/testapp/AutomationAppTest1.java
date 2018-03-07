package com.microsoft.aad.automation.testapp;


import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;


import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;


import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.microsoft.identity.internal.test.keyvault.*;
import com.microsoft.identity.internal.test.keyvault.api.*;
import com.microsoft.identity.internal.test.keyvault.auth.*;
import com.microsoft.identity.internal.test.keyvault.model.*;

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


    @Ignore
    @Test
    public void testKeyVault(){
        Configuration.getDefaultApiClient().setBasePath("https://msidlabs.vault.azure.net");
        Configuration.getDefaultApiClient().setAccessToken("<your access token here>");

        SecretsApi secretsApi = new SecretsApi();

        try {
            SecretBundle secretBundle = secretsApi.getSecret("msidlab4", "", "2016-10-01");
            Log.d("KEYVAULT", secretBundle.getValue());
        }
        catch (ApiException ex){
            Log.e("KEYVAULT", "api exception", ex);
        }

 }

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
