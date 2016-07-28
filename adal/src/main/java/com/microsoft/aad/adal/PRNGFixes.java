// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

// This code is from Google developer site. It may be altered from original
// Google Licence info from http://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html
// This software is provided 'as-is', without any express or implied
// warranty.  In no event will Google be held liable for any damages
// arising from the use of this software. 
// Permission is granted to anyone to use this software for any purpose,
// including commercial applications, and to alter it and redistribute it
// freely, as long as the origin is not misrepresented.

package com.microsoft.aad.adal;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.SecureRandomSpi;
import java.security.Security;

import android.os.Build;
import android.os.Process;
import android.util.Log;

/**
 * SecureRandm related fixed from Android developer site. The fixes need to be
 * applied before any use of Java Cryptography Architecture primitives. A good
 * place to invoke them is in the application's {@code onCreate}.
 */
final class PRNGFixes {

    private static final int VERSION_CODE_JELLY_BEAN = 16;

    private static final int VERSION_CODE_JELLY_BEAN_MR2 = 18;

    private static final byte[] BUILD_FINGERPRINT_AND_DEVICE_SERIAL = getBuildFingerprintAndDeviceSerial();

    private static final int ONE_KB = 1024;

    private static final String TAG = "PRNGFixes";

    /** Hidden constructor to prevent instantiation. */
    private PRNGFixes() {
    }

    /**
     * Applies OpenSSL fix and installs LinuxPRNGSecureRandom.
     * 
     * @throws SecurityException if a fix is needed but could not be applied.
     */
    public static void apply() {
        applyOpenSSLFix();
        installLinuxPRNGSecureRandom();
    }

    /**
     * Applies the fix for OpenSSL PRNG having low entropy. Does nothing if the
     * fix is not needed.
     * 
     * @throws SecurityException if the fix is needed but could not be applied.
     */
    private static void applyOpenSSLFix() throws SecurityException {
        if (Build.VERSION.SDK_INT < VERSION_CODE_JELLY_BEAN
                || Build.VERSION.SDK_INT > VERSION_CODE_JELLY_BEAN_MR2) {
            // No need to apply the fix
            Logger.v(TAG, "No need to apply the fix");
            return;
        }
        try {
            // Mix in the device- and invocation-specific seed.
            Class.forName("org.apache.harmony.xnet.provider.jsse.NativeCrypto")
                    .getMethod("RAND_seed", byte[].class).invoke(null, generateSeed());
            // Mix output of Linux PRNG into OpenSSL's PRNG
            int bytesRead = (Integer) Class
                    .forName("org.apache.harmony.xnet.provider.jsse.NativeCrypto")
                    .getMethod("RAND_load_file", String.class, long.class)
                    .invoke(null, "/dev/urandom", ONE_KB);
            if (bytesRead != ONE_KB) {
                throw new IOException("Unexpected number of bytes read from Linux PRNG: "
                        + bytesRead);
            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Logger.e(TAG, "Failed to seed OpenSSL PRNG", "", ADALError.DEVICE_PRNG_FIX_ERROR, e);
            throw new SecurityException("Failed to seed OpenSSL PRNG", e);
        }
    }

    /**
     * Installs a Linux PRNG-backed {@code SecureRandom} implementation as the
     * default. Does nothing if the implementation is already the default or if
     * there is not need to install the implementation.
     * 
     * @throws SecurityException if the fix is needed but could not be applied.
     */
    private static void installLinuxPRNGSecureRandom() throws SecurityException {
        if (Build.VERSION.SDK_INT > VERSION_CODE_JELLY_BEAN_MR2) {
            // No need to apply the fix
            Logger.v(TAG, "No need to apply the fix");
            return;
        }
        // Install a Linux PRNG-based SecureRandom implementation as the
        // default, if not yet installed.
        Provider[] secureRandomProviders = Security.getProviders("SecureRandom.SHA1PRNG");
        if (secureRandomProviders == null
                || secureRandomProviders.length < 1
                || !LinuxPRNGSecureRandomProvider.class.equals(secureRandomProviders[0].getClass())) {
            Logger.v(TAG, "insert provider as LinuxPRNGSecureRandomProvider");
            Security.insertProviderAt(new LinuxPRNGSecureRandomProvider(), 1);
        }
        
        // Log info about providers
        // Different libraries could apply same prng fixes with different namespace
        SecureRandom rng1 = new SecureRandom();
        Logger.v(TAG, "LinuxPRNGSecureRandomProvider for SecureRandom:" + rng1.getProvider().getClass().getName());
        
        SecureRandom rng2;
        try {
            rng2 = SecureRandom.getInstance("SHA1PRNG");
            Logger.v(TAG, "LinuxPRNGSecureRandomProvider for SecureRandom with alg SHA1PRNG:" + rng2.getProvider().getClass().getName());
        } catch (NoSuchAlgorithmException e) {
            Logger.v(TAG, "SHA1PRNG not available");
            throw new SecurityException("SHA1PRNG not available", e);
        }
    }

    /**
     * {@code Provider} of {@code SecureRandom} engines which pass through all
     * requests to the Linux PRNG.
     */
    private static class LinuxPRNGSecureRandomProvider extends Provider {
        private static final long serialVersionUID = 1L;

        public LinuxPRNGSecureRandomProvider() {
            super("LinuxPRNG", 1.0, "A Linux-specific random number provider that uses"
                    + " /dev/urandom");
            // Although /dev/urandom is not a SHA-1 PRNG, some apps
            // explicitly request a SHA1PRNG SecureRandom and we thus need to
            // prevent them from getting the default implementation whose output
            // may have low entropy.
            put("SecureRandom.SHA1PRNG", LinuxPRNGSecureRandom.class.getName());
            put("SecureRandom.SHA1PRNG ImplementedIn", "Software");
        }
    }

    /**
     * {@link SecureRandomSpi} which passes all requests to the Linux PRNG (
     * {@code /dev/urandom}).
     */
    public static class LinuxPRNGSecureRandom extends SecureRandomSpi {
        private static final long serialVersionUID = 1L;

        /*
         * IMPLEMENTATION NOTE: Requests to generate bytes and to mix in a seed
         * are passed through to the Linux PRNG (/dev/urandom). Instances of
         * this class seed themselves by mixing in the current time, PID, UID,
         * build fingerprint, and hardware serial number (where available) into
         * Linux PRNG. Concurrency: Read requests to the underlying Linux PRNG
         * are serialized (on SLOCK) to ensure that multiple threads do not get
         * duplicated PRNG output.
         */
        private static final File URANDOM_FILE = new File("/dev/urandom");

        private static final Object SLOCK = new Object();

        /**
         * Input stream for reading from Linux PRNG or {@code null} if not yet
         * opened.
         * 
         * GuardedBy("SLOCK")
         */
        private static DataInputStream sUrandomIn;

        /**
         * Output stream for writing to Linux PRNG or {@code null} if not yet
         * opened.
         * 
         * GuardedBy("SLOCK")
         */
        private static OutputStream sUrandomOut;

        /**
         * Whether this engine instance has been seeded. This is needed because
         * each instance needs to seed itself if the client does not explicitly
         * seed it.
         */
        private boolean mSeeded;

        @Override
        protected void engineSetSeed(byte[] bytes) {
            try {
                OutputStream out;
                synchronized (SLOCK) {
                    out = getUrandomOutputStream();
                }
                out.write(bytes);
                out.flush();
            } catch (IOException e) {
                // On a small fraction of devices /dev/urandom is not writable.
                // Log and ignore.
                Log.w(PRNGFixes.class.getSimpleName(), "Failed to mix seed into " + URANDOM_FILE);
            } finally {
                mSeeded = true;
            }
        }

        @Override
        protected void engineNextBytes(byte[] bytes) {
            if (!mSeeded) {
                // Mix in the device- and invocation-specific seed.
                engineSetSeed(generateSeed());
            }
            try {
                DataInputStream in;
                synchronized (SLOCK) {
                    in = getUrandomInputStream();
                    in.readFully(bytes);
                }
            } catch (IOException e) {
                throw new SecurityException("Failed to read from " + URANDOM_FILE, e);
            }
        }

        @Override
        protected byte[] engineGenerateSeed(int size) {
            byte[] seed = new byte[size];
            engineNextBytes(seed);
            return seed;
        }

        private DataInputStream getUrandomInputStream() {
            synchronized (SLOCK) {
                if (sUrandomIn == null) {
                    // NOTE: Consider inserting a BufferedInputStream between
                    // DataInputStream and FileInputStream if you need higher
                    // PRNG output performance and can live with future PRNG
                    // output being pulled into this process prematurely.
                    try {
                        sUrandomIn = new DataInputStream(new FileInputStream(URANDOM_FILE));
                    } catch (IOException e) {
                        throw new SecurityException("Failed to open " + URANDOM_FILE
                                + " for reading", e);
                    }
                }
                return sUrandomIn;
            }
        }

        private OutputStream getUrandomOutputStream() throws IOException {
            synchronized (SLOCK) {
                if (sUrandomOut == null) {
                    sUrandomOut = new FileOutputStream(URANDOM_FILE);
                }
                return sUrandomOut;
            }
        }
    }

    /**
     * Generates a device- and invocation-specific seed to be mixed into the
     * Linux PRNG.
     */
    private static byte[] generateSeed() {
        try {
            ByteArrayOutputStream seedBuffer = new ByteArrayOutputStream();
            DataOutputStream seedBufferOut = new DataOutputStream(seedBuffer);
            seedBufferOut.writeLong(System.currentTimeMillis());
            seedBufferOut.writeLong(System.nanoTime());
            seedBufferOut.writeInt(Process.myPid());
            seedBufferOut.writeInt(Process.myUid());
            seedBufferOut.write(BUILD_FINGERPRINT_AND_DEVICE_SERIAL);
            seedBufferOut.close();
            return seedBuffer.toByteArray();
        } catch (IOException e) {
            throw new SecurityException("Failed to generate seed", e);
        }
    }

    /**
     * Gets the hardware serial number of this device.
     * 
     * @return serial number or {@code null} if not available.
     */
    private static String getDeviceSerialNumber() {
        // We're using the Reflection API because Build.SERIAL is only available
        // since API Level 9 (Gingerbread, Android 2.3).
        try {
            return (String) Build.class.getField("SERIAL").get(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] getBuildFingerprintAndDeviceSerial() {
        StringBuilder result = new StringBuilder();
        String fingerprint = Build.FINGERPRINT;
        if (fingerprint != null) {
            result.append(fingerprint);
        }
        String serial = getDeviceSerialNumber();
        if (serial != null) {
            result.append(serial);
        }
        try {
            return result.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }
}
