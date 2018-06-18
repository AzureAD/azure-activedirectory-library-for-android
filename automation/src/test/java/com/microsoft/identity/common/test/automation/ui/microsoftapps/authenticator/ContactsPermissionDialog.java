package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class ContactsPermissionDialog {

    public static Target ALLOW_BUTTON = Target.the("Allow button").located(By.id("com.android.packageinstaller:id/permission_allow_button"));
    public static Target DENY_BUTTON = Target.the("Deny button").located(By.id("com.android.packageinstaller:id/permission_deny_button"));

}
