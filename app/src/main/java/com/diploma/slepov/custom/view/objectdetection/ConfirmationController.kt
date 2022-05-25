package com.diploma.slepov.custom.view.objectdetection

import android.os.CountDownTimer
import com.diploma.slepov.custom.view.camera.Overlay

/** Класс, отвечающий за подтверждение успешного детектирования объекта для его дальнейшей передачи в модуль поиска **/
internal class ConfirmationController
    (Overlay: Overlay) {

    private val downTimer: CountDownTimer

    private var objectId: Int? = null

    var detectionProgress = 0f
        private set

    val confirmed: Boolean
        get() = detectionProgress.compareTo(1f) == 0

    /** Логика подтверждения успешного детектирования.
     * После обнаружения объекта требуется 3 дополнительных секунды для подтверждения корректности **/
    init {
        val confirmationDelay = 3000L
        downTimer = object : CountDownTimer(confirmationDelay, 20) {
            override fun onTick(millisUntilFinished: Long) {
                detectionProgress = (confirmationDelay - millisUntilFinished).toFloat() / confirmationDelay
                Overlay.invalidate()
            }

            override fun onFinish() {
                detectionProgress = 1f
            }
        }
    }

    /** Изменения статуса объекта **/
    fun confirming(objectId: Int?) {
        if (objectId == this.objectId) {
            return
        }

        reset()
        this.objectId = objectId
        downTimer.start()
    }

    fun reset() {
        downTimer.cancel()
        objectId = null
        detectionProgress = 0f
    }
}
