package com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal;


import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class WhatsNextView {
    public static Target CONTINUE_BUTTON = Target.the("Next  button").located(By.id("com.microsoft.windowsintune.companyportal:id/bullet_list_page_forward_button"));
    public static Target LEARN_MORE_ABOUT_PERMISSIONS_BUTTON = Target.the("Next  button").located(By.id("com.microsoft.windowsintune.companyportal:id/bullet_list_page_help_link"));
}
