package com.microsoft.identity.common.test.automation;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.ClickDone;
import com.microsoft.identity.common.test.automation.model.Constants;
import com.microsoft.identity.common.test.automation.model.TokenCacheItemReadResult;
import com.microsoft.identity.common.test.automation.questions.ADALError;
import com.microsoft.identity.common.test.automation.questions.ExpectedCacheItemCount;
import com.microsoft.identity.common.test.automation.questions.ExpectedCacheItemCountWithFoci;
import com.microsoft.identity.common.test.automation.questions.TokenCacheItemCount;
import com.microsoft.identity.common.test.automation.questions.TokenCacheItemFromResult;
import com.microsoft.identity.common.test.automation.tasks.AcquireToken;
import com.microsoft.identity.common.test.automation.tasks.AcquireTokenSilent;
import com.microsoft.identity.common.test.automation.tasks.ClearCache;
import com.microsoft.identity.common.test.automation.tasks.ExpireATAndInvalidateRT;
import com.microsoft.identity.common.test.automation.tasks.ReadCache;
import com.microsoft.identity.common.test.automation.ui.Results;
import com.microsoft.identity.common.test.automation.utility.Scenario;
import com.microsoft.identity.common.test.automation.utility.TestConfigurationQuery;

import net.serenitybdd.junit.runners.SerenityParameterizedRunner;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.waits.WaitUntil;
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
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;
import static org.hamcrest.Matchers.is;

@RunWith(SerenityParameterizedRunner.class)
@WithTag("requires:none")
public class AcquireTokenSilentAfterExpireATAndRT {

    @TestData
    public static Collection<Object[]> FederationProviders(){


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
    ExpireATAndInvalidateRT expireATAndInvalidateRT;

    @Steps
    ReadCache readCache;

    @Steps
    ClickDone clickDone;

    @Steps
    ClearCache clearCache;

    static AppiumDriverLocalService appiumService = null;

    @Managed(driver="Appium")
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

    public AcquireTokenSilentAfterExpireATAndRT(String federationProvider){
        this.federationProvider = federationProvider;
    }

    @Before
    public void jamesCanUseAMobileDevice(){
        TestConfigurationQuery query = new TestConfigurationQuery();
        query.federationProvider = this.federationProvider;
        query.isFederated = true;
        query.userType = "Member";
        james = getUser(query);
        james.can(BrowseTheWeb.with(hisMobileDevice));
    }

    private static User getUser(TestConfigurationQuery query){

        Scenario scenario = Scenario.GetScenario(query);

        User newUser = User.named("james");
        newUser.setFederationProvider(scenario.getTestConfiguration().getUsers().getFederationProvider());
        newUser.setTokenRequest(scenario.getTokenRequest());
        newUser.setSilentTokenRequest(scenario.getSilentTokenRequest());
        newUser.setCredential(scenario.getCredential());

        return newUser;
    }


    @Test
    public void should_not_be_able_access_token_after_at_rt_expiry_on_silent_unique_id() {

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

        TokenCacheItemReadResult cacheItem = james.asksFor(TokenCacheItemFromResult.displayed());

        givenThat(james).wasAbleTo(
                clickDone,
                expireATAndInvalidateRT.withTokenCacheItem(cacheItem),
                clickDone,
                acquireTokenSilent.withUniqueId(cacheItem.uniqueUserId),
                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds());

        then(james).should(seeThat(ADALError.displayed(), is(com.microsoft.identity.common.adal.error.ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED.name())));

        when(james).attemptsTo(clickDone, readCache);
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(0)));

    }

    @Test
    public void should_not_be_able_access_token_after_at_rt_expiry_on_silent_displayable_id() {

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

        TokenCacheItemReadResult cacheItem = james.asksFor(TokenCacheItemFromResult.displayed());
        givenThat(james).wasAbleTo(
                clickDone,
                expireATAndInvalidateRT.withTokenCacheItem(cacheItem),
                clickDone,
                acquireTokenSilent.withUniqueId(cacheItem.displayableId),
                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds());

        then(james).should(seeThat(ADALError.displayed(), is(com.microsoft.identity.common.adal.error.ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED.name())));

        when(james).attemptsTo(clickDone, readCache);
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(0)));

    }

    @Test
    public void should_not_be_able_access_token_after_at_frt_expiry_on_silent() {

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

        givenThat(james).wasAbleTo(
                clickDone,
                acquireTokenSilent
                        .withClientId(Constants.ONE_DRIVE_CLIENT_ID)
                        .withRedirectUri(Constants.ONE_DRIVE_REDIRECT_URI),
                clickDone,
                readCache);

        int expectedFinalCacheCountCount = james.asksFor(ExpectedCacheItemCount.displayed()) + expectedCacheCountToken;
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedFinalCacheCountCount)));

        TokenCacheItemReadResult cacheItem = james.asksFor(TokenCacheItemFromResult.displayed());

        givenThat(james).wasAbleTo(
                clickDone,
                expireATAndInvalidateRT
                        .withTokenCacheItem(cacheItem)
                        .withClientId(Constants.OUTLOOK_CLIENT_ID),
                clickDone);

        andThat(james).wasAbleTo(
                expireATAndInvalidateRT
                        .withTokenCacheItem(cacheItem)
                        .withClientId(Constants.ONE_DRIVE_CLIENT_ID),
                clickDone);

        when(james).attemptsTo(
                acquireTokenSilent.withUniqueId(cacheItem.uniqueUserId),
                WaitUntil.the(Results.RESULT_FIELD, isVisible()).forNoMoreThan(10).seconds());

        then(james).should(seeThat(ADALError.displayed(), is(com.microsoft.identity.common.adal.error.ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED.name())));

        when(james).attemptsTo(clickDone, readCache);
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is((expectedCacheCountToken == 20) ? 12 : 8)));

    }


}
