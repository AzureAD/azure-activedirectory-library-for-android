package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class SelectAccount {

    public static Target ONLY_ACCOUNT = Target.the("Add Account Button").located(By.id("com.azure.authenticator:id/account_list_row_default_mode"));

    //android.widget.RelativeLayout
}
