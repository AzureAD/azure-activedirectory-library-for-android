package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class RemoveAccountDialog {
    public static Target REMOVE_BUTTON = Target.the("Remove account button").located(By.id("android:id/button1"));
    public static Target CANCEL_BUTTON = Target.the("Cancel button").located(By.id("android:id/button2"));
}
