package io.customerly;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Gianni on 24/06/15.
 * Project: GGlib
 */
@SuppressWarnings({"unused", "WeakerAccess"})
abstract class Internal_Utils__RecyclerView_DividerDecoration extends RecyclerView.ItemDecoration {

    protected final Paint _Paint;
    protected float _1_dp = 0;

    private Internal_Utils__RecyclerView_DividerDecoration(@NonNull Resources resources) {
        this(resources, R.color.io_customerly__greycc);
    }

    private Internal_Utils__RecyclerView_DividerDecoration(@NonNull Resources resources, @ColorRes int colorRes) {
        this(Internal_Utils__Utils.getColorFromResource(resources, colorRes));
        this._1_dp = Math.max(1, Internal_Utils__Utils.px(1));
    }

    private Internal_Utils__RecyclerView_DividerDecoration(@ColorInt int colorInt) {
        super();
        this._Paint = new Paint();
        this._Paint.setColor(colorInt);
        this._Paint.setStyle(Paint.Style.FILL);
    }

    static class _Vertical extends Internal_Utils__RecyclerView_DividerDecoration {

        enum DIVIDER_WHERE {
            TOP, CENTER, BOTTOM, BOTH
        }
        private final DIVIDER_WHERE _DividerWhere;

        public _Vertical(@NonNull Resources resources) {
            super(resources);
            this._DividerWhere = DIVIDER_WHERE.CENTER;
        }

        _Vertical(@NonNull Resources resources, @NonNull DIVIDER_WHERE dividerWhere) {
            super(resources);
            this._DividerWhere = dividerWhere;
        }

        public _Vertical(@NonNull Resources resources, @ColorRes int colorRes) {
            super(resources, colorRes);
            this._DividerWhere = DIVIDER_WHERE.CENTER;
        }

        public _Vertical(@NonNull Resources resources, @ColorRes int colorRes, @NonNull DIVIDER_WHERE dividerWhere) {
            super(resources, colorRes);
            this._DividerWhere = dividerWhere;
        }

        public _Vertical(@ColorInt int colorInt) {
            super(colorInt);
            this._DividerWhere = DIVIDER_WHERE.CENTER;
        }

        public _Vertical(@ColorInt int colorInt, @NonNull DIVIDER_WHERE dividerWhere) {
            super(colorInt);
            this._DividerWhere = dividerWhere;
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if(this._1_dp == 0)
                this._1_dp = Math.max(1, Internal_Utils__Utils.px(1));

            int top, left = parent.getPaddingLeft(), right = parent.getWidth() - parent.getPaddingRight();
            int childCount = parent.getChildCount() - (this._DividerWhere == DIVIDER_WHERE.CENTER ? 1 : 0);
            next_child: for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                switch (this._DividerWhere) {
                    case BOTTOM:
                    case CENTER:
                        top = child.getBottom() + ((RecyclerView.LayoutParams) child.getLayoutParams()).bottomMargin;
                        c.drawRect(left, top, right, top + this._1_dp, this._Paint);
                        continue next_child;
                    case BOTH:
                        top = child.getBottom() + ((RecyclerView.LayoutParams) child.getLayoutParams()).bottomMargin;
                        c.drawRect(left, top, right, top + this._1_dp, this._Paint);
                        //And continue, drawing LEFT too
                    case TOP:
                    default:
                        top = child.getTop() - ((RecyclerView.LayoutParams) child.getLayoutParams()).topMargin;
                        c.drawRect(left, top, right, top + this._1_dp, this._Paint);
                        //continue next_child;
                }
            }
        }
    }

    public static class _Horizontal extends Internal_Utils__RecyclerView_DividerDecoration {

        enum DIVIDER_WHERE {
            LEFT, CENTER, RIGHT, BOTH
        }
        private final DIVIDER_WHERE _DividerWhere;

        public _Horizontal(@NonNull Resources resources) {
            super(resources);
            this._DividerWhere = DIVIDER_WHERE.CENTER;
        }

        public _Horizontal(@NonNull Resources resources, @NonNull DIVIDER_WHERE dividerWhere) {
            super(resources);
            this._DividerWhere = dividerWhere;
        }

        public _Horizontal(@NonNull Resources resources, @ColorRes int colorRes) {
            super(resources, colorRes);
            this._DividerWhere = DIVIDER_WHERE.CENTER;
        }

        public _Horizontal(@NonNull Resources resources, @ColorRes int colorRes, @NonNull DIVIDER_WHERE dividerWhere) {
            super(resources, colorRes);
            this._DividerWhere = dividerWhere;
        }

        public _Horizontal(@ColorInt int colorInt) {
            super(colorInt);
            this._DividerWhere = DIVIDER_WHERE.CENTER;
        }

        public _Horizontal(@ColorInt int colorInt, @NonNull DIVIDER_WHERE dividerWhere) {
            super(colorInt);
            this._DividerWhere = dividerWhere;
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if(this._1_dp == 0)
                this._1_dp = Math.max(1, Internal_Utils__Utils.px(1));

            int top = parent.getPaddingTop(), left, bottom = parent.getHeight() - parent.getPaddingBottom();
            int childCount = parent.getChildCount() - (this._DividerWhere == DIVIDER_WHERE.CENTER ? 1 : 0);
            next_child: for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                switch (this._DividerWhere) {
                    case RIGHT:
                    case CENTER:
                        left = child.getRight() + ((RecyclerView.LayoutParams) child.getLayoutParams()).rightMargin;
                        c.drawRect(left, top, left + this._1_dp, bottom, this._Paint);
                        continue next_child;
                    case BOTH:
                        left = child.getRight() + ((RecyclerView.LayoutParams) child.getLayoutParams()).rightMargin;
                        c.drawRect(left, top, left + this._1_dp, bottom, this._Paint);
                        //And continue, drawing LEFT too
                    case LEFT:
                    default:
                        left = child.getLeft() - ((RecyclerView.LayoutParams) child.getLayoutParams()).leftMargin;
                        c.drawRect(left, top, left + this._1_dp, bottom, this._Paint);
                        //continue next_child;
                }
            }
        }
    }
}
