package io.customerly.utils.ui

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

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

/**
 * Created by Gianni on 24/06/15.
 * Project: Customerly Android SDK
 */
internal class RvProgressiveScrollListener(
        private val linearLayoutManager: LinearLayoutManager,
        private val onBottomReached: (RvProgressiveScrollListener)->Unit) : RecyclerView.OnScrollListener() {

    private var loading = false

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (this.linearLayoutManager.itemCount <= this.linearLayoutManager.findLastVisibleItemPosition() + 1) {
            synchronized(this.loading) {
                if (this.loading) {
                    return
                }
                this.loading = true
            }
            this.onBottomReached(this)
        }
    }

    internal fun onFinishedUpdating() {
        this.loading = false
    }
}