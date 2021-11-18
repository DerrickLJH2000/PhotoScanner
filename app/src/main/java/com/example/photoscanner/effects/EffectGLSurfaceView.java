package com.example.photoscanner.effects;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;


public class EffectGLSurfaceView {

    private static final String TAG = EffectGLSurfaceView.class.getCanonicalName();

    private EGLConfigChooser mEGLConfigChooser;
    private EGLContextFactory mEGLContextFactory;
    private EglHelper mEglHelper;
    private int mEGLContextClientVersion = 2;
    private final static boolean LOG_EGL = false;
    private static final int PBUFFER_SURFACE_WIDTH = 7168;
    private static final int MIN_PBUFFER_SURFACE_WIDTH = 256;

//    public void init(){
//        initEGL();
//
//        if (mEglHelper == null){
//            mEglHelper = new EglHelper(EffectGLSurfaceView.this);
//            mEglHelper.start(PBUFFER_SURFACE_WIDTH, PBUFFER_SURFACE_WIDTH);
//        }
//    }

    public void init(int bitmapWidth, int bitmapHeight){
         initEGL();

         if (mEglHelper == null){
             mEglHelper = new EglHelper(EffectGLSurfaceView.this);
             mEglHelper.start(bitmapWidth, bitmapHeight);
         }
    }

    public int getMaxPBufferWidth(){
        initEGL();

        mEglHelper = new EglHelper(EffectGLSurfaceView.this);
        try {
            int nWidth = mEglHelper.getMaxPBufferWidth();
            mEglHelper = null;
            return nWidth;
        }catch(Throwable e){
            e.printStackTrace();
        }
        return 0;
    }

    private void initEGL(){
        if (mEGLConfigChooser == null) {
            mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        }
        if (mEGLContextFactory == null) {
            mEGLContextFactory = new DefaultContextFactory();
        }
    }

    public boolean makeCurrent(){
        Log.d(TAG, "makeCurrent::");

        if (mEglHelper != null){
            return mEglHelper.makeCurrent();
        }
        return false;
    }

    public void swap(){
        Log.d(TAG, "swap::");

        if (mEglHelper != null){
            mEglHelper.swap();
        }
    }

    public void close(){
        Log.d(TAG, "close::");

        if (mEglHelper != null){
            mEglHelper.destroySurface();
            mEglHelper.finish();
            mEglHelper = null;
        }
    }

    public interface EGLContextFactory {
        EGLContext createContext(EGLDisplay display, EGLConfig eglConfig);
        void destroyContext(EGLDisplay display, EGLContext context);
    }

    private class DefaultContextFactory implements EGLContextFactory {
        private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        public EGLContext createContext(EGLDisplay display, EGLConfig config) {
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                    EGL14.EGL_NONE };

            return EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, mEGLContextClientVersion != 0 ? attrib_list : null,0);
        }

        public void destroyContext(EGLDisplay display,
                                   EGLContext context) {
            if (!EGL14.eglDestroyContext(display, context)) {
                Log.e("DefaultContextFactory", "display:" + display + " context: " + context);

                EglHelper.throwEglException("eglDestroyContex", EGL14.eglGetError());
            }
        }
    }

    public interface EGLConfigChooser {

        EGLConfig chooseConfig(EGLDisplay display);
    }

    private abstract class BaseConfigChooser
            implements EGLConfigChooser {
        public BaseConfigChooser(int[] configSpec) {
            mConfigSpec = filterConfigSpec(configSpec);
        }

        public EGLConfig chooseConfig(EGLDisplay display) {
            int[] num_config = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            if (!EGL14.eglChooseConfig(display, mConfigSpec,0, configs,0, configs.length,num_config,0)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            EGLConfig config = chooseConfig( display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGLDisplay display,
                                                       EGLConfig[] configs);

        protected int[] mConfigSpec;

        private int[] filterConfigSpec(int[] configSpec) {
            if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
                return configSpec;
            }
            /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
             * And we know the configSpec is well formed.
             */
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len-1);
            newConfigSpec[len-1] = EGL14.EGL_RENDERABLE_TYPE;
            if (mEGLContextClientVersion == 2) {
                newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT;  /* EGL_OPENGL_ES2_BIT */
            } else {
                newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR; /* EGL_OPENGL_ES3_BIT_KHR */
            }
            newConfigSpec[len+1] = EGL14.EGL_NONE;
            return newConfigSpec;
        }
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes,
     * and at least the specified depth and stencil sizes.
     */
    private class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                                    int alphaSize, int depthSize, int stencilSize) {
            super(new int[] {
                    EGL14.EGL_RED_SIZE, redSize,
                    EGL14.EGL_GREEN_SIZE, greenSize,
                    EGL14.EGL_BLUE_SIZE, blueSize,
                    EGL14.EGL_ALPHA_SIZE, alphaSize,
                    EGL14.EGL_DEPTH_SIZE, depthSize,
                    EGL14.EGL_STENCIL_SIZE, stencilSize,
                    EGL14.EGL_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
        }

        @Override
        public EGLConfig chooseConfig(EGLDisplay display,
                                                     EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib( display, config,
                        EGL14.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib( display, config,
                        EGL14.EGL_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib( display, config,
                            EGL14.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib( display, config,
                            EGL14.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib( display, config,
                            EGL14.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib( display, config,
                            EGL14.EGL_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGLDisplay display,
                                     EGLConfig config, int attribute, int defaultValue) {

            if (EGL14.eglGetConfigAttrib(display, config, attribute, mValue,0)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private int[] mValue;
        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
    }

    private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            super(8, 8, 8, 8, withDepthBuffer ? 16 : 0, 0);
        }
    }

    /**
     * An EGL helper class.
     */

    private static class EglHelper {

        private int maxPBufferWidth = 0;
        private int mBitmapWidth;
        private int mBitmapHeight;

        public EglHelper(EffectGLSurfaceView view) {
            this.view = view;
        }

        /**
         * Initialize EGL for a given configuration spec.
         */
        public void start(int bitmapWidth, int bitmapHeight) {
            if (LOG_EGL) {
                Log.w("EglHelper", "start() tid=" + Thread.currentThread().getId());
                Log.w("EglHelper", "start() name=" + Thread.currentThread().getName());

            }
            mBitmapWidth = bitmapWidth;
            mBitmapHeight = bitmapHeight;
            Log.d("EglHelper", "start() bitmap=" + bitmapWidth + " " + bitmapHeight);
            /*
             * Get to the default display.
             */
            mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

            if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }

            /*
             * We can now initialize EGL for that display
             */
            int[] version = new int[2];
            if(!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
                throw new RuntimeException("eglInitialize failed");
            }

            if (view == null) {
                mEglConfig = null;
                mEglContext = null;
            } else {
                mEglConfig = view.mEGLConfigChooser.chooseConfig( mEglDisplay);

                /*
                 * Create an EGL context. We want to do this as rarely as we can, because an
                 * EGL context is a somewhat heavy object.
                 */
                mEglContext = view.mEGLContextFactory.createContext( mEglDisplay, mEglConfig);
            }
            if (mEglContext == null || mEglContext == EGL14.EGL_NO_CONTEXT) {
                mEglContext = null;
                throwEglException("createContext");
            }
            if (LOG_EGL) {
                Log.w("EglHelper", "createContext " + mEglContext + " tid=" + Thread.currentThread().getId());
            }

            mEglSurface = null;

            createSurface();
        }

        /**
         * Create an egl surface for the current SurfaceHolder surface. If a surface
         * already exists, destroy it before creating the new surface.
         *
         * @return true if the surface was created successfully.
         */
        private boolean createSurface() {
            if (LOG_EGL) {
                Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().getId());
                Log.w("EglHelper", "createSurface() name=" + Thread.currentThread().getName());
            }

            if (mEglDisplay == null) {
                throw new RuntimeException("eglDisplay not initialized");
            }
            if (mEglConfig == null) {
                throw new RuntimeException("mEglConfig not initialized");
            }

            /*
             *  The window size has changed, so we need to create a new
             *  surface.
             */
            destroySurfaceImp();

            maxPBufferWidth = computePBufferWidth();
            int[] surfaceAttribs = {
                    EGL14.EGL_WIDTH, maxPBufferWidth,
                    EGL14.EGL_HEIGHT, maxPBufferWidth,
                    EGL14.EGL_NONE
            };
            mEglSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig, surfaceAttribs, 0);

            if (mEglSurface == null || mEglSurface == EGL14.EGL_NO_SURFACE) {
                int error = EGL14.eglGetError();
                if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
                    Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }
                return false;
            }

            /*
             * Before we can issue GL commands, we need to make sure
             * the context is current and bound to a surface.
             */
            if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                /*
                 * Could not make the context current, probably because the underlying
                 * SurfaceView surface has been destroyed.
                 */
                logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", EGL14.eglGetError());
                return false;
            }

            return true;
        }

        public int getMaxPBufferWidth(){
            if (LOG_EGL) {
                Log.w("EglHelper", "getMaxPBufferWidth() tid=" + Thread.currentThread().getId());
                Log.w("EglHelper", "getMaxPBufferWidth() name=" + Thread.currentThread().getName());
            }

            /*
             * Get to the default display.
             */
            EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }

            EGLConfig config;
            if (view == null) {
                config = null;
            } else {
                config = view.mEGLConfigChooser.chooseConfig(eglDisplay);
            }

            int[] maxWidth = new int[1];
            int[] maxHeight = new int[1];
            EGL14.eglGetConfigAttrib(eglDisplay, config, EGL14.EGL_MAX_PBUFFER_WIDTH, maxWidth, 0);
            EGL14.eglGetConfigAttrib(eglDisplay, config, EGL14.EGL_MAX_PBUFFER_HEIGHT, maxHeight, 0);
            Log.d("EglHelper", "getMaxPBufferWidth maxWidth=" + maxWidth[0]);

            return maxWidth[0];
        }

        private int computePBufferWidth() {
            int[] maxWidth = new int[1];
            int[] maxHeight = new int[1];
            EGL14.eglGetConfigAttrib(mEglDisplay, mEglConfig, EGL14.EGL_MAX_PBUFFER_WIDTH, maxWidth, 0);
            EGL14.eglGetConfigAttrib(mEglDisplay, mEglConfig, EGL14.EGL_MAX_PBUFFER_HEIGHT, maxHeight, 0);
            Log.d("EglHelper", "maxWidth: " + maxWidth[0]);
            Log.d("EglHelper", "maxHeight: " + maxHeight[0]);
            int workingWidth = MIN_PBUFFER_SURFACE_WIDTH;

            if (mBitmapWidth == 0 || mBitmapHeight == 0){
                Log.i("EglHelper", "PBufferWidth: " + PBUFFER_SURFACE_WIDTH);
                return PBUFFER_SURFACE_WIDTH;
            }

            while (mBitmapWidth > workingWidth || mBitmapHeight > workingWidth){
                workingWidth *= 2;
            }
//            Log.d("EglHelper", "PBufferWidth: " + workingWidth);

            if (workingWidth > maxWidth[0]){
                workingWidth = maxWidth[0];
            }
            if (workingWidth == 0){
                workingWidth = PBUFFER_SURFACE_WIDTH;
            }
            Log.i("EglHelper", "workingWidth: " + workingWidth);
            return workingWidth;
        }

        private boolean makeCurrent(){
            if (mEglDisplay != EGL14.EGL_NO_DISPLAY && mEglContext != null) {
                if( mEglSurface != null && mEglSurface != EGL14.EGL_NO_SURFACE){
                    if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                        /*
                         * Could not make the context current, probably because the underlying
                         * SurfaceView surface has been destroyed.
                         */
                        logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", EGL14.eglGetError());
                        return false;
                    }

                    return true;
                }else{
                    return false;
                }
            }
            return false;
        }

        /**
         * Display the current render surface.
         * @return the EGL error code from eglSwapBuffers.
         */
        private int swap() {
            if (! EGL14.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                int error = EGL14.eglGetError();
                Log.e("EglHelper", "eglSwapBuffers error=" + error);
                return error;
            }

            return EGL14.EGL_SUCCESS;
        }

        private void destroySurface() {
            if (LOG_EGL) {
                Log.w("EglHelper", "destroySurface()  tid=" + Thread.currentThread().getId());
            }
            destroySurfaceImp();
        }

        private void destroySurfaceImp() {
            EGLDisplay disp = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            Log.w("EglHelper", "destroySurfaceImp() display = " + disp );
            if (disp != EGL14.EGL_NO_DISPLAY) {
                if (mEglSurface != null && mEglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE,
                            EGL14.EGL_NO_SURFACE,
                            EGL14.EGL_NO_CONTEXT);

                    mEglSurface = null;
                    Log.w("EglHelper", "destroySurfaceImp() done" );
                }
            }
        }

        public void finish() {
            if (LOG_EGL) {
                Log.w("EglHelper", "finish() tid=" + Thread.currentThread().getId());
            }
            if (mEglContext != null) {
                if (view != null) {
                    view.mEGLContextFactory.destroyContext( mEglDisplay, mEglContext);
                }
                mEglContext = null;
            }
            if (mEglDisplay != null) {
                EGL14.eglTerminate(mEglDisplay);
                mEglDisplay = null;
            }
        }

        private void throwEglException(String function) {
            throwEglException(function, EGL14.eglGetError());
        }

        public static void throwEglException(String function, int error) {
            String message = formatEglError(function, error);

            throw new RuntimeException(message);
        }

        public static void logEglErrorAsWarning(String tag, String function, int error) {
            Log.w(tag, formatEglError(function, error));
        }

        public static String formatEglError(String function, int error) {
            return function + " failed: " + error ;
        }

        private EffectGLSurfaceView view;
        EGLDisplay mEglDisplay;
        EGLSurface mEglSurface;
        EGLConfig mEglConfig;
        EGLContext mEglContext;

    }
}
