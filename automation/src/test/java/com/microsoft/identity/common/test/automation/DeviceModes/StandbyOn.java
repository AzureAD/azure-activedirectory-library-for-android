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

public class StandbyOn implements Interaction {
    @Override
    public <T extends Actor> void performAs(T actor) {

        WebDriverFacade facade = (WebDriverFacade)BrowseTheWeb.as(actor).getDriver();
        AndroidDriver driver = (AndroidDriver)facade.getProxiedDriver();

        String packageName = "com.microsoft.aad.automation.testapp.adal";

        List<String> battery_args = Arrays.asList("battery", "unplug");
        List<String> inactive_args = Arrays.asList("set-inactive", packageName,"true");

        Map<String, Object> battery = ImmutableMap.of(
                "command", "dumpsys",
                "args", battery_args
        );

        Map<String, Object> inactive = ImmutableMap.of(
                "command", "am",
                "args", inactive_args
        );

        driver.executeScript("mobile: shell", battery);
        driver.executeScript("mobile: shell", inactive);
    }
}