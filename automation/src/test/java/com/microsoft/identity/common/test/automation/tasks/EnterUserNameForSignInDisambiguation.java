package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPageUserName;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;


public class EnterUserNameForSignInDisambiguation implements Task {

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        user.attemptsTo(
                WaitUntil.the(SignInPageUserName.USERNAME, isVisible()).forNoMoreThan(10).seconds(),
                Enter.theValue(user.getCredential().userName).into(SignInPageUserName.USERNAME),
                Click.on(SignInPageUserName.NEXT_BUTTON)
        );
    }

}
