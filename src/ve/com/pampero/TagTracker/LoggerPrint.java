package ve.com.pampero.TagTracker;

import com.qualcomm.ar.pl.DebugLog;



/*
 *CLASE USADA PARA IMPRIMIR EN EL LOGCAT
 */
public class LoggerPrint {
    public static final String ERROR_TAG="ERROR";
    public static final String INFO_TAG="INFO";
    public static final String DEBUG_TAG="DEBUG";
    public static final String WARNING_TAG="WARNING";

    public static void DEBUG(String logEntry){
        DebugLog.LOGD(DEBUG_TAG, logEntry);
    }

    public static void INFO(String logEntry){
        DebugLog.LOGI(INFO_TAG, logEntry);
    }

    public static void WARNING(String logEntry){
        DebugLog.LOGW(WARNING_TAG, logEntry);
    }

    public static void ERROR(String logEntry){
        DebugLog.LOGE(ERROR_TAG, logEntry);
    }
}
