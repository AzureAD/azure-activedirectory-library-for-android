package com.microsoft.aad.automation.testapp.devices;

import android.os.RemoteException;
import android.support.test.uiautomator.UiDevice;

public class MobileDevice {

    private UiDevice device;

    public void pressHome(){
        device.pressHome();
    }

    public void openQuickSettings(){
        device.openQuickSettings();
    }

    public void pressBack(){
        device.pressBack();
    }

    public void freezeRotation() throws RemoteException {
        device.freezeRotation();
    }



}
