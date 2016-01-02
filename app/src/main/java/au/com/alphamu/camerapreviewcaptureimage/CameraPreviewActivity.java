package au.com.alphamu.camerapreviewcaptureimage;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
// ----------------------------------------------------------------------
public class CameraPreviewActivity extends AppCompatActivity
        implements CameraPermissionHelper.CameraPermissionCallback {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the window title.
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        //mPreview = new Preview(this);
        setContentView(R.layout.activity_preview);
        CameraPermissionHelper.attach(getSupportFragmentManager());
    }


    @Override
    public void onCameraPermissionResult(boolean successful) {
        if (successful) {
            findViewById(R.id.container).postDelayed(new Runnable() {
                @Override
                public void run() {
                    CameraPreviewFragment f = CameraPreviewFragment.newInstance();
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
                }
            }, 500);
        }
    }
}