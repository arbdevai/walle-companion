package com.walle.companion.ui

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.walle.companion.data.model.WalleExpression
import kotlin.random.Random

class WalleEyeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paints
    private val outerBezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#262626")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val innerBezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141414")
        style = Paint.Style.FILL
    }

    private val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.FILL
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val eyelidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    // Dynamic Animatable Properties
    private var leftEyeTilt = -2f
    private var rightEyeTilt = 2f

    private var leftEyeScaleY = 1.0f
    private var rightEyeScaleY = 1.0f

    private var eyeScaleX = 1.0f
    private var eyeScaleY = 1.0f

    private var pupilOffsetX = 0f
    private var pupilOffsetY = 0f

    private var topEyelidProgress = 0f // 0.0 (open) to 1.0 (fully closed)
    private var bottomEyelidProgress = 0f // For happy expression

    private var currentExpression: WalleExpression = WalleExpression.IDLE

    // Rectangles & geometry cache
    private val leftEyeOuterRect = RectF()
    private val rightEyeOuterRect = RectF()
    private val leftIrisRect = RectF()
    private val rightIrisRect = RectF()

    private val blinkHandler = Handler(Looper.getMainLooper())
    private var isBlinking = false

    var onBlinkListener: (() -> Unit)? = null
    var onEyeInteractionListener: (() -> Unit)? = null

    init {
        setOnClickListener {
            triggerTapAnimation()
            onEyeInteractionListener?.invoke()
        }
        scheduleNextBlink()
    }

    fun setExpression(expression: WalleExpression, animate: Boolean = true) {
        currentExpression = expression
        if (!animate) {
            applyExpressionProperties(expression)
            invalidate()
            return
        }

        animateToExpression(expression)
    }

    fun getExpression(): WalleExpression = currentExpression

    fun setTiltOffset(tiltX: Float, tiltY: Float) {
        // Subtle pupil tracking based on device tilt
        if (currentExpression == WalleExpression.IDLE || currentExpression == WalleExpression.LISTENING) {
            val maxOffset = width * 0.04f
            pupilOffsetX = (tiltX * maxOffset).coerceIn(-maxOffset, maxOffset)
            pupilOffsetY = (tiltY * maxOffset).coerceIn(-maxOffset, maxOffset)
            invalidate()
        }
    }

    private fun applyExpressionProperties(expr: WalleExpression) {
        when (expr) {
            WalleExpression.IDLE -> {
                leftEyeTilt = -2f
                rightEyeTilt = 2f
                eyeScaleX = 1.0f
                eyeScaleY = 1.0f
                topEyelidProgress = 0f
                bottomEyelidProgress = 0f
                irisPaint.color = Color.parseColor("#00E5FF")
            }
            WalleExpression.HAPPY -> {
                leftEyeTilt = -8f
                rightEyeTilt = 8f
                eyeScaleX = 1.0f
                eyeScaleY = 0.95f
                topEyelidProgress = 0.1f
                bottomEyelidProgress = 0.55f // Upward happy crescent
                irisPaint.color = Color.parseColor("#00E5FF")
            }
            WalleExpression.CURIOUS -> {
                // Iconic Wall-E inquisitive head/eye tilt
                leftEyeTilt = -18f
                rightEyeTilt = -15f
                eyeScaleX = 1.05f
                eyeScaleY = 1.0f
                topEyelidProgress = 0.05f
                bottomEyelidProgress = 0f
                irisPaint.color = Color.parseColor("#00E5FF")
            }
            WalleExpression.SURPRISED -> {
                leftEyeTilt = 0f
                rightEyeTilt = 0f
                eyeScaleX = 1.25f
                eyeScaleY = 1.25f
                topEyelidProgress = 0f
                bottomEyelidProgress = 0f
                irisPaint.color = Color.parseColor("#00B0FF")
            }
            WalleExpression.SLEEPY -> {
                leftEyeTilt = 2f
                rightEyeTilt = -2f
                eyeScaleX = 0.95f
                eyeScaleY = 0.9f
                topEyelidProgress = 0.72f // Drooping eyelids
                bottomEyelidProgress = 0f
                irisPaint.color = Color.parseColor("#00838F")
            }
            WalleExpression.LISTENING -> {
                leftEyeTilt = -5f
                rightEyeTilt = 5f
                eyeScaleX = 1.08f
                eyeScaleY = 1.08f
                topEyelidProgress = 0f
                bottomEyelidProgress = 0f
                irisPaint.color = Color.parseColor("#00E5FF")
            }
            WalleExpression.THINKING -> {
                leftEyeTilt = -12f
                rightEyeTilt = 4f
                eyeScaleX = 0.95f
                eyeScaleY = 0.95f
                topEyelidProgress = 0.25f
                bottomEyelidProgress = 0.15f
                irisPaint.color = Color.parseColor("#40C4FF")
            }
        }
    }

    private fun animateToExpression(targetExpr: WalleExpression) {
        val prevLeftTilt = leftEyeTilt
        val prevRightTilt = rightEyeTilt
        val prevScaleX = eyeScaleX
        val prevScaleY = eyeScaleY
        val prevTopLid = topEyelidProgress
        val prevBottomLid = bottomEyelidProgress

        applyExpressionProperties(targetExpr)

        val targetLeftTilt = leftEyeTilt
        val targetRightTilt = rightEyeTilt
        val targetScaleX = eyeScaleX
        val targetScaleY = eyeScaleY
        val targetTopLid = topEyelidProgress
        val targetBottomLid = bottomEyelidProgress

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 380
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { va ->
                val fraction = va.animatedFraction
                leftEyeTilt = prevLeftTilt + (targetLeftTilt - prevLeftTilt) * fraction
                rightEyeTilt = prevRightTilt + (targetRightTilt - prevRightTilt) * fraction
                eyeScaleX = prevScaleX + (targetScaleX - prevScaleX) * fraction
                eyeScaleY = prevScaleY + (targetScaleY - prevScaleY) * fraction
                topEyelidProgress = prevTopLid + (targetTopLid - prevTopLid) * fraction
                bottomEyelidProgress = prevBottomLid + (targetBottomLid - prevBottomLid) * fraction
                invalidate()
            }
        }
        animator.start()
    }

    fun triggerTapAnimation() {
        // Quick happy wink on tap
        val wink = ValueAnimator.ofFloat(1.0f, 0.05f, 1.0f).apply {
            duration = 180
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                rightEyeScaleY = va.animatedValue as Float
                invalidate()
            }
        }
        wink.start()
    }

    private fun scheduleNextBlink() {
        val delay = Random.nextLong(3200, 6000)
        blinkHandler.postDelayed({
            if (currentExpression != WalleExpression.SLEEPY && !isBlinking) {
                performBlink()
            }
            scheduleNextBlink()
        }, delay)
    }

    private fun performBlink() {
        isBlinking = true
        onBlinkListener?.invoke()

        val closeAnim = ValueAnimator.ofFloat(1.0f, 0.04f).apply {
            duration = 85
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val v = va.animatedValue as Float
                leftEyeScaleY = v
                rightEyeScaleY = v
                invalidate()
            }
        }

        val openAnim = ValueAnimator.ofFloat(0.04f, 1.0f).apply {
            duration = 120
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { va ->
                val v = va.animatedValue as Float
                leftEyeScaleY = v
                rightEyeScaleY = v
                invalidate()
            }
        }

        AnimatorSet().apply {
            playSequentially(closeAnim, openAnim)
            start()
        }

        blinkHandler.postDelayed({ isBlinking = false }, 250)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val eyeWidth = w * 0.36f * eyeScaleX
        val eyeHeight = h * 0.62f * eyeScaleY
        val cornerRadius = eyeHeight * 0.32f
        val interocularSpacing = w * 0.09f

        val centerY = h * 0.5f
        val leftCenterX = (w * 0.5f) - (eyeWidth * 0.5f) - (interocularSpacing * 0.5f)
        val rightCenterX = (w * 0.5f) + (eyeWidth * 0.5f) + (interocularSpacing * 0.5f)

        // Draw Left Eye
        drawSingleEye(
            canvas = canvas,
            centerX = leftCenterX,
            centerY = centerY,
            width = eyeWidth,
            height = eyeHeight * leftEyeScaleY,
            tiltAngle = leftEyeTilt,
            cornerRadius = cornerRadius,
            outerRect = leftEyeOuterRect,
            irisRect = leftIrisRect
        )

        // Draw Right Eye
        drawSingleEye(
            canvas = canvas,
            centerX = rightCenterX,
            centerY = centerY,
            width = eyeWidth,
            height = eyeHeight * rightEyeScaleY,
            tiltAngle = rightEyeTilt,
            cornerRadius = cornerRadius,
            outerRect = rightEyeOuterRect,
            irisRect = rightIrisRect
        )
    }

    private fun drawSingleEye(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        tiltAngle: Float,
        cornerRadius: Float,
        outerRect: RectF,
        irisRect: RectF
    ) {
        canvas.save()
        // Rotate around eye center for Wall-E expressive tilt
        canvas.rotate(tiltAngle, centerX, centerY)

        outerRect.set(
            centerX - width * 0.5f,
            centerY - height * 0.5f,
            centerX + width * 0.5f,
            centerY + height * 0.5f
        )

        // 1. Inner Bezel Background (Dark recessed chamber)
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, innerBezelPaint)

        // 2. Outer Bezel Border (Gunmetal rim)
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, outerBezelPaint)

        // 3. Iris / Pupil (Glow Cyan)
        val irisWidth = width * 0.72f
        val irisHeight = height * 0.74f
        val irisRadius = cornerRadius * 0.75f

        val irisCenterX = centerX + pupilOffsetX
        val irisCenterY = centerY + pupilOffsetY

        irisRect.set(
            irisCenterX - irisWidth * 0.5f,
            irisCenterY - irisHeight * 0.5f,
            irisCenterX + irisWidth * 0.5f,
            irisCenterY + irisHeight * 0.5f
        )
        canvas.drawRoundRect(irisRect, irisRadius, irisRadius, irisPaint)

        // 4. White Pupil Highlight Dot (Gives authentic depth)
        val highlightRadius = irisWidth * 0.12f
        val highlightX = irisRect.right - (irisWidth * 0.28f)
        val highlightY = irisRect.top + (irisHeight * 0.28f)
        canvas.drawCircle(highlightX, highlightY, highlightRadius, highlightPaint)

        // 5. Eyelid Overlays
        // Top eyelid (Droop down for sleepy / blink)
        if (topEyelidProgress > 0f) {
            val topLidHeight = height * topEyelidProgress
            val topLidRect = RectF(
                outerRect.left - 4f,
                outerRect.top - 4f,
                outerRect.right + 4f,
                outerRect.top + topLidHeight
            )
            canvas.drawRect(topLidRect, eyelidPaint)
        }

        // Bottom eyelid (Rise up for happy crescent)
        if (bottomEyelidProgress > 0f) {
            val bottomLidHeight = height * bottomEyelidProgress
            val bottomLidRect = RectF(
                outerRect.left - 4f,
                outerRect.bottom - bottomLidHeight,
                outerRect.right + 4f,
                outerRect.bottom + 4f
            )
            canvas.drawRoundRect(bottomLidRect, cornerRadius * 0.6f, cornerRadius * 0.6f, eyelidPaint)
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        blinkHandler.removeCallbacksAndMessages(null)
    }
}
