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

public class ExpireAccessToken implements Task {

    @Steps
    CloseKeyboard closeKeyboard;

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        TokenRequest tokenRequest = user.getTokenRequest();
        tokenRequest.setAuthority("https://login.windows.net/common");
        tokenRequest.setUserIdentitfier(user.getCredential().userName);
      actor.attemptsTo(
              WaitUntil.the(Main.EXPIRE_ACCESS_TOKEN,  isVisible()).forNoMoreThan(10).seconds(),
              Click.on(Main.EXPIRE_ACCESS_TOKEN),
              Enter.theValue(user.getTokenRequestAsJson()).into(Request.REQUEST_INFO_FIELD),
              closeKeyboard,
              WaitUntil.the(Request.SUBMIT_REQUEST_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
              Click.on(Request.SUBMIT_REQUEST_BUTTON)
      );
    }
}
