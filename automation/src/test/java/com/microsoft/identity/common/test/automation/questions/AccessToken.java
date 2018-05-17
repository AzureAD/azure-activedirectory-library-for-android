package com.microsoft.identity.common.test.automation.questions;

import com.microsoft.identity.common.test.automation.actors.User;
import com.microsoft.identity.common.test.automation.model.ReadCacheResult;
import com.microsoft.identity.common.test.automation.model.ResultsMapper;
import com.microsoft.identity.common.test.automation.model.TokenCacheItemReadResult;
import com.microsoft.identity.common.test.automation.ui.Results;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.questions.Text;

public class AccessToken implements Question<String> {
    @Override
    public String answeredBy(Actor actor) {
        String results = Text.of(Results.RESULT_FIELD).viewedBy(actor).asString();
        ReadCacheResult readCacheResult = ResultsMapper.GetReadCacheResultFromString(results);
        User user = (User) actor;
        user.setCacheResult(readCacheResult.tokenCacheItems.get(0));
        //TODO : Understand why access token could be null in a cache item
        String accessToken = null;
        for(TokenCacheItemReadResult readResult :  readCacheResult.tokenCacheItems){
            if(readResult.accessToken != null){
                accessToken = readResult.accessToken;
                break;
            }
        }
        return accessToken;
    }

    public static Question<String>  displayed(){
         return new AccessToken();
    }
}
