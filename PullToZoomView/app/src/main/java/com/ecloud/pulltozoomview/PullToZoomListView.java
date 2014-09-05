package com.ecloud.pulltozoomview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;

/**
 * Author:    ZhuWenWu
 * Version    V1.0
 * Date:      2014/9/1  10:50.
 * Description:
 * Modification  History:
 * Date         	Author        		Version        	Description
 * -----------------------------------------------------------------------------------
 * 2014/9/1        ZhuWenWu            1.0                    1.0
 * Why & What is modified:
 */

public class PullToZoomListView extends ListView implements
        AbsListView.OnScrollListener {

    private static final int INVALID_VALUE = -1;
    private static final String TAG = PullToZoomListView.class.getSimpleName();

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float paramAnonymousFloat) {
            float f = paramAnonymousFloat - 1.0F;
            return 1.0F + f * (f * (f * (f * f)));
        }
    };

    private FrameLayout mHeaderContainer;
    private FrameLayout mZoomContainer;
    private View mHeaderView;
    private View mZoomView;

    private OnScrollListener mOnScrollListener;
    private ScalingRunnable mScalingRunnable;

    private int mScreenHeight;
    private int mHeaderHeight;
    private int mActivePointerId = -1;
    private float mLastMotionY = -1.0F;
    private float mLastScale = -1.0F;
    private float mMaxScale = -1.0F;
    private boolean isHeaderTop = true;

    public PullToZoomListView(Context paramContext) {
        this(paramContext, null);
    }

    public PullToZoomListView(Context paramContext, AttributeSet paramAttributeSet) {
        this(paramContext, paramAttributeSet, 0);
    }

    public PullToZoomListView(Context paramContext, AttributeSet attrs, int paramInt) {
        super(paramContext, attrs, paramInt);
        init(attrs);
    }

    private void endScaling() {
        if (mHeaderContainer.getBottom() >= mHeaderHeight)
            Log.d(TAG, "endScaling");
        mScalingRunnable.startAnimation(200L);
    }

    private void init(AttributeSet attrs) {
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(localDisplayMetrics);
        mScreenHeight = localDisplayMetrics.heightPixels;
        mHeaderContainer = new FrameLayout(getContext());
        mZoomContainer = new FrameLayout(getContext());
        if (attrs != null) {
            LayoutInflater mLayoutInflater = LayoutInflater.from(getContext());
            //初始化状态View
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PullToZoomListView);

            int zoomViewResId = a.getResourceId(R.styleable.PullToZoomListView_listZoomView, 0);
            if (zoomViewResId > 0) {
                mZoomView = mLayoutInflater.inflate(zoomViewResId, null, false);
                mZoomContainer.addView(mZoomView);
                mHeaderContainer.addView(mZoomContainer);
            }

            int headViewResId = a.getResourceId(R.styleable.PullToZoomListView_listHeadView, 0);
            if (headViewResId > 0) {
                mHeaderView = mLayoutInflater.inflate(headViewResId, null, false);
                mHeaderContainer.addView(mHeaderView);
            }

            a.recycle();
        }

        int i = localDisplayMetrics.widthPixels;
        setHeaderViewSize(i, (int) (9.0F * (i / 16.0F)));
        addHeaderView(mHeaderContainer);
        mScalingRunnable = new ScalingRunnable();
        super.setOnScrollListener(this);
    }

    private void onSecondaryPointerUp(MotionEvent paramMotionEvent) {
        int i = (paramMotionEvent.getAction()) >> 8;
        if (paramMotionEvent.getPointerId(i) == mActivePointerId)
            if (i != 0) {
                mLastMotionY = paramMotionEvent.getY(0);
                mActivePointerId = paramMotionEvent.getPointerId(0);
            }
    }

    private void reset() {
        mActivePointerId = -1;
        mLastMotionY = -1.0F;
        mMaxScale = -1.0F;
        mLastScale = -1.0F;
    }

    public boolean onInterceptTouchEvent(MotionEvent paramMotionEvent) {
        return super.onInterceptTouchEvent(paramMotionEvent);
    }

    protected void onLayout(boolean paramBoolean, int paramInt1, int paramInt2,
                            int paramInt3, int paramInt4) {
        super.onLayout(paramBoolean, paramInt1, paramInt2, paramInt3, paramInt4);
        if (mHeaderHeight == 0) {
            mHeaderHeight = mHeaderContainer.getHeight();
        }
    }

    @Override
    public void onScroll(AbsListView paramAbsListView, int paramInt1, int paramInt2, int paramInt3) {
        Log.d(TAG, "onScroll");
        float f = mHeaderHeight - mHeaderContainer.getBottom();
        isHeaderTop = getScrollY() <= 0;
        Log.d(TAG, "f|" + f);
        if ((f > 0.0F) && (f < mHeaderHeight)) {
            int i = (int) (0.65D * f);
            mZoomContainer.scrollTo(0, -i);
        } else if (mZoomContainer.getScrollY() != 0) {
            mZoomContainer.scrollTo(0, 0);
        }
        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(paramAbsListView, paramInt1, paramInt2, paramInt3);
        }
    }

    public void onScrollStateChanged(AbsListView paramAbsListView, int paramInt) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(paramAbsListView, paramInt);
        }
    }

    public boolean onTouchEvent(MotionEvent paramMotionEvent) {
        Log.d(TAG, "action = " + (0xFF & paramMotionEvent.getAction()));
//        if(isHeaderTop) {
        switch (0xFF & paramMotionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_OUTSIDE:
                if (!mScalingRunnable.isFinished()) {
                    mScalingRunnable.abortAnimation();
                }
                mLastMotionY = paramMotionEvent.getY();
                mActivePointerId = paramMotionEvent.getPointerId(0);
                mMaxScale = (mScreenHeight / mHeaderHeight);
                mLastScale = (mHeaderContainer.getBottom() / mHeaderHeight);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "mActivePointerId" + mActivePointerId);
                int j = paramMotionEvent.findPointerIndex(mActivePointerId);
                if (j == INVALID_VALUE) {
                    Log.e("PullToZoomListView", "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                } else {
                    if (mLastMotionY == -1.0F)
                        mLastMotionY = paramMotionEvent.getY(j);
                    if (mHeaderContainer.getBottom() >= mHeaderHeight) {
                        ViewGroup.LayoutParams localLayoutParams = mHeaderContainer.getLayoutParams();
                        float f = ((paramMotionEvent.getY(j) - mLastMotionY + mHeaderContainer.getBottom()) / mHeaderHeight - mLastScale) / 2.0F + mLastScale;
                        if ((mLastScale <= 1.0D) && (f < mLastScale)) {
                            localLayoutParams.height = mHeaderHeight;
                            mHeaderContainer.setLayoutParams(localLayoutParams);
                            return super.onTouchEvent(paramMotionEvent);
                        }
                        mLastScale = Math.min(Math.max(f, 1.0F), mMaxScale);
                        localLayoutParams.height = ((int) (mHeaderHeight * mLastScale));
                        if (localLayoutParams.height < mScreenHeight) {
                            mHeaderContainer.setLayoutParams(localLayoutParams);
                        }
                        mLastMotionY = paramMotionEvent.getY(j);
                        return true;
                    }
                    mLastMotionY = paramMotionEvent.getY(j);
                }
                break;
            case MotionEvent.ACTION_UP:
                reset();
                endScaling();
                break;
            case MotionEvent.ACTION_CANCEL:
                int i = paramMotionEvent.getActionIndex();
                mLastMotionY = paramMotionEvent.getY(i);
                mActivePointerId = paramMotionEvent.getPointerId(i);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                onSecondaryPointerUp(paramMotionEvent);
                mLastMotionY = paramMotionEvent.getY(paramMotionEvent.findPointerIndex(mActivePointerId));
                break;
        }
//        }
        return super.onTouchEvent(paramMotionEvent);
    }

    public void setHeaderViewSize(int paramInt1, int paramInt2) {
        Object localObject = mHeaderContainer.getLayoutParams();
        if (localObject == null) {
            localObject = new LayoutParams(paramInt1, paramInt2);
        }
        ((ViewGroup.LayoutParams) localObject).width = paramInt1;
        ((ViewGroup.LayoutParams) localObject).height = paramInt2;
        mHeaderContainer.setLayoutParams((ViewGroup.LayoutParams) localObject);
        mHeaderHeight = paramInt2;
    }

    public void setOnScrollListener(OnScrollListener paramOnScrollListener) {
        mOnScrollListener = paramOnScrollListener;
    }

    public View getZoomView() {
        return mZoomView;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    class ScalingRunnable implements Runnable {
        protected long mDuration;
        protected boolean mIsFinished = true;
        protected float mScale;
        protected long mStartTime;

        ScalingRunnable() {
        }

        public void abortAnimation() {
            mIsFinished = true;
        }

        public boolean isFinished() {
            return mIsFinished;
        }

        public void run() {
            float f2;
            ViewGroup.LayoutParams localLayoutParams;
            if ((!mIsFinished) && (mScale > 1.0D)) {
                float f1 = ((float) SystemClock.currentThreadTimeMillis() - (float) mStartTime) / (float) mDuration;
                f2 = mScale - (mScale - 1.0F) * PullToZoomListView.sInterpolator.getInterpolation(f1);
                localLayoutParams = mHeaderContainer.getLayoutParams();
                if (f2 > 1.0F) {
                    Log.d(TAG, "f2>1.0");
                    localLayoutParams.height = ((int) (f2 * mHeaderHeight));
                    mHeaderContainer.setLayoutParams(localLayoutParams);
                    post(this);
                    return;
                }
                mIsFinished = true;
            }
        }

        public void startAnimation(long paramLong) {
            mStartTime = SystemClock.currentThreadTimeMillis();
            mDuration = paramLong;
            mScale = ((float) (mHeaderContainer.getBottom()) / mHeaderHeight);
            mIsFinished = false;
            post(this);
        }
    }
}