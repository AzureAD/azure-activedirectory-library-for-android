package com.microsoft.aad.automation.testapp.actors;

import net.serenitybdd.screenplay.Actor;

public class User extends Actor {
    public User(String name) {
        super(name);
    }

    public static User named(String name){
        return new User(name);
    }

}
