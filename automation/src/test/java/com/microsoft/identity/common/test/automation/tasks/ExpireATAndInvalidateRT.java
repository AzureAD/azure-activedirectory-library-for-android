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

public class ExpireATAndInvalidateRT implements Task {

    private String clientId;

    @Steps
    CloseKeyboard closeKeyboard;

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User) actor;
        TokenRequest tokenRequest = user.getTokenRequest();
        tokenRequest.setAuthority(user.getCacheResult().authority);
        tokenRequest.setUserIdentitfier(user.getCredential().userName);
        tokenRequest.setUniqueUserId(user.getCacheResult().uniqueUserId);
        tokenRequest.setTenantId(user.getCacheResult().tenantId);
        tokenRequest.setFamiiyClientId(user.getCacheResult().familyClientId);
        if (!TextUtils.isEmpty(clientId)) {
            tokenRequest.setClientId(clientId);
        }

        actor.attemptsTo(
                WaitUntil.the(Main.EXPIRE_AT_INVALIDATE_RT, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(Main.EXPIRE_AT_INVALIDATE_RT),
                Enter.theValue(user.getTokenRequestAsJson()).into(Request.REQUEST_INFO_FIELD),
                closeKeyboard,
                WaitUntil.the(Request.SUBMIT_REQUEST_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(Request.SUBMIT_REQUEST_BUTTON)
        );
    }

    public ExpireATAndInvalidateRT withClientId(String clientId){
        this.clientId = clientId;
        return this;
    }
}
