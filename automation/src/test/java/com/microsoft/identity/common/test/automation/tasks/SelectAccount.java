package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.CloseKeyboard;
import com.microsoft.identity.common.test.automation.ui.Main;
import com.microsoft.identity.common.test.automation.ui.Request;
import com.microsoft.identity.common.test.automation.utility.TokenRequest;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;
import net.thucydides.core.annotations.Steps;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class SelectAccount implements Task{

    @Steps
    CloseKeyboard closeKeyboard;

    String prompt;
    String userIdentifier;

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        TokenRequest tokenRequest = user.getTokenRequest();
        tokenRequest.setPromptBehavior(prompt);
        tokenRequest.setUserIdentitfier(userIdentifier);
        SignInUser signInUser = SignInUser.GetSignInUserByFederationProvider(user.getFederationProvider());
        actor.attemptsTo(
                WaitUntil.the(Main.ACQUIRE_TOKEN_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(Main.ACQUIRE_TOKEN_BUTTON),
                Enter.theValue(user.getTokenRequestAsJson()).into(Request.REQUEST_INFO_FIELD),
                closeKeyboard,
                WaitUntil.the(Request.SUBMIT_REQUEST_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(Request.SUBMIT_REQUEST_BUTTON),
                WaitUntil.the(Request.SELECT_ACCOUNT_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(Request.SELECT_ACCOUNT_BUTTON)
        );
    }

    public SelectAccount withPrompt(String prompt){
        this.prompt = prompt;
        return this;
    }

    public SelectAccount withUserIdentifier(String userIdentifier){
        this.userIdentifier = userIdentifier;
        return this;
    }


}
