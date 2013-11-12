
package com.microsoft.adal;

import android.util.Log;

import com.microsoft.adal.ErrorCodes.ADALError;

/**
 * if logcat is available, it logs there by default. If externalLogger is set, it will use that as well.
 * @author omercan
 *
 */
public class Logger {

    private LogLevel mLogLevel;

    public enum LogLevel {
        Error, Warn, Info, Verbose, Debug
    }

    /**
     * one callback logger
     */
    private ILogger mExternalLogger = null;
    
    private static Logger sInstance = new Logger();

    public static Logger getInstance() {
        return sInstance;
    }

    Logger() {
        mLogLevel = LogLevel.Debug;
    }

    
    public interface ILogger {
        void Log(String tag, String message, String additionalMessage, LogLevel level, ADALError errorCode);
    }

    public LogLevel getLogLevel() {
        return mLogLevel;
    }

    public void setLogLevel(LogLevel level) {
        this.mLogLevel = level;
    }

   

    public void setExternalLogger(ILogger externalLogger) {
        this.mExternalLogger = externalLogger;
    }
    
    public void d(String tag, String message){
        if(isLogCatAvailable()){
            Log.d(tag, message);
        }
        
        if(mExternalLogger != null){
            mExternalLogger.Log(tag, message, null, LogLevel.Debug, null);
        }
    }
    
    public void v(String tag, String message, String additionalMessage, ADALError errorCode){
        if(isLogCatAvailable()){
            Log.v(tag, message+" "+additionalMessage);
        }
        
        if(mExternalLogger != null){
            mExternalLogger.Log(tag, message, additionalMessage, LogLevel.Verbose, errorCode);
        }
    }
    
    public void e(String tag, String message, String additionalMessage, ADALError errorCode){
        if(isLogCatAvailable()){
            Log.e(tag, message+" "+additionalMessage);
        }
        
        if(mExternalLogger != null){
            mExternalLogger.Log(tag, message, additionalMessage, LogLevel.Error, errorCode);
        }
    }
    
    public void e(String tag, String message, String additionalMessage, ADALError errorCode, Throwable err){
        if(isLogCatAvailable()){
            Log.e(tag, message+" "+additionalMessage, err);
        }
        
        if(mExternalLogger != null){
            mExternalLogger.Log(tag, message, additionalMessage, LogLevel.Error, errorCode);
        }
    }
    
    
    
    private boolean isLogCatAvailable(){
        //TODO lookup logcat availability
        return true;
    }
}
