package com.microsoft.aad.automation.testapp.AutomationTests;

// class to return device to client.
public class DeviceFactory {
    public static Device newDevice(){
        String deviceName = android.os.Build.MODEL;
        Device device = null;
        switch(deviceName){
            case "LG-H860":
                device = new LG_G5();
                break;

            case "Nexus 6P":
                device = new Nexus_6P();
                break;
        }
        return device;
    }
}
