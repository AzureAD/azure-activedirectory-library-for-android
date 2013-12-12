
package com.microsoft.adal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;

/**
 * Persisted cache that keeps cache in-memory until write operation. Filename
 * should not be used on another instance of FiletokenCacheStore since read
 * operations are not synced to file.
 * 
 * @author omercan
 */
public class FileTokenCacheStore implements ITokenCacheStore {

    /**
     * default serial#
     */
    private static final long serialVersionUID = -8252291336171327870L;

    private static final String TAG = null;

    private final String mFileName;

    private final File mDirectory;

    private final File mFile;

    private final MemoryTokenCacheStore mInMemoryCache;

    private final Object mCacheLock = new Object();

    /**
     * it tracks data in memory until it writes that to a file with write
     * operation.
     * 
     * @param context
     * @param fileName filename should be unique to this instance since read
     *            operations don't read from file directly. write operations
     *            write to a file.
     */
    public FileTokenCacheStore(Context context, String fileName) {
        if (context == null) {
            throw new IllegalArgumentException("context");
        }

        if (StringExtensions.IsNullOrBlank(fileName)) {
            throw new IllegalArgumentException("fileName");
        }

        mFileName = fileName;
        mDirectory = context.getDir(AuthenticationConstants.AUTHENTICATION_FILE_DIRECTORY,
                Context.MODE_PRIVATE);

        if (mDirectory == null) {
            throw new IllegalStateException("It could not access the Authorization cache directory");
        }

        // Initialize cache from file if it exists
        try {
            mFile = new File(mDirectory, mFileName);

            if (mFile.exists()) {
                Logger.v(TAG, "There is previous cache file to load cache.");
                FileInputStream inputStream = new FileInputStream(mFile);
                ObjectInputStream objectStream = new ObjectInputStream(inputStream);
                Object cacheObj = objectStream.readObject();
                inputStream.close();
                objectStream.close();

                if (cacheObj instanceof MemoryTokenCacheStore) {
                    mInMemoryCache = (MemoryTokenCacheStore)cacheObj;
                } else {
                    Logger.e(TAG, "Existing cache format is wrong", "",
                            ADALError.DEVICE_FILE_CACHE_FORMAT_IS_WRONG);
                    throw new AuthenticationException(ADALError.DEVICE_FILE_CACHE_FORMAT_IS_WRONG);
                }
            } else {
                Logger.v(TAG, "There is not any previous cache file to load cache.");
                mInMemoryCache = new MemoryTokenCacheStore();
            }
        } catch (Exception ex) {
            Logger.e(TAG, "Exception during cache load",
                    ExceptionExtensions.getExceptionMessage(ex),
                    ADALError.DEVICE_FILE_CACHE_IS_NOT_LOADED_FROM_FILE);
            // if it is not possible to load the cache because of permissions or
            // similar, it will not work again. File cache is not working
            throw new AuthenticationException(ADALError.DEVICE_FILE_CACHE_IS_NOT_LOADED_FROM_FILE);

        }
    }

    @Override
    public TokenCacheItem getItem(CacheKey key) {
        return mInMemoryCache.getItem(key);
    }

    @Override
    public boolean contains(CacheKey key) {
        return mInMemoryCache.contains(key);
    }

    @Override
    public void setItem(TokenCacheItem item) {
        mInMemoryCache.setItem(item);
        writeToFile();
    }

    @Override
    public void removeItem(CacheKey key) {
        mInMemoryCache.removeItem(key);
        writeToFile();
    }

    @Override
    public void removeItem(TokenCacheItem item) {
        mInMemoryCache.removeItem(item);
        writeToFile();
    }

    @Override
    public void removeAll() {
        mInMemoryCache.removeAll();
        writeToFile();
    }

    private void writeToFile() {
        synchronized (mCacheLock) {
            if (mFile != null && mInMemoryCache != null) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(mFile);
                    ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
                    objectStream.writeObject(mInMemoryCache);
                    objectStream.flush();
                    objectStream.close();
                    outputStream.close();
                } catch (Exception ex) {
                    Logger.e(TAG, "Exception during cache flush",
                            ExceptionExtensions.getExceptionMessage(ex),
                            ADALError.DEVICE_FILE_CACHE_IS_NOT_WRITING_TO_FILE);
                }
            }
        }
    }
}
