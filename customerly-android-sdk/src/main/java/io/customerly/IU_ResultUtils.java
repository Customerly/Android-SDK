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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by Gianni on 25/06/15.
 * Project: Customerly Android SDK
 */
@SuppressWarnings("unused")
class IU_ResultUtils {
    interface OnNoResult {
        void onResult();
    }
    interface OnResult<D> {
        void onResult(@Nullable D result);
    }
    interface OnResultList<D> {
        void onResult(@Nullable List<D> result);
    }
    interface OnNonNullResult<D> {
        void onResult(@NonNull D result);
    }
}
