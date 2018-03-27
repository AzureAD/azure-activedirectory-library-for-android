package com.microsoft.aad.automation.testapp.UIAutomatorTests;


import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;


@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class ManualTests1 {

    @Test
    public void clear_data()
    {
        SampleFunctions.clear_app_data("Authenticator");
    }

    @Test
    public void test_scroll() {
        //SampleFunctions.open_Applications_View();
        SampleFunctions.launch_App("adalR");
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
}