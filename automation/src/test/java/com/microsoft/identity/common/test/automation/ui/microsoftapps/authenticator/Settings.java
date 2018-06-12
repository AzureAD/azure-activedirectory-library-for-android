package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

import io.appium.java_client.MobileBy;

public class Settings {


    public static Target MANAGE_DEVICE_REGISTRATION_BUTTON = Target.the("Manage device registration button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.TextView\").text(\"Register your device with your organization\")"));



}
