package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

import io.appium.java_client.MobileBy;

public class ManageDeviceRegistration {

    public static Target ORGANIZATIONAL_EMAIL_TEXTBOX = Target.the("Organizational email textbox").located(By.id("com.azure.authenticator:id/manage_device_registration_email_input"));
    public static Target REGISTER_BUTTON = Target.the("Register button").located(By.id("com.azure.authenticator:id/manage_device_registration_register_button"));
    public static Target ENABLE_BROWSER_ACCESS_BUTTON = Target.the("Enable browser access button").located(By.id("com.azure.authenticator:id/manage_device_registration_enable_browser_access_button"));
}
