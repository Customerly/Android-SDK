@file:Suppress("unused")

/*
 * Copyright (C) 2017 Customerly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.customerly.utils.ggkext

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import io.customerly.sxdependencies.annotations.SXIntDef
import java.lang.ref.WeakReference

/**
 * Created by Gianni on 02/11/17.
 */
internal val PopupWindow.activity : Activity?
    get() = this.contentView.activity

internal val View.activity : Activity?
    get() = this.context.tryBaseContextActivity

internal infix fun View.goneFor(show: View) {
    viewsGone(this)
    viewsVisible(show)
}

internal infix fun View.invisibleFor(show: View) {
    viewsInvisible(this)
    viewsVisible(show)
}

internal infix fun View.visibleIfIsVisible(check: View) {
    if(check.visibility == View.VISIBLE) {
        viewsVisible(this)
    }
}

internal fun View.visibleIfNotVisible() {
    if (this.visibility != View.VISIBLE) {
        this.visibility = View.VISIBLE
    }
}

@SXIntDef(View.VISIBLE, View.INVISIBLE, View.GONE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class ViewVisibility

internal fun viewsVisibility(vararg views: View, @ViewVisibility visibility : Int) {
    views.forEach { v -> v.visibility = visibility }
}

internal fun viewsVisible(vararg hide : View) {
    viewsVisibility(*hide, visibility = View.VISIBLE)
}

internal fun viewsInvisible(vararg hide : View) {
    viewsVisibility(*hide, visibility = View.INVISIBLE)
}

internal fun viewsGone(vararg hide : View) {
    viewsVisibility(*hide, visibility = View.GONE)
}

internal fun <R1, VIEW : View> VIEW.setOnClickListenerWithWeak(r1 : R1, onClick : (View,R1?)->Unit) {
    val w1 = WeakReference(r1)
    this.setOnClickListener {
        @Suppress("UNCHECKED_CAST")
        onClick(it as VIEW,w1.get())
    }
}

@ViewVisibility internal val Boolean.toViewVisibility : Int
    get() = if(this) View.VISIBLE else View.GONE


internal fun overrideValueAnimatorDurationScale(durationScale : Float = 1f) {
    try {
        android.animation.ValueAnimator::class.java
                .getMethod("setDurationScale", Float::class.javaPrimitiveType!!)
                .invoke(null, durationScale)
        /*
        Proguard:
            -keepclasseswithmembers class android.animation.ValueAnimator {
                public static void setDurationScale(float);
            }
         */
    } catch (justInCase_es_methodNameRefactoring: Throwable) { }
}

internal fun View.dismissKeyboard() {
    (this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(this.windowToken, 0)
}