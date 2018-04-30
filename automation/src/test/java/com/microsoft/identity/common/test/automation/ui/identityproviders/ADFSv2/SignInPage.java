package com.microsoft.identity.common.test.automation.ui.identityproviders.ADFSv2;


import net.serenitybdd.screenplay.targets.Target;

import io.appium.java_client.MobileBy;

public class SignInPage {
    public static Target USERNAME_FIELD = Target.the("Username field").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.EditText\").instance(0)"));
    public static Target PASSWORD_FIELD = Target.the("Password field").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.EditText\").instance(1)"));
    public static Target SIGN_IN_BUTTON = Target.the("SignIn button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(0)"));
}
