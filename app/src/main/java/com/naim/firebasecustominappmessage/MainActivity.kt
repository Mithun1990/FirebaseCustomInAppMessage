package com.naim.firebasecustominappmessage

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.inappmessaging.display.internal.FiamImageLoader
import com.naim.firebasecustominappmessage.fiam.BindingWrapperFactory
import com.naim.firebasecustominappmessage.fiam.FIAMDisplayImpl
import com.naim.firebasecustominappmessage.fiam.FiamWindowManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

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
}