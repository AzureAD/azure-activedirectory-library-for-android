package com.microsoft.identity.common.test.automation.model;


import com.microsoft.identity.common.internal.net.ObjectMapper;

public class ResultsMapper {

    private static final int COMMON_CACHE_COUNT = 8;
    private static final int ADAL_LEGACY_CACHE_COUNT = 6;

    public static ReadCacheResult GetReadCacheResultFromString(String results){
        ReadCacheResult readCacheResult = (ReadCacheResult) ObjectMapper.deserializeJsonStringToObject(results, ReadCacheResult.class);

        for(String result : readCacheResult.items){
            TokenCacheItemReadResult tokenCacheItemReadResult = (TokenCacheItemReadResult) ObjectMapper.deserializeJsonStringToObject(result, TokenCacheItemReadResult.class);
            readCacheResult.tokenCacheItems.add(tokenCacheItemReadResult);
        }

        return readCacheResult;

    }

    public static ADALErrorResult GetADALErrorResultFromString(String results){
        ADALErrorResult adalErrorResult = (ADALErrorResult) ObjectMapper.deserializeJsonStringToObject(results, ADALErrorResult.class);
        return adalErrorResult;
    }

    public static int getExpectedCacheCount(String results) {
        ReadCacheResult readCacheResult = (ReadCacheResult) ObjectMapper.deserializeJsonStringToObject(results, ReadCacheResult.class);
        return readCacheResult.isCommonCache ? COMMON_CACHE_COUNT : ADAL_LEGACY_CACHE_COUNT;

    }
}
