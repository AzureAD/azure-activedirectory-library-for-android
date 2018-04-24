package com.microsoft.identity.common.test.automation.interactions;

import net.serenitybdd.core.steps.Instrumented;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.webdriver.WebDriverFacade;

import java.time.Duration;

import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;

public class Swipe implements Interaction {

    private PointOption start;
    private PointOption end;

    public Swipe(PointOption start, PointOption end){
        this.start = start;
        this.end = end;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        WebDriverFacade facade = (WebDriverFacade) BrowseTheWeb.as(actor).getDriver();
        AndroidDriver androidDriver = (AndroidDriver)facade.getProxiedDriver();

        TouchAction actions = new TouchAction(androidDriver);

        actions.press(start)
                .waitAction(WaitOptions.waitOptions(Duration.ofMillis(1000)))
                .moveTo(end)
                .release()
                .perform();

    }

    public static Swipe points(PointOption start, PointOption end){
        return Instrumented.instanceOf(Swipe.class).withProperties(start, end);
    }

}

