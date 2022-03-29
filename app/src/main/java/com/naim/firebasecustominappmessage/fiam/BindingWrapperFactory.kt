package com.naim.firebasecustominappmessage.fiam

import android.app.Application
import android.content.Context
import android.view.LayoutInflater
import com.google.firebase.inappmessaging.display.internal.InAppMessageLayoutConfig
import com.google.firebase.inappmessaging.display.internal.bindingwrappers.BindingWrapper
import com.google.firebase.inappmessaging.model.InAppMessage
import javax.inject.Singleton
/** @hide
 */

class BindingWrapperFactory(private val application: Application) {
    fun createImageBindingWrapper(
        config: InAppMessageLayoutConfig?, inAppMessage: InAppMessage?
    ): BindingWrapper {
        val inflater =
            application.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return ImageBindingWrapper(config, inflater, inAppMessage)
    }
}