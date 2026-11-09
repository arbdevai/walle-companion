package com.walle.companion.ui

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.walle.companion.data.model.WalleExpression
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cozmo / Vector style expressive organic OLED robot eyes.
 * Borderless, floating glowing cyber-pet eyes designed for Super AMOLED display.
 */
class WalleEyeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Main Glowing Eye Paint
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Secondary subtle eyelid/cutout mask paint (Pure OLED Black)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    // Geometry Paths
    private val leftEyePath = Path()
    private val rightEyePath = Path()
    private val leftMaskPath = Path()
    private val rightMaskPath = Path()

    // Animatable State Variables
    private var eyeScaleX = 1.0f
    private var leftEyeScaleY = 1.0f
    private var rightEyeScaleY = 1.0f

    // Slant offsets for expressive eyebrow simulation (in pixels)
    private var leftSlantTopOuter = 0f
    private var leftSlantTopInner = 0f
    private var rightSlantTopInner = 0f
    private var rightSlantTopOuter = 0f

    // Bottom cutout progress for Happy / Squint (0.0 = flat bottom, 1.0 = deep smile arch)
    private var happyArchProgress = 0f

    // Sleepy droop progress (0.0 = wide open, 1.0 = fully closed slit)
    private var sleepyDroopProgress = 0f

    // Subtle breathing pulse
    private var breathingScale = 1.0f
    private var breathingPhase = 0.0

    // Eye shift / Gaze offset (tracking tilt or looking sideways)
    private var gazeOffsetX = 0f
    private var gazeOffsetY = 0f

    // Colors
    private var primaryEyeColor = Color.parseColor("#00E5FF")
    private var secondaryEyeColor = Color.parseColor("#00B0FF")

    private var currentExpression: WalleExpression = WalleExpression.IDLE

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isBlinking = false

    var onBlinkListener: (() -> Unit)? = null
    var onEyeInteractionListener: (() -> Unit)? = null

    init {
        setOnClickListener {
            triggerTapAnimation()
            onEyeInteractionListener?.invoke()
        }
        scheduleNextBlink()
        startBreathingLoop()
    }

    fun setExpression(expression: WalleExpression, animate: Boolean = true) {
        currentExpression = expression
        if (!animate) {
            applyExpressionTarget(expression)
            invalidate()
            return
        }
        animateToExpression(expression)
    }

    fun getExpression(): WalleExpression = currentExpression

    fun setTiltOffset(tiltX: Float, tiltY: Float) {
        if (currentExpression == WalleExpression.IDLE || currentExpression == WalleExpression.LISTENING) {
            val maxShift = width * 0.06f
            gazeOffsetX = (tiltX * maxShift).coerceIn(-maxShift, maxShift)
            gazeOffsetY = (tiltY * maxShift).coerceIn(-maxShift, maxShift)
            invalidate()
        }
    }

    private fun applyExpressionTarget(expr: WalleExpression) {
        val h = if (height > 0) height.toFloat() else 300f
        val slantBase = h * 0.08f

        when (expr) {
            WalleExpression.IDLE -> {
                // Resting cute Cozmo slant: tops slant inward slightly
                leftSlantTopOuter = -slantBase * 0.35f
                leftSlantTopInner = slantBase * 0.35f
                rightSlantTopInner = slantBase * 0.35f
                rightSlantTopOuter = -slantBase * 0.35f

                eyeScaleX = 1.0f
                leftEyeScaleY = 1.0f
                rightEyeScaleY = 1.0f
                happyArchProgress = 0f
                sleepyDroopProgress = 0f

                primaryEyeColor = Color.parseColor("#00E5FF")
                secondaryEyeColor = Color.parseColor("#00B0FF")
            }
            WalleExpression.HAPPY -> {
                // Playful tilted squint with deep upward happy bottom arch
                leftSlantTopOuter = -slantBase * 0.5f
                leftSlantTopInner = slantBase * 0.6f
                rightSlantTopInner = slantBase * 0.6f
                rightSlantTopOuter = -slantBase * 0.5f

                eyeScaleX = 1.02f
                leftEyeScaleY = 0.92f
                rightEyeScaleY = 0.92f
                happyArchProgress = 0.68f // Arch cuts up from bottom
                sleepyDroopProgress = 0.1f

                primaryEyeColor = Color.parseColor("#00E5FF")
                secondaryEyeColor = Color.parseColor("#0091EA")
            }
            WalleExpression.CURIOUS -> {
                // One eye raised inquisitive arch, other eye lowered
                leftSlantTopOuter = -slantBase * 1.3f
                leftSlantTopInner = -slantBase * 0.2f
                rightSlantTopInner = slantBase * 0.9f
                rightSlantTopOuter = -slantBase * 0.2f

                eyeScaleX = 1.0f
                leftEyeScaleY = 1.15f
                rightEyeScaleY = 0.88f
                happyArchProgress = 0f
                sleepyDroopProgress = 0.05f

                primaryEyeColor = Color.parseColor("#00E5FF")
                secondaryEyeColor = Color.parseColor("#00B4D8")
            }
            WalleExpression.SURPRISED -> {
                // Wide open tall circular eyes
                leftSlantTopOuter = 0f
                leftSlantTopInner = 0f
                rightSlantTopInner = 0f
                rightSlantTopOuter = 0f

                eyeScaleX = 1.18f
                leftEyeScaleY = 1.32f
                rightEyeScaleY = 1.32f
                happyArchProgress = 0f
                sleepyDroopProgress = 0f

                primaryEyeColor = Color.parseColor("#40C4FF")
                secondaryEyeColor = Color.parseColor("#00E5FF")
            }
            WalleExpression.SLEEPY -> {
                // Heavy drooping eyelids, dim amber/cyan color
                leftSlantTopOuter = slantBase * 0.2f
                leftSlantTopInner = slantBase * 0.2f
                rightSlantTopInner = slantBase * 0.2f
                rightSlantTopOuter = slantBase * 0.2f

                eyeScaleX = 0.98f
                leftEyeScaleY = 0.85f
                rightEyeScaleY = 0.85f
                happyArchProgress = 0f
                sleepyDroopProgress = 0.72f // Droop covers 72%

                primaryEyeColor = Color.parseColor("#00838F")
                secondaryEyeColor = Color.parseColor("#006064")
            }
            WalleExpression.LISTENING -> {
                // Attentive forward lean, subtle glow
                leftSlantTopOuter = -slantBase * 0.6f
                leftSlantTopInner = slantBase * 0.4f
                rightSlantTopInner = slantBase * 0.4f
                rightSlantTopOuter = -slantBase * 0.6f

                eyeScaleX = 1.05f
                leftEyeScaleY = 1.05f
                rightEyeScaleY = 1.05f
                happyArchProgress = 0f
                sleepyDroopProgress = 0f

                primaryEyeColor = Color.parseColor("#00E5FF")
                secondaryEyeColor = Color.parseColor("#18FFFF")
            }
            WalleExpression.THINKING -> {
                // Slanted thoughtful look
                leftSlantTopOuter = -slantBase * 0.9f
                leftSlantTopInner = slantBase * 0.5f
                rightSlantTopInner = -slantBase * 0.2f
                rightSlantTopOuter = slantBase * 0.8f

                eyeScaleX = 0.95f
                leftEyeScaleY = 0.95f
                rightEyeScaleY = 0.95f
                happyArchProgress = 0.2f
                sleepyDroopProgress = 0.25f

                primaryEyeColor = Color.parseColor("#80D8FF")
                secondaryEyeColor = Color.parseColor("#0091EA")
            }
        }
    }

    private fun animateToExpression(targetExpr: WalleExpression) {
        val sLeftOuter = leftSlantTopOuter
        val sLeftInner = leftSlantTopInner
        val sRightInner = rightSlantTopInner
        val sRightOuter = rightSlantTopOuter
        val sScaleX = eyeScaleX
        val sLeftScaleY = leftEyeScaleY
        val sRightScaleY = rightEyeScaleY
        val sHappy = happyArchProgress
        val sSleepy = sleepyDroopProgress

        applyExpressionTarget(targetExpr)

        val eLeftOuter = leftSlantTopOuter
        val eLeftInner = leftSlantTopInner
        val eRightInner = rightSlantTopInner
        val eRightOuter = rightSlantTopOuter
        val eScaleX = eyeScaleX
        val eLeftScaleY = leftEyeScaleY
        val eRightScaleY = rightEyeScaleY
        val eHappy = happyArchProgress
        val eSleepy = sleepyDroopProgress

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 340
            interpolator = OvershootInterpolator(1.15f)
            addUpdateListener { va ->
                val f = va.animatedFraction
                leftSlantTopOuter = sLeftOuter + (eLeftOuter - sLeftOuter) * f
                leftSlantTopInner = sLeftInner + (eLeftInner - sLeftInner) * f
                rightSlantTopInner = sRightInner + (eRightInner - sRightInner) * f
                rightSlantTopOuter = sRightOuter + (eRightOuter - sRightOuter) * f
                eyeScaleX = sScaleX + (eScaleX - sScaleX) * f
                leftEyeScaleY = sLeftScaleY + (eLeftScaleY - sLeftScaleY) * f
                rightEyeScaleY = sRightScaleY + (eRightScaleY - sRightScaleY) * f
                happyArchProgress = sHappy + (eHappy - sHappy) * f
                sleepyDroopProgress = sSleepy + (eSleepy - sSleepy) * f
                invalidate()
            }
            start()
        }
    }

    fun triggerTapAnimation() {
        // Playful double wink
        val wink = ValueAnimator.ofFloat(1.0f, 0.08f, 1.0f).apply {
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
        val delay = Random.nextLong(3000, 5500)
        mainHandler.postDelayed({
            if (currentExpression != WalleExpression.SLEEPY && !isBlinking) {
                performBlink()
            }
            scheduleNextBlink()
        }, delay)
    }

    private fun performBlink() {
        isBlinking = true
        onBlinkListener?.invoke()

        val closeAnim = ValueAnimator.ofFloat(1.0f, 0.05f).apply {
            duration = 75
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val v = va.animatedValue as Float
                leftEyeScaleY = v
                rightEyeScaleY = v
                invalidate()
            }
        }

        val openAnim = ValueAnimator.ofFloat(0.05f, 1.0f).apply {
            duration = 110
            interpolator = OvershootInterpolator(1.25f)
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

        mainHandler.postDelayed({ isBlinking = false }, 220)
    }

    private fun startBreathingLoop() {
        val breathingRunnable = object : Runnable {
            override fun run() {
                if (currentExpression == WalleExpression.IDLE || currentExpression == WalleExpression.LISTENING) {
                    breathingPhase += 0.08
                    breathingScale = 1.0f + (sin(breathingPhase) * 0.025f).toFloat()
                    invalidate()
                }
                mainHandler.postDelayed(this, 40)
            }
        }
        mainHandler.post(breathingRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // Proportions: Cozmo eyes are CHUBBY HORIZONTAL (wider than tall)
        // Eye Width is ~30% of total width, Height is ~48% of total height
        val eyeWidth = (w * 0.31f) * eyeScaleX
        val baseEyeHeight = (h * 0.52f) * breathingScale
        val cornerRadius = eyeWidth * 0.28f // Generously rounded organic corners
        val eyeSpacing = w * 0.08f // Compact distance between eyes

        val centerY = (h * 0.48f) + gazeOffsetY
        val leftCenterX = (w * 0.5f) - (eyeWidth * 0.5f) - (eyeSpacing * 0.5f) + gazeOffsetX
        val rightCenterX = (w * 0.5f) + (eyeWidth * 0.5f) + (eyeSpacing * 0.5f) + gazeOffsetX

        // Gradient shader for rich glowing depth
        eyePaint.shader = LinearGradient(
            0f, centerY - baseEyeHeight * 0.6f,
            0f, centerY + baseEyeHeight * 0.6f,
            primaryEyeColor, secondaryEyeColor,
            Shader.TileMode.CLAMP
        )

        // Draw Left Eye
        drawOrganicEye(
            canvas = canvas,
            centerX = leftCenterX,
            centerY = centerY,
            width = eyeWidth,
            height = baseEyeHeight * leftEyeScaleY,
            cornerRadius = cornerRadius,
            slantTopOuter = leftSlantTopOuter,
            slantTopInner = leftSlantTopInner,
            happyArch = happyArchProgress,
            sleepyDroop = sleepyDroopProgress,
            isLeft = true,
            eyePath = leftEyePath,
            maskPath = leftMaskPath
        )

        // Draw Right Eye
        drawOrganicEye(
            canvas = canvas,
            centerX = rightCenterX,
            centerY = centerY,
            width = eyeWidth,
            height = baseEyeHeight * rightEyeScaleY,
            cornerRadius = cornerRadius,
            slantTopOuter = rightSlantTopOuter,
            slantTopInner = rightSlantTopInner,
            happyArch = happyArchProgress,
            sleepyDroop = sleepyDroopProgress,
            isLeft = false,
            eyePath = rightEyePath,
            maskPath = rightMaskPath
        )
    }

    private fun drawOrganicEye(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        cornerRadius: Float,
        slantTopOuter: Float,
        slantTopInner: Float,
        happyArch: Float,
        sleepyDroop: Float,
        isLeft: Boolean,
        eyePath: Path,
        maskPath: Path
    ) {
        val halfW = width * 0.5f
        val halfH = height * 0.5f

        val left = centerX - halfW
        val right = centerX + halfW
        val top = centerY - halfH
        val bottom = centerY + halfH

        // Calculate corner control coordinates with top slant
        val (topL, topR) = if (isLeft) {
            Pair(top + slantTopOuter, top + slantTopInner)
        } else {
            Pair(top + slantTopInner, top + slantTopOuter)
        }

        eyePath.reset()
        // Start top-left
        eyePath.moveTo(left + cornerRadius, topL)
        // Top edge with expressive slant
        eyePath.lineTo(right - cornerRadius, topR)
        // Top-right corner
        eyePath.quadTo(right, topR, right, topR + cornerRadius)
        // Right edge
        eyePath.lineTo(right, bottom - cornerRadius)
        // Bottom-right corner
        eyePath.quadTo(right, bottom, right - cornerRadius, bottom)
        // Bottom edge
        eyePath.lineTo(left + cornerRadius, bottom)
        // Bottom-left corner
        eyePath.quadTo(left, bottom, left, bottom - cornerRadius)
        // Left edge
        eyePath.lineTo(left, topL + cornerRadius)
        // Top-left corner
        eyePath.quadTo(left, topL, left + cornerRadius, topL)
        eyePath.close()

        // 1. Draw glowing eye shape
        canvas.drawPath(eyePath, eyePaint)

        // 2. Apply Happy Crescent Cutout (from bottom)
        if (happyArch > 0f) {
            maskPath.reset()
            val archHeight = height * happyArch
            val archTop = bottom - archHeight

            maskPath.moveTo(left - 10f, bottom + 10f)
            maskPath.lineTo(right + 10f, bottom + 10f)
            maskPath.lineTo(right + 10f, archTop + (archHeight * 0.45f))
            // Smooth bezier arch curving UP into the eye
            maskPath.quadTo(centerX, archTop - (archHeight * 0.25f), left - 10f, archTop + (archHeight * 0.45f))
            maskPath.close()

            canvas.drawPath(maskPath, maskPaint)
        }

        // 3. Apply Sleepy Droop Cutout (from top)
        if (sleepyDroop > 0f) {
            maskPath.reset()
            val droopHeight = height * sleepyDroop
            val droopBottom = top + droopHeight

            maskPath.moveTo(left - 10f, top - 20f)
            maskPath.lineTo(right + 10f, top - 20f)
            maskPath.lineTo(right + 10f, droopBottom)
            maskPath.quadTo(centerX, droopBottom + (height * 0.08f), left - 10f, droopBottom)
            maskPath.close()

            canvas.drawPath(maskPath, maskPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mainHandler.removeCallbacksAndMessages(null)
    }
}
