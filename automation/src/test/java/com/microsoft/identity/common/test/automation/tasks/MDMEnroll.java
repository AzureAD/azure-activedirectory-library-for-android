package com.microsoft.identity.common.test.automation.tasks;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.Swipe;
import com.microsoft.identity.common.test.automation.ui.googleplaystore.AppDetailView;
import com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1.MDMEnrollChallenge;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal.AccessSetupView;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal.ActivateDeviceAdministratorView;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal.ConsentDialog;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal.ExplainView;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal.SignInPageUsername;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal.StartView;
import com.microsoft.identity.common.test.automation.ui.microsoftapps.companyportal.WhatsNextView;
import com.microsoft.identity.common.test.automation.ui.microsoftservices.intune.EnrollmentAppLinkPage;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;

import io.appium.java_client.touch.offset.PointOption;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class MDMEnroll implements Task{

    @Override
    public <T extends Actor> void performAs(T actor) {

        User user = (User)actor;
        SignInUser signInUser = SignInUser.GetSignInUserByFederationProvider(user.getFederationProvider());

        actor.attemptsTo(
                Click.on(MDMEnrollChallenge.ENROLL_BUTTON),
                Click.on(EnrollmentAppLinkPage.ENROLL_BUTTON),
                WaitUntil.the(AppDetailView.INSTALL_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(AppDetailView.INSTALL_BUTTON),
                WaitUntil.the(AppDetailView.OPEN_BUTTON, isVisible()).forNoMoreThan(15).seconds(),
                Click.on(AppDetailView.OPEN_BUTTON),
                Click.on(StartView.SIGNIN_BUTTON),
                Enter.theValue(user.getCredential().userName).into(SignInPageUsername.USERNAME),
                Click.on(SignInPageUsername.NEXT_BUTTON),
                signInUser,
                Click.on(AccessSetupView.CONTINUE_BUTTON),
                Click.on(ExplainView.CONTINUE_BUTTON),
                Click.on(WhatsNextView.CONTINUE_BUTTON),
                Click.on(ConsentDialog.ALLOW_BUTTON),
                Swipe.points(PointOption.point(100, 600), PointOption.point(100, 50)),
                WaitUntil.the(ActivateDeviceAdministratorView.ACTIVATE_BUTTON, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(ActivateDeviceAdministratorView.ACTIVATE_BUTTON),
                WaitUntil.the(AccessSetupView.CONTINUE_BUTTON, isVisible()).forNoMoreThan(60).seconds(),
                Click.on(AccessSetupView.CONTINUE_BUTTON)
        );

    }

}