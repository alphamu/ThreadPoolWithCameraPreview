package au.com.alphamu.camerapreviewcaptureimage;


import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import java.math.BigDecimal;


/**
 * A simple {@link Fragment} subclass.
 */
public class CameraPreviewFragment extends Fragment {

    private CameraPreviewSurfaceView mPreview;
    private ViewGroup mContainer;
    private RecyclerView mRecycler;
    private ImageButton mButtonClick;
    private TextView mCounterView;
    private CheckBox mCheckBox;

    private CameraThumbnailImageAdapter mAdapter;
    private CounterAsyncTask mCounterTask;

    Camera mCamera;
    int mNumberOfCameras;
    int cameraCurrentlyLocked;
    // The first rear facing camera
    int defaultCameraId;
    private CameraHandlerThread mThread = null;

    public CameraPreviewFragment() {
        // Required empty public constructor
    }

    public static CameraPreviewFragment newInstance() {
        return new CameraPreviewFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_camera_preview, container, false);

        mContainer = (ViewGroup) v.findViewById(R.id.preview_container);
        mPreview = (CameraPreviewSurfaceView) v.findViewById(R.id.camera_preview);
        mRecycler = (RecyclerView) v.findViewById(R.id.images);
        mButtonClick = (ImageButton) v.findViewById(R.id.button_click);
        mCounterView = (TextView) v.findViewById(R.id.count);
        mCheckBox = (CheckBox) v.findViewById(R.id.checkbox_threads);

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        mRecycler.setLayoutManager(layoutManager);

        mRecycler.setAdapter(mAdapter = new CameraThumbnailImageAdapter());

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Find the total number of cameras available
        mNumberOfCameras = Camera.getNumberOfCameras();
        // Find the ID of the default camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                defaultCameraId = i;
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        // Open the default i.e. the first rear facing camera.
        openCameraNew();
        cameraCurrentlyLocked = defaultCameraId;
        mPreview.setCamera(mCamera);
        if (mPreview.getParent() == null) {
            mContainer.addView(mPreview);
        }
        mCounterTask = new CounterAsyncTask(mCounterView);
        mCounterTask.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
            mContainer.removeView(mPreview);
        }

        if (mThread != null) {
            mThread.quit();
            mThread = null;
        }

        mCounterTask.cancel();
    }

    public void openCameraOld() {
        mCamera = Camera.open();
    }

    private void openCameraNew() {
        if (mThread == null) {
            mThread = new CameraHandlerThread(this);
            mButtonClick.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setEnabled(false); //Only take one picture at a time.
                    //mCamera.takePicture(null, null, mThread);
                    mCamera.setPreviewCallback(mThread);
                    mThread.startBurst();
                }
            });
        }

        synchronized (mThread) {
            mThread.openCamera();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate our menu which can gather user input for switching camera
        inflater.inflate(R.menu.camera_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.switch_cam:
                // check for availability of multiple cameras
                if (mNumberOfCameras == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(this.getString(R.string.camera_alert))
                            .setNeutralButton("Close", null);
                    AlertDialog alert = builder.create();
                    alert.show();
                    return true;
                }
                // OK, we have multiple cameras.
                // Release this camera -> cameraCurrentlyLocked
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mPreview.setCamera(null);
                    mCamera.release();
                    mCamera = null;
                }
                // Acquire the next camera and request Preview to reconfigure
                // parameters.
                mCamera = Camera
                        .open((cameraCurrentlyLocked + 1) % mNumberOfCameras);
                cameraCurrentlyLocked = (cameraCurrentlyLocked + 1)
                        % mNumberOfCameras;
                mPreview.switchCamera(mCamera);
                // Start the preview
                mCamera.startPreview();
                return true;
            case R.id.terms:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://cdn.rawgit.com/alphamu/ThreadPoolWithCameraPreview/master/privacypolicy.html"));
                startActivity(browserIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void readyForPicture() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mButtonClick.setEnabled(true);
            }
        });
    }

    public void addImage(Bitmap bitmap, String fileName) {
        mAdapter.add(bitmap, fileName); //still on BG thread.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }


    public float getOrientation() {
        return mPreview.getOrientation();
    }

    public int getCameraCurrentlyLocked() {
        return cameraCurrentlyLocked;
    }

    public Camera.Size getCameraPreviewSize() {
        return mPreview.getPreviewSize();
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

    public boolean getUseThreadPool() {
        return mCheckBox.isChecked();
    }

    private static class CounterAsyncTask extends AsyncTask<Integer, Integer, Void> {

        private final BigDecimal SIXTY = new BigDecimal(60);
        TextView mView = null;
        boolean mCancelled = false;

        public CounterAsyncTask(TextView view) {
            mView = view;
        }

        @Override
        protected Void doInBackground(Integer... params) {
            mCancelled = false;
            try {
                int count = 0;
                do {
                    Thread.sleep(100);
                    publishProgress(++count);
                } while (!mCancelled && count < Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mView.setText(String.format("%d", values[0]));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        public void cancel() {
            mCancelled = true;
        }
    }
}
