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
import net.serenitybdd.screenplay.waits.WaitUntil;
import net.thucydides.core.annotations.Steps;


import io.appium.java_client.MobileElement;

import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;


public class WorkplaceJoin implements Task{

    @Steps
    OpenAuthenticatorApp openAuthenticatorApp;

    @Override
    public <T extends Actor> void performAs(T actor) {

        User user = (User)actor;
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