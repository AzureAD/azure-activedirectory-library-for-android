package com.microsoft.identity.common.test.automation;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.ClickDone;
import com.microsoft.identity.common.test.automation.model.TokenCacheItemReadResult;
import com.microsoft.identity.common.test.automation.questions.AccessToken;
import com.microsoft.identity.common.test.automation.questions.ExpectedCacheItemCount;
import com.microsoft.identity.common.test.automation.questions.TokenCacheItemCount;
import com.microsoft.identity.common.test.automation.questions.TokenCacheItemFromResult;
import com.microsoft.identity.common.test.automation.tasks.AcquireToken;
import com.microsoft.identity.common.test.automation.tasks.AcquireTokenSilent;
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

import static net.serenitybdd.screenplay.GivenWhenThen.givenThat;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.GivenWhenThen.then;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Test case : https://identitydivision.visualstudio.com/IDDP/_workitems/edit/98555
 */

@RunWith(SerenityParameterizedRunner.class)
@WithTag("requires:none")
public class AcquireTokenAfterExpireAT {

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
    ExpireAccessToken expireAccessToken;

    @Steps
    ReadCache readCache;

    @Steps
    ClickDone clickDone;

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

    public AcquireTokenAfterExpireAT(String federationProvider){
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
    public void should_be_able_to_new_access_token_after_expiry_on_silent() {

        givenThat(james).wasAbleTo(
                acquireToken,
                clickDone,
                readCache);
        int expectedCacheCount = james.asksFor(ExpectedCacheItemCount.displayed());
        then(james).should(seeThat(TokenCacheItemCount.displayed(), is(expectedCacheCount)));

        String accessToken1 = james.asksFor(AccessToken.displayed());
        TokenCacheItemReadResult cacheItem = james.asksFor(TokenCacheItemFromResult.displayed());

        givenThat(james).wasAbleTo(
                clickDone,
                expireAccessToken.withTokenCacheItem(cacheItem),
                clickDone,
                acquireToken
                        .tokenExists(true)
                        .withUserIdentifier(cacheItem.displayableId),
                clickDone,
                readCache);

        then(james).should(seeThat(AccessToken.displayed(), not(accessToken1)));


    }

}
