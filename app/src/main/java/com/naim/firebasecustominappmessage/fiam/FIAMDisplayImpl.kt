package com.naim.firebasecustominappmessage.fiam

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnTouchListener
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks.InAppMessagingDismissType
import com.google.firebase.inappmessaging.display.internal.FiamImageLoader
import com.google.firebase.inappmessaging.display.internal.FirebaseInAppMessagingDisplayImpl
import com.google.firebase.inappmessaging.display.internal.InAppMessageLayoutConfig
import com.google.firebase.inappmessaging.display.internal.Logging
import com.google.firebase.inappmessaging.display.internal.bindingwrappers.BindingWrapper
import com.google.firebase.inappmessaging.display.internal.injection.modules.InflaterConfigModule.DISABLED_BG_FLAG
import com.google.firebase.inappmessaging.model.*


class FIAMDisplayImpl(
    private val app: Application, private val headlessInAppMessaging: FirebaseInAppMessaging,
    private val bindingWrapperFactory: BindingWrapperFactory,
    private val imageLoader: FiamImageLoader,
    private val windowManager: FiamWindowManager

) : FirebaseInAppMessagingDisplayImpl(),
    Application.ActivityLifecycleCallbacks {
    var bindingWrapper: BindingWrapper? = null
    private var inAppMessage: InAppMessage? = null
    private var callbacks: FirebaseInAppMessagingDisplayCallbacks? = null
    var currentlyBoundActivityName: String? = null

    override fun displayMessage(p0: InAppMessage, p1: FirebaseInAppMessagingDisplayCallbacks) {
        when (p0.messageType) {
            MessageType.IMAGE_ONLY -> {
                println("In app message called ${extractImageData(p0)?.imageUrl}")
            }
            else -> {}
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        println("In app message called onActivityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        println("In app message called onActivityStarted")

    }

    override fun onActivityResumed(activity: Activity) {
        println("In app message called onActivityResumed")
        super.onActivityResumed(activity);
        bindFiamToActivity(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        println("In app message called onActivityPaused")
        unbindFiamFromActivity(activity);
        headlessInAppMessaging.removeAllListeners();
        super.onActivityPaused(activity);
    }

    override fun onActivityStopped(activity: Activity) {
        println("In app message called onActivityStopped")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        println("In app message called onActivityDestroyed")
    }

    companion object {
        const val TAG = "FirebaseIAMImpl"
        fun register(
            app: Application, headlessInAppMessaging: FirebaseInAppMessaging,
            bindingWrapperFactory: BindingWrapperFactory,
            imageLoader: FiamImageLoader,
            windowManager: FiamWindowManager
        ) {
            val fiam by lazy {
                FIAMDisplayImpl(
                    app, headlessInAppMessaging, bindingWrapperFactory,
                    imageLoader, windowManager
                )
            }
            FirebaseInAppMessaging.getInstance().isAutomaticDataCollectionEnabled = true
            FirebaseInAppMessaging.getInstance().setMessageDisplayComponent(fiam)
            app.registerActivityLifecycleCallbacks(fiam)
        }
    }


    private fun bindFiamToActivity(activity: Activity) {
        // If we have no currently bound activity or are currently bound to a different activity then
        // bind to this new activity.
        if (currentlyBoundActivityName == null
            || !currentlyBoundActivityName.equals(activity.localClassName)
        ) {
            Logging.logi("Binding to activity: " + activity.localClassName)
            headlessInAppMessaging.apply {
                setMessageDisplayComponent { iam: InAppMessage, cb: FirebaseInAppMessagingDisplayCallbacks ->
                    // When we are in the middle of showing a message, we ignore other notifications these
                    // messages will be fired when the corresponding events happen the next time.
                    if (inAppMessage != null || this.areMessagesSuppressed()) {
                        Logging.logd("Active FIAM exists. Skipping trigger")
                        return@setMessageDisplayComponent
                    }
                    inAppMessage = iam
                    callbacks = cb
                    showActiveFiam(activity)
                }
                // set the current activity to be the one passed in so that we know not to bind again to the
                // same activity
                currentlyBoundActivityName = activity.localClassName
            }

            if (inAppMessage != null) {
                showActiveFiam(activity)
            }
        }
    }

    private fun showActiveFiam(activity: Activity) {
        if (inAppMessage == null || headlessInAppMessaging.areMessagesSuppressed()) {
            Logging.loge("No active message found to render")
            return
        }
        if (inAppMessage!!.messageType == MessageType.UNSUPPORTED) {
            Logging.loge("The message being triggered is not supported by this version of the sdk.")
            return
        }
        val config = providesPortraitImageLayoutConfig(providesDisplayMetrics(app))
        bindingWrapper = when (inAppMessage!!.messageType) {
            MessageType.IMAGE_ONLY -> bindingWrapperFactory.createImageBindingWrapper(
                config,
                inAppMessage
            )
            else -> {
                Logging.loge("No bindings found for this message type")
                // so we should break out completely and not attempt to show anything
                return
            }
        }
        // The WindowManager LayoutParams.TYPE_APPLICATION_PANEL requires tokens from the activity
        // which does not become available until after all lifecycle methods are complete.
        activity
            .findViewById<View>(R.id.content)
            .post { inflateBinding(activity, bindingWrapper!!) }
    }


    // Since we handle only touch outside events and let the underlying views handle all other events,
    // it is safe to ignore this warning
    @SuppressLint("ClickableViewAccessibility")
    private fun inflateBinding(activity: Activity, bindingWrapper: BindingWrapper) {
        // On click listener when X button or collapse button is clicked
        val dismissListener = View.OnClickListener {
            if (callbacks != null) {
                callbacks!!.messageDismissed(InAppMessagingDismissType.CLICK)
            }
            dismissFiam(activity)
        }

        val actionListeners: MutableMap<Action?, View.OnClickListener> = HashMap()
        for (action in extractActions(inAppMessage!!)) {
            val actionListener: View.OnClickListener
            // TODO: need an onclick listener per action
            // If the message has an action and an action url, set up an intent to handle the url
            if (action != null && !TextUtils.isEmpty(action.actionUrl)) {
                actionListener = View.OnClickListener {
                    if (callbacks != null) {
                        Logging.logi("Calling callback for click action")
                        callbacks!!.messageClicked(action)
                        inAppMessageAction(inAppMessage!!, activity)
                        removeDisplayedFiam(activity);
                        inAppMessage = null
                        callbacks = null
                    }
                }
            } else {
                Logging.logi("No action url found for action. Treating as dismiss.")
                actionListener = dismissListener
            }
            actionListeners[action] = actionListener
        }

        val layoutListener = bindingWrapper.inflate(actionListeners, dismissListener)
        if (layoutListener != null) {
            bindingWrapper.imageView.viewTreeObserver
                .addOnGlobalLayoutListener(layoutListener);
        }

        // Show fiam after image successfully loads
        // Show fiam after image successfully loads
        loadNullableImage(
            activity,
            bindingWrapper,
            extractImageData(inAppMessage!!)!!,
            object : FiamImageLoader.Callback() {
                override fun onSuccess() {
                    // Setup dismiss on touch outside
                    bindingWrapper
                        .rootView
                        .setOnTouchListener(
                            OnTouchListener { v, event ->
                                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                                    if (callbacks != null) {
                                        callbacks!!.messageDismissed(
                                            InAppMessagingDismissType.UNKNOWN_DISMISS_TYPE
                                        )
                                    }
                                    dismissFiam(activity)
                                    return@OnTouchListener true
                                }
                                false
                            })

                    activity.runOnUiThread {
                        windowManager.show(bindingWrapper, activity);
                        val layoutParams: WindowManager.LayoutParams =
                            getLayoutParams(
                                providesPortraitImageLayoutConfig(
                                    providesDisplayMetrics(
                                        app
                                    )
                                ), activity
                            )
                    }
                }

                override fun onError(e: Exception) {
                    Logging.loge("Image download failure ")
                    if (layoutListener != null) {
                        bindingWrapper
                            .imageView
                            .viewTreeObserver
                            .removeGlobalOnLayoutListener(layoutListener)
                    }
                    inAppMessage = null
                    callbacks = null
                }
            })
    }

    private fun loadNullableImage(
        activity: Activity,
        fiam: BindingWrapper,
        imageData: ImageData,
        callback: FiamImageLoader.Callback
    ) {
        if (isValidImageData(imageData)) {
            imageLoader.apply {
                load(imageData.imageUrl)
                    .tag(activity.javaClass)
                    .placeholder(R.drawable.ic_dialog_alert)
                    .into(fiam.imageView, callback)
            }
        } else {
            callback.onSuccess()
        }
    }

    private fun extractActions(message: InAppMessage): List<Action?> {
        val actions: MutableList<Action?> = ArrayList()
        when (message.messageType) {
            MessageType.BANNER -> actions.add((message as BannerMessage).action)
            MessageType.CARD -> {
                actions.add((message as CardMessage).primaryAction)
                actions.add(message.secondaryAction)
            }
            MessageType.IMAGE_ONLY -> actions.add((message as ImageOnlyMessage).action)
            MessageType.MODAL -> actions.add((message as ModalMessage).action)
            else ->         // An empty action is treated like a dismiss
                actions.add(Action.builder().build())
        }
        return actions
    }

    // TODO: Factor this into the InAppMessage API.
    private fun extractImageData(message: InAppMessage): ImageData? {
        // Handle getting image data for card type
        if (message.messageType == MessageType.IMAGE_ONLY) {
            val imageData: ImageData = (message as ImageOnlyMessage).imageData
            return if (isValidImageData(imageData)) imageData else null
        }
        // For now this is how we get all other fiam types image data.
        return null
    }

    // TODO: Factor this into the InAppMessage API
    private fun isValidImageData(imageData: ImageData?): Boolean {
        return imageData != null && !TextUtils.isEmpty(imageData.imageUrl)
    }

    @SuppressLint("WrongConstant")
    private fun getWindowManager(activity: Activity): WindowManager {
        return activity.getSystemService("window") as WindowManager
    }

    private val DEFAULT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
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

    private fun getInsetDimensions(activity: Activity): Rect {
        val padding = Rect()
        val visibleFrame: Rect = this.getVisibleFrame(activity)
        val size: Point = this.getDisplaySize(activity)
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

    private fun getDisplaySize(activity: Activity): Point {
        val size = Point()
        val display = getWindowManager(activity).defaultDisplay
        if (VERSION.SDK_INT >= 17) {
            display.getRealSize(size)
        } else {
            display.getSize(size)
        }
        return size
    }

    fun providesPortraitImageLayoutConfig(displayMetrics: DisplayMetrics): InAppMessageLayoutConfig {
        return InAppMessageLayoutConfig.builder()
            .setMaxDialogHeightPx((0.9f * displayMetrics.heightPixels).toInt())
            .setMaxDialogWidthPx((0.9f * displayMetrics.widthPixels).toInt())
            .setMaxImageWidthWeight(0.9f)
            .setMaxImageHeightWeight(0.9f)
            .setViewWindowGravity(Gravity.CENTER)
            .setWindowFlag(DISABLED_BG_FLAG)
            .setWindowWidth(ViewGroup.LayoutParams.WRAP_CONTENT)
            .setWindowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            .setBackgroundEnabled(false)
            .setAnimate(false)
            .setAutoDismiss(false)
            .build()
    }

    fun providesDisplayMetrics(application: Application): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        val windowManager =
            application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }

    // This action needs to be idempotent since multiple callbacks compete to dismiss.
    // For example, a swipe and a click on the banner compete.
    private fun dismissFiam(activity: Activity) {
        Logging.logd("Dismissing fiam")
        removeDisplayedFiam(activity)
        inAppMessage = null
        callbacks = null
    }

    private fun removeDisplayedFiam(activity: Activity) {
        if (windowManager.isFiamDisplayed) {
            imageLoader.cancelTag(activity.javaClass)
            windowManager.destroy(activity)
        }
    }


    private fun unbindFiamFromActivity(activity: Activity) {
        // If we are attempting to unbind from an activity, first check to see that we are currently
        // bound to it
        if (currentlyBoundActivityName != null
            && currentlyBoundActivityName == activity.localClassName
        ) {
            Logging.logi("Unbinding from activity: " + activity.localClassName)
            headlessInAppMessaging.clearDisplayListener()
            removeDisplayedFiam(activity)
            currentlyBoundActivityName = null
        }
    }

    private fun inAppMessageAction(inAppMessage: InAppMessage, activity: Activity) {
        val data = inAppMessage.data
        //Do your action when clicked on image
    }
}