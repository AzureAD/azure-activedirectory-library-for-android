package com.microsoft.identity.common.test.automation;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.tasks.AcquireToken;
import com.microsoft.identity.common.test.automation.utility.Scenario;
import com.microsoft.identity.common.test.automation.utility.TestConfigurationQuery;

import net.serenitybdd.junit.runners.SerenityParameterizedRunner;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.annotations.Managed;
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

@RunWith(SerenityParameterizedRunner.class)
public class SerenityTest {

    @TestData
    public static Collection<Object[]> users(){
        TestConfigurationQuery query = new TestConfigurationQuery();
        query.federationProvider = "ADFSv3";
        User user1 = getUser(query);
        query.federationProvider = "ADFSv2";
        User user2 = getUser(query);

        return Arrays.asList(new Object[][]{
                {user1},
                {user2}
        });

    }

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

    private final User james;

    public SerenityTest(User user){
        this.james = user;
    }

    @Before
    public void jamesCanUseAMobileDevice(){

        james.can(BrowseTheWeb.with(hisMobileDevice));
    }

    private static User getUser(TestConfigurationQuery query){

        Scenario scenario = Scenario.GetScenario(query);

        User newUser = User.named("james");
        newUser.setFederationProvider(scenario.getTestConfiguration().getUsers().getFederationProvider());
        newUser.setTokenRequest(scenario.getTokenRequest());
        newUser.setCredential(scenario.getCredential());

        return newUser;
    }


    @Test
    public void should_be_able_to_acquire_token() {

        james.attemptsTo(AcquireToken.with(james.getTokenRequestAsJson()));
        //Thread.sleep(10000);

    }

}
