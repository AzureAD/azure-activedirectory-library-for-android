package com.microsoft.identity.common.test.automation;

import com.microsoft.identity.common.test.automation.DeviceModes.DozeOff;
import com.microsoft.identity.common.test.automation.DeviceModes.DozeOn;
import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.ClickDone;

import com.microsoft.identity.common.test.automation.tasks.AcquireTokenSilent;
import com.microsoft.identity.common.test.automation.tasks.ReadCache;
import com.microsoft.identity.common.test.automation.utility.Scenario;
import com.microsoft.identity.common.test.automation.utility.TestConfigurationQuery;

import net.serenitybdd.core.Serenity;
import net.serenitybdd.junit.runners.SerenityParameterizedRunner;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.annotations.Managed;
import net.thucydides.core.annotations.Steps;
import net.thucydides.core.util.PropertiesFileLocalPreferences;
import net.thucydides.junit.annotations.TestData;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;


@RunWith(SerenityParameterizedRunner.class)
public class SilentAuthDozeModePromptAuto {

    @TestData
    public static Collection<Object[]> FederationProviders(){


        return Arrays.asList(new Object[][]{
                {"ADFSv2"}
        });

    }

    @Managed(driver="Appium")
    WebDriver hisMobileDevice;
    public static Process appium;

    @BeforeClass
    public static void  startAppiumServer_with_relaxed_security() throws java.io.IOException {
        appium = Runtime.getRuntime().exec(
                "cmd /c start cmd.exe /C appium --relaxed-security"
        );
    }

    @AfterClass
    public static void stopAppiumServer() throws java.io.IOException{
        appium.destroy();
        Runtime.getRuntime().exec("taskkill /f /im node.exe");
    }


    private User james;
    private String federationProvider;

    @Steps
    DozeOn DozeOn;

    @Steps
    DozeOff DozeOff;

    @Steps
    AcquireTokenSilent acquireTokenSilent;

    @Steps
    ReadCache readCache;

    @Steps
    ClickDone clickDone;

    public SilentAuthDozeModePromptAuto(String federationProvider){
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
        newUser.setCredential(scenario.getCredential());

        return newUser;
    }

    @Test
    public void set_doze_on() {
        james.attemptsTo(
                DozeOn,
                acquireTokenSilent.withPrompt("Auto"),
                DozeOff
        );
    }

}
