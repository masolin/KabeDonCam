/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.facetracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends Activity {
    private static final String TAG = "FaceTracker";
    private static final int AR_SHARE_TO_INSTAGRAM = 100;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    public static final String INSTAGRAM_PACKAGE_NAME = "com.instagram.android";
    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    private void shareToInstagram(File file) throws FileNotFoundException {
        Intent intent = prepareShareFileIntent(getInstagramServiceInfo(), file);
//        intent.putExtra(Intent.EXTRA_TEXT, bodyBuilder.toString());
        startActivityForResult(intent, AR_SHARE_TO_INSTAGRAM);
    }

    public Intent prepareShareFileIntent(ActivityInfo activityInfo, File file) throws FileNotFoundException {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        String path = file.getAbsolutePath();
        if (path.endsWith("mp4")) {
            intent.setType("video/*").putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        } else {
            String imageUrl = null;
            try {
                imageUrl = MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), null);
            } catch(OutOfMemoryError ignored) {}
            intent.setType("image/jpg").putExtra(Intent.EXTRA_STREAM, TextUtils.isEmpty(imageUrl) ?
                    Uri.fromFile(file) : Uri.parse(imageUrl));
        }
        if (activityInfo != null) {
            intent.setClassName(activityInfo.packageName, activityInfo.name);
        }
        // File cannot be deleted as the intent needs time to process
        // s.delete();
        return intent;

    }

    private ActivityInfo getInstagramServiceInfo() {
        PackageManager pm = getPackageManager();
        final Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
        sendIntent.setType("image/jpg");
        final List<ResolveInfo> otherActs = pm.queryIntentActivities(sendIntent, 0);
        for (ResolveInfo resolveInfo : otherActs) {
            ActivityInfo info = resolveInfo.activityInfo;
            String string = info.packageName;
            if (string.equalsIgnoreCase(INSTAGRAM_PACKAGE_NAME)) {
                return info;
            }
        }
        return null;
    }

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        findViewById(R.id.btn_take_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.takePicture(new CameraSource.PictureCallback() {
                    @Override
                    public void onPictureTaken(final byte[] bytes) {
                        // background thread
                        Task.callInBackground(new Callable<File>() {
                            @Override
                            public File call() throws Exception {
                                Bitmap bm = BitmapUtils.rotateBitmap(BitmapFactory
                                        .decodeByteArray(bytes, 0, bytes.length), 90, true);
                                Canvas canvas = new Canvas(bm);
                                Matrix m = canvas.getMatrix();
//                                float scale = Math.min((float) bm.getWidth() / mGraphicOverlay.getWidth(),
//                                        (float)bm.getHeight() / mGraphicOverlay.getHeight());
//                                m.setScale(scale, scale);
                                m.setScale((float) bm.getWidth() / mGraphicOverlay.getWidth(),
                                        (float) bm.getHeight() / mGraphicOverlay.getHeight());
                                canvas.concat(m);
                                mGraphicOverlay.testDraw(canvas);
                                File file = new File(Environment
                                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "bf1.jpg");
                                bm.compress(Bitmap.CompressFormat.JPEG, 90, new FileOutputStream(file));
                                return file;
                            }
                        }).onSuccess(new Continuation<File, Void>() {
                            @Override
                            public Void then(Task<File> task) throws Exception {
                                shareToInstagram(task.getResult());
                                return null;
                            }
                        }, Task.UI_THREAD_EXECUTOR).continueWith(new Continuation<Void, Void>() {
                            @Override
                            public Void then(Task<Void> task) throws Exception {
                                if (task.isFaulted()) {
                                    task.getError().printStackTrace();
                                }
                                return null;
                            }
                        });


                    }
                });
            }
        });

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
//                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }
        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640*2, 480*2)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraSource.release();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        try {
            mPreview.start(mCameraSource, mGraphicOverlay);

        } catch (IOException e) {
            Log.e(TAG, "Unable to start camera source.", e);
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
//            try {
//                mFaceGraphic.addHand(BitmapFactory.decodeStream(getAssets().open("hand.png")));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            try {
//                mFaceGraphic.setFeedPicture(BitmapFactory.decodeStream(getAssets().open("feed.png")));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            try {
                mFaceGraphic.setShoulderPicture(BitmapFactory.decodeStream(getAssets().open("shoulder.png")), BitmapFactory.decodeStream(getAssets().open("shoulder2.png")));
            } catch (IOException e) {
                e.printStackTrace();
            }

            mFaceGraphic.setRotation(face.getEulerZ());
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }
}
