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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by Gianni on 31/05/16.
 * Project: Customerly Android SDK
 */
public class IU_MaxHeightScrollView extends ScrollView {

    private static final int DEFAULT_HEIGHT = 200;

    private int maxHeight;

    public IU_MaxHeightScrollView(Context context) {
        super(context);
    }

    public IU_MaxHeightScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context, attrs);
    }

    public IU_MaxHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context, attrs);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public IU_MaxHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (! this.isInEditMode() && attrs != null) {
            TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.IU_MaxHeightScrollView);
            this.maxHeight = styledAttrs.getDimensionPixelSize(R.styleable.IU_MaxHeightScrollView_maxHeight, DEFAULT_HEIGHT);
            styledAttrs.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(this.maxHeight, MeasureSpec.AT_MOST));
    }
}