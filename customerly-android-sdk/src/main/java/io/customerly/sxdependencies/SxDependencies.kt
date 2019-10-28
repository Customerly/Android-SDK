package io.customerly.sxdependencies

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar

/* AndroidX
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.TextView
 */

/* Support Libraries
import android.content.Context
import android.graphics.Canvas
import android.support.design.widget.AppBarLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.*
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.widget.ImageViewCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.util.AttributeSet
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.widget.TextView
 */

/**
 * Created by Gianni on 20/11/2018.
 * Project: Customerly-KAndroid-SDK
 */

/* targetSdkVersion >=29 */

fun sxSetColorFilterMultiply(drawable: Drawable, color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        drawable.colorFilter = BlendModeColorFilter(color, BlendMode.MULTIPLY)
    } else {
        @Suppress("DEPRECATION")
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
    }
}

const val BUILD_VERSION_CODES_Q = Build.VERSION_CODES.Q

fun textviewSingleLine(textView: TextView, isSingleLine: Boolean) {
    textView.isSingleLine = isSingleLine
}

/* targetSdkVersion <=28

fun sxSetColorFilterMultiply(drawable: Drawable, color: Int) {
    drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
}

const val BUILD_VERSION_CODES_Q = 29

fun textviewSingleLine(textView: TextView, isSingleLine: Boolean) {
    textView.setSingleLine(isSingleLine)
}

*/

// androidx.appcompat
typealias SXAlertDialogBuilder = AlertDialog.Builder
typealias SXAppCompatActivity = AppCompatActivity
class SXAppCompatEditText: AppCompatEditText {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}
open class SXAppCompatImageView: AppCompatImageView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}
class SXAppCompatRadioButton: AppCompatRadioButton {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}
class SXAppCompatRatingBar: AppCompatRatingBar {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}
class SXAppCompatSeekBar: AppCompatSeekBar {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}
class SXAppCompatSpinner: AppCompatSpinner {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}
class SXToolbar: Toolbar {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}

// androidx.cardview
class SXCardView: CardView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}

// com.google.android.material
class SXAppBarLayout: AppBarLayout {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
}
typealias SXSnackbar = Snackbar

// legacy/core
typealias SXFileProvider = FileProvider
typealias SXActivityCompat = ActivityCompat
typealias SXContextCompat = ContextCompat
typealias SXDrawableCompat = DrawableCompat
typealias SXImageViewCompat = ImageViewCompat
typealias SXNotificationCompatBuilder = NotificationCompat.Builder
// legacy/fragment
typealias SXFragmentActivity = FragmentActivity
typealias SXFragmentManager = FragmentManager
typealias SXDialogFragment = DialogFragment
// legacy/swiperefreshlayout
class SXSwipeRefreshLayout: SwipeRefreshLayout {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
}
typealias SXSwipeRefreshLayoutOnRefreshListener = SwipeRefreshLayout.OnRefreshListener
// legacy/recyclerview
typealias SXDefaultItemAnimator = DefaultItemAnimator
typealias SXLinearLayoutManager = LinearLayoutManager
class SXRecyclerView: RecyclerView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}
typealias SXRecyclerViewLayoutParams = RecyclerView.LayoutParams
typealias SXRecyclerViewViewHolder = RecyclerView.ViewHolder
abstract class SXRecyclerViewItemDecoration: RecyclerView.ItemDecoration() {
    final override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if(parent is SXRecyclerView) {
            this.onDrawOver(c, parent)
        }
    }
    abstract fun onDrawOver(c: Canvas, parent: SXRecyclerView)
}
abstract class SXRecyclerViewOnScrollListener: RecyclerView.OnScrollListener() {
    @Deprecated("use onScrolled(SXRecyclerView,Int,Int)", replaceWith = ReplaceWith("onScrolled(SXRecyclerView,Int,Int)"))
    final override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if(recyclerView is SXRecyclerView) {
            this.onScrolled(recyclerView, dx, dy)
        }
    }
    abstract fun onScrolled(recyclerView: SXRecyclerView, dx: Int, dy: Int)
}
typealias SXRecyclerViewAdapter<VH> = RecyclerView.Adapter<VH>