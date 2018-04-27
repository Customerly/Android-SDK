package io.customerly.XXXXXcancellare;

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

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by Gianni on 24/06/15.
 * Project: Customerly Android SDK
 */
@SuppressWarnings("unused")
class XXXIU_ProgressiveScrollListener extends RecyclerView.OnScrollListener {
    private final @NonNull LinearLayoutManager _LinearLayoutManager;
    private boolean _Loading = false;
    private final @NonNull OnBottomReachedListener _OnBottomReached;

    XXXIU_ProgressiveScrollListener(@NonNull LinearLayoutManager linearLayoutManager, @NonNull OnBottomReachedListener onBottomReached) {
        super();
        this._LinearLayoutManager = linearLayoutManager;
        this._OnBottomReached = onBottomReached;
    }
    @Override
    public final void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int totalItemCount = this._LinearLayoutManager.getItemCount();
        int lastVisibleItem = this._LinearLayoutManager.findLastVisibleItemPosition();

        if (totalItemCount <= (lastVisibleItem + 1)) {
            synchronized (this) {
                if(this._Loading)
                    return;
                this._Loading = true;
            }
            this._OnBottomReached.onReached(this);
        }
    }
    final void onFinishedUpdating() {
        this._Loading = false;
    }

    interface OnBottomReachedListener {
        void onReached(XXXIU_ProgressiveScrollListener listener);
    }
}
