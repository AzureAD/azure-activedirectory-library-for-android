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

        String packageName = "com.microsoft.aad.automation.testapp.adal";
        List<String> activate_args = Arrays.asList("set-inactive", packageName,"false");
        List<String> activate_args2 = Arrays.asList("get-inactive", packageName);

        Map<String, Object> activate1 = ImmutableMap.of(
                "command", "dumpsys",
                "args", activate_args
        );

        Map<String, Object> activate2 = ImmutableMap.of(
                "command", "am",
                "args", activate_args2
        );

        driver.executeScript("mobile: shell", activate1);
        driver.executeScript("mobile: shell", activate2);
    }
}
