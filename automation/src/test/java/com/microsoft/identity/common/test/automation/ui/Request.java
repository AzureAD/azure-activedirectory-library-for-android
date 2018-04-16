package com.microsoft.identity.common.test.automation.ui;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class Request {

    public static Target REQUEST_INFO_FIELD = Target.the("Request info field").located(By.id("com.microsoft.aad.automation.testapp.adal:id/requestInfo"));
    public static Target SUBMIT_REQUEST_BUTTON = Target.the("Go button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/requestGo"));

}
