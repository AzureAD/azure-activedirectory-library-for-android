package com.microsoft.aad.automation.testapp.AutomationTests;

interface Device {
    public void open_app_view();
    public void open_app(String appName);
    public void clear_cache(String appName);
    public void clear_data(String appName);
}
