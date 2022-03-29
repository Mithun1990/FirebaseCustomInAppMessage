package com.naim.firebasecustominappmessage.fiam


// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.ImageView
import com.google.firebase.inappmessaging.display.internal.InAppMessageLayoutConfig
import com.google.firebase.inappmessaging.display.internal.bindingwrappers.BindingWrapper
import com.google.firebase.inappmessaging.display.internal.layout.FiamFrameLayout
import com.google.firebase.inappmessaging.model.Action
import com.google.firebase.inappmessaging.model.ImageOnlyMessage
import com.google.firebase.inappmessaging.model.InAppMessage
import com.google.firebase.inappmessaging.model.MessageType
import com.naim.firebasecustominappmessage.R


/**
 * Wrapper for bindings for Image only modal. This class currently is not unit tested since it is
 * purely declarative.
 *
 * @hide
 */

class ImageBindingWrapper constructor(
    config: InAppMessageLayoutConfig?,
    private val _inflater: LayoutInflater?,
    message: InAppMessage?
) :
    BindingWrapper(config, _inflater, message) {
    private var imageRoot: FiamFrameLayout? = null
    private var imageContentRoot: ViewGroup? = null
    private var imageView: ImageView? = null
    private var collapseButton: Button? = null
    override fun inflate(
        actionListeners: Map<Action, View.OnClickListener>,
        dismissOnClickListener: View.OnClickListener
    ): OnGlobalLayoutListener? {
        val v: View = _inflater!!.inflate(R.layout.fiam_image, null)
        imageRoot = v.findViewById(R.id.image_root)
        imageContentRoot = v.findViewById(R.id.image_content_root)
        imageView = v.findViewById(R.id.image_view)
        collapseButton = v.findViewById(R.id.collapse_button)

        // Setup ImageView.
        if (message.messageType == MessageType.IMAGE_ONLY) {
            val msg = message as ImageOnlyMessage
            imageView!!.visibility = if (TextUtils.isEmpty(msg.imageData.imageUrl)) View.GONE else View.VISIBLE
            imageView!!.setOnClickListener(actionListeners[msg.action])
        }

        // Setup dismiss button.
        imageRoot!!.setDismissListener(dismissOnClickListener)
        collapseButton!!.setOnClickListener(dismissOnClickListener)
        return null
    }

    override fun getImageView(): ImageView {
        return imageView!!
    }

    override fun getRootView(): ViewGroup {
        return imageRoot!!
    }

    override fun getDialogView(): View {
        return imageContentRoot!!
    }

    fun getCollapseButton(): View {
        return collapseButton!!
    }
}