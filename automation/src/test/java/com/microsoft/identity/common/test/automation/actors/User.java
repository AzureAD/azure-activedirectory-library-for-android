package com.microsoft.identity.common.test.automation.actors;

import com.google.gson.Gson;
import com.microsoft.identity.common.test.automation.utility.Credential;
import com.microsoft.identity.common.test.automation.utility.TokenRequest;

import net.serenitybdd.screenplay.Actor;

import jdk.nashorn.internal.parser.Token;

public class User extends Actor {

    private TokenRequest tokenRequest;
    private String federationProvider;
    private Credential credential;

    public void setTokenRequest(TokenRequest tokenRequest){
        this.tokenRequest = tokenRequest;
    }

    public TokenRequest getTokenRequest(){
        return this.tokenRequest;
    }

    public String getTokenRequestAsJson() {
        Gson gson = new Gson();
        String requestJson = gson.toJson(this.tokenRequest);
        return requestJson;
    }

    public void setCredential(Credential credential){
        this.credential = credential;
    }

    public Credential getCredential(){
        return this.credential;
    }

    public String getFederationProvider(){
        return this.federationProvider;
    }

    public void setFederationProvider(String federationProvider){
        this.federationProvider = federationProvider;
    }

    public User(String name) {
        super(name);
    }

    public static User named(String name){
        return new User(name);
    }



}
