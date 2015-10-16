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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Landmark;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.MAGENTA,
        Color.RED,
        Color.WHITE,
        Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private Bitmap mHandBitmap;
    private float mRotation;
    private Bitmap mFeedBitmap;
    private Bitmap mLeftShoulder;
    private Bitmap mRightShoulder;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    void setId(int id) {
        mFaceId = id;
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }
    public void addHand(Bitmap bitmap) {
        mHandBitmap = bitmap;
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        drawInCanvas(canvas);
    }
    public void drawInCanvas(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);
//        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
//        float TH = 0.9f;
//        boolean isSmile = face.getIsSmilingProbability() > TH;
//        boolean isLeftEyeOpen = face.getIsLeftEyeOpenProbability() > TH;
//        boolean isRightEyeOpen = face.getIsRightEyeOpenProbability() > TH;
//        canvas.drawText("id: " + mFaceId + "\nSmile:" +  (isSmile ? "yes" : "no"), x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);
//        canvas.drawText("Left Eye:" + (isLeftEyeOpen ? "open" : "close"), x + ID_X_OFFSET, y + ID_Y_OFFSET + 30, mIdPaint);
//        canvas.drawText("Right Eye:" + (isRightEyeOpen ? "open" : "close"), x + ID_X_OFFSET, y + ID_Y_OFFSET + 60, mIdPaint);
//        canvas.drawText("rotation:" + mRotation, x + ID_X_OFFSET, y + ID_Y_OFFSET + 90, mIdPaint);
//
//        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;

//        canvas.drawRect(left, top, right, bottom, mBoxPaint);
//        drawFaceAnnotations(canvas, 1f);
        if (mHandBitmap != null) {
            int handLeft = (int) Math.min(0, left - 610);
            int handTop = (int) top - 30;
            canvas.drawBitmap(mHandBitmap, handLeft, handTop, null);
        }
        if (mFeedBitmap != null && bottom >= canvas.getHeight() / 2 && face.getIsSmilingProbability() > 0.2f) {
            int feedLeft = (int) (left + right) / 2;
            int feedTop = Math.max((int) bottom, canvas.getHeight() - mFeedBitmap.getHeight());
            canvas.drawBitmap(mFeedBitmap, feedLeft, feedTop, null);
        }
        if (mLeftShoulder != null && mRotation < -10) {
            float shoulderScale = canvas.getHeight() / mLeftShoulder.getHeight();
            canvas.save();
            canvas.scale(shoulderScale, shoulderScale);
            canvas.drawBitmap(mLeftShoulder, 0, 0, null);
            canvas.restore();
        }
        if (mRightShoulder != null && mRotation > 10) {
            float shoulderScale = canvas.getHeight() / mRightShoulder.getHeight();
            canvas.save();
            canvas.scale(shoulderScale, shoulderScale);
            canvas.drawBitmap(mRightShoulder, canvas.getWidth() - mRightShoulder.getWidth(), 0, null);
            canvas.restore();
        }
    }

    /**
     * Draws a small circle for each detected landmark, centered at the detected landmark position.
     * <p>
     *
     * Note that eye landmarks are defined to be the midpoint between the detected eye corner
     * positions, which tends to place the eye landmarks at the lower eyelid rather than at the
     * pupil position.
     */
    private void drawFaceAnnotations(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        Face face = mFace;
        for (Landmark landmark : face.getLandmarks()) {

            int cx = (int) (landmark.getPosition().x * scale);
            int cy = (int) (landmark.getPosition().y * scale);
            canvas.drawCircle(cx, cy, 10, paint);
        }
    }

    public void setRotation(float rotation) {
        this.mRotation = rotation;
    }

    public void setFeedPicture(Bitmap bitmap) {
        mFeedBitmap = bitmap;
    }
    public void setShoulderPicture(Bitmap left, Bitmap right) {
        mLeftShoulder = left;
        mRightShoulder = right;
    }
}
