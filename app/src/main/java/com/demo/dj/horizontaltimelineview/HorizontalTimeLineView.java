package com.demo.dj.horizontaltimelineview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * @author DragonJiang
 * @Date 2015/1/7
 * @Time 18:24
 * @description
 */
public class HorizontalTimeLineView extends View {
    /**
     * 颜色
     */
    private static final int YELLOW = R.color.colorAccent;
    private static final int GRAY = R.color.gray;
    private static final int GRAY_TEXT = R.color.gray_text;
    private static final int WHITE = R.color.white;

    /**
     * 节点半径
     */
    private static final int SELECTED_NODE_RADIUS = 9;
    private static final int HOUR_NODE_RADIUS = 7;
    private static final int HALF_HOUR_NODE_RADIUS = 2;

    /**
     * 节点类型
     */
    private static final int TYPE_HOUR = 0;
    private static final int TYPE_HALF_HOUR = 1;
    private static final int TYPE_SELECTED = 2;

    /**
     * 半个小时的毫秒数
     */
    private static final int HALF_HOUR = 1800 * 1000;

    /**
     * 滑动最大速度
     */
    private static final int MAX_SPEED = 80;
    /**
     * 横线的宽度
     */
    private float mLineWidth;

    /**
     * 上下文
     */
    private Context mContext;
    /**
     * view 的 width
     */
    private int mWidth;
    /**
     * view's height
     */
    private int mHeight;

    /**
     * 24个时间节点
     */
    private List<NodeInfo> mNodes;
    /**
     * 选中的时间节点
     */
    private NodeInfo mSelectedNode;
    /**
     * 画笔
     */
    private Paint mNodePaint;
    private Paint mTextPaint;
    /**
     * 字体高度
     */
    private float mTextHeight;
    /**
     * TimeText宽度
     */
    private float mTextWidth;
    /**
     * 节点间的距离
     */
    private float mNodeDistance;
    /**
     * 所有节点合成的距离
     */
    private float mTotalDistance;
    /**
     * 已选择的时间（转换成毫秒）
     */
    private long mSelectedTimeMillis;
    /**
     * 是否设置默认的时间
     */
    private boolean mSetDefaultHour;
    /**
     * 是否需要初始化
     */
    private boolean mShouldInit;
    /**
     * 每次点击的x坐标
     */
    private float mDownX;
    /**
     * actionmove滑动的距离
     */
    private float mMoveDist;
    /**
     * 滑动的速度
     */
    private float mSpeed;
    /**
     * 速度追踪
     */
    private VelocityTracker mVelocityTracker;
    /**
     * 滑动的动画
     */
    private Animation mAnimation;

    /**
     * 回调接口
     */
    private ITimeSetCallback mTimeSetCallback;

    /**
     * 时间设置完成的回调
     */
    public interface ITimeSetCallback {
        void onTimeSet(long timeInMillis);
    }

    /**
     * 处理惯性滑动
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //sliding
            float interpolatedTime = (float) msg.obj;
            updateNodesByTouch((1 - interpolatedTime) * mSpeed);
            super.handleMessage(msg);
        }
    };

    public HorizontalTimeLineView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public HorizontalTimeLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public HorizontalTimeLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        mLineWidth = dp2px(4f);

        mNodePaint = new Paint();
        mNodePaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setAlpha(100);
        mTextPaint.setTextSize(dp2px(13));
        mTextPaint.setColor(getResources().getColor(GRAY));
        //取得字体的高度
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.descent - fontMetrics.ascent;
        mTextWidth = mTextPaint.measureText(formatTime(System.currentTimeMillis()));

        mSetDefaultHour = true;
        mShouldInit = true;
    }


    /**
     * 由外面调用，设置日期，不精确具体几点。
     *
     * @param timeMillis
     */
    public void setDate(long timeMillis) {
        mSelectedTimeMillis = timeMillis;
        mSetDefaultHour = true;
        mShouldInit = true;
        invalidate();
    }

    /**
     * 由外面调用，设置具体时间
     *
     * @param timeMillis
     */
    public void bindTime(long timeMillis) {
        mSelectedTimeMillis = timeMillis;
        mSetDefaultHour = false;
        mShouldInit = true;
        invalidate();
    }

    /**
     * 取设置的时间
     *
     * @return
     */
    public long getSelectedTime() {
        if (mSelectedNode != null) {
            return mSelectedNode.time;
        }

        return mSelectedTimeMillis;
    }

    /**
     * 设置回调
     *
     * @param callback
     */
    public void setTimeSetCallback(ITimeSetCallback callback) {
        mTimeSetCallback = callback;
    }

    /**
     * 初始化所有节点
     */
    private void initNodes() {
        if (mNodes == null) {
            mNodes = new ArrayList<>();
            for (int i = 0; i < 48; i++) {
                mNodes.add(new NodeInfo());
            }
        }

        calcNodeDistance();

        long selectedHour = getSelectedHour(mSetDefaultHour);
        float selectedCx = mWidth / 2;
        float selectedCy = mHeight / 2;

        int index = 0;
        for (int i = -23; i <= 24; i++) {
            float cx = selectedCx + i * mNodeDistance;
            NodeInfo node = mNodes.get(index++);
            if (i % 2 == 0) {
                //整点
                node.setInfo(cx, selectedCy, HOUR_NODE_RADIUS, null, TYPE_HOUR, selectedHour + i * HALF_HOUR);
            } else {
                //半点
                node.setInfo(cx, selectedCy, HALF_HOUR_NODE_RADIUS, null, TYPE_HALF_HOUR, selectedHour + i * HALF_HOUR);
            }

            if (i == 0) {//selected node
                if (mSelectedNode == null) {
                    mSelectedNode = new NodeInfo(cx, selectedCy, SELECTED_NODE_RADIUS, null, TYPE_SELECTED, selectedHour
                            + i * HALF_HOUR);
                } else {
                    mSelectedNode.setInfo(cx, selectedCy, SELECTED_NODE_RADIUS, null, TYPE_SELECTED, selectedHour + i *
                            HALF_HOUR);
                }
            }
        }

        //当前时间
//        RectF rectF = calcCurTimeRect();
//        if (mCurTimeNode == null) {
//            mCurTimeNode = new NodeInfo(rectF.centerX(), rectF.centerY(), 0, rectF, TYPE_SELECTED, System
//                    .currentTimeMillis());
//        } else {
//            mCurTimeNode.setInfo(rectF.centerX(), rectF.centerY(), 0, rectF, TYPE_SELECTED, System
// .currentTimeMillis());
//        }

        if (mTimeSetCallback != null) {
            mTimeSetCallback.onTimeSet(mSelectedNode.time);
        }
    }

    /**
     * 初始化的时候取得默认选择的小时。
     *
     * @return 返回的时间以毫秒计时
     */
    private long getSelectedHour(boolean setDefault) {
        if (setDefault) {
            //设置默认的时间
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            int curDay = calendar.get(Calendar.DAY_OF_YEAR);
            int choseDay = curDay;
            if (mSelectedTimeMillis != 0) {
                calendar.setTimeInMillis(mSelectedTimeMillis);
                choseDay = calendar.get(Calendar.DAY_OF_YEAR);
            }

            if (choseDay != curDay) {
                calendar.set(Calendar.HOUR_OF_DAY, 12);
                calendar.set(Calendar.MINUTE, 0);
                return calendar.getTimeInMillis();
            } else {
                return getSelectedHourByCurTime();
            }
        } else {
            //设置传入的时间
            return getSelectedHourByTime(mSelectedTimeMillis);
        }
    }

    /**
     * 根据当前时间选择默认的时间
     *
     * @return 返回的时间以毫秒计时
     */
    private long getSelectedHourByCurTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() - 2 * 3600 * 1000);//往前推两个小时

        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        return calendar.getTimeInMillis();
    }

    /**
     * 根据当前时间选择默认的时间
     *
     * @return 返回的时间以毫秒计时
     */
    private long getSelectedHourByTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);

        int minute = calendar.get(Calendar.MINUTE);
        if (minute > 15 && minute <= 45) {
            calendar.set(Calendar.MINUTE, 30);
        } else if (minute > 45) {
            calendar.set(Calendar.MINUTE, 0);
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        } else {
            calendar.set(Calendar.MINUTE, 0);
        }

        return calendar.getTimeInMillis();
    }

    /**
     * 更新所有节点
     *
     * @param dx
     */
    private void updateNodesByTouch(float dx) {
        if (mNodes == null || mNodes.isEmpty()) {
            return;
        }

        for (NodeInfo node : mNodes) {
            dispatchUpdateNode(node, dx);
        }

        if (mSelectedNode != null) {
            dispatchUpdateNode(mSelectedNode, dx);
        }

        invalidate();
    }

    /**
     * 更新一个节点的操作
     *
     * @param node
     * @param dx
     */
    private void dispatchUpdateNode(NodeInfo node, float dx) {
        if (node != null) {
            node.cx += dx;
            if (node.cx > mTotalDistance) {
                node.cx -= mTotalDistance;
            } else if (node.cx < -2 * mNodeDistance) {
                node.cx += mTotalDistance;
            }
        }
    }

    /**
     * 用户点击之后重新设置已选择的节点
     */
    private void updateSelectedNodeByTouch() {

        for (NodeInfo node : mNodes) {
            float distance = Math.abs(node.cx - mDownX);
            if (distance < mNodeDistance / 2) {
                mSelectedNode.cx = node.cx;
                mSelectedNode.setTime(node.time);
                break;
            }
        }

        invalidate();
    }

    /**
     * 格式化时间为HH:mm
     *
     * @param time
     * @return
     */
    public static final String formatTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return format.format(calendar.getTime());
    }

    /**
     * 计算两个节点之间的距离
     */
    private void calcNodeDistance() {
        mNodeDistance = mWidth / 11f;
        mTotalDistance = mNodeDistance * 48;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float curX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                stopScroll();
                mDownX = event.getX();
                mMoveDist = curX;

                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
                mVelocityTracker.addMovement(event);
                break;

            case MotionEvent.ACTION_UP:
                mMoveDist = curX - mMoveDist;
                if (Math.abs(mMoveDist) < ViewConfiguration.get(mContext).getScaledTouchSlop()) {//过滤点击不小心滑动
                    //如果是点击操作
                    updateSelectedNodeByTouch();
                    if (mTimeSetCallback != null) {
                        mTimeSetCallback.onTimeSet(mSelectedNode.time);
                    }
                } else {
                    //滑动之后
                    sliding();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(10, MAX_SPEED);

                final float dx = curX - mDownX;
                mDownX = curX;
                updateNodesByTouch(dx);//更新所有节点的位置。
                break;
            default:
                break;
        }

        return true;
    }


    /**
     * 滑动操作后的惯性滑动。
     * 利用animation实现
     */
    private void sliding() {
        mSpeed = mVelocityTracker.getXVelocity();

        if (mAnimation == null) {
            mAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    Message msg = Message.obtain();
                    msg.obj = interpolatedTime;
                    mHandler.sendMessage(msg);
                }
            };
            mAnimation.setInterpolator(new DecelerateInterpolator());
        }

        stopScroll();
        mAnimation.setDuration(mSpeed == MAX_SPEED ? 3000 : 2000);
        startAnimation(mAnimation);
    }

    /**
     * 停止滑动
     */
    private void stopScroll() {
        if (mAnimation != null && !mAnimation.hasEnded()) {
            mAnimation.cancel();
            clearAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mShouldInit) {
            mShouldInit = false;
            //初始化所有节点，需要在onMeasure取得宽度和高度之后执行。
            initNodes();
        }

        canvas.drawColor(getResources().getColor(WHITE));
        drawNodesAndTime(canvas);
    }

    /**
     * 画节点和时间
     *
     * @param canvas
     */
    private void drawNodesAndTime(Canvas canvas) {
        if (mNodes == null || mNodes.isEmpty()) {
            return;
        }

        for (NodeInfo node : mNodes) {
            drawnNormalNode(canvas, node);
            drawTimeBottom(canvas, node);
        }

        if (mSelectedNode != null) {
            drawSelectedNode(canvas, mSelectedNode);
            drawTimeBottom(canvas, mSelectedNode);
        }
    }

    /**
     * 画选中的节点
     *
     * @param canvas
     * @param node
     */
    private void drawSelectedNode(Canvas canvas, NodeInfo node) {
        if (node == null) {
            return;
        }

        //clear
        mNodePaint.setColor(getResources().getColor(WHITE));
        mNodePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(node.cx, node.cy, dp2px(SELECTED_NODE_RADIUS), mNodePaint);

        //画圆心
        mNodePaint.setColor(getResources().getColor(YELLOW));
        mNodePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(node.cx, node.cy, dp2px((int) node.radius / 4), mNodePaint);

        //画外环
        mNodePaint.setStyle(Paint.Style.STROKE);
        mNodePaint.setStrokeWidth(5);
        canvas.drawCircle(node.cx, node.cy, dp2px((int) node.radius), mNodePaint);
    }

    /**
     * 画正常的时间节点
     *
     * @param canvas
     * @param node
     */
    private void drawnNormalNode(Canvas canvas, NodeInfo node) {
        if (node == null) {
            return;
        }

        if (node.type == TYPE_HOUR) {
            mNodePaint.setColor(getResources().getColor(GRAY));
            mNodePaint.setStyle(Paint.Style.STROKE);
            mNodePaint.setStrokeWidth(8);
        } else {
            mNodePaint.setColor(getResources().getColor(GRAY));
            mNodePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        canvas.drawCircle(node.cx, node.cy, dp2px((int) node.radius), mNodePaint);
    }

    /**
     * 画当前时间节点
     *
     * @param canvas
     * @param node
     */
    private void drawCurTimeNode(Canvas canvas, NodeInfo node) {
        if (node == null || node.rect == null) {
            return;
        }

        mNodePaint.setColor(getResources().getColor(YELLOW));
        mNodePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawRect(node.rect, mNodePaint);
    }


    /**
     * 画时间，位于横线下方
     *
     * @param canvas
     * @param node
     */
    private void drawTimeBottom(Canvas canvas, NodeInfo node) {
        if (node == null || TextUtils.isEmpty(node.formatTime)) {
            return;
        }

        if (node.type == TYPE_SELECTED && checkIsHourNode(node.time)) {
            mTextPaint.setColor(getResources().getColor(YELLOW));
        } else if (node.type == TYPE_HOUR) {
            mTextPaint.setColor(getResources().getColor(GRAY_TEXT));
        } else {
            return;
        }
        canvas.drawText(node.formatTime, node.cx - mTextWidth / 2, node.cy + mTextHeight + dp2px(15), mTextPaint);
    }

    /**
     * 画时间，位于横线上方
     *
     * @param canvas
     * @param node
     */
    private void drawTimeTop(Canvas canvas, NodeInfo node) {
        if (node == null || TextUtils.isEmpty(node.formatTime)) {
            return;
        }

        mTextPaint.setColor(getResources().getColor(YELLOW));
        canvas.drawText(node.formatTime, node.cx - mTextWidth / 2, node.cy - (mTextHeight + dp2px(3)), mTextPaint);
    }

    /**
     * 通过节点的时间判断节点是否是整点
     *
     * @param time
     * @return
     */
    private boolean checkIsHourNode(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int minute = calendar.get(Calendar.MINUTE);
        if (minute == 0) {
            return true;
        } else {
            return false;
        }
    }

    private float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mContext.getResources().getDisplayMetrics());
    }

    private int px2dp(float pxValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 节点信息类
     */
    private static class NodeInfo {
        public float cx;
        public float cy;
        public float radius;
        public RectF rect;
        public int type;
        public long time;
        String formatTime;

        public NodeInfo() {

        }

        public NodeInfo(float cx, float cy, float radius, RectF rect, int type, long time) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.rect = rect;
            this.type = type;
            this.time = time;
            this.formatTime = formatTime(time);
        }

        public void setInfo(float cx, float cy, float radius, RectF rect, int type, long time) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.rect = rect;
            this.type = type;
            this.time = time;
            this.formatTime = formatTime(time);
        }

        public void setTime(long time) {
            this.time = time;
            this.formatTime = formatTime(time);
        }
    }

}
