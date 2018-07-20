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

package com.microsoft.identity.common.test.automation.tests_broker;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.ClickDone;
import com.microsoft.identity.common.test.automation.model.Constants;
import com.microsoft.identity.common.test.automation.questions.AccessTokenFromAuthenticationResult;
import com.microsoft.identity.common.test.automation.tasks.AcquireToken;
import com.microsoft.identity.common.test.automation.tasks.AcquireTokenSilent;
import com.microsoft.identity.common.test.automation.tasks.authenticatorapp.WorkplaceLeave;
import com.microsoft.identity.common.test.automation.ui.Results;
import com.microsoft.identity.common.test.automation.utility.Scenario;
import com.microsoft.identity.common.test.automation.utility.TestConfigurationQuery;

import net.serenitybdd.junit.runners.SerenityParameterizedRunner;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.waits.WaitUntil;
import net.thucydides.core.annotations.Managed;
import net.thucydides.core.annotations.Steps;
import net.thucydides.junit.annotations.TestData;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import io.appium.java_client.service.local.AppiumDriverLocalService;

import static net.serenitybdd.screenplay.GivenWhenThen.and;
import static net.serenitybdd.screenplay.GivenWhenThen.givenThat;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.GivenWhenThen.then;
import static net.serenitybdd.screenplay.GivenWhenThen.when;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(SerenityParameterizedRunner.class)
public class AcquireTokenSilentTestWithBroker {

    @TestData
    public static Collection<Object[]> FederationProviders() {


        return Arrays.asList(new Object[][]{
                {"ADFSv2"}
        });

    }

    static AppiumDriverLocalService appiumService = null;

    @Managed(driver = "Appium")
    WebDriver hisMobileDevice;

    @BeforeClass
    public static void startAppiumServer() throws IOException {
        appiumService = AppiumDriverLocalService.buildDefaultService();
        appiumService.start();
    }

    @AfterClass
    public static void stopAppiumServer() {
        appiumService.stop();
    }

    private User james;
    private String federationProvider;
    private String mSecondaryResource;

    @Steps
    AcquireToken acquireToken;

    @Steps
    AcquireTokenSilent acquireTokenSilent;

    @Steps
    ClickDone clickDone;

    @Steps
    WorkplaceLeave workplaceLeave;

    public AcquireTokenSilentTestWithBroker(String federationProvider) {
        this.federationProvider = federationProvider;
    }

    @Before
    public void jamesCanUseAMobileDevice() {
        TestConfigurationQuery query = new TestConfigurationQuery();
        query.federationProvider = this.federationProvider;
        query.isFederated = true;
        query.userType = "Member";
        james = getUser(query);
        james.can(BrowseTheWeb.with(hisMobileDevice));
    }

    private User getUser(TestConfigurationQuery query) {

        Scenario scenario = Scenario.GetScenario(query);

        User newUser = User.named("james");
        newUser.setFederationProvider(scenario.getTestConfiguration().getUsers().getFederationProvider());
        newUser.setTokenRequest(scenario.getTokenRequest());
        newUser.getTokenRequest().setRedirectUri(Constants.AUTOMATION_TEST_APP_BROKER_REDIRECT_URI);
        newUser.setCredential(scenario.getCredential());
        newUser.setSilentTokenRequest(scenario.getSilentTokenRequest());
        newUser.getSilentTokenRequest().setRedirectUri(Constants.AUTOMATION_TEST_APP_BROKER_REDIRECT_URI);
        newUser.setWorkplaceJoined(false);
        mSecondaryResource = scenario.getTestConfiguration().getResourceIds().get(1);
        return newUser;
    }


    @Test
    public void should_be_able_to_acquire_token_silent() {

        james.attemptsTo(acquireTokenSilent);

    }

    @Test
    public void should_be_able_to_acquire_token_and_then_acquire_silent() {

        givenThat(james).wasAbleTo(
                acquireToken.withBroker(),
                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds()
        );
        String accessToken1 = james.asksFor(AccessTokenFromAuthenticationResult.displayed());

        when(james).attemptsTo(
                clickDone,
                acquireTokenSilent
                        .withBroker(),
                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds()
        );

        then(james).should(seeThat(AccessTokenFromAuthenticationResult.displayed(), is(accessToken1)));

        james.attemptsTo(workplaceLeave);

    }

    @Test
    public void should_be_able_to_acquire_token_and_then_acquire_silent_with_login_hint() {

        givenThat(james).wasAbleTo(
                acquireToken.withBroker(),
                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds()
        );
        String accessToken1 = james.asksFor(AccessTokenFromAuthenticationResult.displayed());

        when(james).attemptsTo(
                clickDone,
                acquireTokenSilent
                        .withBroker()
                        .withUserIdentifier(james.getCredential().userName),
                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds()
        );

        then(james).should(seeThat(AccessTokenFromAuthenticationResult.displayed(), is(accessToken1)));

        james.attemptsTo(workplaceLeave);


    }

    @Test
    public void should_be_able_to_acquire_token_and_then_acquire_silent_with_different_resource() {

        givenThat(james).wasAbleTo(
                acquireToken.withBroker(),
                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds()
        );
        String accessToken1 = james.asksFor(AccessTokenFromAuthenticationResult.displayed());

        when(james).attemptsTo(
                clickDone,
                acquireTokenSilent
                        .withBroker()
                        .withResourceId(mSecondaryResource)
                        .withUserIdentifier(james.getCredential().userName),
                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds()
        );

        then(james).should(seeThat(AccessTokenFromAuthenticationResult.displayed(), notNullValue()));
        and(james).should(seeThat(AccessTokenFromAuthenticationResult.displayed(), not(accessToken1)));
        james.attemptsTo(workplaceLeave);
    }

    //TODO : Understand how to test the FRT scenario for broker

//    @Test
//    public void should_be_able_to_acquire_token_and_then_acquire_silent_frt() {
//
//        givenThat(james).wasAbleTo(
//                acquireToken
//                        .withBroker()
//                        .withClientId(Constants.OUTLOOK_CLIENT_ID)
//                        .withRedirectUri(Constants.OUTLOOK_REDIRECT_URI),
//                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds()
//        );
//        String accessToken1 = james.asksFor(AccessTokenFromAuthenticationResult.displayed());
//        then(james).should(seeThat(AccessTokenFromAuthenticationResult.displayed(), notNullValue()));
//
//        when(james).attemptsTo(
//                clickDone,
//                acquireTokenSilent
//                        .withBroker()
//                        .withClientId(Constants.ONE_DRIVE_CLIENT_ID)
//                        .withRedirectUri(Constants.ONE_DRIVE_REDIRECT_URI),
//                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds()
//        );
//
//        then(james).should(seeThat(AccessTokenFromAuthenticationResult.displayed(), notNullValue()));
//        and(james).should(seeThat(AccessTokenFromAuthenticationResult.displayed(), not(accessToken1)));
//
//        james.attemptsTo(workplaceLeave);
//
//    }

}
