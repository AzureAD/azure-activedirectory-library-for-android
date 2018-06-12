package com.microsoft.identity.common.test.automation.questions;

import com.microsoft.identity.common.test.automation.model.AuthenticationResult;
import com.microsoft.identity.common.test.automation.model.ResultsMapper;
import com.microsoft.identity.common.test.automation.ui.Results;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.questions.Text;

public class AccessTokenFromAuthenticationResult implements Question<String> {
    @Override
    public String answeredBy(Actor actor) {
        String results = Text.of(Results.RESULT_FIELD).viewedBy(actor).asString();
        AuthenticationResult readCacheResult = ResultsMapper.GetAuthenticationResultFromString(results);
        return readCacheResult.accessToken;
    }

    public static Question<String>  displayed(){
        return new AccessTokenFromAuthenticationResult();
    }
}
