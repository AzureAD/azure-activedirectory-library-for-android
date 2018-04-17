package com.microsoft.identity.common.test.automation.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;


import static com.microsoft.identity.common.test.automation.model.Constants.*;

public class ReadCacheResult {

    @SerializedName(ITEM_COUNT)
    public Integer itemCount;

    @SerializedName(READ_CACHE)
    public List<TokenCacheItemReadResult> items;
}
