package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.authenticatorapp.TapAccount;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPageUserName;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPageUserNameBroker;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator.SelectAccount;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;
import net.thucydides.core.annotations.Steps;

import org.openqa.selenium.interactions.internal.TouchAction;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;


public class EnterUserNameForSignInDisambiguation implements Task {

    Boolean withBroker;

    @Steps
    TapAccount tapAccount;

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;

        if(withBroker){
            if(user.getWorkplaceJoined()){
                user.attemptsTo(
                        tapAccount
                );
            }else {
                user.attemptsTo(
                        WaitUntil.the(SignInPageUserNameBroker.USERNAME, isVisible()).forNoMoreThan(10).seconds(),
                        Enter.theValue(user.getCredential().userName).into(SignInPageUserNameBroker.USERNAME),
                        Click.on(SignInPageUserNameBroker.NEXT_BUTTON)
                );
            }
        }else {
            user.attemptsTo(
                    WaitUntil.the(SignInPageUserName.USERNAME, isVisible()).forNoMoreThan(10).seconds(),
                    Enter.theValue(user.getCredential().userName).into(SignInPageUserName.USERNAME),
                    Click.on(SignInPageUserName.NEXT_BUTTON)
            );
        }
    }

    public EnterUserNameForSignInDisambiguation withBroker(Boolean withBroker){
        this.withBroker = withBroker;
        return this;
    }

}
