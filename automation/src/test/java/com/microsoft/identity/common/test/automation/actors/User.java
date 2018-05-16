package com.microsoft.identity.common.test.automation.actors;

import com.google.gson.Gson;
import com.microsoft.identity.common.test.automation.model.TokenCacheItemReadResult;
import com.microsoft.identity.common.test.automation.utility.Credential;
import com.microsoft.identity.common.test.automation.utility.TokenRequest;

import net.serenitybdd.screenplay.Actor;

public class User extends Actor {

    private TokenRequest tokenRequest;
    private TokenRequest silentTokenRequest;
    private String federationProvider;
    private Credential credential;
    private TokenCacheItemReadResult cacheResult;

    public void setTokenRequest(TokenRequest tokenRequest){
        this.tokenRequest = tokenRequest;
    }

    public TokenRequest getTokenRequest(){
        return this.tokenRequest;
    }

    public void setSilentTokenRequest(TokenRequest silentTokenRequest){
        this.silentTokenRequest = silentTokenRequest;
    }

    public TokenRequest getSilentTokenRequest(){
        return this.silentTokenRequest;
    }


    public String getTokenRequestAsJson() {
        Gson gson = new Gson();
        String requestJson = gson.toJson(this.tokenRequest);
        return requestJson;
    }

    public String getSilentTokenRequestAsJson() {
        Gson gson = new Gson();
        String requestJson = gson.toJson(this.silentTokenRequest);
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


    public TokenCacheItemReadResult getCacheResult() {
        return cacheResult;
    }

    public void setCacheResult(TokenCacheItemReadResult cacheResult) {
        this.cacheResult = cacheResult;
    }

    public String getCacheResultAsJson(){
        String cacheJson = new Gson().toJson(this.cacheResult);
        return cacheJson;
    }
}
