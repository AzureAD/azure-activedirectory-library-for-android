package com.microsoft.identity.common.test.automation.interactions;

import net.serenitybdd.core.steps.Instrumented;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.webdriver.WebDriverFacade;

import java.lang.instrument.Instrumentation;

import io.appium.java_client.MobileBy;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidKeyCode;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;

public class ScrollTo implements Interaction {

    private String targetText;

    public ScrollTo(String targetText){
        this.targetText = targetText;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        WebDriverFacade facade = (WebDriverFacade) BrowseTheWeb.as(actor).getDriver();
        AndroidDriver androidDriver = (AndroidDriver)facade.getProxiedDriver();

        TouchAction actions = new TouchAction(androidDriver);

        actions.press(PointOption.point(100, 200))
                .moveTo(PointOption.point(100, 400))
                .release()
                .perform();

    }

    public static ScrollTo text(String targetText){
        return Instrumented.instanceOf(ScrollTo.class).withProperties(targetText);
    }

}

