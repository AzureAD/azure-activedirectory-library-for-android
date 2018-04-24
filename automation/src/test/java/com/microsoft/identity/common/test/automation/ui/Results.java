package com.microsoft.identity.common.test.automation.ui;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;


public class Results {
    public static Target RESULT_FIELD = Target.the("Result Field").located(By.id("com.microsoft.aad.automation.testapp.adal:id/resultInfo"));
    public static Target DONE_BUTTON =  Target.the("Done Button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/resultDone"));
    public static Target LOGS_FIELD =  Target.the("Log Field").located(By.id("com.microsoft.aad.automation.testapp.adal:id/adalLogs"));
}
