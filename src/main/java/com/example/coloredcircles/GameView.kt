package com.example.coloredcircles

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import kotlin.random.Random
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GameView(context: Context) : View(context) {
    private val circles = mutableListOf<Circle>()
    private val confetti = mutableListOf<Confetti>()
    private val colors = listOf(
        0xFFFF0000.toInt(), // Красный
        0xFF0000FF.toInt(), // Синий
        0xFF00FF00.toInt(), // Зеленый
        0xFFFFFF00.toInt(), // Желтый
        0xFFFF00FF.toInt()  // Пурпурный
    )
    private var targetColor = colors[0]
    private var selectedCircle: Circle? = null
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f
    private val targetRect = RectF(50f, 50f, 250f, 150f)
    private var isGameActive = true
    private var isConfettiActive = false
    private var lastFrameTime = SystemClock.elapsedRealtime()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastTouchTime = 0L

    // Краски и эффекты для лузы
    private val holePaint = Paint().apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            150f, 100f, 120f,
            intArrayOf(Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        alpha = 120  // Уменьшаем непрозрачность тени
    }

    private val holeGlowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.WHITE
        alpha = 100
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    private val rectPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, 2000f,
            intArrayOf(Color.parseColor("#E0F7FA"), Color.parseColor("#B2EBF2")),
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (circles.isEmpty()) {
            createInitialCircles(w, h)
        }
        
        backgroundPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.parseColor("#E0F7FA"), Color.parseColor("#B2EBF2")),
            null,
            Shader.TileMode.CLAMP
        )

        // Обновляем градиент для лузы с учетом её позиции
        holePaint.shader = RadialGradient(
            targetRect.centerX(), targetRect.centerY(), targetRect.width(),
            intArrayOf(Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0.3f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun createInitialCircles(viewWidth: Int, viewHeight: Int) {
        val random = Random.Default
        val padding = 100f
        val circleRadius = 40f
        circles.clear()
        confetti.clear()
        isConfettiActive = false

        colors.forEach { color ->
            val x = random.nextFloat() * (viewWidth - 2 * padding) + padding
            val y = random.nextFloat() * (viewHeight - 2 * padding) + padding
            
            circles.add(
                Circle(
                    x = x,
                    y = y,
                    radius = circleRadius,
                    color = color
                )
            )
        }
        targetColor = colors[0]
        isGameActive = true
    }

    private fun createConfetti() {
        isConfettiActive = true
        val confettiColors = listOf(
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.MAGENTA, Color.CYAN, Color.WHITE,
            Color.parseColor("#FFD700"), // Золотой
            Color.parseColor("#FF69B4"), // Розовый
            Color.parseColor("#00FF7F"), // Весенне-зеленый
            Color.parseColor("#FF4500")  // Оранжево-красный
        )

        // Создаем конфетти из нескольких точек
        val spawnPoints = listOf(
            Pair(width * 0.25f, height * 0.5f),
            Pair(width * 0.5f, height * 0.5f),
            Pair(width * 0.75f, height * 0.5f)
        )

        spawnPoints.forEach { (spawnX, spawnY) ->
            repeat(150) { // Увеличиваем количество частиц
                confetti.add(
                    Confetti(
                        x = spawnX,
                        y = spawnY,
                        color = confettiColors.random()
                    )
                )
            }
        }
        invalidate()
    }

    private fun updateConfetti() {
        if (isConfettiActive) {
            confetti.forEach { it.update() }
            confetti.removeAll { it.alpha <= 0 }
            if (confetti.isEmpty()) {
                isConfettiActive = false
            }
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentTime = SystemClock.elapsedRealtime()
        val deltaTime = (currentTime - lastFrameTime) / 1000f
        lastFrameTime = currentTime
        
        // Рисуем градиентный фон
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Рисуем лузу
        canvas.save()
        // Сначала рисуем цветную часть
        rectPaint.color = targetColor
        canvas.drawRoundRect(targetRect, 10f, 10f, rectPaint)
        
        // Добавляем простую тень внутрь
        holePaint.shader = RadialGradient(
            targetRect.centerX(), targetRect.centerY(), 
            targetRect.width() / 2,
            intArrayOf(Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0.2f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(targetRect, 10f, 10f, holePaint)
        
        // Добавляем светящуюся обводку
        canvas.drawRoundRect(targetRect, 10f, 10f, holeGlowPaint)
        canvas.restore()

        // Обновляем физику шаров
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val circlesForRemoval = mutableListOf<Circle>()
        
        circles.forEach { circle ->
            circle.update(deltaTime, bounds)
            // Проверяем попадание в лузу для каждого шара
            if (circle.color == targetColor && isCircleInTarget(circle)) {
                // Замедляем шар при попадании в лузу
                circle.velocityX *= 0.9f
                circle.velocityY *= 0.9f
                // Уменьшаем размер шара только когда он уже в лузе
                val dx = circle.x - targetRect.centerX()
                val dy = circle.y - targetRect.centerY()
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < targetRect.width() / 4) { // Уменьшаем зону уменьшения размера
                    circle.radius = circle.radius * 0.95f // Плавное уменьшение
                    if (circle.radius < 2f) {
                        circlesForRemoval.add(circle)
                    }
                }
            }
        }

        // Удаляем шары, которые провалились в лузу
        circlesForRemoval.forEach { circle ->
            circles.remove(circle)
            // Выбираем новый целевой цвет и сразу перерисовываем
            if (circles.isNotEmpty()) {
                targetColor = circles.random().color
                invalidate() // Запрашиваем перерисовку сразу после изменения цвета
            } else {
                isGameActive = false
                showGameOverDialog()
            }
        }

        // Проверяем столкновения между шарами
        for (i in circles.indices) {
            for (j in i + 1 until circles.size) {
                if (circles[i].intersects(circles[j])) {
                    circles[i].resolveCollision(circles[j])
                }
            }
        }

        // Обновляем и рисуем кружки
        circles.forEach { circle ->
            circle.updateTouchAnimation(deltaTime)
            circle.draw(canvas)
        }

        // Рисуем конфетти
        confetti.forEach { it.draw(canvas) }
        updateConfetti()

        // Запрашиваем следующий кадр, если есть анимации или движение
        if (circles.any { it.touchAnimationTime > 0 || it.velocityX != 0f || it.velocityY != 0f } || isConfettiActive) {
            invalidate()
        }
    }

    private fun animateCircleRemoval(circle: Circle) {
        val scaleAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 300
            interpolator = AccelerateInterpolator()
            addUpdateListener { animator ->
                circle.radius = circle.radius * (animator.animatedValue as Float)
                invalidate()
            }
        }
        scaleAnimator.start()
    }

    private fun showGameOverDialog() {
        createConfetti() // Запускаем конфетти перед показом диалога
        AlertDialog.Builder(context)
            .setTitle("Поздравляем!")
            .setMessage("Вы успешно завершили игру! Хотите сыграть еще раз?")
            .setPositiveButton("Да") { _, _ ->
                createInitialCircles(width, height)
                invalidate()
            }
            .setNegativeButton("Нет", null)
            .setCancelable(false)
            .show()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isGameActive) return false
        
        val currentTime = System.currentTimeMillis()
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedCircle = circles.find { it.contains(event.x, event.y) }
                selectedCircle?.let { circle ->
                    circle.startTouchAnimation()
                    touchOffsetX = event.x - circle.x
                    touchOffsetY = event.y - circle.y
                    // Плавно останавливаем шар при захвате
                    circle.velocityX *= 0.5f
                    circle.velocityY *= 0.5f
                    lastTouchX = event.x
                    lastTouchY = event.y
                    lastTouchTime = currentTime
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                selectedCircle?.let { circle ->
                    val deltaTime = (currentTime - lastTouchTime) / 1000f
                    if (deltaTime > 0) {
                        // Новая позиция шарика
                        val newX = event.x - touchOffsetX
                        val newY = event.y - touchOffsetY
                        
                        // Плавно обновляем скорость
                        val dx = newX - circle.x
                        val dy = newY - circle.y
                        
                        // Используем экспоненциальное сглаживание для скорости
                        val smoothingFactor = 0.3f
                        circle.velocityX = circle.velocityX * (1 - smoothingFactor) + (dx / deltaTime) * smoothingFactor
                        circle.velocityY = circle.velocityY * (1 - smoothingFactor) + (dy / deltaTime) * smoothingFactor
                        
                        // Обновляем позицию
                        circle.x = newX
                        circle.y = newY
                    }
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                    lastTouchTime = currentTime
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                selectedCircle?.let { circle ->
                    val deltaTime = (currentTime - lastTouchTime) / 1000f
                    if (deltaTime > 0 && deltaTime < 0.1f) {
                        // Вычисляем финальную скорость на основе последнего движения
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        
                        // Используем экспоненциальное сглаживание для конечной скорости
                        val smoothingFactor = 0.5f
                        circle.velocityX = circle.velocityX * (1 - smoothingFactor) + (dx / deltaTime) * smoothingFactor
                        circle.velocityY = circle.velocityY * (1 - smoothingFactor) + (dy / deltaTime) * smoothingFactor
                        
                        // Ограничиваем максимальную скорость при отпускании
                        val maxReleaseVelocity = 1000f
                        val currentVelocity = sqrt(circle.velocityX * circle.velocityX + circle.velocityY * circle.velocityY)
                        if (currentVelocity > maxReleaseVelocity) {
                            val scale = maxReleaseVelocity / currentVelocity
                            circle.velocityX *= scale
                            circle.velocityY *= scale
                        }
                    }
                    
                    if (circle.color == targetColor) {
                        val dx = circle.x - targetRect.centerX()
                        val dy = circle.y - targetRect.centerY()
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance < targetRect.width()) {
                            val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                            // Добавляем более мягкое притяжение к лузе
                            val attractionForce = 150f * (1f - distance / targetRect.width())
                            circle.velocityX += (-cos(angle) * attractionForce).toFloat()
                            circle.velocityY += (-sin(angle) * attractionForce).toFloat()
                        }
                    }
                }
                selectedCircle = null
                invalidate()
            }
        }
        return true
    }

    private fun isCircleInTarget(circle: Circle): Boolean {
        val centerX = targetRect.centerX()
        val centerY = targetRect.centerY()
        val dx = circle.x - centerX
        val dy = circle.y - centerY
        
        // Уменьшаем зону притяжения
        val attractionRadius = targetRect.width() / 2
        val distance = sqrt(dx * dx + dy * dy)
        
        // Если шар в зоне притяжения, начинаем его затягивать
        if (distance < attractionRadius) {
            // Упрощенная сила притяжения
            val force = 0.15f * (1.0f - distance / attractionRadius)
            val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
            
            // Применяем силу притяжения к центру
            circle.velocityX += (-cos(angle) * force).toFloat() * 300
            circle.velocityY += (-sin(angle) * force).toFloat() * 300
            
            // Если шар достаточно близко к центру, считаем что он попал в лузу
            if (distance < circle.radius) {
                // Запрашиваем перерисовку при попадании шара в лузу
                invalidate()
                return true
            }
        }
        
        return false
    }
} 