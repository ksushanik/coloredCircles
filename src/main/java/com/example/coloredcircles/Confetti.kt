package com.example.coloredcircles

import android.graphics.Paint
import android.graphics.Path
import kotlin.random.Random

class Confetti(
    var x: Float,
    var y: Float,
    private val color: Int,
    private val size: Float = Random.nextFloat() * 20f + 10f,
    private var rotation: Float = Random.nextFloat() * 360f
) {
    var velocityX: Float = (Random.nextFloat() - 0.5f) * 40f
    var velocityY: Float = Random.nextFloat() * -25f - 15f
    private val rotationSpeed: Float = (Random.nextFloat() - 0.5f) * 30f
    var alpha: Int = 255
    private val shape: Int = Random.nextInt(3) // 0 - квадрат, 1 - круг, 2 - звезда

    private val starPath = Path().apply {
        val innerRadius = size / 4
        val outerRadius = size / 2
        var angle = -Math.PI / 2
        val angleIncrement = Math.PI * 4 / 10
        moveTo(x + outerRadius * Math.cos(angle).toFloat(), y + outerRadius * Math.sin(angle).toFloat())
        for (i in 0..4) {
            angle += angleIncrement
            lineTo(x + innerRadius * Math.cos(angle).toFloat(), y + innerRadius * Math.sin(angle).toFloat())
            angle += angleIncrement
            lineTo(x + outerRadius * Math.cos(angle).toFloat(), y + outerRadius * Math.sin(angle).toFloat())
        }
        close()
    }

    val paint = Paint().apply {
        this.color = this@Confetti.color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun update() {
        x += velocityX
        y += velocityY
        velocityY += 0.5f // гравитация
        velocityX *= 0.99f // сопротивление воздуха
        rotation += rotationSpeed
        // Замедляем исчезновение
        alpha = (alpha * 0.995f).toInt().coerceAtLeast(0)
    }

    fun draw(canvas: android.graphics.Canvas) {
        paint.alpha = alpha
        canvas.save()
        canvas.rotate(rotation, x, y)
        
        when (shape) {
            0 -> canvas.drawRect(x - size/2, y - size/2, x + size/2, y + size/2, paint)
            1 -> canvas.drawCircle(x, y, size/2, paint)
            2 -> {
                canvas.translate(x, y)
                canvas.drawPath(starPath, paint)
                canvas.translate(-x, -y)
            }
        }
        
        canvas.restore()
    }
} 