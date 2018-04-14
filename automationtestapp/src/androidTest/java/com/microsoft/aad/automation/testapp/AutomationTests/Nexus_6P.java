package com.microsoft.aad.automation.testapp.AutomationTests;

import static com.microsoft.aad.automation.testapp.AutomationTests.Controls.*;


public class Nexus_6P implements Device {
    public void open_app_view(){
        click_by_description("Apps");
        scroll_up();
    }

    public void open_app(String appName){
        open_app_view();
        find_click(appName);
    }

    public void clear_cache(String appName){
        open_app("Settings");
        find_click("Apps");
        find_click(appName);
        click("Storage", null, true);
        click("Clear cache", null, true);
        bypass_prompt();
    }

    public void clear_data(String appName){
        open_app("Settings");
        find_click("Apps");
        find_click(appName);
        click("Storage", null, true);
        click("Clear data", null, true);
        bypass_prompt();
    }
}
