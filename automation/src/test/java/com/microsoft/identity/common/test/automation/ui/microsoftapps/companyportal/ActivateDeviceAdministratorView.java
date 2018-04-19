package com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal;


import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

import io.appium.java_client.MobileBy;

public class ActivateDeviceAdministratorView {
    //public static Target SCROLL_VIEW = Target.the("Scroll view").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.ScrollView\").instance(0)"));
    //public static Target CATEGORY = Target.the("Category").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.RelativeLayout\").instance(6)"));
    public static Target ACTIVATE_BUTTON = Target.the("Category").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(0)"));
    //public static Target ACTIVATE_BUTTON = Target.the("Activate button").located(By.id("com.android.settings:id/action_button"));
    public static Target CANCEL_BUTTON = Target.the("Cancel button").located(By.id("com.android.settings:id/cancel_button"));
    public static Target UNINSTALL_BUTTON = Target.the("Uninstall button").located(By.id("com.android.settings:id/uninstall_button"));
}
