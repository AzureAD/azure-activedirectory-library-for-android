package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

import io.appium.java_client.MobileBy;



public class EditAccounts {

    /*
    The following assumes that there is only one account in the account list.  I'm starting with something.... and then we'll get better.
     */
    public static Target LIST_ROW_DELETE = Target.the("Add Account Button").located(By.id("com.azure.authenticator:id/account_list_row_delete"));

}
