ThreadPool with Camera Preview
-------------------------------

This project demonstrate how to use `HandlerThread`s and `ThreadPool`s to burst capture 
images from Android's Camera API on a background thread.
The images are then:

- Converted from YUV `byte[]` to RGB `int[]`.
- Converted from RGB `int[]` to `Bitmap`.
- Bitmap is rotated if needed to fix orientation.
- A Thumbnail size Bitmap is made for displaying.
- Full size bitmap is written to disk as a Jpeg.
- Jpeg image is uploaded to a server.

The CameraPreview YUV `byte[]` is captured on a background `HandlerThread` and then 
ThreadPools are used to perform the operations above to improve speed and performance.

Performance Before TheadPool
=================
[![Performance before ThreadPool](http://img.youtube.com/vi/YmU8ogom_5g/0.jpg)](http://www.youtube.com/watch?v=YmU8ogom_5g)


Performance After ThreadPool
=================
[![Performance after ThreadPool](http://img.youtube.com/vi/77Lh9XpXArw/0.jpg)](http://www.youtube.com/watch?v=77Lh9XpXArw)

Demo App on Google Play
======================
Download the demo app on [Google Play](https://play.google.com/store/apps/details?id=au.com.alphamu.camerapreviewcaptureimage).