package com.microsoft.identity.common.test.automation.interactions;

import com.microsoft.identity.common.test.automation.ui.Results;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class ClickDone implements Interaction {

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(Results.DONE_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(Results.DONE_BUTTON)
        );
    }
}
