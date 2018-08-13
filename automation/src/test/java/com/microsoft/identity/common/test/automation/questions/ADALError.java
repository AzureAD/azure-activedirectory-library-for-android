package com.microsoft.identity.common.test.automation.questions;

import com.microsoft.identity.common.test.automation.model.ADALErrorResult;
import com.microsoft.identity.common.test.automation.model.ResultsMapper;
import com.microsoft.identity.common.test.automation.ui.Results;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.questions.Text;

public class ADALError implements Question<String> {
    @Override
    public String answeredBy(Actor actor) {
        String results = Text.of(Results.RESULT_FIELD).viewedBy(actor).asString();
        ADALErrorResult adalErrorResult = ResultsMapper.GetADALErrorResultFromString(results);
        return adalErrorResult.error;
    }

    public static Question<String> displayed(){
        return new ADALError();
    }
}
