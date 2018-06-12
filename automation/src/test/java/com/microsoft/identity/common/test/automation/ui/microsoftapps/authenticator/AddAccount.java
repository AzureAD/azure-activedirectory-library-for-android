package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class AddAccount {
    public static Target WORK_ACCOUNT_BUTTON = Target.the("Add Account Button").located(By.id("com.azure.authenticator:id/add_account_work_btn"));
}
