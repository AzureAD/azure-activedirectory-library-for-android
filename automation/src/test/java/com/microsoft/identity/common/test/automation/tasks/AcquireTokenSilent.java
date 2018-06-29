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
import com.microsoft.identity.common.test.automation.utility.TokenRequest;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;
import net.thucydides.core.annotations.Steps;

import org.apache.http.util.TextUtils;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class AcquireTokenSilent implements Task {

    private String userIdentifier = "";
    private String uniqueId;
    private Boolean forceRefresh = false;
    private String authority = "";
    private String clientId = "";
    private String redirectUri = "";

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
        if(!TextUtils.isEmpty(authority)){
            tokenRequest.setAuthority(authority);
        }
        if(!TextUtils.isEmpty(clientId)){
            tokenRequest.setClientId(clientId);
        }
        if(!TextUtils.isEmpty(redirectUri)){
            tokenRequest.setRedirectUri(redirectUri);
        }
        tokenRequest.setForceRefresh(forceRefresh);
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

    public AcquireTokenSilent withUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
        return this;
    }

    public AcquireTokenSilent withForceRefresh() {
        this.forceRefresh = true;
        return this;
    }

    public AcquireTokenSilent withAuthority(String authority){
        this.authority = authority;
        return this;
    }

    public AcquireTokenSilent withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public AcquireTokenSilent withRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

}
