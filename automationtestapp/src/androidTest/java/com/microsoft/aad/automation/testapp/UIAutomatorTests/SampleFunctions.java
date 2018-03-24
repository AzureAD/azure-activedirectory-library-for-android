package com.microsoft.aad.automation.testapp.UIAutomatorTests;

import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;


import junit.framework.Assert;

import static android.support.test.InstrumentationRegistry.getInstrumentation;


public class SampleFunctions {

    private static UiDevice mDevice;

    private static final int WINDOW_TIMEOUT = 3000;

    private static String mAppName;

    private static void click_by_Text(String text)
    {
        UiObject appButton = mDevice.findObject(new UiSelector()
                .text(text));
        try{
            appButton.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find text to click on: " + text);
        }
    }

    private static void click_and_await(String text)
    {
        UiObject appButton = mDevice.findObject(new UiSelector()
                .text(text));
        try{
            appButton.clickAndWaitForNewWindow(WINDOW_TIMEOUT);
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find text to click on: " + text);
        }
    }

    private static void set_text_by_Text(String UIElementText,String text_to_set)
    {
        UiObject appButton = mDevice.findObject(new UiSelector()
                .text(UIElementText));
        try{
            appButton.setText(text_to_set);
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find element");
        }
    }

    private static void open_Applications_View()
    {
        // press the home button
        mDevice.pressHome();
        // Bring up the default launcher by searching for a UI component
        // that matches the content description for the launcher button.
        UiObject allAppsButton = mDevice.findObject(new UiSelector().description("Apps"));
        // Perform a click on the button to load the launcher.
        try{
            allAppsButton.clickAndWaitForNewWindow();
        } catch (Exception e){
            Assert.fail("Didnt find app");
        }
    }

    public static void launch_App(String AppName)
    {
        // need to finish this.

    }

    public static void unenroll_authenticator(String AppName)
    {
        mAppName = AppName;
        mDevice = UiDevice.getInstance(getInstrumentation());

        UiObject appButton = mDevice.findObject(new UiSelector()
                .text(AppName));
        try{
            appButton.longClick();
        } catch (UiObjectNotFoundException e) {
            Assert.fail("");
        }
    }


    public static void uninstall_App(String AppName)
    {
        mAppName = AppName;
        mDevice = UiDevice.getInstance(getInstrumentation());

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
}
