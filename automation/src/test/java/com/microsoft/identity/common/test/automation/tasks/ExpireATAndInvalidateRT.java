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

public class ExpireATAndInvalidateRT implements Task {

    @Steps
    CloseKeyboard closeKeyboard;

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        TokenRequest tokenRequest = user.getTokenRequest();
        tokenRequest.setAuthority(user.getCacheResult().authority);
        tokenRequest.setUserIdentitfier(user.getCredential().userName);
        tokenRequest.setUniqueUserId(user.getCacheResult().uniqueUserId);
        tokenRequest.setTenantId(user.getCacheResult().tenantId);
      actor.attemptsTo(
              WaitUntil.the(Main.EXPIRE_AT_INVALIDATE_RT,  isVisible()).forNoMoreThan(10).seconds(),
              Click.on(Main.EXPIRE_AT_INVALIDATE_RT),
              Enter.theValue(user.getTokenRequestAsJson()).into(Request.REQUEST_INFO_FIELD),
              closeKeyboard,
              WaitUntil.the(Request.SUBMIT_REQUEST_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
              Click.on(Request.SUBMIT_REQUEST_BUTTON)
     );
    }
}
