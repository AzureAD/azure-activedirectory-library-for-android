package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.CloseKeyboard;
import com.microsoft.identity.common.test.automation.ui.Main;
import com.microsoft.identity.common.test.automation.ui.Request;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPagePassword;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPageUserName;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.actions.EnterValue;
import net.serenitybdd.screenplay.actions.EnterValueIntoTarget;
import net.thucydides.core.annotations.Steps;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class AcquireToken implements Task{

    @Steps
    CloseKeyboard closeKeyboard;

    private final String tokenRequest;

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        actor.attemptsTo(
                Click.on(Main.ACQUIRE_TOKEN_BUTTON),
                Enter.theValue(tokenRequest).into(Request.REQUEST_INFO_FIELD),
                closeKeyboard,
                Click.on(Request.SUBMIT_REQUEST_BUTTON),
                Enter.theValue(user.getCredential().userName).into(SignInPageUserName.USERNAME),
                Click.on(SignInPageUserName.NEXT_BUTTON),
                //Not using static method here to avoid logging the password via instrumentation... this won't show up as a step
                new EnterValueIntoTarget(user.getCredential().password, SignInPagePassword.PASSWORD),
                Click.on(SignInPagePassword.NEXT_BUTTON)
        );
    }

    protected AcquireToken(String tokenRequest){
        this.tokenRequest = tokenRequest;
    }

    public static AcquireToken with(String tokenRequest){
        return instrumented(AcquireToken.class, tokenRequest);
    }

}
