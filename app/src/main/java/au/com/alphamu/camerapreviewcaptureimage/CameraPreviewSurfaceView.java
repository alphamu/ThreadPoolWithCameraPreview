package au.com.alphamu.camerapreviewcaptureimage;

/**
 * Created by Ali Muzaffar on 19/12/2015.
 */

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
public class CameraPreviewSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private final String TAG = "Preview";
    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera.Size mPreviewSize;
    Camera.Size mPictureSize;
    List<Camera.Size> mSupportedPreviewSizes;
    List<Camera.Size> mSupportedPictureSizes;
    Camera mCamera;

    float mOrientation = 0;

    public CameraPreviewSurfaceView(Context context) {
        super(context);
        mSurfaceView = new SurfaceView(context);
        mSurfaceView = this;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public CameraPreviewSurfaceView(Context context, AttributeSet set) {
        super(context, set);
        mSurfaceView = new SurfaceView(context);
        mSurfaceView = this;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            mSupportedPictureSizes = mCamera.getParameters().getSupportedPictureSizes();
            requestLayout();
        }
    }

    public void switchCamera(Camera camera) {
        setCamera(camera);
        try {
            camera.setPreviewDisplay(mHolder);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        Camera.Parameters parameters = camera.getParameters();
        fixOrientation(parameters);
        final int width = resolveSize(getSuggestedMinimumWidth(), getWidth());
        final int height = resolveSize(getSuggestedMinimumHeight(), getHeight());
        mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        mPictureSize = getOptimalPreviewSize(mSupportedPictureSizes, 786, 1024);
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.setPictureSize(mPictureSize.width, mPictureSize.height);
        requestLayout();
        camera.setParameters(parameters);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
            mPictureSize = getOptimalPreviewSize(mSupportedPictureSizes, 786, 1024);
        }
    }
/*
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);
            final int width = r - l;
            final int height = b - t;
            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }
            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }
*/
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();

        fixOrientation(parameters);
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.setPictureSize(mPictureSize.width, mPictureSize.height);
        requestLayout();
        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    private void fixOrientation(Camera.Parameters parameters) {
        Display display = ((WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        if (!isRunningOnEmulator()) {
            // The preview is rotated on devices, so we have to straighten it.
            // We do not need to do this in an emulator
            if (display.getRotation() == Surface.ROTATION_0) {
                //parameters.setPreviewSize(mPreviewSize.height, mPreviewSize.width);
                mOrientation = 90;
                mCamera.setDisplayOrientation(90);
            }

            if (display.getRotation() == Surface.ROTATION_90) {
                //parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                mOrientation = 0;
            }

            if (display.getRotation() == Surface.ROTATION_180) {
                //parameters.setPreviewSize(mPreviewSize.height, mPreviewSize.width);
                mOrientation = 0;
            }

            if (display.getRotation() == Surface.ROTATION_270) {
                //parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                mOrientation = 180;
                mCamera.setDisplayOrientation(180);
            }
        }
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
    }

    public boolean isRunningOnEmulator() {
        boolean result =//
                Build.FINGERPRINT.startsWith("generic")//
                        || Build.FINGERPRINT.startsWith("unknown")//
                        || Build.MODEL.contains("google_sdk")//
                        || Build.MODEL.contains("Emulator")//
                        || Build.MODEL.contains("Android SDK built for x86")
                        || Build.MANUFACTURER.contains("Genymotion");
        if (result)
            return true;
        result |= Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic");
        if (result)
            return true;
        result |= "google_sdk".equals(Build.PRODUCT);
        return result;
    }


    public float getOrientation() {
        return mOrientation;
    }

    public Camera.Size getPreviewSize() {
        return mPreviewSize;
    }

}