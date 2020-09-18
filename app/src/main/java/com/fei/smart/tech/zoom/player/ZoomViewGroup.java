package com.fei.smart.tech.zoom.player;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 使用时，可以使子View 填充ZoomViewGroup，如果有需要，也可以把FramLayout换成其他类型的ViewGroup
 */
public class ZoomViewGroup extends FrameLayout implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    String TAG = "ZoomViewGroup_";

    private final int DELAY_MILLIS = 16;

    private final float SCALE_MAX = 3.0f;
    private final float SCALE_NORMAL = 1.0f;
    private final float SCALE_MID = SCALE_NORMAL + (SCALE_MAX - SCALE_NORMAL) / 3;
    private final float SCALE_MIN = 0.5f;


    private final float OFFSET_BORDER = 0; //可以超出的边界
    /**
     * 用于双击检测
     */
    private GestureDetector mGestureDetector = null;
    /**
     * 缩放的手势检测
     */
    private ScaleGestureDetector mScaleGestureDetector = null;

    View targetView;

    private int width;
    private int height;
    private int lastWidth;

    float mLastX = 0;
    float mLastY = 0;
    private int lastPointerCount;

    private boolean once = true;

    private boolean isAutoScale;

    /**
     * 用于存放矩阵的9个值
     */
    private final float[] matrixValues = new float[9];


    public ZoomViewGroup(@NonNull Context context) {
        super(context, null);
    }

    public ZoomViewGroup(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        width = getWidth();
        height = getHeight();
        Log.i(TAG, "onGlobalLayout() -- width: " + width + ", height: " + height);
        //判断屏幕方向是否改变了
        if (lastWidth != 0 && lastWidth != width) {
            Log.i(TAG, "屏幕方向是否改变了");
            if (targetView != null) {
                setTargetScale(SCALE_NORMAL);
            }
        }
        lastWidth = width;

        if (once) {
            targetView = getChildAt(0);
            if (targetView != null) {
                initMultiTochEvent();
                once = false;
            }
        }
    }

    private void initMultiTochEvent() {
        //Log.i(TAG, "initMultiTochEvent()");
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isAutoScale == true)
                    return true;
                float x = e.getX();
                float y = e.getY();
                float scaleX = targetView.getScaleX();
                if (scaleX < SCALE_MID) {
                    setPivot(x, y);
                    ZoomViewGroup.this.postDelayed(new ZoomViewGroup.AutoScaleRunnable(SCALE_MID), DELAY_MILLIS);
                    isAutoScale = true;
                } else if (scaleX >= SCALE_MID && scaleX < SCALE_MAX) { //连续双击放大 可放开
                    setPivot(x, y);
                    ZoomViewGroup.this.postDelayed(new ZoomViewGroup.AutoScaleRunnable(SCALE_MAX), DELAY_MILLIS);
                    isAutoScale = true;
                } else {
                    setPivot(x, y);
                    ZoomViewGroup.this.postDelayed(new ZoomViewGroup.AutoScaleRunnable(SCALE_NORMAL), DELAY_MILLIS);
                    isAutoScale = true;
                }
                return true;
            }
        });
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        setOnTouchListener(this);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        float currentScale = targetView.getScaleX();
        if (scaleFactor < SCALE_NORMAL && currentScale > SCALE_MIN) {
            currentScale = currentScale * scaleFactor;
            if (currentScale < SCALE_MIN)
                currentScale = SCALE_MIN;
        }
        if (scaleFactor > SCALE_NORMAL && currentScale < SCALE_MAX) {
            currentScale = currentScale * scaleFactor;
            if (currentScale > SCALE_MAX)
                currentScale = SCALE_MAX;
        }
        setTargetScale(currentScale);
        return true;
    }

    private void setTargetScale(float scale) {
        targetView.setScaleX(scale);
        targetView.setScaleY(scale);
        if (scale >= SCALE_NORMAL) {
            checkMatrixBounds();
        }
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        setPivot(detector.getFocusX(), detector.getFocusY());
        return true;
    }

    private void setPivot(float newPivotX, float newPivotY) {
        float translationX = targetView.getTranslationX() + (targetView.getPivotX() - newPivotX) * (1 - targetView.getScaleX());
        float translationY = targetView.getTranslationY() + (targetView.getPivotY() - newPivotY) * (1 - targetView.getScaleY());
        targetView.setTranslationX(translationX);
        targetView.setTranslationY(translationY);
        targetView.setPivotX(newPivotX);
        targetView.setPivotY(newPivotY);
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event))
            return true;
        mScaleGestureDetector.onTouchEvent(event);

        float x = 0, y = 0;
        // 拿到触摸点的个数
        final int pointerCount = event.getPointerCount();
        // 得到多个触摸点的x与y均值
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }
        x = x / pointerCount;
        y = y / pointerCount;

        /**
         * 每当触摸点发生变化时，重置mLasX , mLastY
         */
        if (pointerCount != lastPointerCount) {
            mLastX = x;
            mLastY = y;
        }

        lastPointerCount = pointerCount;
//        RectF rectF = getMatrixRectF();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                if (rectF.width() > width || rectF.height() > height) {
//                    getParent().requestDisallowInterceptTouchEvent(true);
//                }
                break;
            case MotionEvent.ACTION_MOVE:
//                if (rectF.width() > width || rectF.height() > height) {
//                    getParent().requestDisallowInterceptTouchEvent(true);
//                }
                float dx = x - mLastX;
                float dy = y - mLastY;
                if (dx != 0 && dy != 0) {
                    PointF pointF = checkMatrixBounds(dx, dy);
                    if (pointF.x != 0) {
                        targetView.setTranslationX(targetView.getTranslationX() + pointF.x);
                    }
                    if (pointF.y != 0) {
                        targetView.setTranslationY(targetView.getTranslationY() + pointF.y);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //Log.e(TAG, "ACTION_UP");
                lastPointerCount = 0;
                if (targetView.getScaleX() < SCALE_NORMAL) {
                    setTargetScale(SCALE_NORMAL);
                }
                break;
        }
        return true;
    }

    /**
     * 缩放时，进行边界判断，主要判断宽或高大于屏幕的
     */
    private void checkMatrixBounds() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0, deltaY = 0;
        if (rectF.left > OFFSET_BORDER) {
            deltaX = OFFSET_BORDER - rectF.left;
        }
        if (rectF.right < -OFFSET_BORDER) {
            deltaX = -OFFSET_BORDER - rectF.right;
        }
        if (rectF.top > OFFSET_BORDER) {
            deltaY = OFFSET_BORDER - rectF.top;
        }
        if (rectF.bottom < -OFFSET_BORDER) {
            deltaY = -OFFSET_BORDER - rectF.bottom;
        }
        if (deltaX != 0) {
            targetView.setTranslationX(targetView.getTranslationX() + deltaX);
        }
        if (deltaY != 0) {
            targetView.setTranslationY(targetView.getTranslationY() + deltaY);
        }
    }

    /**
     * 移动时，进行边界判断，主要判断宽或高大于屏幕的
     */
    private PointF checkMatrixBounds(float dx, float dy) {
        if (targetView.getScaleX() < SCALE_NORMAL) {
            return new PointF(dx, dy);
        }
        RectF rectF = getMatrixRectF();
        float deltaX = 0, deltaY = 0;
        if (dx > 0) {
            if (rectF.left > OFFSET_BORDER) {
                deltaX = OFFSET_BORDER - rectF.left;
            } else if (rectF.left < OFFSET_BORDER) {
                if ((rectF.left + dx) <= OFFSET_BORDER) {
                    deltaX = dx;
                } else {
                    deltaX = OFFSET_BORDER - rectF.left;
                }
            }
        } else if (dx < 0) {
            if (rectF.right < -OFFSET_BORDER) {
                deltaX = -OFFSET_BORDER - rectF.right;
            } else if (rectF.right > -OFFSET_BORDER) {
                if ((rectF.right + dx) >= -OFFSET_BORDER) {
                    deltaX = dx;
                } else {
                    deltaX = -OFFSET_BORDER - rectF.right;
                }
            }
        }

        if (dy > 0) {
            if (rectF.top > OFFSET_BORDER) {
                deltaY = OFFSET_BORDER - rectF.top;
            } else if (rectF.top < OFFSET_BORDER) {
                if ((rectF.top + dy) <= OFFSET_BORDER) {
                    deltaY = dy;
                } else {
                    deltaY = OFFSET_BORDER - rectF.top;
                }
            }
        } else if (dy < 0) {
            if (rectF.bottom < -OFFSET_BORDER) {
                deltaY = -OFFSET_BORDER - rectF.bottom;
            } else if (rectF.bottom > -OFFSET_BORDER) {
                if ((rectF.bottom + dy) >= -OFFSET_BORDER) {
                    deltaY = dy;
                } else {
                    deltaY = -OFFSET_BORDER - rectF.bottom;
                }
            }
        }
        return new PointF(deltaX, deltaY);
    }

    /**
     * 获取当前targetView相对于父布局的边界位置
     *
     * @return
     */
    private RectF getMatrixRectF() {
        Matrix matrix = targetView.getMatrix();
        matrix.getValues(matrixValues);
        float mScaleX = matrixValues[Matrix.MSCALE_X];
        float rectLeft = matrixValues[Matrix.MTRANS_X];
        float rectTop = matrixValues[Matrix.MTRANS_Y];
        float rectRight = width * mScaleX + rectLeft - width;
        float rectBottom = height * mScaleX + rectTop - height;
        RectF rectF = new RectF(rectLeft, rectTop, rectRight, rectBottom);
//        Log.i(TAG, "getMatrixRectF() -- rectF -- left: " + rectF.left + ", right: " + rectF.right + ", top: " + rectF.top + ", bottom: " + rectF.bottom);
//        Log.i(TAG, "getMatrixRectF() -- rectF -- width: " + rectF.width() + ", height: " + rectF.height());
        return rectF;
    }


    /**
     * 自动缩放的任务
     *
     * @author zhy
     */
    private class AutoScaleRunnable implements Runnable {
        static final float BIGGER = 1.05f;
        static final float SMALLER = 0.95f;
        private float mTargetScale;
        private float tmpScale;

        private float currentScale;

        /**
         * 传入目标缩放值，根据目标值与当前值，判断应该放大还是缩小
         *
         * @param targetScale
         */
        public AutoScaleRunnable(float targetScale) {
            this.mTargetScale = targetScale;
            currentScale = targetView.getScaleX();
            if (currentScale < mTargetScale) {
                tmpScale = BIGGER;
            } else {
                tmpScale = SMALLER;
            }
        }

        @Override
        public void run() {
            // 进行缩放
            currentScale = currentScale * tmpScale;
            setTargetScale(currentScale);

            // 如果值在合法范围内，继续缩放
            if (((tmpScale > SCALE_NORMAL) && (currentScale < mTargetScale))
                    || ((tmpScale < SCALE_NORMAL) && (mTargetScale < currentScale))) {
                ZoomViewGroup.this.postDelayed(this, DELAY_MILLIS);
            } else {
                // 设置为目标的缩放比例
                setTargetScale(mTargetScale);
                isAutoScale = false;
            }
        }
    }
}
