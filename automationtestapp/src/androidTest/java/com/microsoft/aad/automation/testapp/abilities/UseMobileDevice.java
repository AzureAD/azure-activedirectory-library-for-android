package com.microsoft.aad.automation.testapp.abilities;


import com.microsoft.aad.automation.testapp.devices.MobileDevice;

import net.serenitybdd.screenplay.Ability;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.RefersToActor;

public class UseMobileDevice implements Ability, RefersToActor{

    private Actor actor;
    private MobileDevice mobileDevice;

    protected UseMobileDevice(MobileDevice mobileDevice){
        this.mobileDevice = mobileDevice;
    }

    public static UseMobileDevice with(MobileDevice device) { return new UseMobileDevice(device); }

    @Override
    public <T extends Ability> T asActor(Actor actor) {
        this.actor = actor;
        return (T) this;
    }

    public static UseMobileDevice as(Actor actor) {
        if (actor.abilityTo(UseMobileDevice.class) == null) {
            throw new IllegalArgumentException("The supplied actor cannot use a mobile device");
        }
        return actor.abilityTo(UseMobileDevice.class).asActor(actor);
    }

    public MobileDevice getMobileDevice(){
        return mobileDevice;
    }

}
