package com.microsoft.aad.adal;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ADFSWebFingerValidatorTests extends TestCase {

    @SmallTest
    public void testTrustedRealmFieldInitialized()
            throws NoSuchFieldException, IllegalAccessException {
        Field trustedRealmURI = ADFSWebFingerValidator.class.getDeclaredField("TRUSTED_REALM_REL");
        trustedRealmURI.setAccessible(true);
        Assert.assertEquals(
                trustedRealmURI.get(null).toString(),
                "http://schemas.microsoft.com/rel/trusted-realm"
        );
    }

    @SmallTest
    public void testRealmIsTrustedEmptyMetadata() throws URISyntaxException {
        final URI testAuthority = new URI("https://fs.ngctest.nttest.microsoft.com/adfs/ls/");
        WebFingerMetadata metadata = new WebFingerMetadata();
        Assert.assertEquals(
                false,
                ADFSWebFingerValidator.realmIsTrusted(
                        testAuthority,
                        metadata
                )
        );
    }

    public void testRealmIsTrusted() throws URISyntaxException {
        final URI testAuthority = new URI("https://fs.ngctest.nttest.microsoft.com/adfs/ls/");
        WebFingerMetadata metadata = new WebFingerMetadata();
        final Link link = new Link();
        link.setHref("https://fs.ngctest.nttest.microsoft.com");
        link.setRel("http://schemas.microsoft.com/rel/trusted-realm");
        List<Link> links = new ArrayList<>();
        links.add(link);
        metadata.setLinks(links);
        Assert.assertEquals(
                true,
                ADFSWebFingerValidator.realmIsTrusted(
                        testAuthority,
                        metadata
                )
        );
    }
}
