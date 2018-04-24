package com.microsoft.identity.common.test.automation.ui;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class ChooseCertificate {
    public static Target CERTIFICATE_RADIO_BUTTON = Target.the("Automation certificate radio button").located(By.id("com.android.keychain:id/cert_item_selected"));
    public static Target INSTALL_BUTTON = Target.the("Install button").located(By.id("com.android.keychain:id/cert_chooser_install_button"));
    public static Target DENY_LINK_BUTTON = Target.the("Deny link button").located(By.id("android:id/button2"));
    public static Target ALLOW_LINK_BUTTON = Target.the("Allow link button").located(By.id("android:id/button1"));


}
