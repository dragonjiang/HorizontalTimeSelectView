# 用VelocityTracker和Animation实现平滑滚动

本文采用VelocityTracker和Animation实现自定义View的平滑滚动，（注： 是view内容的滚动，而不是view控件的滚动）

### VelocityTracker简介
VelocityTracker即速度跟踪，android.view.VelocityTracker主要用与跟踪触摸屏滑动的速率。

static public VelocityTracker obtain();  //获取一个VelocityTracker对象

public void recycle();   //用完回收

addMovement(MotionEvent) //函数将Motion event加入到VelocityTracker类实例中

public void computeCurrentVelocity(int units, float maxVelocity); //计算当前速度, 其中units是单位, 1代表px/毫秒, 1000代表px/秒

public float getXVelocity();  //获取横向速度， 速度值有正负，负值表示相反方向

public float getYVelocity();  //获取纵向速度


### Animation介绍

这里主要用动画的时间插值Interpolator

Interpolator用于动画中的时间插值，其作用就是把0到1的浮点值变化映射到另一个浮点值变化。

简单的插值器有

1. AccelerateInterpolator  加速插值器


2. DecelerateInterpolator 减速插值器


3. AccelerateDecelerateInterpolator  加速减速插值器


4. LinearInterpolator 线性插值器

### 实例
    /**
     * 每次点击的x坐标
     */
    private float mDownX;
    /**
     * actionmove滑动的距离
     */
    private float mMoveDist;
    /**
     * 速度追踪
     */
    private VelocityTracker mVelocityTracker;
    /**
     * 滑动的动画
     */
    private Animation mAnimation;

	@Override
    public boolean onTouchEvent(MotionEvent event) {

        float curX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                stopScroll();       //停止滑动
                mDownX = event.getX();
                mMoveDist = curX;

                //取得velocityTracker实例
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
					//do something
                } else {
                    //手势滑动之后继续滚动
                    sliding();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(10, MAX_SPEED);//计算速度

                final float dx = curX - mDownX;
                mDownX = curX;
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
        if (mAnimation == null) {
            mAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    float speed = mVelocityTracker.getXVelocity();
					//interpolatedTime是当前的时间插值， 从0-1减速变化
					//所以(1 - interpolatedTime)就是从1-0减速变化， 
					//而(1 - interpolatedTime) * speed就是将当前速度乘以插值，速度也会跟着从speed-0减速变化，
					//将(1 - interpolatedTime) * speed)用于重绘，就可以实现平滑的滚动
                    Log.d("sliding", "cur speed = " + String.valueOf((1 - interpolatedTime) * speed));
                }
            };
            mAnimation.setInterpolator(new DecelerateInterpolator());//设置一个减速插值器
        }

        stopScroll();
        mAnimation.setDuration(2000);
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
        if (mVelocityTracker != null) {//要记得回收
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }