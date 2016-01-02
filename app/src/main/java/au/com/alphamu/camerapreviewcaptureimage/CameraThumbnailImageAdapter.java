package au.com.alphamu.camerapreviewcaptureimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CameraThumbnailImageAdapter extends
        RecyclerView.Adapter<CameraThumbnailImageAdapter.ViewHolder> {
    public static final Integer IMG_READY_FOR_UPLOAD = 0;
    public static final Integer IMG_UPLOAD_IN_PROGRESS = 1;
    public static final Integer IMG_UPLOAD_SUCCESSFUL = 2;
    public static final Integer IMG_UPLOAD_FAILED = 3;

    List<Bitmap> mImages = new ArrayList<>();
    List<String> mFileNames = new ArrayList<>();
    List<Integer> mStatus = new ArrayList<>();

    public CameraThumbnailImageAdapter(){
    }

    public void add(Bitmap bitmap, String filenames) {
        synchronized (mImages) {
            mImages.add(0, bitmap);
            mFileNames.add(0, filenames);
            mStatus.add(0, IMG_READY_FOR_UPLOAD);
        }
    }

    public void remove(String fileName) {
        synchronized (mImages) {
            int index = mFileNames.indexOf(fileName);
            if (index > -1) {
                mImages.remove(index);
                mFileNames.remove(index);
                mStatus.remove(index);
            }
        }
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public CameraThumbnailImageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.view_camera_image, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(CameraThumbnailImageAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Bitmap bitmap = mImages.get(position);

        // Set item views based on the data model
        ImageView imageView = viewHolder.imageView;
        imageView.setImageBitmap(bitmap);
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return mImages.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
        }
    }
}