package com.microsoft.aad.automation.testapp.UIAutomatorTests;


import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;


@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class ManualTests1 {

    @Test
    public void phone_test() {
        //main flow after app is installed to test phone compatibility.
        SampleFunctions.launch_appView();
        SampleFunctions.launch_App("adal");
        SampleFunctions.clear_app_cache("adal");
        SampleFunctions.re_install_app("Microsoft Authenticator","Authenticator");
        SampleFunctions.re_install_app("Intune Company Portal","Company Portal");
    }

    @Test
    public void uninstall() {
        // to uninstall apps
        SampleFunctions.uninstall_App("Authenticator");
        SampleFunctions.uninstall_App("Company Portal");
    }

    @Test
    public void download_install_from_store() {

        // to just download install
        //SampleFunctions.download_install_app("Microsoft Authenticator");
        //SampleFunctions.download_install_app("Intune Company Portal");

        // to reinstall (uninstall and install)
        SampleFunctions.re_install_app("Microsoft Authenticator","Authenticator");
        SampleFunctions.re_install_app("Intune Company Portal","Company Portal");
    }

    @Test
    public void functions_testing()
    {
        // we cannot clear data for for adal and adalR.
        String[] company_apps = {"Authenticator","Company Portal"};
        for(String app : company_apps){
            // launch the app
            SampleFunctions.launch_App(app);
            // clear app data
            SampleFunctions.clear_app_cache(app);
        }

        String[] local_apps = {"adal","adalR"};
        for(String app : local_apps){
            // launch the app
            SampleFunctions.launch_App(app);
            // clear app data
            SampleFunctions.clear_app_cache(app);
        }
    }

    @Test
    public void enroll() {
        //SampleFunctions.enroll_authenticator("", "");
        //SampleFunctions.remove_authenticator_account();
        SampleFunctions.enroll_company_portal("", "");
    }

    @Test
    public void unenroll() {
        //SampleFunctions.remove_authenticator_account();
        SampleFunctions.remove_company_portal_account();
    }



    @Test
    public void removeAccount()
    {
        SampleFunctions.remove_authenticator_account();
    }

    @Test
    public void clear_data()
    {
        SampleFunctions.clear_app_data("adalR");
        SampleFunctions.clear_app_data("adal");
    }


    @Test
    public void test_case1()
    {

        String upn = "";
        String secret = "";

        //SampleFunctions.enroll_authenticator(upn, secret);

        SampleFunctions.launch_App("adal");
        //SampleFunctions.adal_clear_cache();
        SampleFunctions.launch_App("adalR");
        //SampleFunctions.adal_clear_cache();

        // this code block acquires a token via automationtest app.
        SampleFunctions.click("Acquire Token", null, true);
        String json = SampleFunctions.json_body(
                "https://login.microsoftonline.com/common",
                "00000002-0000-0000-c000-000000000000",
                "urn:ietf:wg:oauth:2.0:oob",
                "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0"
        );

        SampleFunctions.set_text_by_Text("Please enter the sign in info here...",json);
        SampleFunctions.click("Go", null, true);
        SampleFunctions.authenticate_webview2(upn,secret);
        SampleFunctions.click("Done", null, true);

    }


}
