package com.microsoft.identity.common.test.automation.tasks;


import net.serenitybdd.screenplay.Task;

public abstract class SignInUser implements Task {
    public static SignInUser GetSignInUserByFederationProvider(String federationProvider){
        SignInUser signInUserTask = null;

        switch(federationProvider){
            case "ADFSv3":
                signInUserTask = new SignInUserADFSv3();
                break;
            case "ADFSv2":
                signInUserTask = new SignInUserADFSv2();
                break;
        }

        return signInUserTask;
    }
}
