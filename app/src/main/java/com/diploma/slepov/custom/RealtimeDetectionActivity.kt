package com.diploma.slepov.custom

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.common.base.Objects
import com.google.common.collect.ImmutableList
import com.diploma.slepov.custom.view.camera.Overlay
import com.diploma.slepov.custom.processor.WorkflowModel
import com.diploma.slepov.custom.processor.WorkflowModel.WorkflowState
import com.diploma.slepov.custom.view.camera.Source
import com.diploma.slepov.custom.view.camera.CameraPreview
import com.diploma.slepov.custom.processor.ProminentObjectProcessor
import com.diploma.slepov.custom.view.productsearch.ScrimView
import com.diploma.slepov.custom.view.productsearch.ProductRepresentation
import com.diploma.slepov.custom.processor.SearchEngine
import java.io.IOException

/** Класс для модуля поиска объектов при детектировании в реальном времени **/
class RealtimeDetectionActivity : AppCompatActivity(), OnClickListener {

    private var cameraSource: Source? = null
    private var preview: CameraPreview? = null
    private var Overlay: Overlay? = null
    private var flashButton: View? = null
    private var promptChip: Chip? = null
    private var promptChipAnimator: AnimatorSet? = null
    private var searchProgressBar: ProgressBar? = null
    private var workflowModel: WorkflowModel? = null
    private var currentWorkflowState: WorkflowState? = null
    private var searchEngine: SearchEngine? = null

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var bottomSheetScrimView: ScrimView? = null
    private var productRecyclerView: RecyclerView? = null
    private var bottomSheetTitleView: TextView? = null
    private var objectThumbnailForBottomSheet: Bitmap? = null
    private var slidingSheetUpFromHiddenState: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        searchEngine = SearchEngine(applicationContext)

        setContentView(R.layout.realtime_detection_view)
        preview = findViewById(R.id.camera_preview)
        Overlay = findViewById<Overlay>(R.id.camera_preview_graphic_overlay).apply {
            setOnClickListener(this@RealtimeDetectionActivity)
            cameraSource = Source(this)
        }
        promptChip = findViewById(R.id.bottom_prompt_chip)
        promptChipAnimator =
            (AnimatorInflater.loadAnimator(this, R.animator.detection_hint) as AnimatorSet).apply {
                setTarget(promptChip)
            }
        searchProgressBar = findViewById(R.id.search_progress_bar)
        setUpBottomSheet()
        findViewById<View>(R.id.close_button).setOnClickListener(this)
        flashButton = findViewById<View>(R.id.flash_button).apply {
            setOnClickListener(this@RealtimeDetectionActivity)
        }
        setUpWorkflowModel()
    }

    override fun onResume() {
        super.onResume()

        workflowModel?.markCameraFrozen()
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        currentWorkflowState = WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(
        ProminentObjectProcessor(Overlay!!, workflowModel!!)
        )
        workflowModel?.changeState(WorkflowState.DETECTING)
    }

    override fun onPause() {
        super.onPause()
        currentWorkflowState = WorkflowState.NOT_STARTED
        stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
        cameraSource = null
        searchEngine?.shutdown()
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior?.setState(BottomSheetBehavior.STATE_HIDDEN)
        } else {
            super.onBackPressed()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.bottom_sheet_scrim_view -> bottomSheetBehavior?.setState(BottomSheetBehavior.STATE_HIDDEN)
            R.id.close_button -> onBackPressed()
            R.id.flash_button -> {
                if (flashButton?.isSelected == true) {
                    flashButton?.isSelected = false
                    cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF)
                } else {
                    flashButton?.isSelected = true
                    cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                }
            }
        }
    }

    private fun startCameraPreview() {
        val cameraSource = this.cameraSource ?: return
        val workflowModel = this.workflowModel ?: return
        if (!workflowModel.isCameraLive) {
            try {
                workflowModel.markCameraLive()
                preview?.start(cameraSource)
            } catch (e: IOException) {
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        if (workflowModel?.isCameraLive == true) {
            workflowModel!!.markCameraFrozen()
            flashButton?.isSelected = false
            preview?.stop()
        }
    }

    private fun setUpBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior?.setBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    bottomSheetScrimView?.visibility =
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) View.GONE else View.VISIBLE
                    Overlay?.clear()

                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> workflowModel?.changeState(WorkflowState.DETECTING)
                        BottomSheetBehavior.STATE_COLLAPSED,
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> slidingSheetUpFromHiddenState = false
                        BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val searchedObject = workflowModel!!.searchedObject.value
                    if (searchedObject == null || java.lang.Float.isNaN(slideOffset)) {
                        return
                    }

                    val Overlay = Overlay ?: return
                    val bottomSheetBehavior = bottomSheetBehavior ?: return
                    val collapsedStateHeight = bottomSheetBehavior.peekHeight.coerceAtMost(bottomSheet.height)
                    val bottomBitmap = objectThumbnailForBottomSheet ?: return
                    if (slidingSheetUpFromHiddenState) {
                        val thumbnailSrcRect = Overlay.translateRect(searchedObject.boundingBox)
                        bottomSheetScrimView?.updateWithThumbnailTranslateAndScale(
                            bottomBitmap,
                            collapsedStateHeight,
                            slideOffset,
                            thumbnailSrcRect
                        )
                    } else {
                        bottomSheetScrimView?.updateWithThumbnailTranslate(
                            bottomBitmap, collapsedStateHeight, slideOffset, bottomSheet
                        )
                    }
                }
            })

        bottomSheetScrimView = findViewById<ScrimView>(R.id.bottom_sheet_scrim_view).apply {
            setOnClickListener(this@RealtimeDetectionActivity)
        }

        bottomSheetTitleView = findViewById(R.id.bottom_sheet_title)
        productRecyclerView = findViewById<RecyclerView>(R.id.product_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@RealtimeDetectionActivity)
            adapter = ProductRepresentation(ImmutableList.of())
        }
    }

    private fun setUpWorkflowModel() {
        workflowModel = ViewModelProviders.of(this).get(WorkflowModel::class.java).apply {

            currentState.observe(this@RealtimeDetectionActivity, Observer { currentState ->
                if (currentState == null || Objects.equal(currentWorkflowState, currentState)) {
                    return@Observer
                }
                currentWorkflowState = currentState

                stateChangeInAutoSearchMode(currentState)
            })

            detectedObject.observe(this@RealtimeDetectionActivity, Observer { detectObject ->
                searchEngine!!.search(detectObject) { detectedObject, products ->
                    workflowModel?.onSearchCompleted(detectedObject, products)
                }
            })

            searchedObject.observe(this@RealtimeDetectionActivity, Observer { nullableSearchedObject ->
                val searchedObject = nullableSearchedObject ?: return@Observer
                val productList = searchedObject.productList
                objectThumbnailForBottomSheet = searchedObject.getObjectThumbnail()
                productRecyclerView?.adapter = ProductRepresentation(productList)
                slidingSheetUpFromHiddenState = true
                bottomSheetBehavior?.peekHeight =
                    preview?.height?.div(2) ?: BottomSheetBehavior.PEEK_HEIGHT_AUTO
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            })
        }
    }

    private fun stateChangeInAutoSearchMode(currentState: WorkflowState) {
        val wasPromptChipGone = promptChip!!.visibility == View.GONE

        searchProgressBar?.visibility = View.GONE
        when (currentState) {
            WorkflowState.DETECTING, WorkflowState.DETECTED, WorkflowState.CONFIRMING -> {
                promptChip?.visibility = View.VISIBLE
                promptChip?.setText(
                    if (currentState == WorkflowState.CONFIRMING)
                        R.string.prompt_hold_camera_steady
                    else
                        R.string.prompt_point_at_an_object
                )
                startCameraPreview()
            }
            WorkflowState.CONFIRMED -> {
                promptChip?.visibility = View.VISIBLE
                promptChip?.setText(R.string.prompt_searching)
                stopCameraPreview()
            }
            WorkflowState.SEARCHING -> {
                searchProgressBar?.visibility = View.VISIBLE
                promptChip?.visibility = View.VISIBLE
                promptChip?.setText(R.string.prompt_searching)
                stopCameraPreview()
            }
            WorkflowState.SEARCHED -> {
                promptChip?.visibility = View.GONE
                stopCameraPreview()
            }
            else -> promptChip?.visibility = View.GONE
        }

        val shouldPlayPromptChipEnteringAnimation = wasPromptChipGone && promptChip?.visibility == View.VISIBLE
        if (shouldPlayPromptChipEnteringAnimation && promptChipAnimator?.isRunning == false) {
            promptChipAnimator?.start()
        }
    }
}
