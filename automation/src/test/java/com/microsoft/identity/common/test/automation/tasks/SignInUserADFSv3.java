package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.CloseKeyboard;
import com.microsoft.identity.common.test.automation.ui.identityproviders.ADFSv3.SignInPage;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.EnterValueIntoTarget;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class SignInUserADFSv3 extends SignInUser {



    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        user.attemptsTo(
            //Not using static method here to avoid logging the password via instrumentation... this won't show up as a step
            WaitUntil.the(SignInPage.PASSWORD_FIELD, isVisible()).forNoMoreThan(10).seconds(),
            new EnterValueIntoTarget(user.getCredential().password, SignInPage.PASSWORD_FIELD),
            new CloseKeyboard(),
            Click.on(SignInPage.SIGN_IN_BUTTON)
        );
    }
}
