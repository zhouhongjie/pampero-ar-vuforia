package ve.com.pampero.TagTracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ImageView;
import com.qualcomm.QCAR.QCAR;
//import com.qualcomm.QCARSamples.ImageTargets.*;

/* CLASE PRINCIPAL TagInspector.java
 * AQUI SE REALIZA LA INICIALIZACION DE TODOS LOS COMPONENTES QUE UTILIZA VUFORIA
 */
public class TagInspector extends Activity {
    private static final String NATIVE_LIB_QCAR = "QCAR";
    private static final String NATIVE_LIB_PAMPERO = "Pampero";
    private static final int INIT_APP_STATUS = 0;
    private static final int INIT_QCAR_STATUS = 1;
    private static final int INIT_TRACKER_STATUS = 2;
    private static final int INIT_APP_AR_STATUS = 3;
    private static final int LOAD_TRACKER_STATUS = 4;
    private static final int APP_INITIATED_STATUS = 5;
    private static final int START_CAMERA = 6;
    private static final int STOP_CAMERA = 7;

    private static final long MIN_FIRST_SCREEN_TIME = 2000;

    private InitQCARTask initQCARTask;
    private LoadTracker loadTracker;
    private PamperoInitSurface mGlView;
    private PamperoInitRenderer mRenderer;
    private Object shutdownLock = new Object();
    private int QCARFlags = 0;
    private int CURRENT_STATUS = -1;
    private int screenWidth = 0;
    private int screenHeight = 0;

    /*
     *METODO PARA OBTENER LA PANATALLA INICIAL DE LA APLICACION 
     */
    public ImageView getInitialScreen() {
        return initialScreen;
    }

    public void setInitialScreen(ImageView initialScreen) {
        this.initialScreen = initialScreen;
    }

    private ImageView initialScreen;
    private long initialScreenStartTime = 0;
    boolean isBelmontDataSetActive = false;
    private Handler initialScreenHandler;
    private Runnable initialScreenRunnable;

    private int backgroundImage = 0;

    
    /*
     * BLOQUE ESTATICO QUE SE EJECUTARA UNA VEZ INICIADA LA ACTIVIDAD
     * EN ESTE BLOQUE SE CARGAN LAS LIBRERIAS COMPILADAS EN C++
     * DE QUALCOMM AUGMENTED REALITY (QCAR) Y LIBRERIAS PROPIAS DE LA APLICACION (Pampero) 
     */
    static
    {
        LoggerPrint.INFO("Loading Library: " + NATIVE_LIB_QCAR);
        loadLibrary(NATIVE_LIB_QCAR);
        LoggerPrint.INFO("Loading Library: " + NATIVE_LIB_PAMPERO);
        loadLibrary(NATIVE_LIB_PAMPERO);
    }
    
    /*
     * METODO DE INICIO. ESTE METODO SE EJECUTA AL INICIAR LA APLICACION DESDE EL DISPOSITIVO
     */
    
    protected void onCreate(Bundle savedInstanceState){
        LoggerPrint.INFO("Starting method: TagInspector.onCreate()");
        super.onCreate(savedInstanceState);
        LoggerPrint.INFO(TagInspector.class.getName() + ".OnCreate - Getting Initialization flags");        
        backgroundImage = R.drawable.abstract_backgorund_portait;
        // OBTENIENDO LOS FLAGS DE VERSION DE OPENGL PARA QCAR
        QCARFlags = getInitializationFlags();
        updateApplicationStatus(INIT_APP_STATUS);
    }
    
    /*
     * METODO PARA MODIFICAR EL ESTADO DE LA APLICAICON
     */
    private synchronized void updateApplicationStatus(int status){
        if (CURRENT_STATUS == status){
            return;
        }

        CURRENT_STATUS = status;        
        
        switch(CURRENT_STATUS){
            case INIT_APP_STATUS:
                LoggerPrint.INFO(TagInspector.class.getName() + ".updateApplicationStatus - CURRENT STATUS: " + CURRENT_STATUS );
                //SE REALIZAN CONFIGURACIONES INICIALES PARA LA APLICACION
                initApplication();
                updateApplicationStatus(INIT_QCAR_STATUS);
            break;
            case INIT_QCAR_STATUS:
                try
                {
                	//SE INICIALIZA EL COMPONENTE QCAR
                    initQCARTask = new InitQCARTask();
                    initQCARTask.execute();
                }
                catch (Exception e)
                {
                    LoggerPrint.INFO(TagInspector.class.getName() + ".updateApplicationStatus - ERROR initializing QCAR");
                }
            break;
            case INIT_TRACKER_STATUS:
            	//SE INICIALIZA EL TRACKER
                if(NativeInterfaceCaller.initTracker() > 0){
                    updateApplicationStatus(INIT_APP_AR_STATUS);
                }
            break;
            case INIT_APP_AR_STATUS:
            	//SE INICIAN LOS COMPONENTES PARA EL RENDERIZADO
                initApplicationAR();
                updateApplicationStatus(LOAD_TRACKER_STATUS);
            break;
            case LOAD_TRACKER_STATUS:
            	//SE CARGA LA INFORMACION DE LAS IMAGENES AL TRACKER 
                try{
                    loadTracker = new LoadTracker();
                    loadTracker.execute();
                }catch (Exception e){
                    LoggerPrint.INFO(TagInspector.class.getName() + ".updateApplicationStatus - ERROR loading tracker data set");
                }
            break;
            case APP_INITIATED_STATUS:
            	//GARBAGE COLLECTOR PERFORM
                System.gc();

                // Native post initialization:
                NativeInterfaceCaller.onQCARInitializedNative();

                // The elapsed time since the splash screen was visible:
                long screenStartTime = System.currentTimeMillis() -
                        initialScreenStartTime;
                long newStartScreenTime = 0;
                if (screenStartTime < MIN_FIRST_SCREEN_TIME){
                    newStartScreenTime = MIN_FIRST_SCREEN_TIME - screenStartTime;
                }
                
                //SE INICIALIZA EL HANDLER PARA MANEJAR LOS CAMBIOS Y ACCIONES REFERENTES AL RENDERIZADO
                initialScreenHandler = new Handler();
                //mRenderer._TagInspector=this;
                //SE INICIALIZA LA SUPERFICIE DE RENDERIZADO
                initialScreenRunnable = new Runnable() {

                    public void run() {
                        //To change body of implemented methods use File | Settings | File Templates.
                        initialScreen.setVisibility(View.INVISIBLE);
                        mRenderer.mIsActive = true;

                        addContentView(mGlView, new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.FILL_PARENT,
                                ViewGroup.LayoutParams.FILL_PARENT));


                        updateApplicationStatus(START_CAMERA);


                    }
                };
                LoggerPrint.INFO("******* initialScreenHandler");
                initialScreenHandler.postDelayed(initialScreenRunnable, newStartScreenTime);
            break;
            case START_CAMERA:
            	//SE INICIALIZA LA CAMARA
                NativeInterfaceCaller.startCamera();
                NativeInterfaceCaller.setProjectionMatrix();
            break;
            case STOP_CAMERA:
            	//HACE UN STOP DE LA CAMARA
                NativeInterfaceCaller.stopCamera();
                break;
            default:
                LoggerPrint.INFO(TagInspector.class.getName() + ".updateApplicationStatus - Invalid application state");
            break;

        }

    }

    private void initApplicationAR(){
        // PARA INICIALIZAR LOS ELEMENTOS DE TEXTURAS REALIZADOS POR ANIMACION

        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = QCAR.requiresAlpha();
        mGlView = new PamperoInitSurface(this);
        mGlView.init(QCARFlags, translucent, depthSize, stencilSize);

        mRenderer = new PamperoInitRenderer(this);
        mGlView.setRenderer(mRenderer);
    }

    private void initApplication(){
        LoggerPrint.INFO(TagInspector.class.getName() + ".initApplication - setting screen orientation");
        /*
         *  SE SETEA LA ORIENTACION DE LA PANTALLA
         *  PORTRAIT = VERTICAL
         *  LANDSCAPE = HORIZONTAL
         */
        int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        setRequestedOrientation(screenOrientation);
        
        /*
         * SETEA EL VALOR isInPotraitMode EN LA CLASE NATIVA DE Pampero
         */
        LoggerPrint.INFO(TagInspector.class.getName() + ".initApplication - setting activity portrait mode");
        NativeInterfaceCaller.setActivityPortraitMode(screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        /*
         * SE ALMACENAN LOS VALORES DE LA DIMENSION DE LA PANTALLA DEL DISPOSITIVO
         */
        LoggerPrint.INFO(TagInspector.class.getName() + ".initApplication - storing screen dimensions");
        storeScreenDimensions();
        
        /*
         * SE SETEA QUE LA PANTALLA SE MANTENGA ACTIVA E ILUMINADA DURANTE LA EJECUCION DE LA APLICACION
         */
        LoggerPrint.INFO(TagInspector.class.getName() + ".initApplication - setting screen options");
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*
         * SE LEVANTA LA IMAGEN DE INICIO 
         */
        LoggerPrint.INFO(TagInspector.class.getName() + ".initApplication - adding ImageView");
        initialScreen = new ImageView(this);
        initialScreen.setImageResource(backgroundImage);
        addContentView(initialScreen, new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        initialScreenStartTime = System.currentTimeMillis();

    }

    private void storeScreenDimensions()
    {
        // Query display dimensions
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    public int getInitializationFlags(){
        int flags = 0;
        // Query the native code:
        if (NativeInterfaceCaller.getOpenGlEsVersionNative() == 1){
            flags = QCAR.GL_11;
        }else{
            flags = QCAR.GL_20;
        }
        return flags;
    }

    private static void loadLibrary(String libName){
        System.loadLibrary(libName);
    }

    /*
     * CLASE PARA LA INICIALIZACION DEL COMPONENTE QCAR
     */
    private class InitQCARTask2 extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    private class InitQCARTask extends AsyncTask<Void, Integer, Boolean> {
        private int mProgressValue = -1;

        protected Boolean doInBackground(Void... params){
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (shutdownLock){
            	//SE INICIALIZA EL COMPONENTE QCAR CON LOS PARAMETROS NECESARIOS
                QCAR.setInitParameters(TagInspector.this, QCARFlags);
                do{
                    mProgressValue = QCAR.init();
                    publishProgress(mProgressValue);
                }while (!isCancelled() && mProgressValue >= 0 && mProgressValue < 100);
                return (mProgressValue > 0);
            }
        }

        protected void onProgressUpdate(Integer... values){
            LoggerPrint.INFO(InitQCARTask.class.getName() + ".onProgressUpdate - Proggress Percentage: " + mProgressValue);
        }

        protected void onPostExecute(Boolean result){
            // Done initializing QCAR, proceed to next application
            // initialization status:
            if (result){
                LoggerPrint.INFO(InitQCARTask.class.getName() + ".onPostExecute - QCAR Initialized succesfully");
                updateApplicationStatus(INIT_TRACKER_STATUS);
            }else{
                // Create dialog box for display error:
                AlertDialog dialogError = new AlertDialog.Builder(TagInspector.this).create();
                dialogError.setButton(
                        "Quit",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                // Exiting application
                                System.exit(1);
                            }
                        }
                );

                String logMessage;

                // NOTE: Check if initialization failed because the device is
                // not supported. At this point the user should be informed
                // with a message.
                if (mProgressValue == QCAR.INIT_DEVICE_NOT_SUPPORTED){
                    logMessage = "Failed to initialize QCAR because this device is not supported.";
                }else{
                    logMessage = "Failed to initialize QCAR.";
                }
                // Log error:
                LoggerPrint.ERROR(InitQCARTask.class.getName() + logMessage);
                // Show dialog box with error message:
                dialogError.setMessage(logMessage);
                dialogError.show();
            }
        }
    }
    
    /*
     * CLASE PARA LA CARGA DE LAS IMAGENES TRACKEADAS (.XML .DAT) AL TRACKER
     */
    /** An async task to load the tracker data asynchronously. */
    private class LoadTracker extends AsyncTask<Void, Integer, Boolean>{
        protected Boolean doInBackground(Void... params){
            // Prevent the onDestroy() method to overlap:
            synchronized (shutdownLock){
                // SE CARGA LA INFORMACION DE LAS IMAGENES CON EL METODO NATIVO loadTrackerData()            	
                return (NativeInterfaceCaller.loadTrackerData() > 0);
            }
        }

        protected void onPostExecute(Boolean result){
            //DebugLog.LOGD(LoadTracker.class.getName() + " onPostExecute" + (result ? "successful" : "failed"));
            if (result){
                isBelmontDataSetActive = true;
                updateApplicationStatus(APP_INITIATED_STATUS);
            }else{
                AlertDialog dialogError = new AlertDialog.Builder(TagInspector.this).create();
                dialogError.setButton(
                        "Close",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                // Exiting application
                                System.exit(1);
                            }
                        }
                );
                // Show dialog box with error message:
                dialogError.setMessage("Failed to load tracker data.");
                dialogError.show();
            }
        }
    }

    /*
     * ESTE METODO SE EJECUTA AL MOMENTO QUE LA APLICACION ESTA EN SEGUNDO PLANO Y SE VUELVE
     * A COLOCAR EN PRIMER PLANO
     */
    protected void onResume()
    {
        //DebugLog.LOGD("ImageTargets::onResume");
        super.onResume();

        // QCAR-specific resume operation
        QCAR.onResume();

        // We may start the camera only if the QCAR SDK has already been
        // initialized
        if (CURRENT_STATUS == STOP_CAMERA)
        {
            updateApplicationStatus(START_CAMERA);

            // Reactivate flash if it was active before pausing the app
            /*if (mFlash)
            {
                boolean result = activateFlash(mFlash);
                DebugLog.LOGI("Turning flash "+(mFlash?"ON":"OFF")+" "+(result?"WORKED":"FAILED")+"!!");
            } */
        }

        // Resume the GL view:
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    public void onConfigurationChanged(Configuration config)
    {
        //DebugLog.LOGD("ImageTargets::onConfigurationChanged");
        super.onConfigurationChanged(config);

        storeScreenDimensions();

        // Set projection matrix:
        if (QCAR.isInitialized())
            NativeInterfaceCaller.setProjectionMatrix();
    }

    /*
     * AL MOMENTO EN QUE LA APLICACION ENTRA EN SEGUNDO PLANO SE EJECUTA ESTE METODO
     */
    protected void onPause()
    {
        //DebugLog.LOGD("ImageTargets::onPause");
        super.onPause();

        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        if (CURRENT_STATUS == START_CAMERA)
        {
            updateApplicationStatus(STOP_CAMERA);
        }


        // QCAR-specific pause operation
        QCAR.onPause();
    }

    /*
     * ESTE METODO SE EJECUTA AL SALIR DE LA APLICACION
     */
    protected void onDestroy()
    {
        //DebugLog.LOGD("ImageTargets::onDestroy");
        super.onDestroy();

        // Dismiss the splash screen time out handler:
        if (initialScreenHandler != null)
        {
            initialScreenHandler.removeCallbacks(initialScreenRunnable);
            initialScreenRunnable = null;
            initialScreenHandler = null;
        }

        // Cancel potentially running tasks
        if (initQCARTask != null &&
                initQCARTask.getStatus() != InitQCARTask.Status.FINISHED)
        {
            initQCARTask.cancel(true);
            initQCARTask = null;
        }

        if (loadTracker != null &&
                loadTracker.getStatus() != LoadTracker.Status.FINISHED)
        {
            loadTracker.cancel(true);
            loadTracker = null;
        }

        // Ensure that all asynchronous operations to initialize QCAR and loading
        // the tracker datasets do not overlap:
        synchronized (shutdownLock) {

            // Do application deinitialization in native code
            //deinitApplicationNative();

            // Unload texture
            //mTextures.clear();
            //mTextures = null;

            // Destroy the tracking data set:
            NativeInterfaceCaller.destroyTrackerData();

            // Deinit the tracker:
            NativeInterfaceCaller.deinitTracker();

            // Deinitialize QCAR SDK
            QCAR.deinit();
        }

        System.gc();
    }

    /*
     * METODO QUE INICIA LA ACTIVIDAD DE MENU EN EL MOMENTO QUE SE HACE UN TRACKING DE ALGUNA IMAGEN
     */
    public  void onImageTrack(int trackerId){
        LoggerPrint.INFO("onImageTrack - Obteniendo el ID de la imagen");
        updateApplicationStatus(STOP_CAMERA);
        LoggerPrint.INFO("Iniciando Actividad MenuActivity");
        Intent intent= new Intent(this, MenuActivity.class);

        switch(trackerId){
            case Contents.CONTENT_ID1:
                intent.putExtra("id",String.valueOf(trackerId));
                break;
            case Contents.CONTENT_ID2:
                intent.putExtra("id",String.valueOf(trackerId));
                break;
        }
        startActivity(intent);
    }


}
