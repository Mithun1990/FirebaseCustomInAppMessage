package com.amiprobashi.root.fcm.fiam

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.google.firebase.inappmessaging.display.internal.Logging


/**
 * The purpose of this Image view is best explained in the SO post:
 * https://stackoverflow.com/questions/8232608/fit-image-into-imageview-keep-aspect-ratio-and-then-resize-imageview-to-image-d
 * Problem: Given an image of any size, how do we fit it into an image view of some other size such
 * that its larger dimension is scaled to fit inside the image view and the smaller dimension is
 * shrunk to preserve its aspect ratio. While this can be achieved without the help of this custom
 * view, the problematic behavior is that the image view does not shrink to the lower dimension
 * resulting in an empty space surrounding the image view.
 *
 * @hide
 */
// TODO (ashwinraghav) tests pending
class ResizableImageView : AppCompatImageView {
    private var mDensityDpi = 0

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        val displayMetrics = context.resources.displayMetrics
        mDensityDpi = (displayMetrics.density * 160f).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            val d = drawable
            val adjustViewBounds = adjustViewBounds
            if (d != null && adjustViewBounds) {
                scalePxToDp(d)
                checkMinDim()
            }
        }
    }

    private fun checkMinDim() {
        val minWidth = Math.max(minimumWidth, suggestedMinimumWidth)
        val minHeight = Math.max(minimumHeight, suggestedMinimumHeight)
        val widthSpec = measuredWidth
        val heightSpec = measuredHeight
        // Logging.logdPair("Image: min width, height", minWidth, minHeight)
        // Logging.logdPair("Image: actual width, height", widthSpec, heightSpec)

        // Scale TOP if the size is too small
        var scaleW = 1.0f
        var scaleH = 1.0f
        if (widthSpec < minWidth) {
            scaleW = minWidth.toFloat() / widthSpec.toFloat()
        }
        if (heightSpec < minHeight) {
            scaleH = minHeight.toFloat() / heightSpec.toFloat()
        }
        val scale = if (scaleW > scaleH) scaleW else scaleH
        if (scale > 1.0) {
            val targetW = Math.ceil((widthSpec * scale).toDouble()).toInt()
            val targetH = Math.ceil((heightSpec * scale).toDouble()).toInt()
            Logging.logd(
                "Measured dimension ("
                        + widthSpec
                        + "x"
                        + heightSpec
                        + ") too small.  Resizing to "
                        + targetW
                        + "x"
                        + targetH
            )
            val t = bound(targetW, targetH)
            setMeasuredDimension(t.w, t.h)
        }
    }

    private fun scalePxToDp(d: Drawable) {
        val widthSpec = d.intrinsicWidth
        val heightSpec = d.intrinsicHeight
//        Logging.logdPair("Image: intrinsic width, height", widthSpec, heightSpec)

        // Convert 1px to 1dp while keeping bounds
        val targetW = Math.ceil((widthSpec * mDensityDpi / 160).toDouble()).toInt()
        val targetH = Math.ceil((heightSpec * mDensityDpi / 160).toDouble()).toInt()
        val t = bound(targetW, targetH)
//        Logging.logdPair("Image: new target dimensions", t.w, t.h)
        setMeasuredDimension(
            t.w - Math.ceil((40 * mDensityDpi / 160).toDouble()).toInt(),
            t.h - Math.ceil((40 * mDensityDpi / 160).toDouble()).toInt()
        )
    }

    private fun bound(targetW: Int, targetH: Int): Dimensions {
        var targetW = targetW
        var targetH = targetH
        val maxWidth = maxWidth
        val maxHeight = maxHeight
        if (targetW > maxWidth) {
            //Logging.logdNumber("Image: capping width", maxWidth)
            targetH = targetH * maxWidth / targetW
            targetW = maxWidth
        }
        if (targetH > maxHeight) {
            //Logging.logdNumber("Image: capping height", maxHeight)
            targetW = targetW * maxHeight / targetH
            targetH = maxHeight
        }
        return Dimensions(targetW, targetH)
    }

    /** Basically a Pair of integers  */

}

data class Dimensions(val w: Int, val h: Int)