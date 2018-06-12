package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class EnableBrowserAccessDialog {
    public static Target CONTINUE_BUTTON = Target.the("Continue button").located(By.id("android:id/button1"));
    public static Target CANCEL_BUTTON = Target.the("Cancel button").located(By.id("android:id/button2"));
}
