package io.customerly

import android.support.annotation.IntDef

/**
 * Created by Gianni on 11/04/18.
 * Project: Customerly-KAndroid-SDK
 */

const val USER_TYPE__ANONYMOUS = 1 //hex 0x01 dec: 1
const val USER_TYPE__LEAD = 2 //hex 0x02 dec: 2
const val USER_TYPE__USER = 4 //hex 0x04 dec: 4

@IntDef(USER_TYPE__ANONYMOUS, USER_TYPE__LEAD, USER_TYPE__USER)
@Retention(value = AnnotationRetention.SOURCE)
internal annotation class UserType



const val WRITER_TYPE__ACCOUNT = 0
const val WRITER_TYPE__USER = 1

@IntDef(WRITER_TYPE__ACCOUNT, WRITER_TYPE__USER, USER_TYPE__USER)
@Retention(value = AnnotationRetention.SOURCE)
internal annotation class WriterType