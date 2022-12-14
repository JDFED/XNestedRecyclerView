package com.jd.lib.xnestedrecyclerview;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jd.lib.xnestedrecyclerview.manager.INestLayoutManager;

public class ParentRecyclerView extends RecyclerView implements NestedScrollingParent {

    private boolean startFling;
    private int velocityY;
    private int totalDy;
    private float lastY;

    private FlingHelper helper;
    private boolean allowNestScroll;

    private OnNestFlingListener onNestFlingListener = new OnNestFlingListener() {
        @Override
        public void onNestFling(int velocityX, int velocityY) {
            setInBottom(false);
            fling(velocityX, velocityY);
        }

        @Override
        public boolean canParentScroll() {
            return !isInBottom();
        }
    };

    public ParentRecyclerView(Context context) {
        super(context);
        initView(context);
    }

    public ParentRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ParentRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        helper = new FlingHelper(context);
        addOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (allowNestScroll && dy !=0) {
                    if (startFling) {
                        totalDy = 0;
                    }
                    totalDy += dy;
                    if (startFling) {
                        startFling = false;
                    }
                    boolean isInBottom = isInBottom();
                    if (isInBottom && velocityY != 0
                        //&& allowNestScroll()
                            ) {
                        double distance = helper.getSplineFlingDistance(velocityY);
                        if (distance > totalDy) {
                            int childVelocityY = helper.getVelocityByDistance(distance - totalDy);
                            childFling(childVelocityY);
                        }
                    }
                    setInBottom(isInBottom);//??????onScrollStateChanged????????????????????????view?????????????????????view????????????
                } else {
                    setInBottom(false);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    velocityY = 0;
                    if (allowNestScroll) {
                        setInBottom(isInBottom());//???view?????????view??????fling?????????????????????onScrolled?????????????????????
                    }
                }
            }
        });
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        post(new Runnable() {
            @Override
            public void run() {
                velocityY = 0;
                if (allowNestScroll) {
                    setInBottom(false);//???view?????????view??????fling?????????????????????onScrolled?????????????????????
                }
            }
        });
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return true;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
    }

    @Override
    public void onStopNestedScroll(View target) {
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {

        boolean isInBottom = isInBottom();
        boolean isChildTop = isChildOnTop(target);

        if (isInBottom && isChildTop && dy < 0) {
            setInBottom(false);
            scrollBy(0, dy);
            consumed[1] = dy;
        }
        if (!isInBottom) {
            scrollBy(0, dy);
            consumed[1] = dy;
        }
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        if (target instanceof NestRecyclerView) {
            ((NestRecyclerView) target).setOnNestFlingListener(onNestFlingListener);
        }
        return false;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (!isInBottom() && velocityY != 0) {//???view????????????view??????fling
            fling(0, (int) velocityY);
            return true;
        }
        return false;
    }

    @Override
    public int getNestedScrollAxes() {
        return 0;
    }

    public boolean isNestCanScorll() {
        return !getLayoutManager().canScrollVertically() && !isChildOnTop();
    }

    private boolean isChildOnTop() {
        View view = getChildAt(getChildCount() - 1);
        if (null != view && (view instanceof NestViewPagerView)) {
            return ((NestViewPagerView) view).isChildOnTop();
        }
        return true;
    }

//    private boolean allowNestScroll() {
//        View view = getChildAt(getChildCount() - 1);
//        if (null != view && (view instanceof NestViewPagerView)) {
//            return true;
//        }
//        return false;
//    }

    public void setAllowNestScroll(boolean allowNestScroll) {
        this.allowNestScroll = allowNestScroll;
    }

    private boolean isInBottom() {
        return !ViewCompat.canScrollVertically(this, 1);
    }

    private void setInBottom(boolean isInBottom) {
//        requestDisallowInterceptTouchEvent(!isInBottom);
        if (getLayoutManager() instanceof INestLayoutManager) {
            ((INestLayoutManager) getLayoutManager()).setInBottom(isInBottom);
        }
    }

    public boolean isChildOnTop(View view) {
        return !view.canScrollVertically(-1);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        boolean flag = super.fling(velocityX, velocityY);
        if (allowNestScroll && flag && velocityY > 0) {
            startFling = true;
            this.velocityY = velocityY;
        } else {
            this.velocityY = 0;
        }
        return flag;
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        RecyclerView child = getChild();
        if (allowNestScroll && isInBottom() && null != child && !isChildOnTop(child)) {//???view????????????view??????fling
            child.fling(0, (int) velocityY);
            return true;
        }
        return super.dispatchNestedPreFling(velocityX, velocityY);
    }


    public NestRecyclerView getChild() {
        View view = getChildAt(getChildCount() - 1);
        if (null != view && (view instanceof NestViewPagerView)) {
            RecyclerView recyclerView = ((NestViewPagerView) view).getCurrent();
            return (null != recyclerView && recyclerView instanceof NestRecyclerView)
                    ? (NestRecyclerView) recyclerView : null;
        }
        return null;
    }

    public void childFling(int velocityY) {
        RecyclerView recyclerView = getChild();
        if (null != recyclerView) {
            recyclerView.fling(0, velocityY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (allowNestScroll && isInBottom()) {
            NestRecyclerView recyclerView = getChild();
            if (null != recyclerView) {
                int dy = (int) (lastY - e.getY());
                if (dy <= 0 && isChildOnTop(recyclerView)) {
                    setInBottom(false);
                } else {
                    recyclerView.onNestedScroll(this, 0, 0, 0, dy);
                }
            }
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            setInBottom(false);
        }
        lastY = e.getY();
        return super.onTouchEvent(e);
    }

}
