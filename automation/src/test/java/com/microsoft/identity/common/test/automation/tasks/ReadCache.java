package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.ui.Main;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;

public class ReadCache implements Task{

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(Main.READ_CACHE)
        );
    }

}
