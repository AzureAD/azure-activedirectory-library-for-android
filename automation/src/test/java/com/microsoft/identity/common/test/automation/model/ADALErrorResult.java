package com.microsoft.identity.common.test.automation.model;

import com.google.gson.annotations.SerializedName;

public class ADALErrorResult {

    @SerializedName(Constants.ADAL_ERROR_DATA.ERROR)
    public String error;

    @SerializedName(Constants.ADAL_ERROR_DATA.ERROR_DESCRIPTION)
    public String errorDescription;

}
