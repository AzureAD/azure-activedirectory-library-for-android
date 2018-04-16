package com.microsoft.identity.common.test.automation.ui;

import net.serenitybdd.screenplay.targets.Target;

import io.appium.java_client.MobileBy;

public class Device {


    public static Target Home = Target.the("Notes ").located(
            MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.FrameLayout\").clickable(true)"));

}
