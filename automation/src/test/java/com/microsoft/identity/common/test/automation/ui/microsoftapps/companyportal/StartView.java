package com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class StartView {
    public static Target SIGNIN_BUTTON = Target.the("Sign in  button").located(By.id("com.microsoft.windowsintune.companyportal:id/sign_in_button"));
}