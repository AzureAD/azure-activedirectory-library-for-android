package com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal;


import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class ActivateDeviceAdministratorView {
    public static Target ACTIVATE_BUTTON = Target.the("Activate button").located(By.id("com.android.settings:id/action_button"));
    public static Target CANCEL_BUTTON = Target.the("Cancel button").located(By.id("com.android.settings:id/cancel_button"));
    public static Target UNINSTALL_BUTTON = Target.the("Uninstall button").located(By.id("com.android.settings:id/uninstall_button"));
}
