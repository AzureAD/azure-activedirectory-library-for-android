package com.microsoft.aad.automation.testapp;

import com.microsoft.aad.automation.testapp.actors.User;

import net.serenitybdd.junit.runners.SerenityRunner;
import net.serenitybdd.screenplay.Actor;

import org.junit.runner.RunWith;

@RunWith(SerenityRunner.class)
public class SerenityTest {

    private User james = User.named("james");

    public Actor getJames() {
        return james;
    }
}
