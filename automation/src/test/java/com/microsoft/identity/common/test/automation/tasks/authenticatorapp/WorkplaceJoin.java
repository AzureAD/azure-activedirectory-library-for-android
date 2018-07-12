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

package com.microsoft.identity.common.test.automation.tasks.authenticatorapp;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.authenticatorapp.OpenAuthenticatorApp;
import com.microsoft.identity.common.test.automation.tasks.SignInUser;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator.Main;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator.ManageDeviceRegistration;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator.Settings;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.thucydides.core.annotations.Steps;


public class WorkplaceJoin implements Task {

    @Steps
    OpenAuthenticatorApp openAuthenticatorApp;

    @Override
    public <T extends Actor> void performAs(T actor) {

        User user = (User) actor;
        SignInUser signInUser = SignInUser.GetSignInUserByFederationProvider(user.getFederationProvider());

        actor.attemptsTo(
                openAuthenticatorApp,
                Click.on(Main.MENU_BUTTON),
                Click.on(Main.SETTINGS_MENU_BUTTON),
                Click.on(Settings.MANAGE_DEVICE_REGISTRATION_BUTTON),
                //Need to figure out how to deal with new installation where permission to read contacts is required.... better to grant ahead of time i think.
                Click.on(ManageDeviceRegistration.ORGANIZATIONAL_EMAIL_TEXTBOX),
                Enter.theValue(user.getCredential().userName).into(ManageDeviceRegistration.ORGANIZATIONAL_EMAIL_TEXTBOX),
                Click.on(ManageDeviceRegistration.REGISTER_BUTTON),
                signInUser

        );

    }

}