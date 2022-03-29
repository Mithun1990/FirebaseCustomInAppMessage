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
        //Do your own code to receive in app message
        //Details will be found in code block
        super.onActivityResumed(activity);
    }

    override fun onActivityPaused(activity: Activity) {
        println("In app message called onActivityPaused")
         //Pause in app message receiver 
        //Details will be found in code block
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
>> Firebase in app message library register when application started. So to receive in app message in your own implementation register that implemenation with the headless Firebase In-App Messaging SDK. In the code block we register through FIAMDisplayImpl class's register method from the activity where we want to receive the in app message. As we message will be received after resume the activity so we register during onStart lifecycle of the activity.
```kotlin 
    override fun onStart() {
        super.onStart()
        FIAMDisplayImpl.register(
            application,
            FirebaseInAppMessaging.getInstance(),
            BindingWrapperFactory(application),
            FiamImageLoader(Glide.with(application)),
            FiamWindowManager()
        )
    }
```
