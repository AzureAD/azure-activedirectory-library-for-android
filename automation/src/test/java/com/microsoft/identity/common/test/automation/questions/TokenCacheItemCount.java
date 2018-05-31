package com.microsoft.identity.common.test.automation.questions;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.model.ReadCacheResult;
import com.microsoft.identity.common.test.automation.model.ResultsMapper;
import com.microsoft.identity.common.test.automation.model.TokenCacheItemReadResult;
import com.microsoft.identity.common.test.automation.ui.Results;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.questions.Text;

import java.util.List;

public class TokenCacheItemCount implements Question<Integer> {

    private static final int COMMON_CACHE_COUNT = 8;
    private static final int ADAL_LEGACY_CACHE_COUNT = 6;

    private static boolean isCommonCache;

    @Override
    public Integer answeredBy(Actor actor) {
        String results = Text.of(Results.RESULT_FIELD).viewedBy(actor).asString();
        ReadCacheResult readCacheResult = ResultsMapper.GetReadCacheResultFromString(results);
        List<TokenCacheItemReadResult> tokenCacheItems= readCacheResult.tokenCacheItems;
        if(tokenCacheItems!=null && !tokenCacheItems.isEmpty()) {
            User user = (User) actor;
            user.setCacheResult(readCacheResult.tokenCacheItems.get(0));
        }
        isCommonCache = readCacheResult.isCommonCache;
        return readCacheResult.itemCount;
    }

    public static Question<Integer> displayed(){
        return new TokenCacheItemCount();
    }

    public static int getExpectedCacheCount(){
        return isCommonCache ? COMMON_CACHE_COUNT : ADAL_LEGACY_CACHE_COUNT;
    }
}
