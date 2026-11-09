package com.walle.companion.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DigitalClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ClockStyle {
        CYBER_SEVEN_SEGMENT, // Retro 7-Segment LED / VFD Display
        FUTURISTIC_OLED,     // Sleek High-Contrast Minimalist Monospace
        RETRO_DOT_MATRIX     // Dot Matrix Glowing Cyber Grid
    }

    private var currentStyle: ClockStyle = ClockStyle.CYBER_SEVEN_SEGMENT
    private var primaryColor: Int = Color.parseColor("#00E5FF")
    private var unlitColor: Int = Color.parseColor("#102026") // Dim ghost segments for authentic LED

    // Paints
    private val litSegmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val unlitSegmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textClockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        color = Color.parseColor("#888888")
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val handler = Handler(Looper.getMainLooper())
    private var colonVisible = true
    private val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID"))

    private var currentTimeString = "000000"
    private var currentDateString = ""

    var onClockStyleChanged: ((ClockStyle) -> Unit)? = null

    init {
        setOnClickListener {
            cycleNextStyle()
        }
        startTimeLoop()
    }

    fun setColor(color: Int) {
        primaryColor = color
        litSegmentPaint.color = color
        textClockPaint.color = color
        val r = (Color.red(color) * 0.12f).toInt()
        val g = (Color.green(color) * 0.12f).toInt()
        val b = (Color.blue(color) * 0.12f).toInt()
        unlitColor = Color.rgb(r, g, b)
        unlitSegmentPaint.color = unlitColor
        invalidate()
    }

    fun setStyle(style: ClockStyle) {
        currentStyle = style
        invalidate()
    }

    fun cycleNextStyle() {
        val nextOrdinal = (currentStyle.ordinal + 1) % ClockStyle.entries.size
        currentStyle = ClockStyle.entries[nextOrdinal]
        onClockStyleChanged?.invoke(currentStyle)

        // Pop bounce animation on tap
        val pop = ValueAnimator.ofFloat(0.85f, 1.05f, 1.0f).apply {
            duration = 200
            addUpdateListener {
                scaleX = it.animatedValue as Float
                scaleY = it.animatedValue as Float
            }
        }
        pop.start()
        invalidate()
    }

    private fun startTimeLoop() {
        val runnable = object : Runnable {
            override fun run() {
                val now = Calendar.getInstance().time
                currentTimeString = timeFormat.format(now)
                currentDateString = dateFormat.format(now).uppercase()
                colonVisible = !colonVisible
                invalidate()
                handler.postDelayed(this, 500)
            }
        }
        handler.post(runnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        when (currentStyle) {
            ClockStyle.CYBER_SEVEN_SEGMENT -> drawSevenSegmentClock(canvas, w, h)
            ClockStyle.FUTURISTIC_OLED -> drawFuturisticClock(canvas, w, h)
            ClockStyle.RETRO_DOT_MATRIX -> drawDotMatrixClock(canvas, w, h)
        }
    }

    // ==========================================
    // STYLE 1: 7-SEGMENT CYBERPUNK CLOCK
    // ==========================================
    private fun drawSevenSegmentClock(canvas: Canvas, w: Float, h: Float) {
        // Format: HH : mm : ss (or HH : mm with seconds indicator)
        val digitWidth = w * 0.12f
        val digitHeight = h * 0.65f
        val spacing = w * 0.02f
        val colonWidth = w * 0.04f

        val totalWidth = (digitWidth * 4) + (spacing * 2) + colonWidth
        var startX = (w - totalWidth) * 0.5f
        val startY = h * 0.15f

        val hours = currentTimeString.substring(0, 2)
        val minutes = currentTimeString.substring(2, 4)

        // Digit 1
        drawDigit7Segment(canvas, hours[0] - '0', startX, startY, digitWidth, digitHeight)
        startX += digitWidth + spacing

        // Digit 2
        drawDigit7Segment(canvas, hours[1] - '0', startX, startY, digitWidth, digitHeight)
        startX += digitWidth + spacing

        // Colon
        drawColon(canvas, startX, startY, colonWidth, digitHeight, colonVisible)
        startX += colonWidth + spacing

        // Digit 3
        drawDigit7Segment(canvas, minutes[0] - '0', startX, startY, digitWidth, digitHeight)
        startX += digitWidth + spacing

        // Digit 4
        drawDigit7Segment(canvas, minutes[1] - '0', startX, startY, digitWidth, digitHeight)

        // Date text below
        datePaint.textSize = h * 0.16f
        canvas.drawText(currentDateString, w * 0.5f, h * 0.95f, datePaint)
    }

    private fun drawColon(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, isLit: Boolean) {
        val paint = if (isLit) litSegmentPaint else unlitSegmentPaint
        val dotRadius = width * 0.35f
        val centerX = x + width * 0.5f
        canvas.drawCircle(centerX, y + height * 0.35f, dotRadius, paint)
        canvas.drawCircle(centerX, y + height * 0.65f, dotRadius, paint)
    }

    private fun drawDigit7Segment(
        canvas: Canvas,
        digit: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        val segThickness = width * 0.18f
        val halfH = height * 0.5f

        // 7 Segments: a(top), b(top-right), c(bottom-right), d(bottom), e(bottom-left), f(top-left), g(middle)
        val patterns = arrayOf(
            booleanArrayOf(true, true, true, true, true, true, false),     // 0
            booleanArrayOf(false, true, true, false, false, false, false), // 1
            booleanArrayOf(true, true, false, true, true, false, true),    // 2
            booleanArrayOf(true, true, true, true, false, false, true),    // 3
            booleanArrayOf(false, true, true, false, false, true, true),   // 4
            booleanArrayOf(true, false, true, true, false, true, true),    // 5
            booleanArrayOf(true, false, true, true, true, true, true),     // 6
            booleanArrayOf(true, true, true, false, false, false, false),  // 7
            booleanArrayOf(true, true, true, true, true, true, true),      // 8
            booleanArrayOf(true, true, true, true, false, true, true)      // 9
        )

        val pattern = if (digit in 0..9) patterns[digit] else patterns[0]

        // a (top horizontal)
        drawHorizSegment(canvas, x + segThickness, y, width - segThickness * 2, segThickness, pattern[0])
        // b (top-right vertical)
        drawVertSegment(canvas, x + width - segThickness, y + segThickness, segThickness, halfH - segThickness, pattern[1])
        // c (bottom-right vertical)
        drawVertSegment(canvas, x + width - segThickness, y + halfH, segThickness, halfH - segThickness, pattern[2])
        // d (bottom horizontal)
        drawHorizSegment(canvas, x + segThickness, y + height - segThickness, width - segThickness * 2, segThickness, pattern[3])
        // e (bottom-left vertical)
        drawVertSegment(canvas, x, y + halfH, segThickness, halfH - segThickness, pattern[4])
        // f (top-left vertical)
        drawVertSegment(canvas, x, y + segThickness, segThickness, halfH - segThickness, pattern[5])
        // g (middle horizontal)
        drawHorizSegment(canvas, x + segThickness, y + halfH - segThickness * 0.5f, width - segThickness * 2, segThickness, pattern[6])
    }

    private fun drawHorizSegment(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, isLit: Boolean) {
        val paint = if (isLit) litSegmentPaint else unlitSegmentPaint
        val rect = RectF(x + 2f, y, x + w - 2f, y + h)
        canvas.drawRoundRect(rect, h * 0.35f, h * 0.35f, paint)
    }

    private fun drawVertSegment(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, isLit: Boolean) {
        val paint = if (isLit) litSegmentPaint else unlitSegmentPaint
        val rect = RectF(x, y + 2f, x + w, y + h - 2f)
        canvas.drawRoundRect(rect, w * 0.35f, w * 0.35f, paint)
    }

    // ==========================================
    // STYLE 2: FUTURISTIC MONOSPACE OLED
    // ==========================================
    private fun drawFuturisticClock(canvas: Canvas, w: Float, h: Float) {
        val hh = currentTimeString.substring(0, 2)
        val mm = currentTimeString.substring(2, 4)
        val ss = currentTimeString.substring(4, 6)

        textClockPaint.textSize = h * 0.55f
        textClockPaint.color = primaryColor

        val colon = if (colonVisible) ":" else " "
        val displayTime = "$hh$colon$mm.$ss"

        canvas.drawText(displayTime, w * 0.5f, h * 0.62f, textClockPaint)

        datePaint.textSize = h * 0.16f
        canvas.drawText(currentDateString, w * 0.5f, h * 0.90f, datePaint)
    }

    // ==========================================
    // STYLE 3: RETRO DOT-MATRIX CLOCK
    // ==========================================
    private fun drawDotMatrixClock(canvas: Canvas, w: Float, h: Float) {
        val hh = currentTimeString.substring(0, 2)
        val mm = currentTimeString.substring(2, 4)
        val colon = if (colonVisible) ":" else " "
        val displayTime = "$hh$colon$mm"

        textClockPaint.textSize = h * 0.60f
        textClockPaint.color = primaryColor
        textClockPaint.letterSpacing = 0.12f

        canvas.drawText(displayTime, w * 0.5f, h * 0.65f, textClockPaint)

        datePaint.textSize = h * 0.16f
        canvas.drawText(currentDateString, w * 0.5f, h * 0.90f, datePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }
}
