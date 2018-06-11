package com.microsoft.identity.common.test.automation.ui.microsoftapps.broker;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class SelectAccount {

    public static Target ADD_ACCOUNT_BUTTON = Target.the("Add Account").located(By.id("com.azure.authenticator:id/button_add_work_account"));

}
