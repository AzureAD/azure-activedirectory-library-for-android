package com.microsoft.aad.automation.testapp.UIAutomatorTests;


import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;


@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class ManualTests1 {

    @Test
    public void testing()
    {
        SampleFunctions.find_click("Apps");
    }

    @Test
    public void enroll()
    {
        //SampleFunctions.launch_App("Authenticator");
        SampleFunctions.enroll_authenticator(
                "",
                ""
        );
    }

    @Test
    public void un_enroll() {
        SampleFunctions.remove_authenticator_account();
    }

    @Test
    public void removeAccount()
    {
        SampleFunctions.remove_authenticator_account();
    }

    @Test
    public void Open_Applications_View()
    {
       //SampleFunctions.launch_App("adal");
       //SampleFunctions.launch_App("adalR");

        SampleFunctions.clear_app_data("Authenticator");
       //SampleFunctions.uninstall_App("adalR");
    }


    @Test
    public void clear_data()
    {
        SampleFunctions.clear_app_data("adalR");
        SampleFunctions.clear_app_data("adal");
    }


    @Test
    public void test_case()
    {

        String upn = "";
        String secret = "";

        //SampleFunctions.enroll_authenticator(upn, secret);

        //SampleFunctions.launch_App("adal");
        //SampleFunctions.adal_clear_cache();
        SampleFunctions.launch_App("adal");
        //SampleFunctions.adal_clear_cache();

        // this code block acquires a token via automationtest app.
        SampleFunctions.click_and_await("Acquire Token");
        String json = SampleFunctions.json_body(
                "https://login.microsoftonline.com/common",
                "00000002-0000-0000-c000-000000000000",
                "urn:ietf:wg:oauth:2.0:oob",
                "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0"
        );

        SampleFunctions.set_text_by_Text("Please enter the sign in info here...",json);
        SampleFunctions.click_and_await("Go");
        SampleFunctions.authenticate_webview2(upn,secret);
        SampleFunctions.click_and_await("Done");

    }

    @Test
    public void test_download() {
        SampleFunctions.download_install_app("Microsoft Authenticator");
        SampleFunctions.download_install_app("Intune Company Portal");
    }
}