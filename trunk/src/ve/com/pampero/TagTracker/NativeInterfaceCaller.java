package ve.com.pampero.TagTracker;

/**
 * Created with IntelliJ IDEA.
 * User: Jose Manuel Aguirre
 * Date: 30/10/12
 * Time: 04:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class NativeInterfaceCaller {
    public NativeInterfaceCaller(){

    }

    public static native int getOpenGlEsVersionNative();
    public static native void setActivityPortraitMode(boolean isPortrait);

    public static native int initTracker();
    public static native void deinitTracker();

    public static native int loadTrackerData();
    public static native void destroyTrackerData();

    public static native void onQCARInitializedNative();

    public static native void startCamera();
    public static native void stopCamera();
    public static native void setProjectionMatrix();

    public static native void initApplicationNative(int width, int height);
    public static native void deinitApplicationNative();

    public static native void initRendering();
    public static native int renderFrame();
    public static native void updateRendering(int w, int h);

}
