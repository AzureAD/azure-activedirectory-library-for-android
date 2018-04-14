package com.microsoft.aad.automation.testapp.AutomationTests;


import static com.microsoft.aad.automation.testapp.AutomationTests.Controls.*;

class LG_G5 implements Device {
    public void open_app_view(){
        pressHome();
        pressHome();
    }

    public void open_app(String appName){
        open_app_view();
        if(!text_exists(appName)){
            swipe_right();
        }
        click(appName,null,true);
    }

    public void open_settings(){
        open_app("Settings");
    }

    public void clear_cache(String appName){
        open_app("Settings");
        click("General",null,true);
        find_click("Apps");
        find_click(appName);
        click("Storage", null, true);
        click("Clear cache", null, true);
        bypass_prompt();
    }

    public void clear_data(String appName){
        open_app("Settings");
        click("General",null,true);
        find_click("Apps");
        find_click(appName);
        click("Storage", null, true);
        click("Clear data", null, true);
        bypass_prompt();
    }
}
