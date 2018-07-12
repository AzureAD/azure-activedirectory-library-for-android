//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.test.automation.ui.microsoftapps.authenticator;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

import io.appium.java_client.MobileBy;

public class Main {

    public static Target ADD_ACCOUNT_BUTTON = Target.the("Add Account Button").located(By.id("com.azure.authenticator:id/button_add_work_account"));
    public static Target ZERO_ACCOUNTS_ADD_ACCOUNT_BUTTON = Target.the("Zero Accounts - Add First Account Button").located(By.id("com.azure.authenticator:id/zero_accounts_add_account_button"));
    public static Target MENU_BUTTON = Target.the("Menu Button").located(By.id("com.azure.authenticator:id/menu_overflow"));
    public static Target SETTINGS_MENU_BUTTON = Target.the("Settings Button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.TextView\").text(\"Settings\")"));
    public static Target EDIT_ACCOUNTS_MENU_BUTTON = Target.the("Edit accounts button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.TextView\").text(\"Edit accounts\")"));
    public static Target ADD_ACCOUNT_MENU_BUTTON = Target.the("Add account button").located(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.TextView\").text(\"Add account\")"));


}
