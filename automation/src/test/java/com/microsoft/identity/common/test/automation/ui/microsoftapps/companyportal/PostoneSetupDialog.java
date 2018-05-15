package com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class PostoneSetupDialog {

    public static Target OK_BUTTON = Target.the("Ok  button").located(By.id("android:id/button1"));
    public static Target CANCEL_BUTTON = Target.the("Cancel  button").located(By.id("android:id/button2"));

}
