package com.kbjay.scrollscaleimageview

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat

/**
 * todo 自定义
 * 1.src
 * 2.span_over_scroll
 * 3.scale_ratio
 */
class KJScrollScaleImageView(context: Context, attrs: AttributeSet?) : View(context, attrs),
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, Runnable,
    ScaleGestureDetector.OnScaleGestureListener {
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBitmap = if (Math.random() > 0.5f) {
        BitmapFactory.decodeResource(context.resources, R.drawable.kb)
    } else {
        BitmapFactory.decodeResource(context.resources, R.drawable.timg)
    }

    companion object {
        const val SPAN_OVER_SCROLL = 200
    }

    private var mTranslateX = 0f
        set(value) {
            field = value
            invalidate()
        }
    private var mTranslateY = 0f
        set(value) {
            field = value
            invalidate()
        }
    private var mScaleBig = 1f
    private var mScaleSmall = 1f
    private var mIsBig = false

    /**
     * 放缩动画
     */
    private var mScaleRate = 1f
        set(value) {
            field = value
            invalidate()
        }

    private val mScaleRateAnimator by lazy {
        ObjectAnimator.ofFloat(this, "mScaleRate", mScaleSmall, mScaleBig)
    }

    /**
     * 手势监听（双击，滚动）
     */
    private var mGestureDetector = GestureDetectorCompat(context, this)

    /**
     * 处理fling
     */
    private val mOverScroller by lazy {
        OverScroller(context)
    }

    private val mScaleGestureDetector = ScaleGestureDetector(context, this)
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 如果view的宽高比小于bitmap的宽高比，横行长图片，否则就是纵向长图片
        if (width.toFloat() / height < mBitmap.width.toFloat() / mBitmap.height) {
            mScaleSmall = width.toFloat() / mBitmap.width
            mScaleBig = height.toFloat() / mBitmap.height
        } else {
            mScaleSmall = height.toFloat() / mBitmap.height
            mScaleBig = width.toFloat() / mBitmap.width
        }
        mScaleRate = mScaleSmall
    }

    override fun onDraw(canvas: Canvas?) {
        canvas!!.translate(mTranslateX, mTranslateY)
        canvas.scale(mScaleRate, mScaleRate, width.toFloat() / 2, height.toFloat() / 2)
        canvas.drawBitmap(
            mBitmap,
            width.toFloat() / 2 - mBitmap.width.toFloat() / 2,
            height.toFloat() / 2 - mBitmap.height.toFloat() / 2,
            mPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        var result = mScaleGestureDetector.onTouchEvent(event)
//        if (!mScaleGestureDetector.isInProgress) {
//            result = mGestureDetector.onTouchEvent(event)
//        }
//        return result
        return mGestureDetector.onTouchEvent(event)
    }

    override fun onShowPress(e: MotionEvent?) {
        // 按下，这里可以添加一些按下的动画，比如波纹效果
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        // 单击事件回调
        return false
    }

    override fun onDown(e: MotionEvent?): Boolean {
        // 这里需要返回true表示我要消费事件，其他回调都无所谓
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        // e1：down事件
        if (mIsBig) {
            mOverScroller.fling(
                mTranslateX.toInt(), mTranslateY.toInt(), velocityX.toInt(), velocityY.toInt(),
                -(mBitmap.width * mScaleRate / 2f - width / 2f).toInt(),
                (mBitmap.width * mScaleRate / 2f - width / 2f).toInt(),
                -(mBitmap.height * mScaleRate / 2f - height / 2f).toInt(),
                (mBitmap.height * mScaleRate / 2f - height / 2f).toInt(),
                SPAN_OVER_SCROLL,
                SPAN_OVER_SCROLL
            )
            postOnAnimation(this)
        }

        return false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        // e1：down事件
        // distanceX: 旧位置-新位置
        if (mIsBig) {
            mTranslateX -= distanceX
            mTranslateX = formatTranslateX(mTranslateX)
            mTranslateY -= distanceY
            mTranslateY = formatTranslateY(mTranslateY)
            invalidate()
        }
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        // 双击事件
        if (mIsBig) {
            ObjectAnimator.ofFloat(this, "mTranslateY", mTranslateY, 0f).start()
            ObjectAnimator.ofFloat(this, "mTranslateX", mTranslateX, 0f).start()
            mScaleRateAnimator.reverse()
        } else {
            // 保证bitmap中点击的位置在放大之后相对view的位置不变
            ObjectAnimator.ofFloat(
                this,
                "mTranslateY",
                0f,
                formatTranslateY(-(e!!.y - height / 2f) * (mScaleBig / mScaleSmall - 1))
            ).start()
            ObjectAnimator.ofFloat(
                this,
                "mTranslateX",
                0f,
                formatTranslateX(-(e.x - width / 2f) * (mScaleBig / mScaleSmall - 1))
            ).start()

            mScaleRateAnimator.start()
        }
        mIsBig = !mIsBig
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        // 双击事件，第二下点击的事件回调（第二下点击了之后move或者直接up等等）
        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        // 如果监听了双击事件，那么单击回调用这个
        return false
    }

    /**
     * 处理fling事件
     */
    override fun run() {
        if (mIsBig && mOverScroller.computeScrollOffset()) {
            mTranslateX = mOverScroller.currX.toFloat()
            mTranslateY = mOverScroller.currY.toFloat()
            invalidate()

            postOnAnimation(this)
        }
    }

    /**
     * big模式下 translateX不能越界
     */
    private fun formatTranslateX(value: Float): Float {
        val gap = mBitmap.width * mScaleBig / 2f - width / 2f
        return when {
            value >= gap -> {
                gap
            }
            value <= -gap -> {
                -gap
            }
            else -> {
                value
            }
        }
    }

    /**
     * big模式下 translateY不能越界
     */
    private fun formatTranslateY(value: Float): Float {
        val gap = mBitmap.height * mScaleBig / 2f - height / 2f
        return when {
            value >= gap -> {
                gap
            }
            value <= -gap -> {
                -gap
            }
            else -> {
                value
            }
        }
    }

    private var mScaleGestureRate = 1f
    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        // 表示消费事件
        mScaleGestureRate = mScaleRate
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        if (detector != null) {
            mScaleRate = mScaleGestureRate * detector.scaleFactor


            invalidate()
        }
        return false
    }
}