package com.microsoft.identity.common.test.automation.ui;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

import io.appium.java_client.MobileBy;

public class Request {

    public static Target REQUEST_INFO_FIELD = Target.the("Request info field").located(By.id("com.microsoft.aad.automation.testapp.adal:id/requestInfo"));
    public static Target SUBMIT_REQUEST_BUTTON = Target.the("Go button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/requestGo"));
    public static Target SELECT_ACCOUNT_BUTTON = Target.the("Select account button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.view.View\").index(1)"));
}
