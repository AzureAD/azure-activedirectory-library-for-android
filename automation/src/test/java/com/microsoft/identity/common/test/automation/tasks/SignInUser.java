package com.microsoft.identity.common.test.automation.tasks;


import net.serenitybdd.screenplay.Task;

public abstract class SignInUser implements Task {
    public static SignInUser GetSignInUserByFederationProvider(String federationProvider){
        SignInUser signInUserTask = null;

        switch(federationProvider){
            case "Cloud":
                signInUserTask = new SignInUserCloud();
                break;
            case "ADFSv2":
                signInUserTask = new SignInUserADFSv2();
                break;
            case "ADFSv3":
                signInUserTask = new SignInUserADFSv3();
                break;
            case "ADFSv4":
                signInUserTask = new SignInUserADFSv4();
                break;
            case "PingFederate V8.3":
                signInUserTask = new SignInUserPing();
                break;
            case "Shibboleth":
                signInUserTask = new SignInUserShibboleth();
                break;
            default:
                signInUserTask = new SignInUserADFSv2();
        }

        return signInUserTask;
    }
}
