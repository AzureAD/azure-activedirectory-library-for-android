package com.microsoft.aad.automation.testapp;

import net.serenitybdd.junit.runners.SerenityRunner;
import net.serenitybdd.screenplay.Actor;

import org.junit.runner.RunWith;

@RunWith(SerenityRunner.class)
public class SerenityTest {

    private Actor james = Actor.named("james");

    public Actor getJames() {
        return james;
    }
}
