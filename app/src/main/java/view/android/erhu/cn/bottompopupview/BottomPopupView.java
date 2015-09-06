package view.android.erhu.cn.bottompopupview;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static view.android.erhu.cn.bottompopupview.BottomPopupView.State.CLOSED;
import static view.android.erhu.cn.bottompopupview.BottomPopupView.State.OPENED;

import android.content.Context;
import android.os.Build;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * 使用 ViewDragHelper 实现的 BottomPopupView
 * <p/>
 * 注：此视图不适合添加垂直的 ScrollView或ListView 来显示内容。
 *
 * @author erhu
 * @version 1.0
 * @since 15/9/4 17:27
 */
public class BottomPopupView extends FrameLayout {

    private static final int DURATION = 300;
    // 触发 PopupView 动画的最小速率
    private static final float INTERPOLATOR = 3;
    private static final int VELOCITY_MIN = 2000;
    private static final float ALPHA_TARGET = 0.3f;

    private View mDimView;
    private LinearLayout mContainer;

    private int mMinTop;
    private int mMaxTop;
    private int mPageHeight;
    private int mHeaderHeight;
    private int mContainerHeight;
    private State mState = CLOSED;
    private ViewDragHelper mDragger;
    private LayoutParams mContainerLp;
    private android.view.View mContentView;
    private AlphaAnimation mAnimHideDimView;
    private AlphaAnimation mAnimShowDimView;
    private int mInitBottomMarginOfContainer;

    public BottomPopupView(Context c) {
        super(c);
        init();
    }

    public BottomPopupView(Context context, AttributeSet a) {
        super(context, a);
        init();
    }

    public BottomPopupView(Context c, AttributeSet a, int defStyleAttr) {
        super(c, a, defStyleAttr);
        init();
    }

    private void init() {
        initAnim();
        initDragger();
    }

    private void initAnim() {
        mAnimShowDimView = new AlphaAnimation(0, ALPHA_TARGET);
        mAnimShowDimView.setFillAfter(true);
        mAnimShowDimView.setDuration(DURATION);
        mAnimHideDimView = new AlphaAnimation(ALPHA_TARGET, 0);
        mAnimHideDimView.setDuration(DURATION);
        mAnimHideDimView.setFillAfter(true);
    }

    private void initDragger() {
        mDragger = ViewDragHelper.create(this, 1.0f,
                new ViewDragHelper.Callback() {
                    @Override
                    public boolean tryCaptureView(View v, int pointerId) {
                        return v == mContainer;
                    }

                    @Override
                    public int clampViewPositionVertical(View v, int top, int dy) {
                        // 垂直方向中 top 的范围[mMinTop, mMaxTop]
                        return Math.min(Math.max(mMinTop, top), mMaxTop);
                    }

                    @Override
                    public void onViewReleased(View v, float xVelocity, float vY) {
                        if (vY > VELOCITY_MIN) { // 下滑
                            _popDown();
                        } else if (vY < -VELOCITY_MIN) {
                            _popup();
                        } else {
                            int top = v.getTop();
                            // 改变 Sheet 回弹方向的分界值
                            int topWhenChangeAnimDirection = (int) (mPageHeight - mContainerHeight * 0.5f);
                            if (top < topWhenChangeAnimDirection) {
                                _popup();
                            } else {
                                _popDown();
                            }
                        }
                        invalidate();
                    }

                    void _popup() {
                        mDragger.settleCapturedViewAt(0, mMinTop);
                        changeStateWithDimView(OPENED);
                        // 如果是通过拖拽的方式完成上拉，此处需要设置 bottomMargin,
                        // 以避免点击 dimView 收起时卡顿的问题。
                        setBottomMargin(0);
                    }

                    void _popDown() {
                        mDragger.settleCapturedViewAt(0, mMaxTop);
                        changeStateWithDimView(CLOSED);
                        // 同上
                        setBottomMargin(mInitBottomMarginOfContainer);
                    }

                    @Override
                    public int getViewHorizontalDragRange(View v) {
                        return getMeasuredWidth() - v.getMeasuredWidth();
                    }

                    @Override
                    public int getViewVerticalDragRange(View v) {
                        return getMeasuredHeight() - v.getMeasuredHeight();
                    }

                    @Override
                    public void onViewDragStateChanged(int s) {
                        super.onViewDragStateChanged(s);
                    }
                });
    }

    private void changeStateWithDimView(State s) {
        if (changeState(s)) {
            if (mState == CLOSED) {
                hideDimView();
            } else {
                showDimView();
            }
        }
    }

    private boolean changeState(State s) {
        boolean changed = false;
        if (s == CLOSED && mState != CLOSED) {
            mState = CLOSED;
            changed = true;
        } else if (s == OPENED && mState != OPENED) {
            mState = OPENED;
            changed = true;
        }
        return changed;
    }

    /**
     * 显示蒙板
     */
    private void showDimView() {
        if (Build.VERSION.SDK_INT < 14) {
            return;
        }

        mDimView.setVisibility(View.VISIBLE);
        mDimView.setBackgroundColor(0xff000000);
        mDimView.clearAnimation();
        mDimView.startAnimation(mAnimShowDimView);
    }

    /**
     * 隐藏蒙板
     */
    private void hideDimView() {
        if (Build.VERSION.SDK_INT < 14) {
            return;
        }
        mDimView.clearAnimation();
        mDimView.startAnimation(mAnimHideDimView);
    }

    /**
     * 视图是否展开
     */
    public boolean isOpened() {
        return mState == OPENED;
    }

    /**
     * 收起展开
     */
    public void popup() {
        int startY = mContainerLp.bottomMargin;
        final int endY = 0;

        TranslateAnimation popAnimation = new TranslateAnimation(0, 0, 0, startY - endY);
        popAnimation.setDuration(Math.abs(DURATION));
        popAnimation.setFillAfter(true);
        popAnimation.setInterpolator(new DecelerateInterpolator(INTERPOLATOR));
        popAnimation.setAnimationListener(new MyAnimListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                changeState(OPENED);
                mContainer.clearAnimation();
                setBottomMargin(endY);
            }
        });
        mContainer.startAnimation(popAnimation);
        showDimView();
    }

    /**
     * 收起视图
     */
    public void popDown() {
        int startY = mContainerLp.bottomMargin;
        final int endY = mInitBottomMarginOfContainer;

        TranslateAnimation popAnim = new TranslateAnimation(0, 0, 0, startY - endY);
        popAnim.setDuration(DURATION);
        popAnim.setFillAfter(true);
        popAnim.setInterpolator(new DecelerateInterpolator(INTERPOLATOR));
        popAnim.setAnimationListener(new MyAnimListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                changeState(CLOSED);
                mContainer.clearAnimation();
                setBottomMargin(endY);
            }
        });

        mContainer.startAnimation(popAnim);
        hideDimView();
    }

    /**
     * 设置 Container 的 bottomMargin
     */
    private void setBottomMargin(int margin) {
        if (margin < mInitBottomMarginOfContainer) {
            margin = mInitBottomMarginOfContainer;
        } else if (margin > 0) {
            margin = 0;
        }
        mContainerLp.bottomMargin = margin;
        mContainer.setLayoutParams(mContainerLp);
    }

    /**
     * 初始化
     *
     * @param v             内容视图
     * @param contentHeight 视图的高度
     * @param headerHeight  视图收起时，露头的高度
     */
    public void init(View v, int contentHeight, int headerHeight) {
        mContainerHeight = contentHeight;
        mHeaderHeight = headerHeight;
        mContentView = v;

        initDimView();

        mContainer = new LinearLayout(getContext());
        mContainer.setOrientation(LinearLayout.VERTICAL);
        mContainerLp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        mContainerLp.gravity = Gravity.BOTTOM;
        mContainerLp.height = mContainerHeight;
        mInitBottomMarginOfContainer = -(mContainerHeight - mHeaderHeight);
        mContainerLp.bottomMargin = mInitBottomMarginOfContainer;
        mContainer.setLayoutParams(mContainerLp);
        mContainer.addView(mContentView);

        addView(mContainer);
    }

    /**
     * 初始化蒙板
     */
    private void initDimView() {
        mDimView = new View(getContext());
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        mDimView.setLayoutParams(p);
        mDimView.setVisibility(GONE);
        mDimView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOpened()) {
                    popDown();
                }
            }
        });
        addView(mDimView);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragger.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        mDragger.processTouchEvent(e);
        return true;
    }

    @Override
    public void computeScroll() {
        if (mDragger.continueSettling(true)) {
            invalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mMaxTop == 0) {
            mMaxTop = mContainer.getTop();
            mPageHeight = getMeasuredHeight();// 请绘图理解
            mMinTop = mMaxTop - (mContainerHeight - mHeaderHeight);
        }
    }

    enum State {
        CLOSED, // 收起
        OPENED // 展开
    }

    private static class MyAnimListener implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}