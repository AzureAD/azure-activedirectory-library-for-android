package com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

import io.appium.java_client.MobileBy;

public class SignInPagePassword {
    public static Target PASSWORD = Target.the("Password field").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.EditText\").instance(0)"));
    public static Target NEXT_BUTTON = Target.the("Next button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(1)"));
    public static Target BACK_BUTTON = Target.the("Next button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(0)"));
    //public static Target PASSWORD_CLOUD = Target.the("Password field").located(By.id("i0118"));
    public static Target PASSWORD_CLOUD = Target.the("Password field").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.EditText\").index(4)"));

}
