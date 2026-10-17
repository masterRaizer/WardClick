package com.wardclick.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Маркер-«пилюля» с ценой для карты: белый с рамкой — обычный, цветной — выбранный.
 * Рисуется на Canvas, поэтому не нужны drawable-ресурсы.
 */
fun createPriceMarker(context: Context, text: String, selected: Boolean): Bitmap {
    val density = context.resources.displayMetrics.density
    fun dp(v: Float) = v * density

    val brand = Color.parseColor("#0B6E99")
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) Color.WHITE else brand
        textSize = dp(if (selected) 15f else 13f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val padH = dp(10f)
    val padV = dp(6f)
    val tail = dp(7f)
    val stroke = dp(1.5f)
    val textWidth = textPaint.measureText(text)
    val fm = textPaint.fontMetrics
    val textHeight = fm.descent - fm.ascent

    val bodyW = textWidth + padH * 2
    val bodyH = textHeight + padV * 2
    val width = Math.ceil((bodyW + stroke * 2).toDouble()).toInt()
    val height = Math.ceil((bodyH + tail + stroke * 2).toDouble()).toInt()

    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val body = RectF(stroke, stroke, stroke + bodyW, stroke + bodyH)
    val radius = bodyH / 2f

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if (selected) brand else Color.WHITE
    }
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = brand
    }

    val cx = body.centerX()
    val tailPath = Path().apply {
        moveTo(cx - tail, body.bottom - 1f)
        lineTo(cx, body.bottom + tail)
        lineTo(cx + tail, body.bottom - 1f)
        close()
    }

    canvas.drawRoundRect(body, radius, radius, fill)
    canvas.drawPath(tailPath, fill)
    canvas.drawRoundRect(body, radius, radius, border)
    // линии хвоста поверх заливки (без шва между «пилюлей» и хвостом)
    val tailBorder = Path().apply {
        moveTo(cx - tail + 1f, body.bottom - 1f)
        lineTo(cx, body.bottom + tail)
        lineTo(cx + tail - 1f, body.bottom - 1f)
    }
    canvas.drawPath(tailBorder, border.apply { style = Paint.Style.STROKE })

    val baseline = body.centerY() - (fm.ascent + fm.descent) / 2f
    canvas.drawText(text, cx, baseline, textPaint)
    return bmp
}
