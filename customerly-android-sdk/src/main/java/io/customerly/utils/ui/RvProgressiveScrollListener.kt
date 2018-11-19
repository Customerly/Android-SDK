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

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.customerly.utils.MyMutableBoolean

/**
 * Created by Gianni on 24/06/15.
 * Project: Customerly Android SDK
 */
internal class RvProgressiveScrollListener(
        private val llm: LinearLayoutManager,
        private val onBottomReached: (RvProgressiveScrollListener)->Unit) : RecyclerView.OnScrollListener() {

    private var loading: MyMutableBoolean = MyMutableBoolean()
    private var skipNext: MyMutableBoolean = MyMutableBoolean()

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (this.llm.itemCount <= this.llm.findLastVisibleItemPosition() + 1) {
            synchronized(this.skipNext) {
                if (this.skipNext.value) {
                    this.skipNext.value = false
                    return
                }
            }
            synchronized(this.loading) {
                if (this.loading.value) {
                    return
                }
                this.loading.value = true
            }
            this.onBottomReached(this)
        }
    }

    internal fun onFinishedUpdating() {
        this.loading.value = false
    }

    fun skipNextBottom() {
        this.skipNext.value = true
    }
}