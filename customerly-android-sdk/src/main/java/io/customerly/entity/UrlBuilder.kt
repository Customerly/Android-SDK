package io.customerly.entity

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

import android.support.annotation.Px
import io.customerly.Customerly
import io.customerly.utils.CUSTOMERLY_PICTURE_ENDPOINT_BASEURL

/**
 * Created by Gianni on 09/04/18.
 * Project: Customerly-KAndroid-SDK
 */

fun urlImageAccount(accountId: Long, @Px sizePX: Int, name: String?): String
        = "${CUSTOMERLY_PICTURE_ENDPOINT_BASEURL}accounts/$accountId/$sizePX?name=${name ?: ""}"

fun urlImageUser(userID: Long, @Px sizePX: Int, name: String? = Customerly.currentUser.name): String
        = "${CUSTOMERLY_PICTURE_ENDPOINT_BASEURL}users/$userID/$sizePX?name=${name ?: ""}"