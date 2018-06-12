package com.microsoft.identity.common.test.automation;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.ClickDone;
import com.microsoft.identity.common.test.automation.questions.AccessTokenFromAuthenticationResult;
import com.microsoft.identity.common.test.automation.tasks.AcquireToken;
import com.microsoft.identity.common.test.automation.tasks.AcquireTokenSilent;
import com.microsoft.identity.common.test.automation.utility.Scenario;
import com.microsoft.identity.common.test.automation.utility.TestConfigurationQuery;

import net.serenitybdd.junit.runners.SerenityParameterizedRunner;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
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

import static net.serenitybdd.screenplay.GivenWhenThen.givenThat;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.GivenWhenThen.then;
import static net.serenitybdd.screenplay.GivenWhenThen.when;
import static org.hamcrest.Matchers.not;

/**
 * Test case : https://identitydivision.visualstudio.com/IDDP/_workitems/edit/98555
 */

@RunWith(SerenityParameterizedRunner.class)
public class AcquireTokenSilentForceRefreshWithBroker {

    @TestData
    public static Collection<Object[]> FederationProviders(){


        return Arrays.asList(new Object[][]{
                {"ADFSv2"}//,
                /*)
                {"ADFSv3"},
                {"ADFSv4"},
                {"PingFederate"},
                {"Shibboleth"}
                */

        });

    }

    @Steps
    AcquireTokenSilent acquireTokenSilent;

    @Steps
    AcquireToken acquireToken;

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

    public AcquireTokenSilentForceRefreshWithBroker(String federationProvider){
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
        newUser.getTokenRequest().setRedirectUri("msauth://com.microsoft.aad.automation.testapp.adal/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D");
        newUser.setSilentTokenRequest(scenario.getSilentTokenRequest());
        newUser.getSilentTokenRequest().setRedirectUri("msauth://com.microsoft.aad.automation.testapp.adal/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D");
        newUser.setCredential(scenario.getCredential());

        return newUser;
    }


    @Test
    public void should_be_able_to_acquire_token_and_then_acquire_silent_with_force_refresh() {

        givenThat(james).wasAbleTo(
                acquireToken.withBroker()
        );

        String accessToken1 = james.asksFor(AccessTokenFromAuthenticationResult.displayed());

        james.attemptsTo(clickDone);

        when(james).attemptsTo(
                acquireTokenSilent.withUserIdentifier(james.getCredential().userName).withForceRefresh()
        );

        then(james).should(seeThat(AccessTokenFromAuthenticationResult.displayed(), not(accessToken1)));


    }

}
