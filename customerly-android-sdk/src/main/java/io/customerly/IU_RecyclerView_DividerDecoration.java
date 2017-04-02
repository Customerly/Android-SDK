package io.customerly;

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

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Gianni on 24/06/15.
 * Project: Customerly Android SDK
 */
@SuppressWarnings("unused,WeakerAccess")
abstract class IU_RecyclerView_DividerDecoration extends RecyclerView.ItemDecoration {

    public static final int DIVIDER_WHERE__LEFTTOP = 0;
    public static final int DIVIDER_WHERE__CENTER = 1;
    public static final int DIVIDER_WHERE__RIGHTBOTTOM = 2;
    public static final int DIVIDER_WHERE__BOTH = 3;
    @IntDef({DIVIDER_WHERE__LEFTTOP, DIVIDER_WHERE__CENTER, DIVIDER_WHERE__RIGHTBOTTOM, DIVIDER_WHERE__BOTH})
    @interface DIVIDER_WHERE {}

    protected final Paint _Paint;
    protected float _1_dp = 0;

    private IU_RecyclerView_DividerDecoration(@NonNull Resources resources) {
        this(R.color.io_customerly__grey_cc, resources);
    }

    private IU_RecyclerView_DividerDecoration(@ColorRes int colorRes, @NonNull Resources resources) {
        this(IU_Utils.getColorFromResource(resources, colorRes));
        this._1_dp = Math.max(1, IU_Utils.px(1));
    }

    private IU_RecyclerView_DividerDecoration(@ColorInt int colorInt) {
        super();
        this._Paint = new Paint();
        this._Paint.setColor(colorInt);
        this._Paint.setStyle(Paint.Style.FILL);
    }

    public static class _Vertical extends IU_RecyclerView_DividerDecoration {

        private final @DIVIDER_WHERE int _DividerWhere;

        public _Vertical(@NonNull Resources resources) {
            super(resources);
            this._DividerWhere = DIVIDER_WHERE__CENTER;
        }

        _Vertical(@NonNull Resources resources, @DIVIDER_WHERE int dividerWhere) {
            super(resources);
            this._DividerWhere = dividerWhere;
        }

        public _Vertical(@ColorRes int colorRes, @NonNull Resources resources) {
            super(colorRes, resources);
            this._DividerWhere = DIVIDER_WHERE__CENTER;
        }

        public _Vertical(@ColorRes int colorRes, @NonNull Resources resources, @DIVIDER_WHERE int dividerWhere) {
            super(colorRes, resources);
            this._DividerWhere = dividerWhere;
        }

        public _Vertical(@ColorInt int colorInt) {
            super(colorInt);
            this._DividerWhere = DIVIDER_WHERE__CENTER;
        }

        public _Vertical(@ColorInt int colorInt, @DIVIDER_WHERE int dividerWhere) {
            super(colorInt);
            this._DividerWhere = dividerWhere;
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if(this._1_dp == 0)
                this._1_dp = Math.max(1, IU_Utils.px(1));

            int top, left = parent.getPaddingLeft(), right = parent.getWidth() - parent.getPaddingRight();
            int childCount = parent.getChildCount() - (this._DividerWhere == DIVIDER_WHERE__CENTER ? 1 : 0);
            next_child: for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                switch (this._DividerWhere) {
                    case DIVIDER_WHERE__RIGHTBOTTOM:
                    case DIVIDER_WHERE__CENTER:
                        top = child.getBottom() + ((RecyclerView.LayoutParams) child.getLayoutParams()).bottomMargin;
                        c.drawRect(left, top, right, top + this._1_dp, this._Paint);
                        continue next_child;
                    case DIVIDER_WHERE__BOTH:
                        top = child.getBottom() + ((RecyclerView.LayoutParams) child.getLayoutParams()).bottomMargin;
                        c.drawRect(left, top, right, top + this._1_dp, this._Paint);
                        //And continue, drawing LEFT too
                    case DIVIDER_WHERE__LEFTTOP:
                    default:
                        top = child.getTop() - ((RecyclerView.LayoutParams) child.getLayoutParams()).topMargin;
                        c.drawRect(left, top, right, top + this._1_dp, this._Paint);
                        //continue next_child;
                }
            }
        }
    }

    public static class _Horizontal extends IU_RecyclerView_DividerDecoration {

        private final @DIVIDER_WHERE int _DividerWhere;

        public _Horizontal(@NonNull Resources resources) {
            super(resources);
            this._DividerWhere = DIVIDER_WHERE__CENTER;
        }

        _Horizontal(@NonNull Resources resources, @DIVIDER_WHERE int dividerWhere) {
            super(resources);
            this._DividerWhere = dividerWhere;
        }

        public _Horizontal(@ColorRes int colorRes, @NonNull Resources resources) {
            super(colorRes, resources);
            this._DividerWhere = DIVIDER_WHERE__CENTER;
        }

        public _Horizontal(@ColorRes int colorRes, @NonNull Resources resources, @DIVIDER_WHERE int dividerWhere) {
            super(colorRes, resources);
            this._DividerWhere = dividerWhere;
        }

        public _Horizontal(@ColorInt int colorInt) {
            super(colorInt);
            this._DividerWhere = DIVIDER_WHERE__CENTER;
        }

        public _Horizontal(@ColorInt int colorInt, @DIVIDER_WHERE int dividerWhere) {
            super(colorInt);
            this._DividerWhere = dividerWhere;
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if(this._1_dp == 0)
                this._1_dp = Math.max(1, IU_Utils.px(1));

            int top = parent.getPaddingTop(), left, bottom = parent.getHeight() - parent.getPaddingBottom();
            int childCount = parent.getChildCount() - (this._DividerWhere == DIVIDER_WHERE__CENTER ? 1 : 0);
            next_child: for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                switch (this._DividerWhere) {
                    case DIVIDER_WHERE__RIGHTBOTTOM:
                    case DIVIDER_WHERE__CENTER:
                        left = child.getRight() + ((RecyclerView.LayoutParams) child.getLayoutParams()).rightMargin;
                        c.drawRect(left, top, left + this._1_dp, bottom, this._Paint);
                        continue next_child;
                    case DIVIDER_WHERE__BOTH:
                        left = child.getRight() + ((RecyclerView.LayoutParams) child.getLayoutParams()).rightMargin;
                        c.drawRect(left, top, left + this._1_dp, bottom, this._Paint);
                        //And continue, drawing LEFT too
                    case DIVIDER_WHERE__LEFTTOP:
                    default:
                        left = child.getLeft() - ((RecyclerView.LayoutParams) child.getLayoutParams()).leftMargin;
                        c.drawRect(left, top, left + this._1_dp, bottom, this._Paint);
                        //continue next_child;
                }
            }
        }
    }
}
