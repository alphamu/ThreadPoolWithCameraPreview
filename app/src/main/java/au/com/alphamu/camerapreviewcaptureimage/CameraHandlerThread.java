package au.com.alphamu.camerapreviewcaptureimage;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.lang.ref.WeakReference;

public class CameraHandlerThread extends HandlerThread implements Camera.PictureCallback, Camera.PreviewCallback {
    private static String TAG = "CameraHandlerThread";

    Handler mHandler = null;
    WeakReference<CameraPreviewFragment> ref = null;

    private PictureUploadHandlerThread mPictureUploadThread;
    private boolean mBurst = false;
    private int mCounter = 1;

    CameraHandlerThread(CameraPreviewFragment cameraPreview) {
        super(TAG);
        start();
        mHandler = new Handler(getLooper());
        ref = new WeakReference<>(cameraPreview);
    }

    synchronized void notifyCameraOpened() {
        notify();
    }

    public void openCamera() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (ref.get() != null) {
                    ref.get().openCameraOld();
                    notifyCameraOpened();
                }
            }
        });
        try {
            wait();
        } catch (InterruptedException e) {
            Log.w("HANDLER", "wait was interrupted");
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        camera.stopPreview();
        CameraPreviewFragment f = ref.get();
        if (f != null) {
            if (mPictureUploadThread == null) {
                mPictureUploadThread = new PictureUploadHandlerThread(f);
                mPictureUploadThread.startUpload();
            }
            mPictureUploadThread.queueMakeBitmap(data, camera);
            f.readyForPicture();
        }
        camera.startPreview();
    }

    @Override
    public boolean quit() {
        if (mPictureUploadThread != null) {
            mPictureUploadThread.quit();
        }
        return super.quit();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean quitSafely() {
        if (mPictureUploadThread != null) {
            mPictureUploadThread.quitSafely();
        }
        return super.quitSafely();
    }

    public void startBurst() {
        mBurst = true;
    }

    public void stopBurst() {
        mBurst = false;
        interrupt();
    }



    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mBurst) {
            CameraPreviewFragment f = ref.get();
            if (f != null) {
                if (mPictureUploadThread == null) {
                    mPictureUploadThread = new PictureUploadHandlerThread(f);
                    mPictureUploadThread.startUpload();
                }
                mPictureUploadThread.queueMakeBitmapFromPreview(data, camera);
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (f.isAdded()) {
                    f.readyForPicture();
                }
            }
            if (mCounter++ == 10) {
                mBurst = false;
                mCounter = 1;
            }
        }
    }
}
