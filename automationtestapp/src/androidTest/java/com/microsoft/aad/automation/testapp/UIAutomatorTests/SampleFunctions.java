package com.microsoft.aad.automation.testapp.UIAutomatorTests;

import android.os.Build;
import android.support.annotation.Nullable;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.aad.adal.PromptBehavior;

import junit.framework.Assert;

import static android.support.test.InstrumentationRegistry.getInstrumentation;


public class SampleFunctions {

    private static UiDevice mDevice = UiDevice.getInstance(getInstrumentation());

    private static final String AUTHENTICATOR_APP_NAME = "Authenticator";

    private static final String PORTAL_APP_NAME = "Company Portal";

    private static final int WINDOW_TIMEOUT = 3000;

    private static final int DOWNLOAD_TIMEOUT = 60000;

    private static String mAppName;

    private static String mUPN;

    private static String mSecret;

    private static final char ANDROID_VERSION = get_android_version();
    private static final String PHONE_MODEL = android.os.Build.MODEL;

    public static char get_android_version()
    {
        return android.os.Build.VERSION.RELEASE.charAt(0);
    }


    public static void scroll_up()
    {
        int H = mDevice.getDisplayHeight();
        int W = mDevice.getDisplayWidth();
        int x = W / 2;
        double y1 = H * .25;
        double y2 =  H * .75;

        mDevice.swipe(x,(int)y1,x,(int)y2,5);
    }

    public static void scroll_down()
    {
        int H = mDevice.getDisplayHeight();
        int W = mDevice.getDisplayWidth();
        int x = W / 2;
        double y1 = H * .25;
        //double y1 = 100;
        double y2 =  H * .95;

        mDevice.swipe(x,(int)y2,x,(int)y1,5);
    }

    public static void click(@Nullable String text, @Nullable String resourceId, boolean await)
    {
        UiObject appButton = null;
        if (text != null) {
            appButton = mDevice.findObject(new UiSelector()
                    .text(text));
        }

        if (resourceId != null) {
            appButton = mDevice.findObject(new UiSelector()
                    .resourceId(resourceId));
        }

        if(text == null && resourceId == null) {
            Assert.fail("Have to either provide text or resource id to click on");
            return;
        }

        if (await)
        {
            try{
                appButton.clickAndWaitForNewWindow(WINDOW_TIMEOUT);
            } catch (UiObjectNotFoundException e) {
                Assert.fail("Didn't find text to click on: " + text);
            }

        }
        else{
            try{
                appButton.click();
            } catch (UiObjectNotFoundException e) {
                Assert.fail("Didn't find text to click on: " + text);
            }
        }

        return;
    }

    public static void find_click(String text){
        try {
            scroll_up();
            UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
            appView.scrollIntoView(new UiSelector().text(text));
            mDevice.findObject(new UiSelector().text(text)).clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Couldn't find app.");
        }
    }

    public static void set_text_by_Text(String UIElementText,String text_to_set)
    {
        UiObject appButton = mDevice.findObject(new UiSelector()
                .text(UIElementText));
        try{
            appButton.setText(text_to_set);
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find element");
        }
    }

    public static void set_text_by_Resource(String UIElementText,String text_to_set)
    {
        UiObject appButton = mDevice.findObject(new UiSelector()
                .resourceId(UIElementText));
        try{
            appButton.setText(text_to_set);
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Didn't find element");
        }
    }

    public static void open_Applications_View()
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
            Assert.fail("Didnt find apps");
        }
        scroll_up();
    }

    public static void uninstall_App(String AppName)
    {
        mAppName = AppName;

        UiObject settingsButton = mDevice.findObject(new UiSelector().description("Settings"));
        // Perform a click on the button to load the launcher.
        try{
            settingsButton.clickAndWaitForNewWindow();
        } catch (Exception e){
            Assert.fail("Didnt find settings");
        }

        click("Apps", null, false);

        click(mAppName, null, true);

        click("Uninstall", null, false);

        click("OK", null, false);

    }

    private static void sleep(int milliseconds)
    {
        try{
            Thread.sleep(milliseconds);
        } catch (InterruptedException e)
        {
            Assert.fail("Sleep thread interrupted");
        }
    }

    public static void clear_app_data(String AppName)
    {
        mDevice.pressHome();
        mAppName = AppName;

        UiObject settingsButton = mDevice.findObject(new UiSelector().description("Settings"));
        // Perform a click on the button to load the launcher.
        try{
            settingsButton.clickAndWaitForNewWindow();
        } catch (Exception e){
            Assert.fail("Didnt find settings");
        }

        find_click("Apps");

        click(mAppName, null, true);

        click("Storage", null, true);

        click("Clear data", null, true);

        click("OK", null, true);

        sleep(1000);
    }

    public static void allow_permission()
    {
        click(null,"com.android.packageinstaller:id/permission_allow_button",false);
    }

    public static void launch_App(String appName){
        mAppName = appName;


        switch (ANDROID_VERSION)
        {
            case '6' :
                open_Applications_View();
                scroll_up();
                break;

            case '7' :
                mDevice.pressHome();
                scroll_down();
                break;
        }

        try {
            UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
            appView.scrollIntoView(new UiSelector().text(appName));
            mDevice.findObject(new UiSelector().text(appName)).clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            Assert.fail("Couldn't find app.");
        }
    }


    public static void enroll_authenticator(String UPN,String Secret)
    {
        mUPN = UPN;
        mSecret = Secret;

        clear_app_data("Authenticator");
        launch_App(AUTHENTICATOR_APP_NAME);
        click("SKIP",null,false);
        click(null, "com.azure.authenticator:id/menu_overflow", false);
        click("Settings",null,false);
        click("Register your device with your organization", null, false);
        allow_permission();
        set_text_by_Text("Organization email",mUPN);
        click(null,
                "com.azure.authenticator:id/manage_device_registration_register_button" ,
                true);
        authenticate_webview();
    }

    public static void enroll_company_portal(String UPN,String Secret)
    {
        mUPN = UPN;
        mSecret = Secret;

        launch_App(PORTAL_APP_NAME);
        click("Sign in",null,true);
        set_text_by_Resource("i0116", mUPN);
        click("Next", null,true);
        authenticate_webview();
        sleep(2000);
        click("OK",null,true);
        click("CONTINUE",null,true);
        click("CONTINUE",null,true);
        click("NEXT",null,true);
        click("ALLOW",null,true);
        click("ALLOW",null,true);
        click("Activate",null,true);
        sleep(30000);
        click("DONE",null,true);
    }

    private static void authenticate_webview()
    {
        set_text_by_Text("Enter password", mSecret);
        click("Sign in",null,true);
    }

    public static void authenticate_webview2(String upn, String secret)
    {
        set_text_by_Text("Enter your email, phone, or Skype.",upn);
        click("Next",null,true);
        set_text_by_Text("Enter password",secret);
        click("Sign in",null,true);
    }

    public static void back_spam(int times_to_press_back_button){
        while(times_to_press_back_button > 0)
        {
            mDevice.pressBack();
            times_to_press_back_button --;
        }
    }



    public static void remove_authenticator_account()
    {
        clear_app_data("Authenticator");
        launch_App(AUTHENTICATOR_APP_NAME);
        click(null,"com.azure.authenticator:id/menu_overflow",false);
        click("Edit accounts",null,true);
        click(null,"com.azure.authenticator:id/account_list_row_delete",true);
        click("Remove account",null,true);
    }

    public static String json_body(
            String Authority,
            String Resource,
            String redirURI,
            String clientID
    ) {
        JsonObject o = new JsonObject();
        o.addProperty("authority", Authority);
        o.addProperty("resource",Resource);
        o.addProperty("redirect_uri",redirURI);
        o.addProperty("client_id",clientID);
        //o.addProperty("prompt_behavior", PromptBehavior.Auto.toString());
        return o.toString();
    }

    public static void adal_clear_cache()
    {
        click("Clear Cache", null, true);
        click("Done", null, true);
    }

    public static void download_install_app(String appName)
    {
        launch_App("Play Store");
        click(null,"com.android.vending:id/search_box_idle_text",false);
        set_text_by_Resource("com.android.vending:id/search_box_text_input",appName);
        mDevice.pressEnter();
        UiObject appButton = mDevice.findObject(new UiSelector()
                .text(appName));
        if(appButton != null){
            click("INSTALL",null,true);
        }

        sleep(DOWNLOAD_TIMEOUT);

        back_spam(5);
    }
}

