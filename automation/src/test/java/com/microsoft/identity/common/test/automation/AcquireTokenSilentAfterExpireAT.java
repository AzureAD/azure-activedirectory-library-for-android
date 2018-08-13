package com.microsoft.identity.common.test.automation;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.ClickDone;
import com.microsoft.identity.common.test.automation.model.Constants;
import com.microsoft.identity.common.test.automation.model.TokenCacheItemReadResult;
import com.microsoft.identity.common.test.automation.questions.AccessToken;
import com.microsoft.identity.common.test.automation.questions.ExpectedCacheItemCount;
import com.microsoft.identity.common.test.automation.questions.ExpectedCacheItemCountWithFoci;
import com.microsoft.identity.common.test.automation.questions.TokenCacheItemCount;
import com.microsoft.identity.common.test.automation.questions.TokenCacheItemFromResult;
import com.microsoft.identity.common.test.automation.tasks.AcquireToken;
import com.microsoft.identity.common.test.automation.tasks.AcquireTokenSilent;
import com.microsoft.identity.common.test.automation.tasks.ClearCache;
import com.microsoft.identity.common.test.automation.tasks.ExpireAccessToken;
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

import static net.serenitybdd.screenplay.GivenWhenThen.andThat;
import static net.serenitybdd.screenplay.GivenWhenThen.givenThat;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.GivenWhenThen.then;
import static net.serenitybdd.screenplay.GivenWhenThen.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(SerenityParameterizedRunner.class)
@WithTag("requires:none")
public class AcquireTokenSilentAfterExpireAT {

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

    @Steps
    AcquireTokenSilent acquireTokenSilent;

    @Steps
    AcquireToken acquireToken;

    @Steps
    ExpireAccessToken expireAccessToken;

    @Steps
    ReadCache readCache;

    @Steps
    ClearCache clearCache;

    @Steps
    ClickDone clickDone;

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

    public AcquireTokenSilentAfterExpireAT(String federationProvider) {
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
        newUser.setSilentTokenRequest(scenario.getSilentTokenRequest());
        newUser.setCredential(scenario.getCredential());

        return newUser;
    }


    @Test
    public void should_be_able_to_new_access_token_after_expiry_on_silent() {

        //clear cache
        givenThat(james).wasAbleTo(clearCache, clickDone, readCache);
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(0)));

        givenThat(james).wasAbleTo(
                clickDone,
                acquireToken,
                clickDone,
                readCache);
        int expectedCacheCount = james.asksFor(ExpectedCacheItemCount.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCount)));

        String accessToken1 = james.asksFor(AccessToken.displayed());
        TokenCacheItemReadResult result = james.asksFor(TokenCacheItemFromResult.displayed());

        givenThat(james).wasAbleTo(
                clickDone,
                expireAccessToken.withTokenCacheItem(result),
                clickDone,
                acquireTokenSilent.withUniqueId(result.uniqueUserId),
                clickDone,
                readCache);

        then(james).should(seeThat(AccessToken.displayed(), not(accessToken1)));

    }

    @Test
    public void should_be_able_to_new_access_token_after_expiry_on_silent_frt() {

        //clear cache
        givenThat(james).wasAbleTo(clearCache, clickDone, readCache);
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(0)));

        givenThat(james).attemptsTo(
                clickDone,
                acquireToken
                        .withClientId(Constants.OUTLOOK_CLIENT_ID)
                        .withRedirectUri(Constants.OUTLOOK_REDIRECT_URI),
                clickDone,
                readCache);

        int expectedCacheCountToken = james.asksFor(ExpectedCacheItemCountWithFoci.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCountToken)));

        String accessToken1 = james.asksFor(AccessToken.displayed());
        TokenCacheItemReadResult result = james.asksFor(TokenCacheItemFromResult.displayed());

        givenThat(james).wasAbleTo(
                clickDone,
                acquireTokenSilent
                        .withUniqueId(result.uniqueUserId)
                        .withClientId(Constants.ONE_DRIVE_CLIENT_ID)
                        .withRedirectUri(Constants.ONE_DRIVE_REDIRECT_URI),
                clickDone,
                readCache);

        int expectedFinalCacheCountCount = james.asksFor(ExpectedCacheItemCount.displayed()) + expectedCacheCountToken;
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedFinalCacheCountCount)));

        givenThat(james).wasAbleTo(
                clickDone,
                expireAccessToken
                        .withTokenCacheItem(result)
                        .withClientId(Constants.OUTLOOK_CLIENT_ID),
                clickDone);

        andThat(james).wasAbleTo(
                acquireTokenSilent
                        .withUniqueId(result.uniqueUserId)
                        .withClientId(Constants.OUTLOOK_CLIENT_ID)
                        .withRedirectUri(Constants.OUTLOOK_REDIRECT_URI),
                clickDone,
                readCache);

        then(james).should(seeThat(AccessToken.displayed(Constants.OUTLOOK_CLIENT_ID), not(accessToken1)));

    }


    @Test
    public void should_be_able_to_new_access_token_after_both_frt_tokens_expiry_on_silent() {

        //clear cache
        givenThat(james).wasAbleTo(clearCache, clickDone, readCache);
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(0)));

        givenThat(james).wasAbleTo(
                clickDone,
                acquireToken
                        .withClientId(Constants.OUTLOOK_CLIENT_ID)
                        .withRedirectUri(Constants.OUTLOOK_REDIRECT_URI),
                clickDone,
                readCache);

        int expectedCacheCountToken = james.asksFor(ExpectedCacheItemCountWithFoci.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCountToken)));

        String accessToken1 = james.asksFor(AccessToken.displayed(Constants.OUTLOOK_CLIENT_ID));
        TokenCacheItemReadResult result = james.asksFor(TokenCacheItemFromResult.displayed());

        givenThat(james).wasAbleTo(
                clickDone,
                acquireTokenSilent
                        .withClientId(Constants.ONE_DRIVE_CLIENT_ID)
                        .withRedirectUri(Constants.ONE_DRIVE_REDIRECT_URI),
                clickDone,
                readCache
        );

        int expectedFinalCacheCountCount = james.asksFor(ExpectedCacheItemCount.displayed()) + expectedCacheCountToken;
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedFinalCacheCountCount)));

        String accessToken2 = james.asksFor(AccessToken.displayed(Constants.ONE_DRIVE_CLIENT_ID));

        //Expire all access tokens
        givenThat(james).wasAbleTo(
                clickDone,
                expireAccessToken
                        .withTokenCacheItem(result)
                        .withClientId(Constants.OUTLOOK_CLIENT_ID),
                clickDone);

        andThat(james).wasAbleTo(
                expireAccessToken
                        .withTokenCacheItem(result)
                        .withClientId(Constants.ONE_DRIVE_CLIENT_ID),
                clickDone);


        // Acquire token silent and verify a new access token is returned
        when(james).attemptsTo(
                acquireTokenSilent
                        .withUniqueId(result.uniqueUserId)
                        .withClientId(Constants.OUTLOOK_CLIENT_ID)
                        .withRedirectUri(Constants.OUTLOOK_REDIRECT_URI),
                clickDone,
                readCache);

        then(james).should(seeThat(AccessToken.displayed(Constants.OUTLOOK_CLIENT_ID), not(accessToken1)));

        // Acquire token silent and verify a new access token is returned
        when(james).attemptsTo(
                clickDone,
                acquireTokenSilent
                        .withUniqueId(result.uniqueUserId)
                        .withClientId(Constants.ONE_DRIVE_CLIENT_ID)
                        .withRedirectUri(Constants.ONE_DRIVE_CLIENT_ID),
                clickDone,
                readCache);

        then(james).should(seeThat(AccessToken.displayed(Constants.ONE_DRIVE_CLIENT_ID), not(accessToken2)));

    }

}
