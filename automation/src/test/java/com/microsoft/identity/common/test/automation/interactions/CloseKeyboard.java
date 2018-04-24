package com.microsoft.identity.common.test.automation.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;

public class CloseKeyboard implements Interaction {

    @Override
    public <T extends Actor> void performAs(T actor) {
        BrowseTheWeb.as(actor).getDriver().navigate().back();
    }
}
