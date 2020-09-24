package com.kbjay.scrollscaleimageview

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat

class KJScrollScaleImageView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    interface DismissListener {
        fun onDismissFinish()
        fun onDismissStart()
    }

    var mDismissListener: DismissListener? = null

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var mBitmap: Bitmap

    /**
     * fling 的overX跟overY
     */
    var mSpanOverFling = 200

    /**
     * 双指放大是回弹的factor
     */
    var mScaleReboundFactor = 0.2f

    /**
     * scaleRate=smallRate时,下滑多少距离触发dismiss动画
     */
    var mDismissTranslateYSpan = 200

    /**
     * 图片放大的最大倍数（相对于smallRate）
     */
    var mMaxScaleRate = 4f

    /**
     * 下滑消失开关
     */
    var mEnableScrollDismiss = true

    init {
        val typesArray =
            context.obtainStyledAttributes(attrs, R.styleable.kj_scroll_scale_attrs, 0, 0)

        mScaleReboundFactor = typesArray.getFloat(
            R.styleable.kj_scroll_scale_attrs_scale_rebound_factor,
            mScaleReboundFactor
        )
        mSpanOverFling =
            typesArray.getInt(R.styleable.kj_scroll_scale_attrs_span_over_fling, mSpanOverFling)
        mDismissTranslateYSpan = typesArray.getInt(
            R.styleable.kj_scroll_scale_attrs_dismiss_translate_y_span,
            mSpanOverFling
        )
        mMaxScaleRate =
            typesArray.getFloat(R.styleable.kj_scroll_scale_attrs_max_scale_rate, mMaxScaleRate)

        mEnableScrollDismiss = typesArray.getBoolean(
            R.styleable.kj_scroll_scale_attrs_enable_scroll_dismiss,
            mEnableScrollDismiss
        )

        typesArray.recycle()

        visibility = GONE
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
    private var mScaleRate = 1f
        set(value) {
            field = value
            invalidate()
        }

    /**
     * 处理fling
     */
    private val mOverScroller by lazy {
        OverScroller(context)
    }

    private var mIsScrolling = false

    /**
     * 手势监听（双击，滚动，fling）
     */
    private var mGestureDetector = GestureDetectorCompat(
        context,
        object : GestureDetector.SimpleOnGestureListener(), GestureDetector.OnDoubleTapListener {
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
                if (mScaleRate > mScaleSmall) {
                    val minX =
                        formatTranslateX(
                            -(mBitmap.width * mScaleRate / 2f - width / 2f),
                            mScaleRate
                        ).toInt()

                    val maxX =
                        formatTranslateX(
                            mBitmap.width * mScaleRate / 2f - width / 2f,
                            mScaleRate
                        ).toInt()

                    val minY =
                        formatTranslateY(
                            -(mBitmap.height * mScaleRate / 2f - height / 2f),
                            mScaleRate
                        ).toInt()

                    val maxY =
                        formatTranslateY(
                            (mBitmap.height * mScaleRate / 2f - height / 2f),
                            mScaleRate
                        ).toInt()

                    mOverScroller.fling(
                        mTranslateX.toInt(), mTranslateY.toInt(),
                        velocityX.toInt(), velocityY.toInt(),
                        minX,
                        maxX,
                        minY,
                        maxY,
                        mSpanOverFling,
                        mSpanOverFling
                    )
                    postOnAnimation { handleFling() }
                }

                return false
            }

            private fun handleFling() {
                if (mOverScroller.computeScrollOffset()) {
                    mTranslateX = mOverScroller.currX.toFloat()
                    mTranslateY = mOverScroller.currY.toFloat()
                    invalidate()
                    postOnAnimation { handleFling() }
                }
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // e1：down事件
                // distanceX: 旧位置-新位置
                if (mScaleRate > mScaleSmall) {
                    // 随着手指移动
                    mTranslateX -= distanceX
                    mTranslateX = formatTranslateX(mTranslateX, mScaleRate)
                    mTranslateY -= distanceY
                    mTranslateY = formatTranslateY(mTranslateY, mScaleRate)
                    invalidate()
                } else {
                    // 下滑dismiss
                    if (mEnableScrollDismiss) {
                        mIsScrolling = true
                        mTranslateX -= distanceX
                        mTranslateY -= distanceY
                        if (mTranslateY >= 0 && e2 != null) {
                            //下滑随着手指移动放缩
                            mScaleRate =
                                (height - mTranslateY) / height * mScaleSmall
                            alpha = 1 - alpha / height * mTranslateY
                        }
                        invalidate()
                    }
                }
                return false
            }

            private var mTranslateXBeforeDoubleTap = 0f
            private var mTranslateYBeforeDoubleTap = 0f
            private var mScaleRateBeforeDoubleTap = 1f
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                if (e == null) {
                    return false
                }
                // 如果双击的时候正在fling，那么停止fling
                if (mOverScroller.computeScrollOffset()) {
                    mOverScroller.forceFinished(true)
                }
                if (mIsBig) {
                    // 双击缩小
                    AnimatorSet().apply {
                        playTogether(
                            ObjectAnimator.ofFloat(
                                this@KJScrollScaleImageView,
                                "mTranslateY",
                                mTranslateY,
                                0f
                            ), ObjectAnimator.ofFloat(
                                this@KJScrollScaleImageView,
                                "mTranslateX",
                                mTranslateX,
                                0f
                            ), ObjectAnimator.ofFloat(
                                this@KJScrollScaleImageView,
                                "mScaleRate",
                                mScaleRate,
                                mScaleSmall
                            )
                        )
                    }.start()
                } else {
                    // 双击放大
                    mTranslateXBeforeDoubleTap = mTranslateX
                    mTranslateYBeforeDoubleTap = mTranslateY
                    mScaleRateBeforeDoubleTap = mScaleRate
                    // 保证bitmap中点击的位置在放大之后相对view的位置不变
                    AnimatorSet().apply {
                        playTogether(
                            ObjectAnimator.ofFloat(
                                this@KJScrollScaleImageView,
                                "mTranslateY",
                                mTranslateYBeforeDoubleTap,
                                formatTranslateY(
                                    (height / 2f - e.y) * (mScaleBig / mScaleRateBeforeDoubleTap - 1) + mTranslateYBeforeDoubleTap * (mScaleBig / mScaleRateBeforeDoubleTap),
                                    mScaleBig
                                )
                            ), ObjectAnimator.ofFloat(
                                this@KJScrollScaleImageView,
                                "mTranslateX",
                                mTranslateXBeforeDoubleTap,
                                formatTranslateX(
                                    (width / 2f - e.x) * (mScaleBig / mScaleRateBeforeDoubleTap - 1) + mTranslateXBeforeDoubleTap * (mScaleBig / mScaleRateBeforeDoubleTap),
                                    mScaleBig
                                )
                            ), ObjectAnimator.ofFloat(
                                this@KJScrollScaleImageView,
                                "mScaleRate",
                                mScaleRate,
                                mScaleBig
                            )
                        )
                    }.start()
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

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return super.onSingleTapUp(e)
            }
        })

    /**
     * 围绕中心双指放缩
     */
    private val mScaleGestureDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
            // 双指放缩 反弹
            private var mScaleMaxRate = 1f
            private var mScaleMinRate = 1f
            private var mScaleRateBeforeScale = 1f
            private var mTranslateXBeforeScale = 0f
            private var mTranslateYBeforeScale = 0f
            private var mFocusX = 0f
            private var mFocusY = 0f
            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                if (mOverScroller.computeScrollOffset()) {
                    return false
                }
                // 表示消费事件
                if (detector != null) {
                    mScaleRateBeforeScale = mScaleRate
                    mScaleMaxRate = mScaleBig * (1 + mScaleReboundFactor)
                    mScaleMinRate = mScaleSmall * (1 - mScaleReboundFactor)
                    mFocusX = detector.focusX
                    mFocusY = detector.focusY
                    mTranslateXBeforeScale = formatTranslateX(mTranslateX, mScaleRate)
                    mTranslateYBeforeScale = formatTranslateY(mTranslateY, mScaleRate)
                }
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector?) {
                // 双指放缩回弹
                if (detector != null) {
                    if (mScaleRate > mScaleBig) {
                        mIsBig = true
                        ObjectAnimator.ofFloat(
                            this@KJScrollScaleImageView,
                            "mScaleRate",
                            mScaleRate,
                            mScaleBig
                        ).start()
                    }
                    if (mScaleRate < mScaleSmall) {
                        mIsBig = false
                        ObjectAnimator.ofFloat(
                            this@KJScrollScaleImageView,
                            "mScaleRate",
                            mScaleRate,
                            mScaleSmall
                        ).start()
                    }
                }
            }

            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                // 双指放缩
                if (detector != null) {
                    mScaleRate = mScaleRateBeforeScale * detector.scaleFactor
                    // 设置tranlateX,要求沿着中心点
                    mScaleRate = formatScaleRate(mScaleRate)
                    val translateX =
                        (width / 2f - mFocusX) * (mScaleRate / mScaleRateBeforeScale - 1) + mTranslateXBeforeScale * (mScaleRate / mScaleRateBeforeScale)
                    val translateY =
                        (height / 2f - mFocusY) * (mScaleRate / mScaleRateBeforeScale - 1) + mTranslateYBeforeScale * (mScaleRate / mScaleRateBeforeScale)
                    mTranslateX = formatTranslateX(translateX, mScaleRate)
                    mTranslateY = formatTranslateY(translateY, mScaleRate)
                    invalidate()
                }
                return false
            }

            private fun formatScaleRate(value: Float): Float {
                return when {
                    value <= mScaleMinRate ->
                        mScaleMinRate
                    value >= mScaleMaxRate ->
                        mScaleMaxRate
                    else ->
                        value
                }
            }
        })


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        init()
    }

    private fun init() {
        if (!this::mBitmap.isInitialized && visibility == VISIBLE) {
            throw NullPointerException("you need call showBitmap!!")
        }

        mScaleSmall = when {
            width.toFloat() / height <= mBitmap.width.toFloat() / mBitmap.height -> {
                width.toFloat() / mBitmap.width
            }
            else -> {
                height.toFloat() / mBitmap.height
            }
        }
        mScaleBig = mScaleSmall * mMaxScaleRate
        mScaleRate = mScaleSmall
        mTranslateY = 0f
        mTranslateX = 0f
        alpha = 1f
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas != null) {
            canvas.translate(mTranslateX, mTranslateY)
            canvas.scale(mScaleRate, mScaleRate, width.toFloat() / 2, height.toFloat() / 2)
            canvas.drawBitmap(
                mBitmap,
                width.toFloat() / 2 - mBitmap.width.toFloat() / 2,
                height.toFloat() / 2 - mBitmap.height.toFloat() / 2,
                mPaint.apply {
                    alpha = (this@KJScrollScaleImageView.alpha * 255).toInt()
                }
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (mIsScrolling && event != null && event.actionMasked == MotionEvent.ACTION_UP && mScaleRate <= mScaleSmall) {
            handlerUpAfterScroll()
            mIsScrolling = false
        }

        var result = mScaleGestureDetector.onTouchEvent(event)
        if (!mScaleGestureDetector.isInProgress) {
            result = mGestureDetector.onTouchEvent(event)
        }
        return result
    }

    /**
     * 处理scroll之后的up
     */
    private fun handlerUpAfterScroll() {
        // 如果translateY 大于阈值，那么朝中心点缩小之后隐藏（translateX=0，translateY=0，scale=0，alpha 1..0）
        // 如果translateY 小于阈值，那么弹回原来的位置（tranlateX=0，translateY0，scale=smallScale）
        val translateX = mTranslateX
        val translateY = mTranslateY
        val scaleRate = mScaleRate
        if (mTranslateY >= mDismissTranslateYSpan) {
            val animatorTranslateX = ObjectAnimator.ofFloat(this, "mTranslateX", translateX, 0f)
            val animatorTranslateY = ObjectAnimator.ofFloat(this, "mTranslateY", translateY, 0f)
            val animatorScale = ObjectAnimator.ofFloat(this, "mScaleRate", scaleRate, 0f)
            val animatorAlpha = ObjectAnimator.ofFloat(this, "alpha", alpha, 0f)
            AnimatorSet().apply {
                playTogether(
                    animatorScale,
                    animatorTranslateX,
                    animatorTranslateY,
                    animatorAlpha
                )
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        this@KJScrollScaleImageView.visibility = GONE
                        if (this@KJScrollScaleImageView.mDismissListener != null) {
                            this@KJScrollScaleImageView.mDismissListener!!.onDismissFinish()
                        }
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationStart(animation: Animator?) {
                        if (this@KJScrollScaleImageView.mDismissListener != null) {
                            this@KJScrollScaleImageView.mDismissListener!!.onDismissStart()
                        }
                    }
                })
            }.start()
        } else {
            val animatorTranslateX = ObjectAnimator.ofFloat(this, "mTranslateX", translateX, 0f)
            val animatorTranslateY = ObjectAnimator.ofFloat(this, "mTranslateY", translateY, 0f)
            val animatorScale = ObjectAnimator.ofFloat(this, "mScaleRate", scaleRate, mScaleSmall)
            AnimatorSet().apply {
                playTogether(
                    animatorScale,
                    animatorTranslateX,
                    animatorTranslateY
                )
            }.start()
        }
    }

    private fun formatTranslateX(value: Float, scaleRate: Float): Float {
        val formatRate = when {
            scaleRate > mScaleBig -> mScaleBig
            scaleRate < mScaleSmall -> mScaleSmall
            else -> {
                scaleRate
            }
        }

        if (width >= mBitmap.width * formatRate) {
            return 0f
        }

        val gap = mBitmap.width * formatRate / 2f - width / 2f
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

    private fun formatTranslateY(value: Float, scaleRate: Float): Float {
        val formatRate = when {
            scaleRate > mScaleBig -> mScaleBig
            scaleRate < mScaleSmall -> mScaleSmall
            else -> {
                scaleRate
            }
        }

        if (height >= mBitmap.height * formatRate) {
            return 0f
        }

        val gap = mBitmap.height * formatRate / 2f - height / 2f
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

    fun showBitmap(bitmap: Bitmap) {
        mBitmap = bitmap
        visibility = VISIBLE
        init()
        requestLayout()
        invalidate()
    }
}