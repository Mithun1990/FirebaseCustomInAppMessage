# FirebaseCustomInAppMessage
## How to customize firebase in app message in Android
> Customization of firebase in app message for Android
> > The customization of firebase in app message for android following the article (https://firebase.google.com/docs/in-app-messaging/customize-messages?platform=android) was hard to decide from where to start. It should have to be done in the following way. 
> > 1. At first, write your own implementation of the FirebaseInAppMessagingDisplay class. 
```kotin
class FIAMDisplayImpl(
    private val app: Application, private val headlessInAppMessaging: FirebaseInAppMessaging,
    private val bindingWrapperFactory: BindingWrapperFactory,
    private val imageLoader: FiamImageLoader,
    private val windowManager: FiamWindowManager

) : FirebaseInAppMessagingDisplayImpl(),
    Application.ActivityLifecycleCallbacks {
    
    
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
    }

    override fun onActivityPaused(activity: Activity) {
        println("In app message called onActivityPaused")
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
    }

```
