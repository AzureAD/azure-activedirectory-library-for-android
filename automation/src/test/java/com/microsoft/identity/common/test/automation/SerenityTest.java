package com.microsoft.identity.common.test.automation;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.PressHome;
import com.microsoft.identity.common.test.automation.tasks.AcquireToken;
import com.microsoft.identity.common.test.automation.utility.Scenario;
import com.microsoft.identity.common.test.automation.utility.TestConfigurationQuery;

import net.serenitybdd.junit.runners.SerenityRunner;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.annotations.Managed;
import net.thucydides.core.annotations.Steps;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

import io.appium.java_client.service.local.AppiumDriverLocalService;

@RunWith(SerenityRunner.class)
public class SerenityTest {

    private User james;


    static AppiumDriverLocalService appiumService = null;

    @Managed(driver="Appium")
    WebDriver hisMobileDevice;

    @Steps
    PressHome pressHome;

    @BeforeClass
    public static void startAppiumServer() throws IOException {
        appiumService = AppiumDriverLocalService.buildDefaultService();
        appiumService.start();
    }

    @AfterClass
    public static void stopAppiumServer() {
        appiumService.stop();
    }


    @Before
    public void jamesCanUseAMobileDevice(){

        TestConfigurationQuery query = new TestConfigurationQuery();
        query.federationProvider = "ADFSv3";
        Scenario scenarioADFSv3 = Scenario.GetScenario(query);

        james = User.named("james");
        james.setFederationProvider(scenarioADFSv3.getTestConfiguration().getUsers().getFederationProvider());
        james.setTokenRequest(scenarioADFSv3.getTokenRequest());
        james.setCredential(scenarioADFSv3.getCredential());

        james.can(BrowseTheWeb.with(hisMobileDevice));
    }

    @Test
    public void should_be_able_to_acquire_token() throws InterruptedException {

        james.attemptsTo(AcquireToken.with(james.getTokenRequestAsJson()));
        //Thread.sleep(10000);

    }

}
