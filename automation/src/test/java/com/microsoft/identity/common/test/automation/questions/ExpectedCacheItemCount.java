package com.microsoft.identity.common.test.automation.questions;

import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.test.automation.model.ReadCacheResult;
import com.microsoft.identity.common.test.automation.ui.Results;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.questions.Text;

public class ExpectedCacheItemCount implements Question<Integer> {

    private static final int COMMON_CACHE_COUNT = 8;
    private static final int ADAL_LEGACY_CACHE_COUNT = 6;

    @Override
    public Integer answeredBy(Actor actor) {
        String results = Text.of(Results.RESULT_FIELD).viewedBy(actor).asString();
        ReadCacheResult readCacheResult = (ReadCacheResult) ObjectMapper.deserializeJsonStringToObject(results, ReadCacheResult.class);
        if(readCacheResult!=null) {
            return readCacheResult.isCommonCache ? COMMON_CACHE_COUNT : ADAL_LEGACY_CACHE_COUNT;
        }
        return COMMON_CACHE_COUNT;
    }

    public static Question<Integer> displayed(){
        return new ExpectedCacheItemCount();
    }
}
