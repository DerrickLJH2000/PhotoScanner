package com.example.digitalizedphotobook.effects;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.IntBuffer;

/**
 * Usage:
 * MvEffects.applyFilter(bitmap, MvEffects.Type.AUTOFIX);
 */

public class MvEffects{

    private static final String TAG = MvEffects.class.getCanonicalName();
    private EffectGLSurfaceView mEffectView;
    private int[] mTextures = new int[2];
    private EffectContext mEffectContext;
    private Effect mEffect;
    private TextureRenderer mTexRenderer = new TextureRenderer();
    private int mImageWidth;
    private int mImageHeight;
    private Type mCurrentEffect = Type.NONE;
    private Bitmap mBitmap;
    private static MvEffects effect;
    private boolean isMakeCurrentContext = false;
    private boolean bIsInitialized;

    public enum Type{
        NONE("null"),
        AUTOFIX("autofix"),
        GRAYSCALE("grayscale"),
        SEPIA("sepia"),
        SUNSET("sunset"),
        COLORINTENSIFY("intensify"),
        FILLLIGHT("filllight"),
        SHARPEN("sharpen");


        private String name;

        Type(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    public static Bitmap applyFilter(Bitmap inBitmap, Type type){
        Log.d(TAG, "applyFilter::");

        if (effect == null){
            effect = new MvEffects();
        }
        effect.setEffect(type);
        effect.setBitmap(inBitmap);
        effect.create();
        Bitmap output = effect.render();
        effect.close();

        return output;
    }

    public static int getMaxSurfaceWidthSupported(){
        Log.d(TAG, "getMaxSurfaceWidthSupported::");
        MvEffects effect = new MvEffects();
        return effect.getMaxPBufferWidth();
    }

    // for page rendering
    public static MvEffects init(){
        Log.i(TAG, "init::");
        MvEffects effect = new MvEffects();
        effect.create();
        return effect;
    }

    // for page rendering
    public Bitmap applyFilterForRendering(Bitmap inBitmap, Type type){
        Log.i(TAG, "applyFilterForRendering:: type: " + type);
        setEffect(type);
        setBitmap(inBitmap);
        return render();
    }

    // for page rendering
    public void unInit(){
        Log.i(TAG, "unInit::");
        close();
    }

    private int getMaxPBufferWidth(){
        mEffectView = new EffectGLSurfaceView();
        return mEffectView.getMaxPBufferWidth();
    }

    private MvEffects setEffect(Type effect) {
        Log.d(TAG, "setEffect:: " + effect);

        mCurrentEffect = effect;
        return this;
    }

    private MvEffects setBitmap(Bitmap bitmap) {
        Log.d(TAG, "setBitmap:: " + bitmap);

        mBitmap = bitmap;
        return this;
    }

    private void create(){
        Log.d(TAG, "create::");
        if (mEffectView == null) {
            mEffectView = new EffectGLSurfaceView();
        }
        Log.d(TAG, "create:: mBitmap " + mBitmap);

        if (mBitmap != null) {
            mEffectView.init(mBitmap.getWidth(), mBitmap.getHeight());
        }
        else{
            mEffectView.init(0, 0);
        }
    }

    private void close(){
        Log.d(TAG, "close::");
        deleteTextures();

        mTexRenderer.tearDown();

        if (mEffectContext != null && isMakeCurrentContext){
            mEffectContext.release();
        }
        mEffectContext = null;

        if (mEffectView != null) {
            mEffectView.close();
        }
        mEffectView = null;

    }

    private Bitmap render(){
        Log.d(TAG, "render::");
        long startTime = System.currentTimeMillis();
        Bitmap output = null;
        isMakeCurrentContext = false;

        if (mEffectView != null) {
            isMakeCurrentContext = mEffectView.makeCurrent();
        }

//        Log.d(TAG, "render:: isMakeCurrentContext: " + isMakeCurrentContext);
        if(isMakeCurrentContext) {
            try {
                if (!bIsInitialized || mEffectContext == null) {
//                    Log.d(TAG, "render:: bIsInitialized: " + bIsInitialized);
                    mEffectContext = EffectContext.createWithCurrentGlContext();
                    mTexRenderer.init();
                    bIsInitialized = true;
                }
                loadTextures();

                if (mCurrentEffect != Type.NONE) {
                    //if an effect is chosen initialize it and apply it to the texture
                    initEffect();
                    applyEffect();
                }

                output = getResult();
                Log.i(TAG, "render: timeTaken: " + (System.currentTimeMillis() - startTime) + " ms");

            }catch(Throwable e){
                e.printStackTrace();
            }
        }

        return output;
    }

    private void loadTextures() {
        Log.d(TAG, "loadTextures::");
        deleteTextures();

        // Generate textures
        GLES20.glGenTextures(2, mTextures, 0);
        Log.d(TAG, "loadTextures:: mTextures: " + mTextures[0] + " " + mTextures[1]);

        mImageWidth = mBitmap.getWidth();
        mImageHeight = mBitmap.getHeight();
        mTexRenderer.updateTextureSize(mImageWidth, mImageHeight);

        // Upload to texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

        // Set texture parameters
        GLToolbox.initTexParams();
    }

    private void deleteTextures() {
        Log.d(TAG, "deleteTextures::" + mTextures[0] + " " + mTextures[1]);
        if (mTextures[0] != 0 || mTextures[1] != 0){
            GLES20.glDeleteTextures(2, IntBuffer.wrap(mTextures));
            mTextures[0] = 0;
            mTextures[1] = 0;
        }
    }

    private void initEffect() {
        Log.d(TAG, "initEffect::");
        EffectFactory effectFactory = mEffectContext.getFactory();
        if (mEffect != null) {
            mEffect.release();
        }

        /**
         * Initialize effect
         */
        switch (mCurrentEffect) {

            case NONE:
                break;

            case AUTOFIX:
                if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_AUTOFIX)) {
                    mEffect = effectFactory.createEffect(
                            EffectFactory.EFFECT_AUTOFIX);
                    mEffect.setParameter("scale", 0.5f);
                }
                else{
                    Log.e(TAG, "Effect not supported: " + mCurrentEffect);
                }
                break;

            case GRAYSCALE:
                if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAYSCALE)) {
                    mEffect = effectFactory.createEffect(
                            EffectFactory.EFFECT_GRAYSCALE);
                }
                else{
                    Log.e(TAG, "Effect not supported: " + mCurrentEffect);
                }
                break;

            case SEPIA:
                if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_SEPIA)) {
                    mEffect = effectFactory.createEffect(
                            EffectFactory.EFFECT_SEPIA);
                }
                else{
                    Log.e(TAG, "Effect not supported: " + mCurrentEffect);
                }
                break;

            case SUNSET:
                if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_TEMPERATURE)) {
                    mEffect = effectFactory.createEffect(
                            EffectFactory.EFFECT_TEMPERATURE);
                    mEffect.setParameter("scale", .9f);
                }
                else{
                    Log.e(TAG, "Effect not supported: " + mCurrentEffect);
                }
                break;

            case SHARPEN:
                mEffect = effectFactory.createEffect(
                        EffectFactory.EFFECT_SHARPEN);
                break;
            case COLORINTENSIFY:
                mEffect = effectFactory.createEffect(
                        EffectFactory.EFFECT_BLACKWHITE);
                mEffect.setParameter("black", .1f);
                mEffect.setParameter("white", .7f);
                break;

            case FILLLIGHT:
                mEffect = effectFactory.createEffect(
                        EffectFactory.EFFECT_FILLLIGHT);
                mEffect.setParameter("strength", .8f);
                break;

//            case DUOTONE:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_DUOTONE);
//                mEffect.setParameter("first_color", Color.YELLOW);
//                mEffect.setParameter("second_color", Color.DKGRAY);
//                break;

//            case R.id.brightness:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_BRIGHTNESS);
//                mEffect.setParameter("brightness", 2.0f);
//                break;
//
//            case R.id.contrast:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_CONTRAST);
//                mEffect.setParameter("contrast", 1.4f);
//                break;
//
//            case R.id.crossprocess:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_CROSSPROCESS);
//                break;
//
//            case R.id.documentary:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_DOCUMENTARY);
//                break;
//

//            case R.id.fisheye:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_FISHEYE);
//                mEffect.setParameter("scale", .5f);
//                break;
//
//            case R.id.flipvert:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_FLIP);
//                mEffect.setParameter("vertical", true);
//                break;
//
//            case R.id.fliphor:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_FLIP);
//                mEffect.setParameter("horizontal", true);
//                break;
//
//            case R.id.grain:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_GRAIN);
//                mEffect.setParameter("strength", 1.0f);
//                break;

//
//            case R.id.lomoish:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_LOMOISH);
//                break;
//
//            case R.id.negative:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_NEGATIVE);
//                break;
//
//            case R.id.posterize:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_POSTERIZE);
//                break;
//
//            case R.id.rotate:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_ROTATE);
//                mEffect.setParameter("angle", 180);
//                break;
//
//            case R.id.saturate:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_SATURATE);
//                mEffect.setParameter("scale", .5f);
//                break;
//



//
//            case R.id.tint:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_TINT);
//                mEffect.setParameter("tint", Color.MAGENTA);
//                break;
//
//            case R.id.vignette:
//                mEffect = effectFactory.createEffect(
//                        EffectFactory.EFFECT_VIGNETTE);
//                mEffect.setParameter("scale", .5f);
//                break;

            default:
                break;

        }
    }

    private void applyEffect() {
        Log.d(TAG, "applyEffect::");
        mEffect.apply(mTextures[0], mImageWidth, mImageHeight, mTextures[1]);
    }

    private Bitmap getResult() {
        Log.d(TAG, "getResult::");
        if (mCurrentEffect != Type.NONE) {
            // render the result of applyEffect()
            mTexRenderer.renderTexture(mTextures[1]);
        }
        else {
            // if no effect is chosen, just render the original bitmap
            mTexRenderer.renderTexture(mTextures[0]);
        }

        if (mEffectView != null){
            mEffectView.swap();
        }

        return mTexRenderer.getTextureBitmap();
    }

}
