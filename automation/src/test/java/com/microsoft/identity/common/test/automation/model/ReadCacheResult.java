package com.microsoft.identity.common.test.automation.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;


import jdk.nashorn.internal.parser.Token;

import static com.microsoft.identity.common.test.automation.model.Constants.*;

public class ReadCacheResult {

    @SerializedName(ITEM_COUNT)
    public Integer itemCount;

    @SerializedName(ITEMS)
    public List<String> items;

    @SerializedName(COMMON_CACHE)
    public boolean isCommonCache;

    public List<TokenCacheItemReadResult> tokenCacheItems = new ArrayList<TokenCacheItemReadResult>();
}
