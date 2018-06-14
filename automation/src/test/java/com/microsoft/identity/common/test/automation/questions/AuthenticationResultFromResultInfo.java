package com.microsoft.identity.common.test.automation.questions;

import com.microsoft.identity.common.test.automation.model.AuthenticationResult;
import com.microsoft.identity.common.test.automation.model.ResultsMapper;
import com.microsoft.identity.common.test.automation.ui.Results;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.questions.Text;

public class AuthenticationResultFromResultInfo implements Question<AuthenticationResult> {
    @Override
    public AuthenticationResult answeredBy(Actor actor) {
        String results = Text.of(Results.RESULT_FIELD).viewedBy(actor).asString();
        AuthenticationResult authenticationResult = ResultsMapper.GetAuthenticationResultFromString(results);
        return authenticationResult;
    }

    public static Question<AuthenticationResult>  displayed(){
        return new AuthenticationResultFromResultInfo();
    }
}