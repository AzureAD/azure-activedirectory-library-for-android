//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.CloseKeyboard;
import com.microsoft.identity.common.test.automation.ui.Main;
import com.microsoft.identity.common.test.automation.ui.Request;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPagePassword;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPageUserName;
import com.microsoft.identity.common.test.automation.utility.TokenRequest;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;
import net.thucydides.core.annotations.Steps;

import org.apache.http.util.TextUtils;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class AcquireToken implements Task {

    @Steps
    CloseKeyboard closeKeyboard;

    String prompt ="";
    String userIdentifier = "";
    boolean tokenExists = false;
    Boolean withBroker = false;
    private String clientId = "";
    private String redirectUri = "";

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User) actor;
        TokenRequest tokenRequest = user.getTokenRequest();
        if(!TextUtils.isEmpty(prompt)) {
            tokenRequest.setPromptBehavior(prompt);
        }
        if(!TextUtils.isEmpty(userIdentifier)) {
            tokenRequest.setUserIdentitfier(userIdentifier);
        }
        if(!TextUtils.isEmpty(clientId)){
            tokenRequest.setClientId(clientId);
        }
        if(!TextUtils.isEmpty(redirectUri)){
            tokenRequest.setRedirectUri(redirectUri);
        }
        tokenRequest.setUseBroker(withBroker);

        SignInUser signInUser = SignInUser.GetSignInUserByFederationProvider(user.getFederationProvider());

        actor.attemptsTo(
                WaitUntil.the(Main.ACQUIRE_TOKEN_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(Main.ACQUIRE_TOKEN_BUTTON),
                Enter.theValue(user.getTokenRequestAsJson()).into(Request.REQUEST_INFO_FIELD),
                closeKeyboard,
                WaitUntil.the(Request.SUBMIT_REQUEST_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(Request.SUBMIT_REQUEST_BUTTON)
        );
        // // acquire token does an interactive flow there is no token
        if (!tokenExists) {
            // If userIdentifier was not provided in acquire token call , attempt to enter username for sign in
            if (TextUtils.isEmpty(userIdentifier)) {
                actor.attemptsTo(
                        WaitUntil.the(SignInPageUserName.USERNAME, isVisible()).forNoMoreThan(10).seconds(),
                        new EnterUserNameForSignInDisambiguation().withBroker(withBroker),
                        WaitUntil.the(SignInPagePassword.PASSWORD, isVisible()).forNoMoreThan(10).seconds()
                );
            }
            // If the user is workplace joined... then they will not need to sign in... because we'll just select that account
            if (!user.getWorkplaceJoined()) {
                actor.attemptsTo(signInUser);
            }
        }else {
            // Token already exists, acquire token does a silent flow here.
            // If userIdentifier was not provided in acquire token call , attempt to enter username for sign in
            if (TextUtils.isEmpty(userIdentifier)) {
                actor.attemptsTo(
                        new EnterUserNameForSignInDisambiguation().withBroker(withBroker)
                );
            }
        }


    }

    public AcquireToken withPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public AcquireToken withUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
        return this;
    }

    public AcquireToken tokenExists(boolean tokenExists) {
        this.tokenExists = tokenExists;
        return this;
    }

    public AcquireToken withBroker() {
        this.withBroker = true;
        return this;
    }

    public AcquireToken withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public AcquireToken withRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }


}
