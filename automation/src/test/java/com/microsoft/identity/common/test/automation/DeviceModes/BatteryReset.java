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

public class BatteryReset implements Interaction {
    @Override
    public <T extends Actor> void performAs(T actor) {
        WebDriverFacade facade = (WebDriverFacade)BrowseTheWeb.as(actor).getDriver();
        AndroidDriver driver = (AndroidDriver)facade.getProxiedDriver();

        List<String> battery_args = Arrays.asList("battery", "reset");

        Map<String, Object> battery = ImmutableMap.of(
                "command", "dumpsys",
                "args", battery_args
        );

        driver.executeScript("mobile: shell", battery);
    }
}
