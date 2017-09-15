package com.kjy.refresh;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;

import com.kjy.R;

/**
 * 下拉刷新控件
 * Created by eiboran-android001 on 2017/9/15.
 */

public class RefreshView extends LinearLayout {
    private Context context;
    public RefreshView(Context context) {
        super(context);
        init(context);
    }

    public RefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.refreshableView);
        for (int i = 0, len = a.length(); i < len; i++) {
            int attrIndex = a.getIndex(i);
            switch (attrIndex) {
                case R.styleable.refreshableView_interceptAllMoveEvents:
                    interceptAllMoveEvents = a.getBoolean(i, false);
                    break;
            }
        }
        a.recycle();
        init(context);
    }

    public RefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 设置布局样式
     * @param context
     */
    private void init(Context context) {
        this.context = context;
        this.setOrientation(VERTICAL);
    }

    //刷新状态
    private int refreshState;
    //静止状态
    public static final int STATE_REFRESH_NORMAL = 0x000001;
    //下拉刷新
    public static final int STATE_REFRESH_NOT_ARRIVED = 0x000002;
    //放下刷新
    public static final int STATE_REFRESH_ARRIVED = 0x000003;
    //正在刷新
    public static final int STATE_REFRESHING = 0x000004;

    // 刷新状态监听
    private RefreshHelper refreshHelper;

    public void setRefreshHelper(RefreshHelper refreshHelper) {
        this.refreshHelper = refreshHelper;
    }

    //刷新的view
    private View refreshHeaderView;
    //view的真实高度
    private int originRefreshHeight;
    //有效下拉刷新需要达到的高度
    private int refreshArrivedStateHeight;
    //刷新时显示的高度
    private int refreshingHeight;
    //正常为刷新的高度
    private int refreshNormalHeight;
    //xml中可设置它的值为false，表示不把移动的事件传递给子控件
    private boolean interceptAllMoveEvents;
    //默认不允许拦截
    private boolean disallowIntercept = true;
    private float downY = Float.MAX_VALUE;

    /**
     * 当view的大小发生变化时触发
     *
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (null != refreshHelper) {
            refreshHeaderView = refreshHelper.onInitRefreshHeaderView();
        }
        if (null == refreshHeaderView) {
            return;
        }
        this.removeView(refreshHeaderView);
        this.addView(refreshHeaderView, 0);
        //计算header尺寸   根据大小和模式创建
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, View.MeasureSpec.AT_MOST);
        refreshHeaderView.measure(widthMeasureSpec, heightMeasureSpec);
        originRefreshHeight = refreshHeaderView.getMeasuredHeight();
        //默认为true
        boolean Default = true;
        if (null != refreshHelper) {
            Default = refreshHelper.onInitRefreshHeight(originRefreshHeight);
        }
        // 初始化各个高度
        if (Default) {
            refreshArrivedStateHeight = originRefreshHeight;
            refreshingHeight = originRefreshHeight;
            refreshNormalHeight = 0;
        }
        //改变控件的高度
        changeViewHeight(refreshHeaderView, refreshNormalHeight);
        //初始化为正常状态
        setRefreshState(STATE_REFRESH_NORMAL);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 事件传递(拦截)
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!interceptAllMoveEvents) {
            return !disallowIntercept;
        }
        // 如果设置了拦截所有move事件，即interceptAllMoveEvents为true
        if (MotionEvent.ACTION_MOVE == ev.getAction()) {
            return true;
        }
        return false;
    }

    /**
     * 阻止父层的View截获touch事件
     * 底层View收到touch的action后调用这个方法那么父层View就不会再调用onInterceptTouchEvent了
     *
     * @param disallowIntercept
     */
    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (this.disallowIntercept == disallowIntercept) {
            return;
        }
        this.disallowIntercept = disallowIntercept;
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = event.getY();
                //保证事件可往下传递
                requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float curY = event.getY();
                float deltaY = curY - downY;
                //是否是有效的往下拖动事件
                boolean isDropDownValidate = Float.MAX_VALUE != downY;
                /**
                 * 修改拦截设置
                 * 如果是有效往下拖动事件，则事件需要在本ViewGroup中处理，所以需要拦截不往子控件传递，
                 * 即不允许拦截设为false
                 * 如果不是有效往下拖动事件，则事件传递给子控件处理，所以不需要拦截，并往子控件传递，
                 * 即不允许拦截设为true
                 */
                requestDisallowInterceptTouchEvent(!isDropDownValidate);
                downY = curY;
                int curHeight = refreshHeaderView.getMeasuredHeight();
                int exceptHeight = curHeight + (int) (deltaY / 2);
                //如果当前没有处在正在刷新状态，则更新刷新状态
                if (STATE_REFRESHING != refreshState) {
                    //达到可刷新状态
                    if (curHeight >= refreshArrivedStateHeight) {
                        setRefreshState(STATE_REFRESH_ARRIVED);
                    } else { // 未达到可刷新状态
                        setRefreshState(STATE_REFRESH_NOT_ARRIVED);
                    }
                }
                if (isDropDownValidate) {
                    changeViewHeight(refreshHeaderView, Math.max(refreshNormalHeight, exceptHeight));
                } else {
                    // 防止从子控件修改拦截后引发的downY为Float.MAX_VALUE的问题
                    changeViewHeight(refreshHeaderView, Math.max(curHeight, exceptHeight));
                }
                break;
            case MotionEvent.ACTION_UP:
                downY = Float.MAX_VALUE;
                //保证事件可往下传递
                requestDisallowInterceptTouchEvent(true);
                // 达到了刷新的状态
                if (STATE_REFRESH_ARRIVED == refreshState) {
                    startHeightAnimation(refreshHeaderView, refreshHeaderView.getMeasuredHeight(), refreshingHeight);
                    setRefreshState(STATE_REFRESHING);
                    //正在刷新的状态
                } else if (STATE_REFRESHING == refreshState) {
                    startHeightAnimation(refreshHeaderView, refreshHeaderView.getMeasuredHeight(), refreshingHeight);
                } else {
                    //执行动画后回归正常状态
                    startHeightAnimation(refreshHeaderView, refreshHeaderView.getMeasuredHeight(), refreshNormalHeight, normalAnimatorListener);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 改变控件的高度
     *
     * @param view
     * @param height
     */
    private void changeViewHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = height;
        view.setLayoutParams(lp);
    }

    /**
     * 修改当前的刷新状态
     *
     * @param expectRefreshState
     */
    private void setRefreshState(int expectRefreshState) {
        if (expectRefreshState != refreshState) {
            refreshState = expectRefreshState;
            if (null != refreshHelper) {
                refreshHelper.onRefreshStateChanged(refreshHeaderView, refreshState);
            }
        }
    }

    /**
     * 刷新完毕后调用此方法
     */
    public void onCompleteRefresh() {
        if (STATE_REFRESHING == refreshState) {
            setRefreshState(STATE_REFRESH_NORMAL);
            startHeightAnimation(refreshHeaderView, refreshHeaderView.getMeasuredHeight(), refreshNormalHeight);
        }
    }

    /**
     * 改变控件的高度动画
     *
     * @param view
     * @param fromHeight
     * @param toHeight
     */
    private void startHeightAnimation(final View view, int fromHeight, int toHeight) {
        startHeightAnimation(view, fromHeight, toHeight, null);
    }

    private void startHeightAnimation(final View view, int fromHeight, int toHeight, Animator.AnimatorListener animatorListener) {
        if (toHeight == view.getMeasuredHeight()) {
            return;
        }
        ValueAnimator heightAnimator = ValueAnimator.ofInt(fromHeight, toHeight);
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Integer value = (Integer) valueAnimator.getAnimatedValue();
                if (null == value) return;
                changeViewHeight(view, value);
            }
        });
        if (null != animatorListener) {
            heightAnimator.addListener(animatorListener);
        }
        heightAnimator.setInterpolator(new LinearInterpolator());
        heightAnimator.setDuration(300/*ms*/);
        heightAnimator.start();
    }

    AnimatorListenerAdapter normalAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            // 回归正常状态
            setRefreshState(STATE_REFRESH_NORMAL);
        }
    };
    public void setRefreshArrivedStateHeight(int refreshArrivedStateHeight) {
        this.refreshArrivedStateHeight = refreshArrivedStateHeight;
    }

    public void setRefreshingHeight(int refreshingHeight) {
        this.refreshingHeight = refreshingHeight;
    }

    public void setRefreshNormalHeight(int refreshNormalHeight) {
        this.refreshNormalHeight = refreshNormalHeight;
    }

    public int getOriginRefreshHeight() {
        return originRefreshHeight;
    }
}
