package com.microsoft.identity.common.test.automation.ui.microsoftservices.intune;

import net.serenitybdd.screenplay.targets.Target;

import io.appium.java_client.MobileBy;

public class EnrollmentAppLinkPage {
    public static Target ENROLL_BUTTON = Target.the("Go to google play store button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.view.View\").instance(5)"));
}