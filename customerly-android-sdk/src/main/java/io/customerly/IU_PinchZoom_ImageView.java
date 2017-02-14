package io.customerly;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Gianni on 14/02/17.
 * Project: CustomerlyAndroidSDK
 */
public class IU_PinchZoom_ImageView extends android.support.v7.widget.AppCompatImageView {

    private final Matrix _Matrix = new Matrix(), _SavedMatrix = new Matrix();

    private static final int NONE = 0, DRAG = 1, ZOOM = 2;
    @IntDef({NONE, DRAG, ZOOM})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {}

    @State private int mode = NONE;

    private final PointF start = new PointF(), mid = new PointF();
    private float oldDist = 1f;

    public IU_PinchZoom_ImageView(Context context) {
        super(context);
        this.init();
    }

    public IU_PinchZoom_ImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public IU_PinchZoom_ImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        super.setScaleType(ScaleType.MATRIX);

        this.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Drawable d = getDrawable();
            final float drawable_width = d.getIntrinsicWidth();
            final float drawable_height = d.getIntrinsicHeight();

            float scaleX = getMeasuredWidth() / drawable_width;
            float scaleY = getMeasuredHeight() / drawable_height;

            this._Matrix.set(this._SavedMatrix);
            if(scaleX < scaleY) {
                this._Matrix.reset();
                this._Matrix.setTranslate(0, (scaleY - scaleX) * drawable_height / 2 / scaleX);
                this._Matrix.postScale(scaleX, scaleX, 0, 0);
            } else {
                this._Matrix.reset();
                this._Matrix.setTranslate((scaleX - scaleY) * drawable_width / 2 / scaleY, 0 );
                this._Matrix.postScale(scaleY, scaleY, 0, 0);
            }
            this.setImageMatrix(this._Matrix);
        });
    }

    @Override
    @Deprecated
    public final void setScaleType(ScaleType scaleType) {
        super.setScaleType(ScaleType.MATRIX);
    }

    @Override
    @Deprecated
    public void setOnTouchListener(OnTouchListener l) {
        //Cannot set on touch listener
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Handle touch events here...
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                this._SavedMatrix.set(this._Matrix);
                this.start.set(event.getX(), event.getY());
                this.mode = DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                this.oldDist = spacing(event);
                if (this.oldDist > 10f) {
                    this._SavedMatrix.set(this._Matrix);
                    this.midPoint(this.mid, event);
                    this.mode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                this.mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (this.mode == DRAG) {
                    this._Matrix.set(this._SavedMatrix);
                    this._Matrix.postTranslate(event.getX() - this.start.x, event.getY() - this.start.y);
                } else if (this.mode == ZOOM) {
                    float newDist = this.spacing(event);
                    if (newDist > 10f) {
                        this._Matrix.set(this._SavedMatrix);
                        float scale = newDist / this.oldDist;
                        this._Matrix.postScale(scale, scale, this.mid.x, this.mid.y);
                    }
                }
                break;
        }

        this.setImageMatrix(this._Matrix);
        return true;
    }

    // Determine the space between the first two fingers
    private float spacing(@NonNull MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    // Calculate the mid point of the first two fingers
    private void midPoint(@NonNull PointF point, @NonNull MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
