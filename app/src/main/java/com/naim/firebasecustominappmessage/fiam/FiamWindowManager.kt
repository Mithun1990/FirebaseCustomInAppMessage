package com.naim.firebasecustominappmessage.fiam

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.*
import com.google.firebase.inappmessaging.display.internal.InAppMessageLayoutConfig
import com.google.firebase.inappmessaging.display.internal.Logging
import com.google.firebase.inappmessaging.display.internal.SwipeDismissTouchListener
import com.google.firebase.inappmessaging.display.internal.SwipeDismissTouchListener.DismissCallbacks
import com.google.firebase.inappmessaging.display.internal.bindingwrappers.BindingWrapper
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Class encapsulating the popup window into which we inflate the in app message. The window manager
 * keeps state of the binding that is currently in view
 *
 * @hide
 */
@Singleton
class FiamWindowManager @Inject internal constructor() {
    private var bindingWrapper: BindingWrapper? = null

    /** Inflate the container into a new popup window  */
    fun show(bindingWrapper: BindingWrapper, activity: Activity) {
        if (isFiamDisplayed) {
            Logging.loge("Fiam already active. Cannot show new Fiam.")
            return
        }
        if (activity.isFinishing) {
            Logging.loge("Activity is finishing or does not have valid window token. Cannot show FIAM.")
            return
        }
        val config = bindingWrapper.config
        val layoutParams = getLayoutParams(config, activity)
        val windowManager = getWindowManager(activity)
        val rootView: View = bindingWrapper.rootView
        windowManager.addView(rootView, layoutParams)

        // Set 'window' left and right padding from the inset, this prevents
        // anything from touching the navigation bar when in landscape.
        val insetDimensions = getInsetDimensions(activity)
//        Logging.logdPair("Inset (top, bottom)", insetDimensions.top, insetDimensions.bottom)
//        Logging.logdPair("Inset (left, right)", insetDimensions.left, insetDimensions.right)

        // TODO: Should use WindowInsetCompat to make sure we don't overlap with the status bar
        //       action bar or anything else. This will become more pressing as notches
        //       become more common on Android phones.
        if (bindingWrapper.canSwipeToDismiss()) {
            val listener = getSwipeListener(config, bindingWrapper, windowManager, layoutParams)
            bindingWrapper.dialogView.setOnTouchListener(listener)
        }
        this.bindingWrapper = bindingWrapper
    }

    val isFiamDisplayed: Boolean
        get() = if (bindingWrapper == null) {
            false
        } else bindingWrapper!!.rootView.isShown

    /** Removes the in app message from the surrounding window  */
    fun destroy(activity: Activity) {
        if (isFiamDisplayed) {
            getWindowManager(activity).removeViewImmediate(bindingWrapper!!.rootView)
            bindingWrapper = null
        }
    }

    private fun getLayoutParams(
        layoutConfig: InAppMessageLayoutConfig, activity: Activity
    ): WindowManager.LayoutParams {
        val layoutParams = WindowManager.LayoutParams(
            layoutConfig.windowWidth(),
            layoutConfig.windowHeight(),
            DEFAULT_TYPE,
            layoutConfig.windowFlag(),
            PixelFormat.TRANSLUCENT
        )

        // If the window gravity is TOP, we move down to avoid hitting the status bar (if shown).
        val insetDimensions = getInsetDimensions(activity)
        if (layoutConfig.viewWindowGravity() and Gravity.TOP == Gravity.TOP) {
            layoutParams.y = insetDimensions.top
        }
        layoutParams.dimAmount = 0.3f
        layoutParams.gravity = layoutConfig.viewWindowGravity()
        layoutParams.windowAnimations = 0
        return layoutParams
    }

    private fun getWindowManager(activity: Activity): WindowManager {
        return activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /**
     * Get the total size of the display in pixels, with no exclusions. For example on a Pixel this
     * would return 1920x1080 rather than the content frame which gives up 63 pixels to the status bar
     * and 126 pixels to the navigation bar.
     */
    private fun getDisplaySize(activity: Activity): Point {
        val size = Point()
        val display = getWindowManager(activity).defaultDisplay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(size)
        } else {
            display.getSize(size)
        }
        return size
    }

    /**
     * Determine how much content should be inset on all sides in order to not overlap with system UI.
     *
     *
     * Ex: Pixel in portrait top = 63 bottom = 126 left = 0 right = 0
     *
     *
     * Ex: Pixel in landscape, nav bar on right top = 63 bottom = 0 left = 0 right = 126
     */
    private fun getInsetDimensions(activity: Activity): Rect {
        val padding = Rect()
        val visibleFrame = getVisibleFrame(activity)
        val size = getDisplaySize(activity)
        padding.top = visibleFrame.top
        padding.left = visibleFrame.left
        padding.right = size.x - visibleFrame.right
        padding.bottom = size.y - visibleFrame.bottom
        return padding
    }

    private fun getVisibleFrame(activity: Activity): Rect {
        val visibleFrame = Rect()
        val window = activity.window
        window.decorView.getWindowVisibleDisplayFrame(visibleFrame)
        return visibleFrame
    }

    /** Get a swipe listener, using knowledge of the LayoutConfig to dictate the behavior.  */
    private fun getSwipeListener(
        layoutConfig: InAppMessageLayoutConfig,
        bindingWrapper: BindingWrapper,
        windowManager: WindowManager,
        layoutParams: WindowManager.LayoutParams
    ): SwipeDismissTouchListener {

        // The dismiss callbacks are the same in any case.
        val callbacks: DismissCallbacks = object : DismissCallbacks {
            override fun canDismiss(token: Any): Boolean {
                return true
            }

            override fun onDismiss(view: View, token: Any) {
                if (bindingWrapper.dismissListener != null) {
                    bindingWrapper.dismissListener!!.onClick(view)
                }
            }
        }
        return if (layoutConfig.windowWidth() == ViewGroup.LayoutParams.MATCH_PARENT) {
            // When we are using the entire view width we can use the default behavior
            SwipeDismissTouchListener(bindingWrapper.dialogView, null, callbacks)
        } else {
            // When we are not using the entire view width we need to use the WindowManager to animate.
            object : SwipeDismissTouchListener(bindingWrapper.dialogView, null, callbacks) {
                override fun getTranslationX(): Float {
                    return layoutParams.x.toFloat()
                }

                override fun setTranslationX(translationX: Float) {
                    layoutParams.x = translationX.toInt()
                    windowManager.updateViewLayout(bindingWrapper.rootView, layoutParams)
                }
            }
        }
    }

    companion object {
        const val DEFAULT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
    }
}