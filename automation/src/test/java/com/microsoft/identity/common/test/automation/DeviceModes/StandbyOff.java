package com.microsoft.identity.common.test.automation.DeviceModes;

import com.google.common.collect.ImmutableMap;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.webdriver.WebDriverFacade;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.appium.java_client.android.AndroidDriver;

public class StandbyOff implements Interaction {
    @Override
    public <T extends Actor> void performAs(T actor) {

        WebDriverFacade facade = (WebDriverFacade)BrowseTheWeb.as(actor).getDriver();
        AndroidDriver driver = (AndroidDriver)facade.getProxiedDriver();

        List<String> Args = Arrays.asList("battery", "unplug");

        Map<String, Object> fullCommand = ImmutableMap.of(
                "command", "dumpsys",
                "args", Args
        );
        driver.executeScript("mobile: shell", fullCommand);

    }
}
