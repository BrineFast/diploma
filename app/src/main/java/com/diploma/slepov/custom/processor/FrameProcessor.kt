package com.diploma.slepov.custom.processor

import com.diploma.slepov.custom.view.camera.GraphicOverlay
import java.nio.ByteBuffer

/** An interface to process the input camera frame and perform detection on it.  */
interface FrameProcessor {

    /** Processes the input frame with the underlying detector.  */
    fun process(data: ByteBuffer, frameMetadata: FrameMetadata, graphicOverlay: GraphicOverlay)

    /** Stops the underlying detector and release resources.  */
    fun stop()
}
