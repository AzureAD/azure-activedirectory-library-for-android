package com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

import io.appium.java_client.MobileBy;

public class SignInPageUserNameBroker {

    public static Target USERNAME = Target.the("Username field").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.EditText\").instance(0)"));
    public static Target NEXT_BUTTON = Target.the("Next button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(1)"));

}
