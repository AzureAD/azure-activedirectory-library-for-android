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

package com.microsoft.identity.common.test.automation;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.ClickDone;
import com.microsoft.identity.common.test.automation.questions.AccessToken;
import com.microsoft.identity.common.test.automation.questions.ExpectedCacheItemCount;
import com.microsoft.identity.common.test.automation.questions.TokenCacheItemCount;
import com.microsoft.identity.common.test.automation.tasks.AcquireToken;
import com.microsoft.identity.common.test.automation.tasks.ClearCache;
import com.microsoft.identity.common.test.automation.tasks.ReadCache;
import com.microsoft.identity.common.test.automation.utility.Scenario;
import com.microsoft.identity.common.test.automation.utility.TestConfigurationQuery;

import net.serenitybdd.junit.runners.SerenityParameterizedRunner;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.annotations.Managed;
import net.thucydides.core.annotations.Steps;
import net.thucydides.core.annotations.WithTag;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(SerenityParameterizedRunner.class)
@WithTag("requires:none")
public class AcquireTokenTest {

    @TestData
    public static Collection<Object[]> FederationProviders() {


        return Arrays.asList(new Object[][]{
                {"ADFSv2"},
                {"ADFSv3"},
                {"ADFSv4"},
                {"PingFederate"},
                {"Shibboleth"}

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

    @Steps
    AcquireToken acquireToken;

    @Steps
    ReadCache readCache;

    @Steps
    ClickDone clickDone;

    @Steps
    ClearCache clearCache;

    public AcquireTokenTest(String federationProvider) {
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

    private static User getUser(TestConfigurationQuery query) {

        Scenario scenario = Scenario.GetScenario(query);

        User newUser = User.named("james");
        newUser.setFederationProvider(scenario.getTestConfiguration().getUsers().getFederationProvider());
        newUser.setTokenRequest(scenario.getTokenRequest());
        newUser.setCredential(scenario.getCredential());

        return newUser;
    }


    @Test
    public void should_be_able_to_acquire_token() {

        james.attemptsTo(
                clearCache,
                clickDone,
                acquireToken,
                clickDone,
                readCache);
        int expectedCacheCount = james.asksFor(ExpectedCacheItemCount.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCount)));
    }

    @Test
    public void should_be_able_to_acquire_token_prompt_always() {

        james.attemptsTo(
                clearCache,
                clickDone,
                acquireToken.withPrompt("Always"),
                clickDone,
                readCache);

        int expectedCacheCount = james.asksFor(ExpectedCacheItemCount.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCount)));

    }

    @Test
    public void should_be_able_to_acquire_token_with_login_hint() {

        james.attemptsTo(
                clearCache,
                clickDone,
                acquireToken.withUserIdentifier(james.getCredential().userName),
                clickDone,
                readCache);
        int expectedCacheCount = james.asksFor(ExpectedCacheItemCount.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCount)));
    }

    @Test
    public void should_be_able_to_acquire_token_prompt_null() {

        james.attemptsTo(
                clearCache,
                clickDone,
                acquireToken.withPrompt(null),
                clickDone,
                readCache);

        int expectedCacheCount = james.asksFor(ExpectedCacheItemCount.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCount)));

    }

    @Test
    public void should_be_able_to_acquire_token_prompt_auto() {

        james.attemptsTo(
                clearCache,
                clickDone,
                acquireToken.withPrompt("Auto"),
                clickDone,
                readCache);

        int expectedCacheCount = james.asksFor(ExpectedCacheItemCount.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCount)));

    }

    @Test
    public void should_be_able_to_acquire_token_refresh_auto() {

        //clear cache
        givenThat(james).wasAbleTo(clearCache, clickDone, readCache);
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(0)));

        givenThat(james).wasAbleTo(
                clickDone,
                acquireToken.withPrompt("Auto").withUserIdentifier(james.getTokenRequest().getUserIdentitfier()),
                clickDone,
                readCache);
        int expectedCacheCount = james.asksFor(ExpectedCacheItemCount.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCount)));

        //Store the access token temporarily to compare later
        String accessToken1 = james.asksFor(AccessToken.displayed());
        james.attemptsTo(clickDone);

        when(james).attemptsTo(
                acquireToken.withPrompt("Always").withUserIdentifier(null),
                clickDone,
                readCache);
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCount)));
        and(james).should(seeThat(AccessToken.displayed(), not(accessToken1)));

    }

}
