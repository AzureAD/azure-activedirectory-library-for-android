package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.CloseKeyboard;
import com.microsoft.identity.common.test.automation.ui.Main;
import com.microsoft.identity.common.test.automation.ui.Request;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.thucydides.core.annotations.Steps;

public class AcquireToken implements Task{

    @Steps
    CloseKeyboard closeKeyboard;


    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        SignInUser signInUser = SignInUser.GetSignInUserByFederationProvider(user.getFederationProvider());
        actor.attemptsTo(
                Click.on(Main.ACQUIRE_TOKEN_BUTTON),
                Enter.theValue(user.getTokenRequestAsJson()).into(Request.REQUEST_INFO_FIELD),
                closeKeyboard,
                Click.on(Request.SUBMIT_REQUEST_BUTTON),
                new EnterUserNameForSignInDisambiguation(),
                signInUser
        );
    }


}
