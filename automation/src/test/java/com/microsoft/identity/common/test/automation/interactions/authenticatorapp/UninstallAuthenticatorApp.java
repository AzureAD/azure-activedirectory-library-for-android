package com.microsoft.identity.common.test.automation.interactions.authenticatorapp;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.webdriver.WebDriverFacade;

import io.appium.java_client.android.Activity;
import io.appium.java_client.android.AndroidDriver;

public class UninstallAuthenticatorApp implements Interaction {

    @Override
    public <T extends Actor> void performAs(T actor) {
        WebDriverFacade facade = (WebDriverFacade)BrowseTheWeb.as(actor).getDriver();
        AndroidDriver androidDriver = (AndroidDriver)facade.getProxiedDriver();
        androidDriver.removeApp(AuthenticatorConstants.PackageName);
    }
}