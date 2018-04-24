package com.microsoft.identity.common.test.automation.ui.googleplaystore;

import net.serenitybdd.screenplay.targets.Target;

import io.appium.java_client.MobileBy;

public class AppDetailView {
    public static Target INSTALL_BUTTON = Target.the("Install button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(0)"));
    public static Target OPEN_BUTTON = Target.the("Install button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(1)"));


}