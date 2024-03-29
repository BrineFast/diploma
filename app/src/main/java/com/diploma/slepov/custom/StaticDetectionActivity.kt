package com.diploma.slepov.custom

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.common.collect.ImmutableList
import com.diploma.slepov.custom.view.productsearch.ScrimView
import com.diploma.slepov.custom.view.objectdetection.DetectedInfo
import com.diploma.slepov.custom.view.objectdetection.DotView
import com.diploma.slepov.custom.view.productsearch.ProductCardPreview
import com.diploma.slepov.custom.processor.Product
import com.diploma.slepov.custom.view.productsearch.ProductRepresentation
import com.diploma.slepov.custom.processor.SearchEngine
import com.diploma.slepov.custom.view.Utils
import com.diploma.slepov.custom.view.productsearch.SearchedObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import java.io.IOException
import java.lang.NullPointerException
import java.util.TreeMap

/** Класс для модуля поиска объектов при детектировании в статическом режиме **/
class StaticDetectionActivity : AppCompatActivity(), View.OnClickListener {

    private val searchedObjectMap = TreeMap<Int, SearchedObject>()

    private var loadingView: View? = null
    private var bottomPromptChip: Chip? = null
    private var inputImageView: ImageView? = null
    private var previewCardCarousel: RecyclerView? = null
    private var dotViewContainer: ViewGroup? = null

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var bottomSheetScrimView: ScrimView? = null
    private var bottomSheetTitleView: TextView? = null
    private var productRecyclerView: RecyclerView? = null

    private var inputBitmap: Bitmap? = null
    private var searchedObjectForBottomSheet: SearchedObject? = null
    private var dotViewSize: Int = 0
    private var detectedObjectNum = 0
    private var currentSelectedObjectIndex = 0

    private var detector: ObjectDetector? = null
    private var searchEngine: SearchEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        searchEngine = SearchEngine(applicationContext)

        setContentView(R.layout.static_detection_view)

        loadingView = findViewById<View>(R.id.loading_view).apply {
            setOnClickListener(this@StaticDetectionActivity)
        }

        bottomPromptChip = findViewById(R.id.bottom_prompt_chip)
        inputImageView = findViewById(R.id.input_image_view)

        previewCardCarousel = findViewById<RecyclerView>(R.id.card_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@StaticDetectionActivity, RecyclerView.HORIZONTAL, false)
            addItemDecoration(
                CardItemDecoration(
                    resources
                )
            )
        }

        dotViewContainer = findViewById(R.id.dot_view_container)
        dotViewSize = resources.getDimensionPixelOffset(R.dimen.static_image_dot_view_size)

        setUpBottomSheet()

        findViewById<View>(R.id.close_button).setOnClickListener(this)
        findViewById<View>(R.id.photo_library_button).setOnClickListener(this)

        detector = ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .build()
        )
        intent.data?.let(::detectObjects)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            detector?.close()
        } catch (e: IOException) {
        }

        searchEngine?.shutdown()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            data?.data?.let(::detectObjects)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
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
            R.id.close_button -> onBackPressed()
            R.id.photo_library_button -> Utils.openInternalStorage(this)
            R.id.bottom_sheet_scrim_view -> bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun showSearchResults(searchedObject: SearchedObject) {
        searchedObjectForBottomSheet = searchedObject
        val productList = searchedObject.productList
        productRecyclerView?.adapter = ProductRepresentation(productList)
        bottomSheetBehavior?.peekHeight = (inputImageView?.parent as View).height / 2
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setUpBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet)).apply {
            setBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        bottomSheetScrimView?.visibility =
                            if (newState == BottomSheetBehavior.STATE_HIDDEN) View.GONE else View.VISIBLE
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        if (java.lang.Float.isNaN(slideOffset)) {
                            return
                        }

                        val collapsedStateHeight = bottomSheetBehavior!!.peekHeight.coerceAtMost(bottomSheet.height)
                        val searchedObjectForBottomSheet = searchedObjectForBottomSheet
                            ?: return
                        bottomSheetScrimView?.updateWithThumbnailTranslate(
                            searchedObjectForBottomSheet.getObjectThumbnail(),
                            collapsedStateHeight,
                            slideOffset,
                            bottomSheet
                        )
                    }
                }
            )
            state = BottomSheetBehavior.STATE_HIDDEN
        }

        bottomSheetScrimView = findViewById<ScrimView>(R.id.bottom_sheet_scrim_view).apply {
            setOnClickListener(this@StaticDetectionActivity)
        }

        bottomSheetTitleView = findViewById(R.id.bottom_sheet_title)
        productRecyclerView = findViewById<RecyclerView>(R.id.product_recycler_view)?.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@StaticDetectionActivity)
            adapter = ProductRepresentation(ImmutableList.of())
        }
    }

    private fun detectObjects(imageUri: Uri) {
        inputImageView?.setImageDrawable(null)
        bottomPromptChip?.visibility = View.GONE
        previewCardCarousel?.adapter = ProductCardPreview(ImmutableList.of()) { showSearchResults(it) }
        previewCardCarousel?.clearOnScrollListeners()
        dotViewContainer?.removeAllViews()
        currentSelectedObjectIndex = 0

        try {
            inputBitmap = Utils.loadImage(
                this, imageUri,
                1024
            )
        } catch (e: IOException) {
            showBottomPromptChip("Failed to load file!")
            return
        }

        inputImageView?.setImageBitmap(inputBitmap)
        loadingView?.visibility = View.VISIBLE
        val image = InputImage.fromBitmap(inputBitmap!!, 0)
        detector?.process(image)
            ?.addOnSuccessListener { objects -> onObjectsDetected(BitmapInputInfo(inputBitmap!!), objects) }
            ?.addOnFailureListener { onObjectsDetected(BitmapInputInfo(inputBitmap!!), ImmutableList.of()) }
    }

    @MainThread
    private fun onObjectsDetected(image: InputInfo, objects: List<DetectedObject>) {
        detectedObjectNum = objects.size
        if (detectedObjectNum == 0) {
            loadingView?.visibility = View.GONE
            showBottomPromptChip(getString(R.string.static_image_prompt_detected_no_results))
        } else {
            searchedObjectMap.clear()
            for (i in objects.indices) {
                searchEngine?.search(DetectedInfo(objects[i], i, image)) { detectedObject, products ->
                    onSearchCompleted(detectedObject, products)
                }
            }
        }
    }

    private fun onSearchCompleted(detectedObject: DetectedInfo, productList: List<Product>) {
        searchedObjectMap[detectedObject.objectIndex] = SearchedObject(resources, detectedObject, productList)
        if (searchedObjectMap.size < detectedObjectNum) {
            return
        }

        showBottomPromptChip(getString(R.string.static_image_prompt_detected_results))
        loadingView?.visibility = View.GONE
        previewCardCarousel?.adapter =
            ProductCardPreview(ImmutableList.copyOf(searchedObjectMap.values)) { showSearchResults(it) }
        previewCardCarousel?.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        for (i in 0 until recyclerView.childCount) {
                            val childView = recyclerView.getChildAt(i)
                            if (childView.x >= 0) {
                                val cardIndex = recyclerView.getChildAdapterPosition(childView)
                                if (cardIndex != currentSelectedObjectIndex) {
                                    selectNewObject(cardIndex)
                                }
                                break
                            }
                        }
                    }
                }
            })

        for (searchedObject in searchedObjectMap.values) {
            val dotView = createDotView(searchedObject)
            dotView.setOnClickListener {
                if (searchedObject.objectIndex == currentSelectedObjectIndex) {
                    showSearchResults(searchedObject)
                } else {
                    selectNewObject(searchedObject.objectIndex)
                    showSearchResults(searchedObject)
                    previewCardCarousel!!.smoothScrollToPosition(searchedObject.objectIndex)
                }
            }

            dotViewContainer?.addView(dotView)
            val animatorSet = AnimatorInflater.loadAnimator(this, R.animator.static_detection_animation) as AnimatorSet
            animatorSet.setTarget(dotView)
            animatorSet.start()
        }
    }

    private fun createDotView(searchedObject: SearchedObject): DotView {
        val viewCoordinateScale: Float
        val horizontalGap: Float
        val verticalGap: Float
        val inputImageView = inputImageView ?: throw NullPointerException()
        val inputBitmap = inputBitmap ?: throw NullPointerException()
        val inputImageViewRatio = inputImageView.width.toFloat() / inputImageView.height
        val inputBitmapRatio = inputBitmap.width.toFloat() / inputBitmap.height
        if (inputBitmapRatio <= inputImageViewRatio) {
            viewCoordinateScale = inputImageView.height.toFloat() / inputBitmap.height
            horizontalGap = (inputImageView.width - inputBitmap.width * viewCoordinateScale) / 2
            verticalGap = 0f
        } else {
            viewCoordinateScale = inputImageView.width.toFloat() / inputBitmap.width
            horizontalGap = 0f
            verticalGap = (inputImageView.height - inputBitmap.height * viewCoordinateScale) / 2
        }

        val boundingBox = searchedObject.boundingBox
        val boxInViewCoordinate = RectF(
            boundingBox.left * viewCoordinateScale + horizontalGap,
            boundingBox.top * viewCoordinateScale + verticalGap,
            boundingBox.right * viewCoordinateScale + horizontalGap,
            boundingBox.bottom * viewCoordinateScale + verticalGap
        )
        val initialSelected = searchedObject.objectIndex == 0
        val dotView = DotView(this, initialSelected)
        val layoutParams = FrameLayout.LayoutParams(dotViewSize, dotViewSize)
        val dotCenter = PointF(
            (boxInViewCoordinate.right + boxInViewCoordinate.left) / 2,
            (boxInViewCoordinate.bottom + boxInViewCoordinate.top) / 2
        )
        layoutParams.setMargins(
            (dotCenter.x - dotViewSize / 2f).toInt(),
            (dotCenter.y - dotViewSize / 2f).toInt(),
            0,
            0
        )
        dotView.layoutParams = layoutParams
        return dotView
    }

    private fun selectNewObject(objectIndex: Int) {
        val dotViewToDeselect = dotViewContainer!!.getChildAt(currentSelectedObjectIndex) as DotView
        dotViewToDeselect.selectedAnimation(false)

        currentSelectedObjectIndex = objectIndex

        val selectedDotView = dotViewContainer!!.getChildAt(currentSelectedObjectIndex) as DotView
        selectedDotView.selectedAnimation(true)
    }

    private fun showBottomPromptChip(message: String) {
        bottomPromptChip?.visibility = View.VISIBLE
        bottomPromptChip?.text = message
    }

    private class CardItemDecoration constructor(resources: Resources) : RecyclerView.ItemDecoration() {

        private val cardSpacing: Int = resources.getDimensionPixelOffset(R.dimen.preview_card_spacing)

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val adapterPosition = parent.getChildAdapterPosition(view)
            outRect.left = if (adapterPosition == 0) cardSpacing * 2 else cardSpacing
            val adapter = parent.adapter ?: return
            if (adapterPosition == adapter.itemCount - 1) {
                outRect.right = cardSpacing
            }
        }
    }
}
