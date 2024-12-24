package com.example.coloredcircles

import android.graphics.*
import kotlin.math.*
import kotlin.random.Random
import kotlin.math.PI

class Circle(
    var x: Float,
    var y: Float,
    var radius: Float,
    val color: Int
) {
    var velocityX = 0f
    var velocityY = 0f
    private var touchScale = 1f
    var touchAnimationTime = 0f
    private val friction = 0.98f  // Коэффициент трения (меньше - больше трения)
    
    // Основная краска для шарика
    val paint: Paint = Paint().apply {
        style = Paint.Style.FILL
        this.color = this@Circle.color
        isAntiAlias = true
    }
    
    // Краска для блика (создает эффект объема сверху)
    private val highlightPaint = Paint().apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            -radius * 0.3f, -radius * 0.3f, radius,
            Color.WHITE, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        alpha = 160
        isAntiAlias = true
    }
    
    // Краска для тени (создает эффект объема снизу)
    private val shadowPaint = Paint().apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            radius * 0.2f, radius * 0.2f, radius,
            Color.BLACK, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        alpha = 80
        isAntiAlias = true
    }

    fun update(deltaTime: Float, bounds: RectF) {
        // Применяем трение
        velocityX *= friction
        velocityY *= friction

        // Обновляем позицию с учетом времени
        x += velocityX * deltaTime
        y += velocityY * deltaTime

        // Останавливаем движение, если скорость очень мала
        if (abs(velocityX) < 1f) velocityX = 0f
        if (abs(velocityY) < 1f) velocityY = 0f

        // Обработка столкновений со стенками
        if (x - radius < bounds.left) {
            x = bounds.left + radius
            velocityX = abs(velocityX) * 0.8f  // Теряем часть энергии при отскоке
        } else if (x + radius > bounds.right) {
            x = bounds.right - radius
            velocityX = -abs(velocityX) * 0.8f
        }

        if (y - radius < bounds.top) {
            y = bounds.top + radius
            velocityY = abs(velocityY) * 0.8f
        } else if (y + radius > bounds.bottom) {
            y = bounds.bottom - radius
            velocityY = -abs(velocityY) * 0.8f
        }
    }

    fun contains(touchX: Float, touchY: Float): Boolean {
        val distance = sqrt(
            (touchX - x).pow(2) +
            (touchY - y).pow(2)
        )
        return distance <= radius
    }

    fun intersects(other: Circle): Boolean {
        val dx = x - other.x
        val dy = y - other.y
        val distance = sqrt(dx * dx + dy * dy)
        return distance < (radius + other.radius)
    }

    fun resolveCollision(other: Circle) {
        val dx = other.x - x
        val dy = other.y - y
        val distance = sqrt(dx * dx + dy * dy)
        
        if (distance == 0f) return  // Избегаем деления на ноль
        
        // Нормализованный вектор направления столкновения
        val nx = dx / distance
        val ny = dy / distance
        
        // Относительная скорость
        val dvx = other.velocityX - velocityX
        val dvy = other.velocityY - velocityY
        
        // Скорость в направлении столкновения
        val velocityAlongNormal = dvx * nx + dvy * ny
        
        // Если шары удаляются друг от друга, игнорируем столкновение
        if (velocityAlongNormal > 0) return
        
        // Коэффициент упругости (1 = абсолютно упругий удар)
        val restitution = 0.85f
        
        // Считаем массу шаров (пропорционально радиусу)
        val mass1 = radius * radius
        val mass2 = other.radius * other.radius
        val totalMass = mass1 + mass2
        
        // Вычисляем импульс с учетом масс
        val j = -(1 + restitution) * velocityAlongNormal
        val impulse1 = j * (mass2 / totalMass)
        val impulse2 = j * (mass1 / totalMass)
        
        // Применяем импульсы к обоим шарам с учетом их масс
        velocityX -= nx * impulse1
        velocityY -= ny * impulse1
        other.velocityX += nx * impulse2
        other.velocityY += ny * impulse2
        
        // Разрешаем перекрытие (отталкиваем шары друг от друга)
        val overlap = (radius + other.radius - distance) * 0.5f
        val moveX = overlap * nx
        val moveY = overlap * ny
        
        // Перемещаем шары пропорционально их массам
        val move1 = mass2 / totalMass
        val move2 = mass1 / totalMass
        
        x -= moveX * move1
        y -= moveY * move1
        other.x += moveX * move2
        other.y += moveY * move2
        
        // Добавляем небольшое случайное отклонение для предотвращения "слипания"
        val randomAngle = Random.nextFloat() * PI.toFloat() * 0.1f
        val randomForce = Random.nextFloat() * 0.5f
        velocityX += cos(randomAngle) * randomForce
        velocityY += sin(randomAngle) * randomForce
        other.velocityX -= cos(randomAngle) * randomForce
        other.velocityY -= sin(randomAngle) * randomForce
    }

    fun startTouchAnimation() {
        touchAnimationTime = 0.3f
        touchScale = 1.2f
    }

    fun updateTouchAnimation(deltaTime: Float) {
        if (touchAnimationTime > 0) {
            touchAnimationTime = max(0f, touchAnimationTime - deltaTime)
            touchScale = 1f + 0.2f * (touchAnimationTime / 0.3f)
        }
    }

    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(touchScale, touchScale)
        
        // Рисуем тень под шариком
        canvas.drawCircle(radius * 0.1f, radius * 0.1f, radius, shadowPaint)
        
        // Рисуем основной цвет шарика
        canvas.drawCircle(0f, 0f, radius, paint)
        
        // Рисуем блик для создания эффекта объема
        canvas.drawCircle(-radius * 0.2f, -radius * 0.2f, radius, highlightPaint)
        
        canvas.restore()
    }
} 