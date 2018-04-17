package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.CloseKeyboard;
import com.microsoft.identity.common.test.automation.ui.Main;
import com.microsoft.identity.common.test.automation.ui.Request;
import com.microsoft.identity.common.test.automation.ui.Results;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.thucydides.core.annotations.Steps;

public class AcquireTokenSilent implements Task{

    @Steps
    CloseKeyboard closeKeyboard;

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        actor.attemptsTo(
                Click.on(Main.ACQUIRE_TOKEN_SILENT),
                Enter.theValue(user.getSilentTokenRequestAsJson()).into(Request.REQUEST_INFO_FIELD),
                closeKeyboard,
                Click.on(Request.SUBMIT_REQUEST_BUTTON),
                Click.on(Results.DONE_BUTTON)

        );
    }

}
