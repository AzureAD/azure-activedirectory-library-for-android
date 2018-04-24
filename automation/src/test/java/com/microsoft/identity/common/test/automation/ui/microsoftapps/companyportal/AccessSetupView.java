package com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class AccessSetupView {
    public static Target POSTPONE_BUTTON = Target.the("Postpone button").located(By.id("com.microsoft.windowsintune.companyportal:id/setup_negative_button"));
    public static Target CONTINUE_BUTTON = Target.the("Continue  button").located(By.id("com.microsoft.windowsintune.companyportal:id/setup_positive_button"));
}
