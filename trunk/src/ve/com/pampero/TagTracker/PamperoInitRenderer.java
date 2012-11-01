package ve.com.pampero.TagTracker;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import com.qualcomm.QCAR.QCAR;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created with IntelliJ IDEA.
 * User: Jose Manuel Aguirre
 * Date: 29/10/12
 * Time: 06:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class PamperoInitRenderer implements GLSurfaceView.Renderer {
    public boolean mIsActive = false;
    public TagInspector _TagInspector;
    public TagInspector onResultActivity;

    public PamperoInitRenderer(TagInspector activity){
        this.onResultActivity = activity;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        //To change body of implemented methods use File | Settings | File Templates.

        Intent intent;
        //TagInspector ti;
        //intent = new Intent(, MenuActivity.class);
        NativeInterfaceCaller.initRendering();
        QCAR.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        //To change body of implemented methods use File | Settings | File Templates.
        NativeInterfaceCaller.updateRendering(i, i1);
        QCAR.onSurfaceChanged(i, i1);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        //To change body of implemented methods use File | Settings | File Templates.
        if (!mIsActive)
        return;
        int frameIndex = -1;
        String message = "";

        // Call our native function to render content
        frameIndex = NativeInterfaceCaller.renderFrame();
        switch(frameIndex){
            case 1:
                LoggerPrint.INFO("SE ESTA OBTENIENDO EL FRAME BELMONT");
                //message = "SE ESTA OBTENIENDO EL FRAME PROMOCION";
                //_TagInspector.setId(frameIndex);
                onResultActivity.onImageTrack(frameIndex);
            break;
            case 2:
                LoggerPrint.INFO("SE ESTA OBTENIENDO EL FRAME PROMOCION");
                onResultActivity.onImageTrack(frameIndex);
                //message= "SE ESTA OBTENIENDO EL FRAME TARMAC";
            break;
        }
    }

    /*public native void initRendering();
    public native int renderFrame();
    public native void updateRendering(int w, int h);*/
}
