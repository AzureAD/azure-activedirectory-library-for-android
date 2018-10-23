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
import com.microsoft.identity.common.test.automation.interactions.authenticatorapp.TapAccount;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPageUserName;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.SignInPageUserNameBroker;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;


public class EnterUserNameForSignInDisambiguation implements Task {

    boolean withBroker;

    @Override
    public <T extends Actor> void performAs(T actor) {
        User user = (User) actor;
        if(withBroker && user.getWorkplaceJoined()){
            actor.attemptsTo(
                    new TapAccount()
            );
        }else {
            Target username = withBroker ? SignInPageUserNameBroker.USERNAME : SignInPageUserName.USERNAME;
            Target nextButton = withBroker ? SignInPageUserNameBroker.NEXT_BUTTON : SignInPageUserName.NEXT_BUTTON;
            actor.attemptsTo(
                    WaitUntil.the(nextButton, isVisible()).forNoMoreThan(10).seconds(),
                    Enter.theValue(user.getCredential().userName).into(username),
                    Click.on(nextButton));
        }
    }

    public EnterUserNameForSignInDisambiguation withBroker(boolean withBroker) {
        this.withBroker = withBroker;
        return this;
    }

}
