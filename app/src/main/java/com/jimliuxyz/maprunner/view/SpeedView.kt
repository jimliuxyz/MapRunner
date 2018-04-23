package com.jimliuxyz.maprunner.view

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import com.jimliuxyz.maprunner.R

class SpeedView : ImageView {
    private val DEGREE_START = -100f
    private val DEGREE_END = 98f

    private val VALUE_START = 0f
    private val VALUE_END = 60f

    private val needleImage: Drawable
    var degree = 0

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MySpeedView, defStyleAttr, 0)

        val d = a.getDrawable(R.styleable.AppCompatImageView_srcCompat)
        if (d != null) {
            setImageDrawable(d)
        }

        needleImage = a.getDrawable(R.styleable.MySpeedView_needle_image)

//        Timer().schedule(object: TimerTask(){
//            override fun run() {
//                postInvalidate()
//                degree+=1
//            }
//
//        }, 1000, 100)
    }

    private var goalValue = 0.0f
    private var currValue = goalValue
    private var valueAnimator: ValueAnimator? = null

    fun setValue(value: Float) {
        if (value > VALUE_END) goalValue = VALUE_END
        else if (value < VALUE_START) goalValue = VALUE_START
        else goalValue = value

        val degree_start = valueToDegress(currValue)
        val degree_end = valueToDegress(goalValue)

        valueAnimator?.removeAllUpdateListeners()
        valueAnimator = ValueAnimator.ofFloat(degree_start.toFloat(), degree_end.toFloat()).apply {
            setInterpolator(OvershootInterpolator())
//        setInterpolator(DecelerateInterpolator())
//        setInterpolator(BounceInterpolator())
            setDuration(800)
            addUpdateListener(ami)
            start()
        }
    }

    private fun degressToValue(degree: Float): Float {
        return VALUE_START + (Math.abs(DEGREE_START - degree) * (Math.abs(VALUE_START - VALUE_END) / Math.abs(DEGREE_START - DEGREE_END)))
    }

    private fun valueToDegress(value: Float): Float {
        return DEGREE_START + (Math.abs(VALUE_START - value) * (Math.abs(DEGREE_START - DEGREE_END) / Math.abs(VALUE_START - VALUE_END)))
    }

    var ami = object : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            var value = animation?.getAnimatedValue() as Float

//            println("degree : ${value} ${animation?.animatedFraction}")
            degree = value.toInt() ?: 0
            postInvalidate()
        }
    }

    val Float.toPx: Float
        get() = (this * Resources.getSystem().displayMetrics.density)

    val Float.toDp: Float
        get() = (this / Resources.getSystem().displayMetrics.density)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var value = degressToValue(degree.toFloat())
        currValue = value

        //draw text
        var text = "${value.toInt()}"

        var paint = Paint()
        paint.textSize = 40f.toPx
        paint.setTextAlign(Paint.Align.LEFT)
        paint.setAntiAlias(true)
        paint.color = Color.GRAY
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))

        var r = Rect()
        val strtpl = if (value.toInt() < 10) "9" else "99"  //different digital number may have different width
        paint.getTextBounds(strtpl, 0, strtpl.length, r)
        val text_x = canvas.width / 2f - r.width() / 2f - r.left / 2
        val text_y = canvas.height / 2f + canvas.height / 4f
        canvas.drawText(text, text_x, text_y, paint)

        var r2 = Rect()
        val text2 = "km/h"
        paint.textSize = 15f.toPx
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL))
        paint.getTextBounds(text2, 0, text2.length, r)
        val text2_x = canvas.width / 2f - r.width() / 2f - r.left / 2
        val text2_y = text_y + r.height()*1.4f
        canvas.drawText(text2, text2_x, text2_y, paint)

        //draw needle
        val needle_w = needleImage.intrinsicWidth
        val needle_h = needleImage.intrinsicHeight

        val needle_x = canvas.width / 2 - needle_w / 2
        val needle_y = canvas.height / 2 - needle_h + needle_w / 2

        canvas.rotate(degree.toFloat(), (canvas.width / 2).toFloat(), (canvas.height / 2).toFloat())

        needleImage.setBounds(needle_x, needle_y, needle_x + needle_w, needle_y + needle_h)
        needleImage.draw(canvas)
    }

}
