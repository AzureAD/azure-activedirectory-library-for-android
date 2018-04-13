package com.microsoft.aad.automation.testapp;

import com.microsoft.aad.automation.testapp.abilities.UseMobileDevice;
import com.microsoft.aad.automation.testapp.actors.User;
import com.microsoft.aad.automation.testapp.devices.MobileDevice;
import com.microsoft.aad.automation.testapp.interactions.PressHome;

import net.serenitybdd.junit.runners.SerenityRunner;
import net.thucydides.core.annotations.Steps;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SerenityRunner.class)
public class SerenityTest {

    private User james = User.named("james");
    private MobileDevice mobileDevice = null;

    @Steps
    PressHome pressHome;

    @Before
    public void jamesCanUseAMobileDevice(){
        mobileDevice = new MobileDevice();
        james.can(UseMobileDevice.with(mobileDevice));
    }

    @Test
    public void should_be_able_to_press_home(){
        james.attemptsTo(pressHome);
    }

}
