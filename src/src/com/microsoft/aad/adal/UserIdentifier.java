// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

public class UserIdentifier {

    enum UserIdentifierType {
        UniqueId, OptionalDisplayableId, RequiredDisplayableId
    }

    private String mId;

    private UserIdentifierType mType;

    private static final String AnyUserId = "AnyUser";

    private static final UserIdentifier AnyUser = new UserIdentifier(AnyUserId,
            UserIdentifierType.UniqueId);

    public UserIdentifier(String id, UserIdentifierType userType) {
        this.mId = id;
        this.mType = userType;
    }

    public String getId() {
        return mId;
    }

    public UserIdentifierType getType() {
        return mType;
    }

    public UserIdentifier getAnyUser() {
        return AnyUser;
    }

    boolean anyUser() {
        return mType.equals(AnyUser.mType) && mId.equalsIgnoreCase(AnyUserId);
    }

    String getUniqueId() {
        return (!this.anyUser() && this.mType.equals(UserIdentifierType.UniqueId)) ? this.mId : "";
    }

    String getDisplayableId() {
        return (!this.anyUser() && (this.mType.equals(UserIdentifierType.RequiredDisplayableId) || this.mType
                .equals(UserIdentifierType.OptionalDisplayableId))) ? this.mId : "";
    }
}
