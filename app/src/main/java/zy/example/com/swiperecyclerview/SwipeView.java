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
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
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
    private String lastTime;
    private View contentView;
    private View footer;
    //  控件高度
    private int headerHeight;
    private int footerHeight;
    //  滑动参数  lastY 上次触摸事件的高度  mScroller 滑动类  mSlop 系统最小滑动距离  autoScrollRange 启动自动滑动的系数
    private float lastY;
    private Scroller mScroller;
    private int mSlop;
    private double autoScrollRange = 0.8;
    //  滑动是否完成的标志
    private boolean footerRefreshCompleted;
    private boolean headerRefreshCompleted;
    //  实现header和footer内部逻辑的接口
    private NewClickListener mListener;
    private int contentViewHeight;


    public SwipeView(Context context) {
        super(context);
    }

    public SwipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SwipeView);
        int viewType = ta.getInt(R.styleable.SwipeView_view_type, 0);
        int headerLayout = ta.getResourceId(R.styleable.SwipeView_header_layout, 0);
        int footerLayout = ta.getResourceId(R.styleable.SwipeView_footer_layout, 0);
        headerVisible=ta.getBoolean(R.styleable.SwipeView_header_visible,true);
        footerVisible=ta.getBoolean(R.styleable.SwipeView_footer_visible,true);
        ta.recycle();

        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);
        autoScrollRange = 0.6;
        switch (headerLayout) {
            case 0:
                header = LayoutInflater.from(context).inflate(R.layout.item_header, this, false);
                tvTime = header.findViewById(R.id.tv_time);
                break;
            default:
                header = LayoutInflater.from(context).inflate(headerLayout, this, false);
                break;
        }
        header.setTag(Header);
        addView(header);

        switch (viewType) {
            case 1:
                contentView = new ListView(context);
                break;
            default:
                contentView = new RecyclerView(context);
                break;
        }
        contentView.setTag(ContentView);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(contentView, lp);

        switch (footerLayout) {
            case 0:
                footer = LayoutInflater.from(context).inflate(R.layout.item_footer, this, false);
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
                        contentViewHeight=mHeight;
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
                if (Math.abs(distance) > mSlop) {
                    if (contentView instanceof RecyclerView) {
                        LinearLayoutManager manager = (LinearLayoutManager) ((RecyclerView) contentView).getLayoutManager();
                        if (manager != null) {
//                       判断滑动到顶端,开始下拉
                            if (headerVisible&&manager.findFirstCompletelyVisibleItemPosition() == 0 && distance < 0) {
                                return true;
                            }
//                     判断滑动到底端,开始上拉
//                            满足条件，允许下拉、当前最后的条目视图就是列表的最后一个条目、最后一个条目不为空，且最后一个条目在页面底部
                            RecyclerView.Adapter adapter=((RecyclerView) contentView).getAdapter();
                            int count=Objects.requireNonNull(adapter).getItemCount();
                            View lastView=manager.findViewByPosition(count-1);
                            if (footerVisible&&(manager.findLastCompletelyVisibleItemPosition() + 1) == count && distance > 0&&lastView!=null&&lastView.getBottom()==contentViewHeight) {
                                return true;
                            }

//                       只要当前显示了header/footer,就拦截事件
                            if (headerRefreshCompleted&&headerVisible){
                                return true;
                            }
                            if (footerRefreshCompleted&&footerVisible){
                                return true;
                            }
                        }
                    }
                    if (contentView instanceof ListView) {
                        ArrayAdapter manager = (ArrayAdapter) ((ListView) contentView).getAdapter();
                        if (manager != null) {
//                       判断滑动到顶端,开始下拉
                            if (headerVisible&&((ListView) contentView).getFirstVisiblePosition() == 0 && distance < 0) {
                                return true;
                            }
//                     判断滑动到底端,开始上拉
//                            排除listView为空，和ListView显示未覆盖屏幕的情况
                            ListView lvList=(ListView)contentView;
                            int count=lvList.getCount();
                            View lastView=lvList.getChildAt(count-lvList.getFirstVisiblePosition()-1);
                            if (footerVisible&&(lvList.getLastVisiblePosition() + 1) ==count && distance > 0&&lastView!=null&&lastView.getBottom()==contentViewHeight) {
                                return true;
                            }
//                       只要当前显示了header/footer,就拦截事件
                            if (headerRefreshCompleted&&headerVisible){
                                return true;
                            }
                            if (footerRefreshCompleted&&footerVisible){
                                return true;
                            }
                        }
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
        float distance = lastY - event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                lastY = event.getRawY();
//                默认可滑动,在move中处理视图内滑动事件,在up中处理视图外滑动事件
                scrollBy(0, (int) distance);
//                在视图内滑动处理
                if (getScrollY() >= -headerHeight && getScrollY() <= footerHeight) {
//                向上滑动,a想要上拉显示footer,b想要上拉取消header
                    if (distance > 0) {
//                        a要上拉显示footer,超过角标的autoScrollRange就自动下拉显示
                        if (!footerRefreshCompleted && getScrollY() >= footerHeight * autoScrollRange) {
                            Log.i(TAG, "onTouchEvent: 自动上拉");
                            mScroller.startScroll(getScrollX(), getScrollY(), 0, footerHeight - getScrollY(),500);
                            footerRefreshCompleted = true;
                            if (mListener != null) {
                                mListener.footerRefreshStart(footer, contentView);
                            } else {
                                Log.e(TAG, "onTouchEvent: mListener=null");
                            }
                        }
//                      b想要上拉取消header,超过角标的autoScrollRange就自动下拉显示
                        if (headerRefreshCompleted && getScrollY() < 0) {
                            Log.i(TAG, "onTouchEvent: 取消下拉");
                            mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY(), 1000);
                            headerRefreshCompleted = false;
                            if (mListener != null) {
                                mListener.headerRefreshCancel();
                            } else {
                                Log.e(TAG, "onTouchEvent: mListener=null");
                            }

                        }

                    }
//                    向下滑动,a想要下拉显示header,b想要下拉取消footer
                    if (distance < 0) {

//                  a判定下拉显示header,超过角标的autoScrollRange就自动下拉显示
                        if (!headerRefreshCompleted && getScrollY() <= -headerHeight * autoScrollRange) {
                            Log.i(TAG, "onTouchEvent: 自动下拉");
                            mScroller.startScroll(getScrollX(), getScrollY(), 0, -headerHeight - getScrollY(),500);
                            headerRefreshCompleted = true;
                            headerRefreshStart();
                        }

//                  b判定想要上拉,取消上拉footer,超过角标的autoScrollRange就自动取消下拉footer
                        if (footerRefreshCompleted && getScrollY() > 0) {
                            Log.i(TAG, "onTouchEvent: 取消上拉");
                            mScroller.startScroll(getScrollX(), getScrollY(), 0, -footerHeight, 1000);
                            footerRefreshCompleted = false;
                            if (mListener != null) {
                                mListener.footerRefreshCancel();
                            } else {
                                Log.e(TAG, "onTouchEvent: mListener=null");
                            }
                        }
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
//                如果移动范围超过视图顶端范围,那么在手指抬起时,返回到视图最顶端
                if (getScrollY() < -headerHeight) {
                    mScroller.startScroll(getScrollX(), getScrollY(), 0, -headerHeight - getScrollY(), 500);
                }
//                如果移动范围超过视图底端范围,那么在手指抬起时,返回到视图最底端
                if (getScrollY() > footerHeight) {
                    mScroller.startScroll(getScrollX(), getScrollY(), 0, footerHeight - getScrollY(),500);
                }
//                如果在视图范围内,手指抬起时,没有触发自动显示header/footer,就自动隐藏
                if (getScrollY() >= -headerHeight && getScrollY() <= footerHeight) {
//                    自动隐藏header
                    if (!headerRefreshCompleted && getScrollY() > -headerHeight * autoScrollRange&&getScaleY()<0) {
                        Log.i(TAG, "onTouchEvent: 自动隐藏header");
                        mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY(),500);
                    }
//                    自动隐藏footer
                    if (!footerRefreshCompleted && getScrollY() < footerHeight * autoScrollRange&&getScrollY()>0) {
                        Log.i(TAG, "onTouchEvent: 自动隐藏footer");
                        mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY(),500);
                    }
                }
                invalidate();
                break;

        }
        return super.onTouchEvent(event);
    }

    private void headerRefreshStart() {
        if (mListener != null) {
            mListener.headerRefreshStart(header, contentView);
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
        if (getScrollY()<0) {
            Log.i(TAG, "onHeaderRefreshCompleted: 手动隐藏header");
            headerRefreshCompleted = false;
            mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY());
            invalidate();
        }

    }

    public void onFooterRefreshCompleted() {
        if (getScrollY()>0){
            Log.i(TAG, "onFooterRefreshCompleted: 手动隐藏footer");
            footerRefreshCompleted = false;
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

    public View getContentView() {
        return contentView;
    }

    public View getFooter() {
        return footer;
    }

    public View getHeader() {
        return header;
    }

    public void setFooterVisible(boolean footerVisible){
        this.footerVisible=footerVisible;
    }

    public void setHeaderVisible(boolean headerVisible){
        this.headerVisible=headerVisible;
    }

    public interface NewClickListener {
        void footerRefreshStart(View footer, View contentView);

        void headerRefreshStart(View header, View contentView);

        void footerRefreshCancel();

        void headerRefreshCancel();
    }
}
