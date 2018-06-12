package com.microsoft.identity.common.test.automation.interactions.authenticatorapp;

import com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator.SelectAccount;

import net.serenitybdd.core.pages.WebElementFacade;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.webdriver.WebDriverFacade;

import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.touch.TapOptions;
import io.appium.java_client.touch.offset.ElementOption;

public class TapAccount implements Interaction {

    @Override
    public <T extends Actor> void performAs(T actor) {
        WebDriverFacade facade = (WebDriverFacade) BrowseTheWeb.as(actor).getDriver();
        AndroidDriver androidDriver = (AndroidDriver) facade.getProxiedDriver();
        TouchAction actions = new TouchAction(androidDriver);
        WebElementFacade elementFacade = SelectAccount.ONLY_ACCOUNT.resolveFor(actor);
        actions.tap(new TapOptions().withElement(new ElementOption().withElement(elementFacade)));
        actions.perform();
    }
}