package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPageUserName;
import com.microsoft.identity.common.test.automation.ui.identityproviders.ADFSv4.SignInPage;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.actions.EnterValueIntoTarget;

public class SignInUserADFSv4 extends SignInUser {

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        user.attemptsTo(
            Enter.theValue(user.getCredential().userName).into(SignInPageUserName.USERNAME),
            Click.on(SignInPageUserName.NEXT_BUTTON),
            //Not using static method here to avoid logging the password via instrumentation... this won't show up as a step
            new EnterValueIntoTarget(user.getCredential().password, SignInPage.PASSWORD_FIELD),
            Click.on(SignInPage.SIGN_IN_BUTTON)
        );
    }
}
