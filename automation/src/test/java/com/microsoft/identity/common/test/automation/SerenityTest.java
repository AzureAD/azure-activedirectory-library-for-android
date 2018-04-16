package com.microsoft.identity.common.test.automation;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.interactions.PressHome;

import net.serenitybdd.screenplay.abilities.BrowseTheWeb;

import net.serenitybdd.junit.runners.SerenityRunner;
import net.thucydides.core.annotations.Managed;
import net.thucydides.core.annotations.Steps;
import net.thucydides.core.util.PropertiesFileLocalPreferences;
import net.thucydides.core.webdriver.appium.AppiumConfiguration;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

@RunWith(SerenityRunner.class)
public class SerenityTest {

    private User james = User.named("james");

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
        james.can(BrowseTheWeb.with(hisMobileDevice));
    }

    @Test
    public void should_be_able_to_press_home(){
        james.attemptsTo(pressHome);
    }

}
