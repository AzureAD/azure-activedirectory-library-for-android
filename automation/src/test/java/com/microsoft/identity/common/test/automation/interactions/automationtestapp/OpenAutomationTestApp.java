package com.microsoft.identity.common.test.automation.interactions.automationtestapp;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.webdriver.WebDriverFacade;

import io.appium.java_client.android.Activity;
import io.appium.java_client.android.AndroidDriver;

public class OpenAutomationTestApp implements Interaction {

    @Override
    public <T extends Actor> void performAs(T actor) {
        WebDriverFacade facade = (WebDriverFacade)BrowseTheWeb.as(actor).getDriver();
        AndroidDriver androidDriver = (AndroidDriver)facade.getProxiedDriver();
        Activity activity = new Activity(AutomationTestAppConstants.PackageName, AutomationTestAppConstants.MainActivityName);
        androidDriver.startActivity(activity);
    }
}
