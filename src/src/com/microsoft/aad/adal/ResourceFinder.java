
package com.microsoft.aad.adal;

import android.util.Log;

class ResourceFinder {

    private static final String TAG = "ResourceFinder";

    /**
     * Finds resource by name from R file. This helps to resolve resources in
     * case App inclues jar file instead of project library link.
     * 
     * @param packageName
     * @param className
     * @param name
     * @return
     */
    public static int getResourseIdByName(String packageName, String className, String name) {
        int id = 0;
        try {
            Class[] classes = Class.forName(packageName + ".R").getClasses();
            Class targetClazz = null;
            for (int i = 0; i < classes.length; i++) {
                if (classes[i].getName().split("\\$")[1].equals(className)) {
                    targetClazz = classes[i];
                    break;
                }
            }

            if (targetClazz != null) {
                id = targetClazz.getField(name).getInt(targetClazz);
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, e.getMessage());
            throw new AuthenticationException(ADALError.RESOURCE_NOT_FOUND, "Resource not found", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
            throw new AuthenticationException(ADALError.RESOURCE_NOT_FOUND, "Resource not found", e);
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
            throw new AuthenticationException(ADALError.RESOURCE_NOT_FOUND, "Resource not found", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.getMessage());
            throw new AuthenticationException(ADALError.RESOURCE_NOT_FOUND, "Resource not found", e);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, e.getMessage());
            throw new AuthenticationException(ADALError.RESOURCE_NOT_FOUND, "Resource not found", e);
        }

        return id;
    }
}
