package com.microsoft.aad.automation.testapp.interactions;

import com.microsoft.aad.automation.testapp.abilities.UseMobileDevice;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;

public class PressHome implements Interaction {

    @Override
    public <T extends Actor> void performAs(T actor) {
        UseMobileDevice.as(actor).getMobileDevice().pressHome();
    }
}
