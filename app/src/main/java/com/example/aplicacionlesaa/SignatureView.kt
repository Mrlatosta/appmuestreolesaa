package com.example.aplicacionlesaa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SignatureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var path = Path()
    private var paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null

    init {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                canvas?.drawPath(path, paint)
                //path.reset()
                invalidate()
            }
            else -> return false
        }
        return true
    }

    fun getSignatureBitmap(): Bitmap {
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(resultBitmap)
        bitmap?.let {
            resultCanvas.drawBitmap(it, 0f, 0f, null)
        }
        draw(resultCanvas)
        return resultBitmap
    }

    fun setSignatureBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        canvas = Canvas(bitmap)
        invalidate()
    }

    fun clear() {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap!!)
        invalidate()
    }
}
