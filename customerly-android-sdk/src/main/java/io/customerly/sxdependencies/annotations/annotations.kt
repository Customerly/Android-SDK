@file:Suppress("unused")

package io.customerly.sxdependencies.annotations

/***********************************************************
 ********************** Px
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE
)
annotation class SXPx


/***********************************************************
 ********************** Size
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.ANNOTATION_CLASS
)
annotation class SXSize(
    /** An exact size (or -1 if not specified)  */
    val value: Long = -1,
    /** A minimum size, inclusive  */
    val min: Long = java.lang.Long.MIN_VALUE,
    /** A maximum size, inclusive  */
    val max: Long = java.lang.Long.MAX_VALUE,
    /** The size must be a multiple of this factor  */
    val multiple: Long = 1
)


/***********************************************************
 ********************** LayoutRes
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE
)
annotation class SXLayoutRes


/***********************************************************
 ********************** DrawableRes
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE
)
annotation class SXDrawableRes


/***********************************************************
 ********************** StringRes
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE
)
annotation class SXStringRes

/***********************************************************
 ********************** StringDef
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SXStringDef(
    vararg val value: String = []
)


/***********************************************************
 ********************** IntDef
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SXIntDef(
    vararg val value: Int = [],
    val flag: Boolean = false
)


/***********************************************************
 ********************** ColorInt
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FIELD
)
annotation class SXColorInt


/***********************************************************
 ********************** ColorRes
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FIELD
)
annotation class SXColorRes


/***********************************************************
 ********************** IntRange
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS
)
annotation class SXIntRange(
    /** Smallest value, inclusive  */
    val from: Long = java.lang.Long.MIN_VALUE,
    /** Largest value, inclusive  */
    val to: Long = java.lang.Long.MAX_VALUE
)


/***********************************************************
 ********************** FloatRange
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS
)
annotation class SXFloatRange(
    /** Smallest value. Whether it is inclusive or not is determined
     * by [.fromInclusive]  */
    val from: Double = java.lang.Double.NEGATIVE_INFINITY,
    /** Largest value. Whether it is inclusive or not is determined
     * by [.toInclusive]  */
    val to: Double = java.lang.Double.POSITIVE_INFINITY,
    /** Whether the from value is included in the range  */
    val fromInclusive: Boolean = true,
    /** Whether the to value is included in the range  */
    val toInclusive: Boolean = true
)


/***********************************************************
 ********************** UiThread
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class SXUiThread


/***********************************************************
 ********************** RequiresApi
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.FILE,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FIELD)
annotation class SXRequiresApi(
        /**
         * The API level to require. Alias for [.api] which allows you to leave out the `api=` part.
         */
        @SXIntRange(from = 1)
        val value: Int = 1,
        /** The API level to require  */
        @SXIntRange(from = 1)
        val api: Int = 1)


/***********************************************************
 ********************** RequiresPermission
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class SXRequiresPermission(
    /**
     * The name of the permission that is required, if precisely one permission
     * is required. If more than one permission is required, specify either
     * [.allOf] or [.anyOf] instead.
     *
     *
     * If specified, [.anyOf] and [.allOf] must both be null.
     */
    val value: String = "",
    /**
     * Specifies a list of permission names that are all required.
     *
     *
     * If specified, [.anyOf] and [.value] must both be null.
     */
    val allOf: Array<String> = [],
    /**
     * Specifies a list of permission names where at least one is required
     *
     *
     * If specified, [.allOf] and [.value] must both be null.
     */
    val anyOf: Array<String> = [],
    /**
     * If true, the permission may not be required in all cases (e.g. it may only be
     * enforced on certain platforms, or for certain call parameters, etc.
     */
    val conditional: Boolean = false
) {

    /**
     * Specifies that the given permission is required for read operations.
     *
     *
     * When specified on a parameter, the annotation indicates that the method requires
     * a permission which depends on the value of the parameter (and typically
     * the corresponding field passed in will be one of a set of constants which have
     * been annotated with a `@RequiresPermission` annotation.)
     */
    @kotlin.annotation.Target(
        AnnotationTarget.FIELD,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.VALUE_PARAMETER
    )
    annotation class Read(val value: SXRequiresPermission = SXRequiresPermission())

    /**
     * Specifies that the given permission is required for write operations.
     *
     *
     * When specified on a parameter, the annotation indicates that the method requires
     * a permission which depends on the value of the parameter (and typically
     * the corresponding field passed in will be one of a set of constants which have
     * been annotated with a `@RequiresPermission` annotation.)
     */
    @kotlin.annotation.Target(
        AnnotationTarget.FIELD,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.VALUE_PARAMETER
    )
    annotation class Write(val value: SXRequiresPermission = SXRequiresPermission())
}


/***********************************************************
 ********************** CheckResult
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class SXCheckResult(
    /** Defines the name of the suggested method to use instead, if applicable (using
     * the same signature format as javadoc.) If there is more than one possibility,
     * list them all separated by commas.
     *
     *
     * For example, ProcessBuilder has a method named `redirectErrorStream()`
     * which sounds like it might redirect the error stream. It does not. It's just
     * a getter which returns whether the process builder will redirect the error stream,
     * and to actually set it, you must call `redirectErrorStream(boolean)`.
     * In that case, the method should be defined like this:
     * <pre>
     * &#64;CheckResult(suggest="#redirectErrorStream(boolean)")
     * public boolean redirectErrorStream() { ... }
    </pre> *
     */
    val suggest: String = ""
)


/***********************************************************
 ********************** NonNull
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS
)
annotation class SXNonNull


/***********************************************************
 ********************** Nullable
 **********************************************************/
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS
)
annotation class SXNullable