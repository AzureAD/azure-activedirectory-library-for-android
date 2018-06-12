package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

import io.appium.java_client.MobileBy;

public class Main {

    public static Target ADD_ACCOUNT_BUTTON = Target.the("Add Account Button").located(By.id("com.azure.authenticator:id/button_add_work_account"));
    public static Target ZERO_ACCOUNTS_ADD_ACCOUNT_BUTTON = Target.the("Zero Accounts - Add First Account Button").located(By.id("com.azure.authenticator:id/zero_accounts_add_account_button"));
    public static Target MENU_BUTTON = Target.the("Menu Button").located(By.id("com.azure.authenticator:id/menu_overflow"));
    public static Target SETTINGS_MENU_BUTTON = Target.the("Settings Button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.TextView\").text(\"Settings\")"));
    public static Target EDIT_ACCOUNTS_MENU_BUTTON = Target.the("Edit accounts button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.TextView\").text(\"Edit accounts\")"));
    public static Target ADD_ACCOUNT_MENU_BUTTON = Target.the("Add account button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.TextView\").text(\"Add account\")"));



}
