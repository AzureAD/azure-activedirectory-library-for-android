package com.microsoft.identity.common.test.automation.ui.identityproviders.AADV1;

import net.serenitybdd.screenplay.targets.Target;

import io.appium.java_client.MobileBy;

public class MDMEnrollChallenge {
    public static Target ENROLL_BUTTON = Target.the("Enroll button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").resourceId(\"ca-enroll-button\")"));
}