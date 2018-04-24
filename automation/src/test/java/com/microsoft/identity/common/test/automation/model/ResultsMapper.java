package com.microsoft.identity.common.test.automation.model;


import com.microsoft.identity.common.internal.net.ObjectMapper;

public class ResultsMapper {

    public static ReadCacheResult GetReadCacheResultFromString(String results){
        ReadCacheResult readCacheResult = (ReadCacheResult) ObjectMapper.deserializeJsonStringToObject(results, ReadCacheResult.class);

        for(String result : readCacheResult.items){
            TokenCacheItemReadResult tokenCacheItemReadResult = (TokenCacheItemReadResult) ObjectMapper.deserializeJsonStringToObject(result, TokenCacheItemReadResult.class);
            readCacheResult.tokenCacheItems.add(tokenCacheItemReadResult);
        }

        return readCacheResult;

    }
}
