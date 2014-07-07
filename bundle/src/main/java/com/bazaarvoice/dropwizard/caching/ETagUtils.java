package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.sun.jersey.core.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class ETagUtils {
    /**
     * Generate an ETag value for some bytes.
     * <p/>
     * Generates the base 64 encoded SHA-1 hash of the given content. However, the output should be treated as an
     * opaque string as the implementation may change.
     *
     * @param content content to generate ETag for
     * @return ETag string
     */
    public static String generateETag(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(content);
            return new String(Base64.encode(hash), Charsets.US_ASCII);
        } catch (NoSuchAlgorithmException ex) {
            // This should not be possible as every Java implementation should support SHA-1.
            // See javadoc for MessageDigest.
            throw Throwables.propagate(ex);
        }
    }
}
