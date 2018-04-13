@file:Suppress("unused")

package io.customerly.utils.ggkext

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import kotlin.reflect.KClass

/**
 * Created by Gianni on 11/08/17.
 */
internal fun <PARCELABLE :Parcelable> Bundle.putParcel(parcelable: PARCELABLE?) : Bundle {
    parcelable?.let {
        this.putParcelable(parcelable.javaClass.simpleName, parcelable)
    }
    return this
}

internal fun <PARCELABLE :Parcelable> Intent.putParcelExtra(parcelable: PARCELABLE?) : Intent {
    parcelable?.let {
        this.putExtra(parcelable.javaClass.simpleName, parcelable)
    }
    return this
}

internal fun <PARCELABLE :Parcelable> Intent.getParcelExtra(valueClass: KClass<PARCELABLE>) : PARCELABLE? {
    return this.getParcelableExtra(valueClass.simpleName)
}

internal fun <PARCELABLE :Parcelable> bundle(vararg parcelables : PARCELABLE?, and :((Bundle)->Unit)? = null): Bundle {
    return Bundle().apply {
        parcelables.asSequence().filterNotNull().forEach { this.putParcelable(it.javaClass.simpleName, it) }
        and?.invoke(this)
    }
}

