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

import org.apache.http.util.TextUtils;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class AcquireTokenSilent implements Task{

    private String userIdentifier;
    private String uniqueId;
    private String authority;

    @Steps
    CloseKeyboard closeKeyboard;

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User)actor;
        TokenRequest tokenRequest = user.getSilentTokenRequest();
        if(!TextUtils.isEmpty(userIdentifier)) {
            tokenRequest.setUserIdentitfier(userIdentifier);
        }
        if(!TextUtils.isEmpty(uniqueId)) {
            tokenRequest.setUniqueUserId(uniqueId);
        }
        actor.attemptsTo(
                WaitUntil.the(Main.ACQUIRE_TOKEN_SILENT, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(Main.ACQUIRE_TOKEN_SILENT),

                Enter.theValue(user.getSilentTokenRequestAsJson()).into(Request.REQUEST_INFO_FIELD),
                closeKeyboard,
                Click.on(Request.SUBMIT_REQUEST_BUTTON)
        );
    }

    public AcquireTokenSilent withUniqueId(String uniqueId){
        this.uniqueId = uniqueId;
        return this;
    }

    public AcquireTokenSilent withUserIdentifier(String userIdentifier){
        this.userIdentifier = userIdentifier;
        return this;
    }

}
