package au.com.alphamu.camerapreviewcaptureimage;

import android.content.Context;

import java.io.File;

public class CameraConstants {
    private static File sUploadDir;

    public static File getUploadDir(Context context) {
        if (sUploadDir == null) {
            sUploadDir = new File(context.getFilesDir(), "to_upload");
        }
        return sUploadDir;
    }

}
