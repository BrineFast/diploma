package com.diploma.slepov.custom.processor

import android.graphics.RectF
import androidx.annotation.MainThread
import com.google.android.gms.tasks.Task
import com.diploma.slepov.custom.view.camera.RecitleAnimator
import com.diploma.slepov.custom.view.camera.Overlay
import com.diploma.slepov.custom.R
import com.diploma.slepov.custom.processor.WorkflowModel.WorkflowState
import com.google.mlkit.common.model.LocalModel
import com.diploma.slepov.custom.InputInfo
import com.diploma.slepov.custom.view.objectdetection.*
import com.diploma.slepov.custom.view.objectdetection.ConfirmationController
import com.diploma.slepov.custom.view.objectdetection.DetectionAreaGraphic
import com.diploma.slepov.custom.view.objectdetection.ObjectReticleGraphic
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.IOException
import java.util.ArrayList

/** Класс для запуска детектирования объектов **/
class ProminentObjectProcessor(
    Overlay: Overlay,
    private val workflow: WorkflowModel,
    private val customModelPath: String? = null
) :
    ImageProcessorBase<List<DetectedObject>>() {

    private val detector: ObjectDetector
    private val confirmationController: ConfirmationController = ConfirmationController(Overlay)
    private val RecitleAnimator: RecitleAnimator = RecitleAnimator(Overlay)
    private val reticleOuterRingRadius: Int = Overlay
        .resources
        .getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius)

    /** Конструктор с выбором модели для детектирования **/
    init {
        val options: ObjectDetectorOptionsBase

        if (customModelPath != null) {
            val localModel = LocalModel.Builder()
                .setAssetFilePath(customModelPath)
                .build()
            options = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build()

        } else {
            options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build()
        }

        this.detector = ObjectDetection.getClient(options)
    }

    /** Метод остановки детектирования **/
    override fun stop() {
        super.stop()
        try {
            detector.close()
        } catch (e: IOException) {
            return
        }
    }


    /** Метод детектирования объекта **/
    override fun detectInImage(lastImage: InputImage): Task<List<DetectedObject>> {
        return detector.process(lastImage)
    }

    /** Метод с логикой, выполняемой при успешном детектировании объекта:
     * 1) Отрисовка анимации и области детектирования
     * 2) Отправка и поиск объекта на сервере **/
    @MainThread
    override fun onSuccess(inputInfo: InputInfo, results: List<DetectedObject>, Overlay: Overlay) {
        var objects = results
        if (!workflow.isCameraLive) {
            return
        }

        val qualifiedObjects = ArrayList<DetectedObject>()
        qualifiedObjects.addAll(objects)
        objects = qualifiedObjects

        val objectIndex = 0
        val hasValidObjects = objects.isNotEmpty() &&
                (customModelPath == null || DetectedInfo.hasValidLabels(objects[objectIndex]))
        if (!hasValidObjects) {
            confirmationController.reset()
            workflow.changeState(WorkflowState.DETECTING)
        } else {
            val visionObject = objects[objectIndex]
            if (objectAreaOverlapping(Overlay, visionObject)) {
                confirmationController.confirming(visionObject.trackingId)
                workflow.confirmingObject(
                    DetectedInfo(visionObject, objectIndex, inputInfo), confirmationController.detectionProgress
                )
            } else {
                confirmationController.reset()
                workflow.changeState(WorkflowState.DETECTED)
            }
        }

        Overlay.clear()
        if (!hasValidObjects) {
            Overlay.add(ObjectReticleGraphic(Overlay, RecitleAnimator))
            RecitleAnimator.start()
        } else {
            if (objectAreaOverlapping(Overlay, objects[0])) {
                RecitleAnimator.cancel()
                Overlay.add(
                    DetectionAreaGraphic(
                        Overlay, objects[0], confirmationController
                    )
                )
                if (!confirmationController.confirmed) {
                    Overlay.add(ConfirmationGraphic(Overlay, confirmationController))
                }
            } else {
                Overlay.add(
                    DetectionAreaGraphic(
                        Overlay, objects[0], confirmationController
                    )
                )
                Overlay.add(ObjectReticleGraphic(Overlay, RecitleAnimator))
                RecitleAnimator.start()
            }
        }
        Overlay.invalidate()
    }


    /** Отрисовка области детектируемого объекта в пересечении с индикатором детектирования **/
    private fun objectAreaOverlapping(Overlay: Overlay, visionObject: DetectedObject): Boolean {
        val boxRect = Overlay.translateRect(visionObject.boundingBox)
        val reticleOX = Overlay.width / 2f
        val reticleOY = Overlay.height / 2f
        val reticleRect = RectF(
            reticleOX - reticleOuterRingRadius,
            reticleOY - reticleOuterRingRadius,
            reticleOX + reticleOuterRingRadius,
            reticleOY + reticleOuterRingRadius
        )
        return reticleRect.intersect(boxRect)
    }


    /** Метод выхода при неудачном детектировании **/
    override fun onFailure(e: Exception) {
        return
    }
}
