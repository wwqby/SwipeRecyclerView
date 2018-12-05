package zy.example.com.swiperecyclerview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Objects;

/**
 * /*@Description 大小尺寸只支持width match_parent/指定大小,groupHeight match_parent/指定大小/wrap_content
 * /*created by wwq on 2018/10/24 0024
 * /*@company zhongyiqiankun
 */
public class SwipeView extends ViewGroup {
    //  标记tag
    private static final String TAG = "SwipeView";
    private static final int Header = -1;
    private static final int ContentView = 0;
    private static final int Footer = 1;
    //    是否显示header/footer
    private boolean headerVisible;
    private boolean footerVisible;
    //  控件view
    private View header;
    private TextView tvTime;
    private TextView tvHeaderTitle;
    private TextView tvFooterTitle;
    private TextView tvNotice;
    private ProgressBar headerProgressBar;
    private ProgressBar footerProgressBar;
    private String lastTime;
    private RecyclerView contentView;
    private View footer;
    //  控件高度
    private int headerHeight;
    private int footerHeight;
    private int contentViewHeight;
    //  滑动参数  lastY 上次触摸事件的高度  mScroller 滑动类  mSlop 系统最小滑动距离  autoScrollRange 启动自动滑动的系数
    private float lastY;
    private Scroller mScroller;
    private int mSlop;
    private int mDuration = 500;
    //  实现header和footer内部逻辑的接口
    private NewClickListener mListener;
    //  state 标记刷新过程中的状态 0 归位 1 拉动 2 释放 3刷新结束
    private int state=0;


    public SwipeView(Context context) {
        super(context);
    }

    public SwipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SwipeView);
        mDuration = ta.getInt(R.styleable.SwipeView_duration, 500);
        int headerLayout = ta.getResourceId(R.styleable.SwipeView_header_layout, 0);
        int footerLayout = ta.getResourceId(R.styleable.SwipeView_footer_layout, 0);
        headerVisible = ta.getBoolean(R.styleable.SwipeView_header_visible, true);
        footerVisible = ta.getBoolean(R.styleable.SwipeView_footer_visible, true);
        ta.recycle();

        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);
        switch (headerLayout) {
            case 0:
                header = LayoutInflater.from(context).inflate(R.layout.item_header, this, false);
                tvTime = header.findViewById(R.id.tv_time);
                tvHeaderTitle=header.findViewById(R.id.tv_header_title);
                tvNotice=header.findViewById(R.id.tv_notice);
                headerProgressBar=header.findViewById(R.id.progressBar);
                break;
            default:
                header = LayoutInflater.from(context).inflate(headerLayout, this, false);
                break;
        }
        header.setTag(Header);
        addView(header);

        contentView = new RecyclerView(context);
        contentView.setTag(ContentView);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(contentView, lp);

        switch (footerLayout) {
            case 0:
                footer = LayoutInflater.from(context).inflate(R.layout.item_footer, this, false);
                tvFooterTitle=footer.findViewById(R.id.tv_footer_title);
                footerProgressBar=footer.findViewById(R.id.progressBar);
                break;
            default:
                footer = LayoutInflater.from(context).inflate(footerLayout, this, false);
                break;
        }
        footer.setTag(Footer);
        addView(footer);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int mHeight = 0;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View mView = getChildAt(i);
            if (mView.getVisibility() != GONE) {
                //测量子View
                measureChild(mView, widthMeasureSpec, heightMeasureSpec);
                //累加子View的高度和margin
                mHeight += mView.getMeasuredHeight();
            }
        }
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, mHeight);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int height = 0;
        for (int i = 0; i < count; i++) {
            View mView = getChildAt(i);
            if (mView.getVisibility() != GONE) {
                int mWidth = mView.getMeasuredWidth();
                int mHeight = mView.getMeasuredHeight();
                switch ((int) mView.getTag()) {
                    case Header:
                        mView.layout(0, -mHeight, mWidth, 0);
                        headerHeight = mHeight;
                        break;
                    case ContentView:
                        mView.layout(0, 0, mWidth, mHeight);
                        height += mHeight;
                        contentViewHeight = mHeight;
                        Log.i(TAG, "onLayout: contentViewHeight=" + contentViewHeight);
                        break;
                    case Footer:
                        mView.layout(0, height, mWidth, mHeight + height);
                        footerHeight = mHeight;
                        break;
                }

            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int distance = (int) (lastY - ev.getRawY());
                LinearLayoutManager manager = (LinearLayoutManager) ( contentView).getLayoutManager();
                if (Math.abs(distance)>mSlop){
                    if (manager != null) {
                        if (headerVisible && manager.findFirstCompletelyVisibleItemPosition() == 0 && distance < 0) {
                            return true;
                        }
//                     判断滑动到底端,开始上拉
//                     满足条件，允许下拉、当前最后的条目视图就是列表的最后一个条目、最后一个条目不为空，且最后一个条目在页面底部
                        RecyclerView.Adapter adapter = ( contentView).getAdapter();
                        int count = Objects.requireNonNull(adapter).getItemCount();
                        View lastView = manager.findViewByPosition(count - 1);
                        if (footerVisible && (manager.findLastCompletelyVisibleItemPosition() + 1) == count && distance > 0 && lastView != null && lastView.getBottom() == contentViewHeight) {
                            return true;
                        }
                    }

//                       只要当前显示了header/footer,就拦截事件
                    if (getScrollY() < 0 && headerVisible) {
                        return true;
                    }
                    if (getScrollY() > 0 && footerVisible) {
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                int distance = (int) (lastY - event.getRawY());
                lastY = event.getRawY();
//                当上拉即将关闭footer露出header时，中断滑动
                if (distance > 0 && (getScrollY() + distance) > 0 && getScrollY() < 0) {
                    mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY(), mDuration);
                    invalidate();
//                 当下拉即将关闭header露出footer时，中断滑动
                } else if (distance < 0 && (getScrollY() + distance) < 0 && getScrollY() > 0) {
                    mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY(), mDuration);
                    invalidate();
                } else {
                    scrollBy(0, distance / 2);
                }
//                更新刷新状态
                if (getScrollY()>-headerHeight&&getScrollY()<footerHeight){
                    if (state==0){
                        state=1;
                    }
                }else {
                    state=2;
                }
                updateView();
                break;
            case MotionEvent.ACTION_UP:
                lastY = event.getRawY();
//                如果移动范围超过视图顶端范围,那么在手指抬起时,返回到视图最顶端,此时开始刷新header
                if (getScrollY() < -headerHeight) {
                    mScroller.startScroll(getScrollX(), getScrollY(), 0, -headerHeight - getScrollY(), mDuration);
                    headerRefreshStart();
                }
//                如果移动范围超过视图底端范围,那么在手指抬起时,返回到视图最底端，此时开始刷新footer
                if (getScrollY() > footerHeight) {
                    mScroller.startScroll(getScrollX(), getScrollY(), 0, footerHeight - getScrollY(), mDuration);
                    if (mListener != null) {
                        mListener.footerRefreshStart(this);
                    } else {
                        Log.e(TAG, "onTouchEvent: mListener=null");
                    }
                }
                if (getScrollY() >= -headerHeight && getScrollY() <= footerHeight) {
//                    自动隐藏header
                    if (getScrollY() > -headerHeight && getScrollY() < 0) {
                        Log.i(TAG, "onTouchEvent: 自动隐藏header");
                        mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY(), mDuration);
                        if (mListener != null) {
                            mListener.headerRefreshCancel();
                        } else {
                            Log.e(TAG, "onTouchEvent: mListener=null");
                        }
                    }
//                    自动隐藏footer
                    if (getScrollY() < footerHeight && getScrollY() > 0) {
                        Log.i(TAG, "onTouchEvent: 自动隐藏footer");
                        mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY(), mDuration);
                        if (mListener != null) {
                            mListener.footerRefreshCancel();
                        } else {
                            Log.e(TAG, "onTouchEvent: mListener=null");
                        }
                    }
                }
                invalidate();
                state=0;
                updateView();
                break;
        }
        return super.onTouchEvent(event);
    }

//    更新header/footer的状态
    private void updateView() {
        if (getScrollY()<0){
            updateHeader();
        }
        if (getScrollY()>0){
            updateFooter();
        }
    }

    private void updateFooter() {
        switch (state){
            //开始刷新、刷新完毕
            case 0:
                tvFooterTitle.setText("正在刷新");
                footerProgressBar.setVisibility(VISIBLE);
                break;
                //拉取
            case 1:
                tvFooterTitle.setText("拉取开始刷新");
                footerProgressBar.setVisibility(GONE);
                break;
            //释放并刷新
            case 2:
                tvFooterTitle.setText("释放并刷新");
                break;
        }
    }

    private void updateHeader() {
        switch (state){
            //开始刷新、刷新完毕
            case 0:
                tvHeaderTitle.setText("正在刷新");
                headerProgressBar.setVisibility(VISIBLE);
                break;
                //拉取
            case 1:
                tvHeaderTitle.setText("拉取开始刷新");
                headerProgressBar.setVisibility(GONE);

                if (lastTime==null){
                    tvNotice.setText("刚刚更新");
                }else{
                    tvNotice.setText("最近更新");
                }
                break;
            //释放并刷新
            case 2:
                tvHeaderTitle.setText("释放并刷新");
                break;
        }
    }


    private void headerRefreshStart() {
        if (mListener != null) {
            mListener.headerRefreshStart(this);
        } else {
            Log.e(TAG, "onTouchEvent: mListener=null");
        }
        if (tvTime != null) {
            if (lastTime != null) {
                tvTime.setText(lastTime);
            }
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            lastTime = hour + ":" + minute;
        }
    }

    public void onHeaderRefreshCompleted() {
        if (getScrollY() < 0) {
            Log.i(TAG, "onHeaderRefreshCompleted: 手动隐藏header");
            mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY());
            invalidate();
        }

    }

    public void onFooterRefreshCompleted() {
        if (getScrollY() > 0) {
            Log.i(TAG, "onFooterRefreshCompleted: 手动隐藏footer");
            mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY());
            invalidate();
        }

    }


    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            this.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }
    }

    public void setListener(NewClickListener listener) {
        this.mListener = listener;
    }

    public void setmDuration(int millionSeconds){
        this.mDuration=millionSeconds;
    }

    public RecyclerView getContentView() {
        return contentView;
    }

    public View getFooter() {
        return footer;
    }

    public View getHeader() {
        return header;
    }

    public void setFooterVisible(boolean footerVisible) {
        this.footerVisible = footerVisible;
    }

    public void setHeaderVisible(boolean headerVisible) {
        this.headerVisible = headerVisible;
    }

    public interface NewClickListener {
        void footerRefreshStart(SwipeView swipeView);

        void headerRefreshStart(SwipeView swipeView);

        void footerRefreshCancel();

        void headerRefreshCancel();
    }
}
