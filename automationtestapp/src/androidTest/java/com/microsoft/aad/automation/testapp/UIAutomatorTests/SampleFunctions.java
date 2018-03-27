package com.microsoft.aad.automation.testapp.UIAutomatorTests;

import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;


import junit.framework.Assert;

import static android.support.test.InstrumentationRegistry.getInstrumentation;


public class SampleFunctions {

    private static UiDevice mDevice;

    private static final String AUTHENTICATOR_APP_NAME = "Authenticator";

    private static final String PORTAL_APP_NAME = "Company Portal";

    private static final int WINDOW_TIMEOUT = 3000;

    private static String mAppName;

    private static String mUPN;
    private static String mSecret;
    private static Boolean mFederated;

    private static void click_by_Text(String text)
    {
        set_device();
        UiObject appButton = mDevice.findObject(new UiSelector()
                .text(text));
        try{
            appButton.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find text to click on: " + text);
        }
    }

    private static void click_by_resourceID(String resourceID)
    {
        set_device();
        UiObject appButton = mDevice.findObject(new UiSelector()
                .resourceId(resourceID));
        try{
            appButton.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find resourceID to click on: " + resourceID);
        }
    }

    private static void click_and_await(String text)
    {
        set_device();
        UiObject appButton = mDevice.findObject(new UiSelector()
                .text(text));
        try{
            appButton.clickAndWaitForNewWindow(WINDOW_TIMEOUT);
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find text to click on: " + text);
        }
    }

    private static void click_and_await_resourceID(String resourceID)
    {
        set_device();
        UiObject appButton = mDevice.findObject(new UiSelector()
                .resourceId(resourceID));
        try{
            appButton.clickAndWaitForNewWindow(WINDOW_TIMEOUT);
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find the resourceID to click on: " + resourceID);
        }
    }

    private static void set_text_by_Text(String UIElementText,String text_to_set)
    {
        set_device();
        UiObject appButton = mDevice.findObject(new UiSelector()
                .text(UIElementText));
        try{
            appButton.setText(text_to_set);
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find element");
        }
    }

    private static void set_device()
    {
        if(mDevice == null){
            mDevice = UiDevice.getInstance(getInstrumentation());
        }
    }

    public static void open_Applications_View()
    {
        set_device();
        // press the home button
        mDevice.pressHome();
        // Bring up the default launcher by searching for a UI component
        // that matches the content description for the launcher button.
        UiObject allAppsButton = mDevice.findObject(new UiSelector().description("Apps"));
        // Perform a click on the button to load the launcher.
        try{
            allAppsButton.clickAndWaitForNewWindow();
        } catch (Exception e){
            Assert.fail("Didnt find apps");
        }
    }

    public static void launch_App(String AppName)
    {
        set_device();
        mAppName = AppName;
        open_Applications_View();
        click_by_Text(mAppName);
    }

    public static void uninstall_App(String AppName)
    {
        set_device();
        mAppName = AppName;

        UiObject settingsButton = mDevice.findObject(new UiSelector().description("Settings"));
        // Perform a click on the button to load the launcher.
        try{
            settingsButton.clickAndWaitForNewWindow();
        } catch (Exception e){
            Assert.fail("Didnt find settings");
        }

        click_by_Text("Apps");

        click_and_await(mAppName);

        click_by_Text("Uninstall");

        click_by_Text("OK");

    }

    public static void clear_app_data(String AppName)
    {
        set_device();
        mDevice.pressHome();
        mAppName = AppName;

        UiObject settingsButton = mDevice.findObject(new UiSelector().description("Settings"));
        // Perform a click on the button to load the launcher.
        try{
            settingsButton.clickAndWaitForNewWindow();
        } catch (Exception e){
            Assert.fail("Didnt find settings");
        }

        click_by_Text("Apps");

        click_and_await(mAppName);

        click_by_Text("Storage");

        click_by_Text("Clear data");

        click_by_Text("OK");

    }

    private static void allow_permission()
    {
        click_by_resourceID("com.android.packageinstaller:id/permission_allow_button");
    }

    public static void scroll_findapp(String textToClick){
        open_Applications_View();
        scroll_up();
        set_device();

        try {
            UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
            appView.scrollIntoView(new UiSelector().text(textToClick));
            mDevice.findObject(new UiSelector().text(textToClick)).clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {

        }
    }

    public static void scroll_up()
    {
        set_device();
        int H = mDevice.getDisplayHeight();
        int W = mDevice.getDisplayWidth();
        int x = W / 2;
        double y1 = H * .25;
        double y2 =  H * .75;

        mDevice.swipe(x,(int)y1,x,(int)y2,5);
    }

    public static void enroll_authenticator(String UPN,String Secret,Boolean Federated)
    {
        mFederated = Federated;
        mUPN = UPN;
        mSecret = Secret;

        launch_App(AUTHENTICATOR_APP_NAME);
        click_by_Text("SKIP");
        click_by_resourceID("com.azure.authenticator:id/menu_overflow");
        click_by_Text("Settings");
        click_by_Text("Register your device with your organization");
        allow_permission();
        set_text_by_Text("Organization email",mUPN);
        click_and_await_resourceID("com.azure.authenticator:id/manage_device_registration_register_button");
        authenticate_webview();
    }

    private static void authenticate_webview()
    {
        //set_text_by_Text("Enter your email, phone, or Skype.",mUPN);
        //click_and_await("Next");
        set_text_by_Text("Enter password",mSecret);
        click_and_await("Sign in");
    }

    public static void remove_authenticator_account()
    {
        launch_App(AUTHENTICATOR_APP_NAME);
        click_by_resourceID("com.azure.authenticator:id/menu_overflow");
        click_by_Text("Edit accounts");
        click_by_resourceID("com.azure.authenticator:id/account_list_row_delete");
        click_by_Text("Remove account");
    }

    public static void unenroll_authenticator()
    {
        launch_App("Authenticator");
    }
}

