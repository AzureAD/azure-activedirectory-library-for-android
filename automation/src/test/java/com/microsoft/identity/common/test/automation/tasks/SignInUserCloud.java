package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.CloseKeyboard;
import com.microsoft.identity.common.test.automation.ui.identityproviders.ADFSv4.SignInPage;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.EnterValueIntoTarget;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class SignInUserCloud extends SignInUser {
    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        user.attemptsTo(
                WaitUntil.the(SignInPage.PASSWORD_FIELD_CLOUD, isVisible()).forNoMoreThan(10).seconds(),
                new EnterValueIntoTarget(user.getCredential().password, SignInPage.PASSWORD_FIELD_CLOUD),
                new CloseKeyboard(),
                WaitUntil.the(SignInPage.SIGN_IN_BUTTON_CLOUD, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(SignInPage.SIGN_IN_BUTTON_CLOUD)
        );
    }
}
