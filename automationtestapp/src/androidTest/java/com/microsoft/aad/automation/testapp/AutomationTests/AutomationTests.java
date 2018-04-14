package com.microsoft.aad.automation.testapp.AutomationTests;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;


@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class AutomationTests {

    @Test
    public void deviceTest(){
        Device testDevice = DeviceFactory.newDevice();
        testDevice.clear_cache("Authenticator");
    }
}