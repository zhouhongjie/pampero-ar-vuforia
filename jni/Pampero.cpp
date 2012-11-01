#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

#ifdef USE_OPENGL_ES_1_1
#include <GLES/gl.h>
#include <GLES/glext.h>
#else
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#endif

#include <QCAR/QCAR.h>
#include <QCAR/CameraDevice.h>
#include <QCAR/Renderer.h>
#include <QCAR/VideoBackgroundConfig.h>
#include <QCAR/Trackable.h>
#include <QCAR/Tool.h>
#include <QCAR/Tracker.h>
#include <QCAR/TrackerManager.h>
#include <QCAR/ImageTracker.h>
#include <QCAR/CameraCalibration.h>
#include <QCAR/UpdateCallback.h>
#include <QCAR/DataSet.h>
#include "PamperoUtils.h"
//#include "CubeShaders.h"
//#include "Texture.h"
//#include "Teapot.h"

#ifdef __cplusplus
extern "C"
{
#endif

#ifdef USE_OPENGL_ES_2_0
unsigned int shaderProgramID    = 0;
GLint vertexHandle              = 0;
GLint normalHandle              = 0;
GLint textureCoordHandle        = 0;
GLint mvpMatrixHandle           = 0;
#endif

bool isActivityInPortraitMode   = false;
unsigned int screenWidth        = 0;
unsigned int screenHeight       = 0;
//QCAR::DataSet* dataSetPampero1  = 0;
QCAR::DataSet* dataSetBelmont    = 0;
//QCAR::DataSet* dataSetChips    = 0;
//QCAR::DataSet* dataSets[2];

bool switchDataSetAsap          = false;
QCAR::Matrix44F projectionMatrix;
static const float kObjectScale = 3.f;

// Object to receive update callbacks from QCAR SDK
class Pampero_UpdateCallback : public QCAR::UpdateCallback
{
    virtual void QCAR_onUpdate(QCAR::State& /*state*/)
    {
        if (switchDataSetAsap)
        {
            switchDataSetAsap = false;

            // Get the image tracker:
            QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
            QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
                trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER));
            if (imageTracker == 0 || dataSetBelmont == 0)
            {
                LOG("Failed to switch data set.");
                return;
            }

            /*if (imageTracker->getActiveDataSet() == dataSetBelmont)
            {
                imageTracker->deactivateDataSet(dataSetBelmont);
                imageTracker->activateDataSet(dataSetBelmont);
            }
            else
            {
                imageTracker->deactivateDataSet(dataSetBelmont);
                imageTracker->activateDataSet(dataSetBelmont);
            } */
        }
    }
};

Pampero_UpdateCallback updateCallback;

JNIEXPORT int JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_getOpenGlEsVersionNative(JNIEnv *, jobject)
{
#ifdef USE_OPENGL_ES_1_1
    return 1;
#else
    return 2;
#endif
}

JNIEXPORT void JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_setActivityPortraitMode(JNIEnv *, jobject, jboolean isPortrait)
{
    isActivityInPortraitMode = isPortrait;
}


JNIEXPORT void JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_switchDatasetAsap(JNIEnv *, jobject)
{
    switchDataSetAsap = true;
}


JNIEXPORT int JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_initTracker(JNIEnv *, jobject)
{
    LOG("Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_initTracker");

    // Initialize the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* tracker = trackerManager.initTracker(QCAR::Tracker::IMAGE_TRACKER);
    if (tracker == NULL)
    {
        LOG("Failed to initialize ImageTracker.");
        return 0;
    }

    LOG("Successfully initialized ImageTracker.");
    return 1;
}

JNIEXPORT void JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_deinitTracker(JNIEnv *, jobject)
{
    LOG("Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_deinitTracker");

    // Deinit the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    trackerManager.deinitTracker(QCAR::Tracker::IMAGE_TRACKER);
}

JNIEXPORT int JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_loadTrackerData(JNIEnv *, jobject)
{
    LOG("Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_loadTrackerData");

    // Get the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER));
    if (imageTracker == NULL)
    {
        LOG("Failed to load tracking data set because the ImageTracker has not"
            " been initialized.");
        return 0;
    }

    dataSetBelmont = imageTracker->createDataSet();
    if (dataSetBelmont == 0)
    {
        LOG("Failed to create a new tracking data.");
        return 0;
    }

    if (!dataSetBelmont->load("BelmontAndPromocion.xml", QCAR::DataSet::STORAGE_APPRESOURCE))
    {
        LOG("Failed to load data set.");
        return 0;
    }

    // Activate the data set:
    if (!imageTracker->activateDataSet(dataSetBelmont))
    {
        LOG("Failed to activate data set.");
        return 0;
    }
      
    LOG("Successfully loaded data sets.");
    return 1;
}

JNIEXPORT int JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_destroyTrackerData(JNIEnv *, jobject)
{
    LOG("Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_destroyTrackerData");

    // Get the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
        trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER));
    if (imageTracker == NULL)
    {
        LOG("Failed to destroy the tracking data set because the ImageTracker has not"
            " been initialized.");
        return 0;
    }

    if (dataSetBelmont != 0)
    {
        if (imageTracker->getActiveDataSet() == dataSetBelmont &&
            !imageTracker->deactivateDataSet(dataSetBelmont))
        {
            LOG("Failed to destroy the tracking data set Tarmac because the data set "
                "could not be deactivated.");
            return 0;
        }

        if (!imageTracker->destroyDataSet(dataSetBelmont))
        {
            LOG("Failed to destroy the tracking data set Tarmac.");
            return 0;
        }

        LOG("Successfully destroyed the data set Tarmac.");
        dataSetBelmont = 0;
    }

    return 1;
}

JNIEXPORT void JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_onQCARInitializedNative(JNIEnv *, jobject)
{
    // Register the update callback where we handle the data set swap:
    //QCAR::setHint(QCAR::HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 2);
    //QCAR::setHint(QCAR::HINT_IMAGE_TARGET_MULTI_FRAME_ENABLED, 1);
    QCAR::registerCallback(&updateCallback);
}

void
configureVideoBackground()
{
    // Get the default video mode:
    QCAR::CameraDevice& cameraDevice = QCAR::CameraDevice::getInstance();
    QCAR::VideoMode videoMode = cameraDevice.
                                getVideoMode(QCAR::CameraDevice::MODE_DEFAULT);


    // Configure the video background
    QCAR::VideoBackgroundConfig config;
    config.mEnabled = true;
    config.mSynchronous = true;
    config.mPosition.data[0] = 0.0f;
    config.mPosition.data[1] = 0.0f;

    if (isActivityInPortraitMode)
    {
        //LOG("configureVideoBackground PORTRAIT");
        config.mSize.data[0] = videoMode.mHeight
                                * (screenHeight / (float)videoMode.mWidth);
        config.mSize.data[1] = screenHeight;

        if(config.mSize.data[0] < screenWidth)
        {
            LOG("Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
            config.mSize.data[0] = screenWidth;
            config.mSize.data[1] = screenWidth *
                              (videoMode.mWidth / (float)videoMode.mHeight);
        }
    }
    else
    {
        //LOG("configureVideoBackground LANDSCAPE");
        config.mSize.data[0] = screenWidth;
        config.mSize.data[1] = videoMode.mHeight
                            * (screenWidth / (float)videoMode.mWidth);

        if(config.mSize.data[1] < screenHeight)
        {
            LOG("Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
            config.mSize.data[0] = screenHeight
                                * (videoMode.mWidth / (float)videoMode.mHeight);
            config.mSize.data[1] = screenHeight;
        }
    }

    LOG("Configure Video Background : Video (%d,%d), Screen (%d,%d), mSize (%d,%d)", videoMode.mWidth, videoMode.mHeight, screenWidth, screenHeight, config.mSize.data[0], config.mSize.data[1]);

    // Set the config:
    QCAR::Renderer::getInstance().setVideoBackgroundConfig(config);
}

JNIEXPORT void JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_startCamera(JNIEnv *, jobject)
{
    LOG("Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_startCamera");

    // Initialize the camera:
    if (!QCAR::CameraDevice::getInstance().init()){
        LOG("ERROR INICIALIZANDO EL CONTEXTO DE LA CAMARA");
        return;
    }

    // Configure the video background
    configureVideoBackground();

    // Select the default mode:
    if (!QCAR::CameraDevice::getInstance().selectVideoMode(
                                QCAR::CameraDevice::MODE_DEFAULT))
        return;

    //setting autofocus mode
    if (!QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_TRIGGERAUTO))
        LOG("This autofocus mode is not supported by this device");

    // Start the camera:
    if (!QCAR::CameraDevice::getInstance().start())
        return;

    // Start the tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* imageTracker = trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER);
    if(imageTracker != 0)
        imageTracker->start();
}

JNIEXPORT void JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_stopCamera(JNIEnv *, jobject)
{
    LOG("Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_stopCamera");

    // Stop the tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* imageTracker = trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER);
    if(imageTracker != 0)
        imageTracker->stop();

    QCAR::CameraDevice::getInstance().stop();
    QCAR::CameraDevice::getInstance().deinit();
}


JNIEXPORT void JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_setProjectionMatrix(JNIEnv *, jobject)
{
    LOG("Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_setProjectionMatrix");

    // Cache the projection matrix:
    const QCAR::CameraCalibration& cameraCalibration =
                                QCAR::CameraDevice::getInstance().getCameraCalibration();
    projectionMatrix = QCAR::Tool::getProjectionGL(cameraCalibration, 2.0f,
                                            2000.0f);
}

JNIEXPORT void JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_initRendering(JNIEnv* env, jobject obj)
{
    LOG("Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_initRendering");
    // Define clear color
    glClearColor(0.0f, 0.0f, 0.0f, QCAR::requiresAlpha() ? 0.0f : 1.0f);
}


JNIEXPORT void JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_updateRendering(JNIEnv* env, jobject obj, jint width, jint height)
{
    LOG("Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_updateRendering");

    // Update screen dimensions
    screenWidth = width;
    screenHeight = height;

    // Reconfigure the video background
    configureVideoBackground();
}

JNIEXPORT int JNICALL
Java_ve_com_pampero_TagTracker_NativeInterfaceCaller_renderFrame(JNIEnv *, jobject)
{

    // Get the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER));
    // Clear color and depth buffer
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    // Get the state from QCAR and mark the beginning of a rendering section
    QCAR::State state = QCAR::Renderer::getInstance().begin();
    // Explicitly render the Video Background
    QCAR::Renderer::getInstance().drawVideoBackground();

    #ifdef USE_OPENGL_ES_1_1
        // Set GL11 flags:
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);

    #endif

    glEnable(GL_DEPTH_TEST);
    glEnable(GL_CULL_FACE);
    for(int tIdx = 0; tIdx < state.getNumActiveTrackables(); tIdx++){
        const QCAR::Trackable* trackable = state.getActiveTrackable(tIdx);
        QCAR::Matrix44F modelViewMatrix = QCAR::Tool::convertPose2GLMatrix(trackable->getPose());

        if (strcmp(trackable->getName(), "Promocion")){
            return 1;
        }
        if (strcmp(trackable->getName(), "Belmont")){
            return 2;
        }

    }

    glDisable(GL_DEPTH_TEST);

#ifdef USE_OPENGL_ES_1_1
    glDisable(GL_TEXTURE_2D);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_NORMAL_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
#else
    glDisableVertexAttribArray(vertexHandle);
    glDisableVertexAttribArray(normalHandle);
    glDisableVertexAttribArray(textureCoordHandle);
#endif

    QCAR::Renderer::getInstance().end();
    return -1;
}

#ifdef __cplusplus
}
#endif