package com.diploma.slepov.custom.view.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.diploma.slepov.custom.view.Utils
import java.util.ArrayList

/** Класс с основной информацией для интерфейса относительно параметров экрана устройства **/
class Overlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val lock = Any()

    private var _width: Int = 0
    private var widthScale = 1.0f
    private var _height: Int = 0
    private var heightScale = 1.0f
    private val graphics = ArrayList<Graphic>()

    /** Отрисовка элементов интерфейса **/
    abstract class Graphic protected constructor(protected val overlay: Overlay) {
        protected val context: Context = overlay.context

        abstract fun draw(canvas: Canvas)
    }

    /** Очистка экрана от всех элементов интерфейса **/
    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    /** Добавление элемента интерфейса **/
    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
    }

    /** Добавление информации об атрибутах камеры для последующих преобразований интерфейса **/
    fun setCameraInfo(cameraSource: Source) {
        val previewSize = cameraSource.previewSize ?: return
        if (Utils.isPortraitMode(context)) {
            _width = previewSize.height
            _height = previewSize.width
        } else {
            _width = previewSize.width
            _height = previewSize.height
        }
    }

    /** Преобразование окна под полученные параметры экрана **/
    fun translateRect(rect: Rect) = RectF(
        rect.left.toFloat() * widthScale,
        rect.top.toFloat() * heightScale,
        rect.right.toFloat() * widthScale,
        rect.bottom.toFloat() * heightScale
    )

    /** Рассчет параметров преобразования элементов и их отрисовка **/
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (_width > 0 && _height > 0) {
            widthScale = _width.toFloat() / _width
            heightScale = height.toFloat() / _height
        }

        synchronized(lock) {
            graphics.forEach { it.draw(canvas) }
        }
    }
}
